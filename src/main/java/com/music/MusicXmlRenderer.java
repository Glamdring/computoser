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

import static jm.constants.Durations.DOTTED_QUARTER_NOTE;
import static jm.constants.Durations.EIGHTH_NOTE;
import static jm.constants.Durations.HALF_NOTE;
import static jm.constants.Durations.QUARTER_NOTE;
import static jm.constants.Durations.SIXTEENTH_NOTE;
import static jm.constants.Durations.THIRTYSECOND_NOTE;
import static jm.constants.Durations.WHOLE_NOTE;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLOutputFactory;

import jm.constants.ProgramChanges;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Score;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.music.model.SpecialNoteType;
import com.music.xmlmodel.Measure;
import com.music.xmlmodel.NoteElement;
import com.music.xmlmodel.Pitch;
import com.music.xmlmodel.RestElement;
import com.music.xmlmodel.ScorePart;
import com.music.xmlmodel.ScorePartDefinition;
import com.music.xmlmodel.ScorePartwise;

public class MusicXmlRenderer {

    private static final Logger logger = LoggerFactory.getLogger(MusicXmlRenderer.class);
    private static Marshaller marshaller = null;
    static {
        try {
            marshaller = JAXBContext.newInstance(ScorePartwise.class).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "utf-8");
            marshaller.setProperty("com.sun.xml.bind.xmlHeaders", "<!DOCTYPE score-partwise PUBLIC \"-//Recordare//DTD MusicXML 3.0 Partwise//EN\" \"http://www.musicxml.org/dtds/partwise.dtd\">\n");
        } catch (JAXBException e) {
            logger.warn("Problem intializing xml renderer", e);
        } catch (Exception ex) {
            logger.warn("Unexpected problem intializing xml renderer", ex);
        }
    }

    @SuppressWarnings("unused")
    private static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();

    private static final String[] NOTES = { "C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B" };
    public static Map<Integer, String> instrumentNames = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static void render(Score score, OutputStream out) {

        double normalizedMeasureSize = 1d * score.getNumerator() * 4 / score.getDenominator();

        ScorePartwise root = new ScorePartwise();
        root.setMovementTitle(score.getTitle());
        int idx = 1;
        for (Part part : score.getPartArray()) {
            ScorePartDefinition def = new ScorePartDefinition();
            def.setPartName(instrumentNames.get(part.getInstrument()));
            def.setId("P" + idx);
            root.getPartDefinitionList().add(def);

            ScorePart xmlPart = new ScorePart();
            xmlPart.setId(def.getId());
            double currentMeasureSize = 0;
            List<Note> partNotes = new ArrayList<Note>();
            for (Phrase phrase : part.getPhraseArray()) {
                partNotes.addAll(phrase.getNoteList());
            }
            Iterator<Note> noteIterator = partNotes.iterator();
            int measureNumber = 1;
            Measure currentMeasure = createMeasure(score, measureNumber++);
            while (noteIterator.hasNext()) {
                Note note = noteIterator.next();
                NoteElement xmlNote = new NoteElement();
                int length = (int) (getClosestLength(note.getRhythmValue()) * 10000);
                switch(length) {
                case (int) (THIRTYSECOND_NOTE * 10000): xmlNote.setType("32nd"); break;
                case (int) (SIXTEENTH_NOTE * 10000): xmlNote.setType("16th"); break;
                case (int) (EIGHTH_NOTE * 10000): xmlNote.setType("eighth"); break;
                case (int) (QUARTER_NOTE * 10000): xmlNote.setType("quarter"); break;
                case (int) (DOTTED_QUARTER_NOTE * 10000): xmlNote.setType("quarter"); xmlNote.setDot(""); break;
                case (int) (HALF_NOTE * 10000): xmlNote.setType("half"); break;
                case (int) (WHOLE_NOTE * 10000): xmlNote.setType("whole"); break;
                default: xmlNote.setType("/" + (length / 10000d));
                }
                xmlNote.setDuration((int) (note.getRhythmValue() * 2));
                xmlNote.setVoice(idx);
                if (!note.isRest()) {
                    xmlNote.setPitch(new Pitch());
                    int pitch = note.getPitch() % 12;
                    String sPitch = NOTES[pitch];
                    int octave = note.getPitch() / 12 - 1;
                    xmlNote.getPitch().setOctave(octave);
                    if (sPitch.length() > 1) {
                        xmlNote.getPitch().setAlter((byte) (sPitch.contains("#") ? 1 : -1));
                        sPitch = sPitch.substring(0, 1);
                    }
                    xmlNote.getPitch().setStep(sPitch);
                } else {
                    xmlNote.setRest(new RestElement());
                }

                currentMeasure.getNotes().add(xmlNote);
                currentMeasureSize += note.getRhythmValue();
                if (currentMeasureSize >= normalizedMeasureSize) {
                    currentMeasureSize = 0;
                    xmlPart.getMeasures().add(currentMeasure);
                    currentMeasure = createMeasure(score, measureNumber++);
                }
            }
            root.getPartList().add(xmlPart);
            idx++;
        }

        try {
            //XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(out, (String) marshaller.getProperty(Marshaller.JAXB_ENCODING));
            //xmlStreamWriter.writeStartDocument((String) marshaller.getProperty(Marshaller.JAXB_ENCODING), "1.0");
            marshaller.marshal(root, out);
            //xmlStreamWriter.writeEndDocument();
            //xmlStreamWriter.close();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static final double[] LENGTHS = new double[] {WHOLE_NOTE, HALF_NOTE, DOTTED_QUARTER_NOTE, QUARTER_NOTE, EIGHTH_NOTE, SIXTEENTH_NOTE, THIRTYSECOND_NOTE};

    private static double getClosestLength(double length) {
        if (length == 0) {
            return 0;
        }
        Map<Double, Double> proximity = new HashMap<>();
        for (double baseLength : LENGTHS) {
            if (baseLength == length) {
                return length;
            }
            for (SpecialNoteType type : SpecialNoteType.values()) {
                if (baseLength * type.getValue() == length) {
                    return baseLength;
                }
            }
            if (length < baseLength && length * 1.2 >= baseLength) {
                return baseLength;
            }
            if (length > baseLength && length * 0.8 <= baseLength) {
                return baseLength;
            }
            proximity.put(baseLength, length > baseLength ? length / baseLength : baseLength / length);
        }
        return ImmutableSortedMap.copyOf(proximity, Ordering.natural().onResultOf(Functions.forMap(proximity))).firstKey();
    }

    static {
        Field[] fields = ProgramChanges.class.getDeclaredFields();
        try {
            for (Field field : fields) {
                Integer value = (Integer) field.get(null);
                if (!instrumentNames.containsKey(value)) {
                    instrumentNames.put(value, StringUtils.capitalize(field.getName().toLowerCase()).replace('_', ' '));
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Measure createMeasure(Score score, int number) {
        Measure measure = new Measure();
        measure.setNumber(number);
        measure.getAttributes().getTime().setBeats(score.getNumerator());
        measure.getAttributes().getTime().setBeatType(score.getDenominator());
        return measure;
    }
}
