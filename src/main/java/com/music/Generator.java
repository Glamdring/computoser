/*
 * Computoser is a music-composition algorithm and a website to present the results
 * Copyright (C) 2012-2014  Bozhidar Bozhanov
 *
 * Computoser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Computoser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Computoser.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.music;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import javax.annotation.PostConstruct;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.swing.JFrame;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import com.google.common.collect.Maps;
import com.music.model.ExtendedPhrase;
import com.music.model.PartType;
import com.music.model.Scale;
import com.music.model.prefs.Tempo;
import com.music.model.prefs.UserPreferences;
import com.music.tools.SongChart.GraphicsPanel;
import com.music.util.MutingPrintStream;
import com.music.util.music.SMFTools;
import com.music.util.music.ToneResolver;
import com.music.web.util.StartupListener;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncodingAttributes;
import jm.midi.MidiSynth;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Score;
import jm.music.tools.PhraseAnalysis;
import jm.util.Play;
import jm.util.Write;

@Service
public class Generator {
    private static final Logger logger = LoggerFactory.getLogger(Generator.class);

    @Value("${music.config.location}")
    private String configLocation;
    @Value("${max.concurrent.generations}")
    private int maxConcurrentGenerations;

    public Semaphore semaphore;

    private static List<Soundbank> soundbanks = new ArrayList<>();

    private List<ScoreManipulator> manipulators = new ArrayList<>();
    private Encoder encoder = new Encoder();

    @PostConstruct
    public void init() throws MidiUnavailableException, IOException, InvalidMidiDataException {
        //TODO http://marsyas.info/ when input signal processing is needed

        semaphore = new Semaphore(maxConcurrentGenerations);

        manipulators.add(new MetreConfigurer());
        manipulators.add(new PartConfigurer());
        manipulators.add(new ScaleConfigurer());
        manipulators.add(new MainPartGenerator());
        manipulators.add(new AccompanimentPartGenerator());
        manipulators.add(new Arpeggiator());
        manipulators.add(new PercussionGenerator());
        manipulators.add(new SimpleBeatGenerator());
        manipulators.add(new BassPartGenerator());
        manipulators.add(new DroneGenerator());
        manipulators.add(new EffectsGenerator());
        manipulators.add(new PadsGenerator());
        manipulators.add(new TimpaniPartGenerator());
        manipulators.add(new TitleGenerator());

        try {
            Collection<File> files = FileUtils.listFiles(new File(configLocation + "/soundbanks/"), new String[] {"sf2"}, false);
            for (File file : files) {
                InputStream is = new BufferedInputStream(new FileInputStream(file));
                soundbanks.add(MidiSystem.getSoundbank(is));
            }
        } catch (IOException | IllegalArgumentException ex) {
            logger.warn("Problem loading soundbank: " + ex.getMessage());
            // ignore
        }

        //initJMusicSynthesizer();
    }

    public ScoreContext generatePiece() {
        return generatePiece(null);
    }
    
    public ScoreContext generatePiece(UserPreferences prefs) {
        Score score = new Score();
        final ScoreContext ctx = new ScoreContext();

        ctx.setScore(score);
        for (ScoreManipulator manipulator : manipulators) {
            manipulator.handleScore(score, ctx, prefs);
        }
        verifyResult(ctx);
        return ctx;
    }

    private void verifyResult(ScoreContext ctx) {
        Part[] parts = ctx.getScore().getPartArray();
        Map<String, Double> lengths = Maps.newHashMap();
        double currentLength = 0;
        for (Part part : parts) {
            if (part.getTitle().equals(PartType.DRONE.getTitle())) {
                continue;
            }
            int partMeasures = 0;
            Phrase[] phrases = part.getPhraseArray();
            boolean hasChords = part.getTitle().equals(PartType.ACCOMPANIMENT.getTitle());
            double noteLength = 0;
            double restLength = 0;
            for (Phrase phrase : phrases) {
                double previousStartTime = -1;
                double currentMeasureSize = 0;
                Note[] notes = phrase.getNoteArray();
                int measures = 0;
                int outOfScaleNotes = 0;
                for (Note note : notes) {
                    if (phrase instanceof ExtendedPhrase && !ToneResolver.isInScale(((ExtendedPhrase) phrase).getScale().getDefinition(), note.getPitch())) {
                        outOfScaleNotes++;
                    }
                    if (note.isRest()) {
                        restLength += note.getRhythmValue();
                    } else {
                        noteLength += note.getRhythmValue();
                    }
                    if (note.getSampleStartTime() != previousStartTime || note.getSampleStartTime() == 0) {
                        currentLength += note.getRhythmValue();
                        previousStartTime = note.getSampleStartTime();
                    }
                    currentMeasureSize += note.getRhythmValue();
                    if (currentMeasureSize > ctx.getNormalizedMeasureSize() && !ignorePartMeasureVerification(part)) {
                        logger.warn(part.getTitle() + " of " + ctx.getScore().getTitle() + " has unbalanced measures");
                    }
                    if (currentMeasureSize >= ctx.getNormalizedMeasureSize()) {
                        currentMeasureSize = 0;
                        measures++;
                    }
                }
                if (phrase instanceof ExtendedPhrase) {
                    int actualMeasures = ((ExtendedPhrase) phrase).getMeasures();
                    if (actualMeasures != measures) {
                        logger.warn("Discrepancy in calculated measures for phrase " + phrase.getTitle() + ". Actual are " + measures + " but stored " + actualMeasures);
                    }
                }
                if (outOfScaleNotes > 1) {
                    logger.warn("Out of scale notes for part " + part.getTitle() + ": " + outOfScaleNotes);
                }
                partMeasures += measures;
            }
            // not exact, but a good approximation
            if (hasChords) {
                currentLength = noteLength / 3 + restLength;
            }
            lengths.put(part.getTitle(), currentLength);
            currentLength = 0;
            if (partMeasures != ctx.getMeasures() && partMeasures > 0) {
                logger.warn("Discrepancy in calculated measures for Part " + part.getTitle() + ". Actual " + partMeasures + " but stored " + ctx.getMeasures());
            }
        }

        logger.debug("Part lengths: " + lengths);
    }

    private boolean ignorePartMeasureVerification(Part part) {
        String title = part.getTitle();
        return title.equals(PartType.ACCOMPANIMENT.getTitle()) || title.equals(PartType.PAD1.getTitle()) || title.equals(PartType.PAD2.getTitle());
    }

    public byte[] toMp3(byte[] midi) throws Exception {
        return toMp3(midi, null);
    }
    
    public byte[] toMp3(byte[] midi, String wavPath) throws Exception {
        //allowing a maximum number users to generate tracks at the same time so that the system remains stable (midi->wav->mp3 is heavy)
        semaphore.acquire();
        try {
            File wav;
            if (wavPath == null) {
                wav = File.createTempFile("gen", ".wav");
            } else {
                wav = new File(wavPath + "/gen.wav");
            }
            
            long start = System.currentTimeMillis();
            try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(wav))) {
                Midi2WavRenderer.midi2wav(new ByteArrayInputStream(midi), fos);
                IOUtils.write(midi, fos);
            }
            logger.info("midi2wav conversion took: " + (System.currentTimeMillis() - start) + " millis");
            start = System.currentTimeMillis();
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setFormat("mp3");
            AudioAttributes audio = new AudioAttributes();
//            audio.setBitRate(36000);
//            audio.setSamplingRate(20000);
            attrs.setAudioAttributes(audio);
            attrs.setThreads(1);
            File mp3 = File.createTempFile("gen", ".mp3");
            encoder.encode(wav, mp3, attrs);
            logger.info("wav2mp3 conversion took: " + (System.currentTimeMillis() - start) + " millis");
            if (wavPath == null) {
                wav.delete(); //cleanup the big wav file
            }
            byte[] mp3Bytes = FileUtils.readFileToByteArray(mp3);
            mp3.delete();
            return mp3Bytes;
        } finally {
            semaphore.release();
        }
    }

    public static void main1(String[] args) throws Exception {
        // testing soundbanks
        Generator generator = new Generator();
        generator.configLocation = "c:/config/music";
        generator.maxConcurrentGenerations = 5;
        generator.init();
        byte[] midi = FileUtils.readFileToByteArray(new File("c:/tmp/classical.midi"));
        byte[] mp3 = generator.toMp3(midi);
        FileUtils.writeByteArrayToFile(new File("c:/tmp/aa" + System.currentTimeMillis() + ".mp3"), mp3);
    }

    public static void mainMusicXml(String[] args) throws Exception {
        InputStream is = new FileInputStream("C:\\Users\\bozho\\Downloads\\7743.midi");
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        Score score = new Score();
        SMFTools localSMF = new SMFTools();
        localSMF.read(is);
        SMFTools.SMFToScore(score, localSMF);
        score.setTitle("foo");
        score.setNumerator(12);
        score.setDenominator(16);
        MusicXmlRenderer.render(score, result);
        System.out.println(new String(result.toByteArray()));
        is.close();

    }
    
    public static void main(String[] args) throws Exception {

        Options options = new Options();
        
        Option opt = new Option("out", true, "Path for output");
        opt.setRequired(true);
        options.addOption(opt);
        
        options.addOption("config", true, "Path for directory that contains a /soundbank/ dir");
        options.addOption("visualize", "Whether to visualize");
        options.addOption("play", "Whether play the generated piece");
        options.addOption("printstats", "Whether to print stats");
        options.addOption("measures", true, "Number of measures in the piece");
        options.addOption("scale", true, "The musical scale, one of: " + Arrays.toString(Scale.values()));
        options.addOption("tempo", true, "The tempo, one of: " + Arrays.toString(Tempo.values()));
        
        CommandLineParser parser = new DefaultParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException ex) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "java -jar computoser.jar", options);
            System.exit(0);
        }
        
        String output = cl.getOptionValue("out");
        boolean visualize = false;
        if (cl.hasOption("visualize")) {
            visualize = Boolean.parseBoolean(cl.getOptionValue("visualize"));
        }
        boolean printStats = false;
        if (cl.hasOption("printstats")) {
            visualize = Boolean.parseBoolean(cl.getOptionValue("printstats"));
        }
        
        boolean play = false;
        if (cl.hasOption("play")) {
            visualize = Boolean.parseBoolean(cl.getOptionValue("play"));
        }
        
        UserPreferences prefs = new UserPreferences();
        if (cl.hasOption("measures")) {
            prefs.setMeasures(Integer.parseInt(cl.getOptionValue("measures")));
        }
        if (cl.hasOption("scale")) {
            prefs.setScale(Scale.valueOf(cl.getOptionValue("scale")));
        }
        if (cl.hasOption("tempo")) {
            prefs.setTempo(Tempo.valueOf(cl.getOptionValue("tempo")));
        }
        
        System.setOut(new MutingPrintStream(new ByteArrayOutputStream(), System.out));
        
        Generator generator = new Generator();
        generator.configLocation = cl.getOptionValue("config");
        generator.maxConcurrentGenerations = 5;
        generator.init();

        final ScoreContext ctx = generator.generatePiece(prefs);
        Score score = ctx.getScore();
        for (Part part : score.getPartArray()) {
            System.out.println(part.getTitle() + ": " + part.getInstrument());
        }

        System.out.println("Metre: " + ctx.getMetre()[0] + "/" + ctx.getMetre()[1]);
        //System.out.println(ctx);
        Write.midi(score, output + "/gen.midi");

        if (visualize) {
            new Thread(new Runnable() {
    
                @Override
                public void run() {
                    JFrame frame = new JFrame();
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setBounds(0, 0, 500, 500);
                    frame.setVisible(true);
                    Part part = ctx.getParts().get(PartType.MAIN);
                    Note[] notes = part.getPhrase(0).getNoteArray();
                    List<Integer> pitches = new ArrayList<Integer>();
                    for (Note note : notes) {
                        if (!note.isRest()) {
                            pitches.add(note.getPitch());
                        }
                    }
                    GraphicsPanel gp = new GraphicsPanel(pitches);
                    frame.setContentPane(gp);
                }
            }).start();
        }
        
        if (printStats) {
            DecimalFormat df = new DecimalFormat("#.##");
    
            for (Part part : score.getPartArray()) {
                StringBuilder sb = new StringBuilder();
                printStatistics(ctx, part);
                System.out.println("------------ " + part.getTitle() + "-----------------");
                Phrase[] phrases = part.getPhraseArray();
                for (Phrase phr : phrases) {
                    if (phr instanceof ExtendedPhrase) {
                        sb.append("Contour=" + ((ExtendedPhrase) phr).getContour() + " ");
                    }
                    double measureSize = 0;
                    int measures = 0;
                    double totalLength = 0;
                    List<String> pitches = new ArrayList<String>();
                    List<String> lengths = new ArrayList<String>();
                    System.out.println("((Phrase notes: " + phr.getNoteArray().length + ")");
                    for (Note note : phr.getNoteArray()) {
                        if (!note.isRest()) {
                            int degree = 0;
                            if (phr instanceof ExtendedPhrase) {
                                degree = Arrays.binarySearch(((ExtendedPhrase) phr).getScale().getDefinition(), note.getPitch() % 12);
                            }
                            pitches.add(String.valueOf(note.getPitch() + " (" + degree + ") "));
                        } else {
                            pitches.add(" R ");
                        }
                        lengths.add(df.format(note.getRhythmValue()));
                        measureSize += note.getRhythmValue();
                        totalLength += note.getRhythmValue();;
                        if (measureSize >= ctx.getNormalizedMeasureSize()) {
                            pitches.add(" || ");
                            lengths.add(" || " + (measureSize > ctx.getNormalizedMeasureSize() ? "!" : ""));
                            measureSize = 0;
                            measures++;
                        }
                    }
                    sb.append(pitches.toString() + "\r\n");
                    sb.append(lengths.toString() + "\r\n");
                    if (part.getTitle().equals(PartType.MAIN.getTitle())) {
                        sb.append("\r\n");
                    }
                    System.out.println("Phrase measures: " + measures);
                    System.out.println("Phrase length: " + totalLength);
                }
                System.out.println(sb.toString());
            }
        }
        
        MutingPrintStream.ignore.set(true);
        Write.midi(score, output + "/gen.midi");
        MutingPrintStream.ignore.set(null);
        if (play) {
            Play.midi(score);
        }

        byte[] mp3 = generator.toMp3(FileUtils.readFileToByteArray(new File(output + "/gen.midi")), output);
        
        FileUtils.writeByteArrayToFile(new File(output + "/gen.mp3"), mp3);
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private static void printStatistics(ScoreContext ctx, Part part) {
        if (false) {
            Hashtable<String, Object> table = PhraseAnalysis.getAllStatistics(part.getPhrase(0), 1, 0, ctx.getScale().getDefinition());
            for (Entry<String, Object> entry : table.entrySet()) {
                System.out.println(entry.getKey() + "=" + entry.getValue());
            }
        }
    }

    @SuppressWarnings("unused")
    private static void initJMusicSynthesizer() {
        try {
            Field fld = ReflectionUtils.findField(Play.class, "ms");
            ReflectionUtils.makeAccessible(fld);
            MidiSynth synth = (MidiSynth) fld.get(null);
            // playing for the first time initializes the synthesizer
            try {
                synth.play(null);
            } catch (Exception ex){};
            Field synthField = ReflectionUtils.findField(MidiSynth.class, "m_synth");
            ReflectionUtils.makeAccessible(synthField);
            Synthesizer synthsizer = (Synthesizer) synthField.get(synth);
            loadSoundbankInstruments(synthsizer);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

    }

    public static void mainConvert(String[] args) throws Exception {
        Generator gen = new Generator();
        gen.maxConcurrentGenerations = 5;
        gen.configLocation = "c:/config/music";
        gen.init();
        byte[] bytes = FileUtils.readFileToByteArray(new File("c:/tmp/183.midi"));
        byte[] mp3 = gen.toMp3(bytes);
        FileUtils.writeByteArrayToFile(new File("c:/tmp/183.mp3"), mp3);
    }

    public static void loadSoundbankInstruments(Synthesizer synthesizer) {
        for (Soundbank soundbank : soundbanks) {
            synthesizer.loadAllInstruments(soundbank);
        }
    }
}