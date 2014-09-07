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

package com.music.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import org.apache.commons.lang.ArrayUtils;

import jm.JMC;

import com.music.MainPartGenerator.MainPartContext;
import com.music.ScoreContext;
import com.music.util.music.Chance;
import com.music.util.music.ToneResolver;

public class ScaleTester {
    private static final int SCALE_SIZE = 7;
    private static final boolean USE_ET = true;
    private static final int CHROMATIC_SCALE_SILZE = 12;
    private static final double FUNDAMENTAL_FREQUENCY = 262.626;
    // chromatic-to-scale ratio: (12/7) 1,7142857142857142857142857142857

    private static Random random = new Random();
    private static int sampleRate = 8000;
    private static Map<Double, long[]> fractionCache = new HashMap<>();
    private static double fundamentalFreq = 0;

    public static void main(String[] args) {
        System.out
                .println("Usage: java ScaleTester <fundamental frequency> <chromatic scale size> <scale size> <use ET>");
        final AudioFormat af = new AudioFormat(sampleRate, 16, 1, true, true);
        try {
            fundamentalFreq = getArgument(args, 0, FUNDAMENTAL_FREQUENCY, Double.class);
            int pitchesInChromaticScale = getArgument(args, 1, CHROMATIC_SCALE_SILZE, Integer.class);

            List<Double> harmonicFrequencies = new ArrayList<>();
            List<String> ratios = new ArrayList<>();
            Set<Double> frequencies = new HashSet<Double>();
            frequencies.add(fundamentalFreq);
            int octaveMultiplier = 2;
            for (int i = 2; i < 100; i++) {
                // Exclude the 7th harmonic TODO exclude the 11th as well?
                // http://www.phy.mtu.edu/~suits/badnote.html
                if (i % 7 == 0) {
                    continue;
                }
                double actualFreq = fundamentalFreq * i;
                double closestTonicRatio = actualFreq / (fundamentalFreq * octaveMultiplier);
                if (closestTonicRatio < 1 || closestTonicRatio > 2) {
                    octaveMultiplier *= 2;
                }
                double closestTonic = actualFreq - actualFreq % (fundamentalFreq * octaveMultiplier);
                double normalizedFreq = fundamentalFreq * (actualFreq / closestTonic);

                harmonicFrequencies.add(actualFreq);
                frequencies.add(normalizedFreq);
                if (frequencies.size() == pitchesInChromaticScale) {
                    break;
                }
            }

            System.out.println("Harmonic (overtone) frequencies: " + harmonicFrequencies);
            System.out.println("Transposed harmonic frequencies: " + frequencies);

            List<Double> chromaticScale = new ArrayList<>(frequencies);
            Collections.sort(chromaticScale);

            // find the "perfect" interval (e.g. perfect fifth)
            int perfectIntervalIndex = 0;
            int idx = 0;
            for (Iterator<Double> it = chromaticScale.iterator(); it.hasNext();) {
                Double noteFreq = it.next();
                long[] fraction = findCommonFraction(noteFreq / fundamentalFreq);
                fractionCache.put(noteFreq, fraction);
                if (fraction[0] == 3 && fraction[1] == 2) {
                    perfectIntervalIndex = idx;
                    System.out.println("Perfect interval (3/2) idx: " + perfectIntervalIndex);
                }
                idx++;
                ratios.add(Arrays.toString(fraction));
            }
            System.out.println("Ratios to fundemental frequency: " + ratios);

            if (getBooleanArgument(args, 4, USE_ET)) {
                chromaticScale = temper(chromaticScale);
            }

            System.out.println();
            System.out.println("Chromatic scale: " + chromaticScale);

            Set<Double> scaleSet = new HashSet<Double>();
            scaleSet.add(chromaticScale.get(0));
            idx = 0;
            List<Double> orderedInCircle = new ArrayList<>();
            // now go around the circle of perfect intervals and put the notes
            // in order
            while (orderedInCircle.size() < chromaticScale.size()) {
                orderedInCircle.add(chromaticScale.get(idx));
                idx += perfectIntervalIndex;
                idx = idx % chromaticScale.size();
            }
            System.out.println("Pitches Ordered in circle of perfect intervals: " + orderedInCircle);

            List<Double> scale = new ArrayList<Double>(scaleSet);
            int currentIdxInCircle = orderedInCircle.size() - 1; // start with
                                                                 // the last
                                                                 // note in the
                                                                 // circle
            int scaleSize = getArgument(args, 3, SCALE_SIZE, Integer.class);
            while (scale.size() < scaleSize) {
                double pitch = orderedInCircle.get(currentIdxInCircle % orderedInCircle.size());
                if (!scale.contains(pitch)) {
                    scale.add(pitch);
                }
                currentIdxInCircle++;
            }
            Collections.sort(scale);

            System.out.println("Scale: " + scale);

            SourceDataLine line = AudioSystem.getSourceDataLine(af);
            line.open(af);
            line.start();

            Double[] scaleFrequencies = scale.toArray(new Double[scale.size()]);

            // first play the whole scale
            WaveMelodyGenerator.playScale(line, scaleFrequencies);
            // then generate a random melody in the scale
            WaveMelodyGenerator.playMelody(line, scaleFrequencies);

            line.drain();
            line.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean getBooleanArgument(String[] args, int i, boolean defaultValue) {
        if (args.length > i) {
            return Boolean.parseBoolean(args[i]);
        } else {
            return defaultValue;
        }
    }

    private static <T extends Number> T getArgument(String[] args, int i, T defaultValue, Class<T> resultClass) {
        if (args.length > i) {
            return resultClass.cast(Double.parseDouble(args[i]));
        } else {
            return defaultValue;
        }
    }

    private static List<Double> temper(List<Double> chromaticScale) {
        System.out.println("Before temper: " + chromaticScale);
        Double currentNote = chromaticScale.get(0);
        List<Double> result = new ArrayList<Double>();
        result.add(currentNote);
        double ratio = Math.pow(2, 1d / chromaticScale.size());
        for (int i = 1; i < chromaticScale.size(); i++) {
            currentNote = currentNote * ratio;
            currentNote = ((int) (currentNote * 1000)) / 1000d;
            result.add(currentNote);
            // Fill the fractions cache with the new values:
            long[] fraction = findCommonFraction(currentNote / fundamentalFreq);
            fractionCache.put(currentNote, fraction);
        }
        return result;
    }

    public static long[] findCommonFraction(double decimal) {
        long multiplier = 100000000l;
        long numerator = (int) (decimal * multiplier);
        long denominator = multiplier;

        long[] result = simplify(numerator, denominator);
        return result;
    }

    private static long[] simplify(long numerator, long denominator) {
        int divisor = 2;
        long maxDivisor = Math.min(numerator, denominator) / 2;
        while (divisor < maxDivisor) {
            if (numerator % divisor == 0 && denominator % divisor == 0) {
                numerator = numerator / divisor;
                denominator = denominator / divisor;
            } else {
                divisor++;
            }
        }

        return new long[] { numerator, denominator };
    }

    /**
     * Low-level sound wave handling
     *
     */
    public static class WavePlayer {

        public static void playNotes(SourceDataLine line, double[] frequencies) {
            for (int i = 0; i < frequencies.length; i++) {
                playNote(line, frequencies[i]);
            }
        }

        public static void playNotes(SourceDataLine line, Double[] frequencies) {
            playNotes(line, ArrayUtils.toPrimitive(frequencies));
        }

        public static void playNote(SourceDataLine line, double frequency) {
            play(line, generateSineWavefreq(frequency, 1));
        }

        private static void play(SourceDataLine line, byte[] array) {
            int length = sampleRate * array.length / 1000;
            line.write(array, 0, array.length);
        }

        private static byte[] generateSineWavefreq(double frequencyOfSignal, double seconds) {
            byte[] sin = new byte[(int) (seconds * sampleRate)];
            double samplingInterval = (double) (sampleRate / frequencyOfSignal);
            for (int i = 0; i < sin.length; i++) {
                double angle = (2.0 * Math.PI * i) / samplingInterval;
                sin[i] = (byte) (Math.sin(angle) * 127);
            }
            return sin;
        }
    }

    /**
     * Simple class that generates and plays a melody in a given scale
     *
     */
    public static class WaveMelodyGenerator {

        private static void playMelody(SourceDataLine line, Double[] scaleFrequencies) {
            int position;
            MainPartContext lCtx = new MainPartContext();
            lCtx.setDirectionUp(true);
            ScoreContext ctx = new ScoreContext();
            double[] melody = new double[30];
            double[] lengths = new double[30];
            for (int i = 0; i < 30; i++) {
                position = getNextNotePitchIndex(ctx, lCtx, scaleFrequencies);
                double freq = scaleFrequencies[position];
                double length = getNoteLength(lCtx);
                melody[i] = freq;
                lengths[i] = length;
            }

            WavePlayer.playNotes(line, melody);
        }

        private static void playScale(SourceDataLine line, Double[] scaleFrequencies) {
            WavePlayer.playNotes(line, scaleFrequencies);
            WavePlayer.playNote(line, scaleFrequencies[0] * 2);
        }


        /**
         * Pieces copied from MainPartGenerator
         */
        private static final int[] PROGRESS_TYPE_PERCENTAGES = new int[] { 25, 48, 25, 2 };
        private static final int[] NOTE_LENGTH_PERCENTAGES = new int[] { 10, 31, 40, 7, 9, 3 };

        public static double getNoteLength(MainPartContext lCtx) {
            double length = 0;
            int lengthSpec = Chance.choose(NOTE_LENGTH_PERCENTAGES);

            // don't allow drastic changes in note length
            if (lCtx.getPreviousLength() != 0 && lCtx.getPreviousLength() < 1 && lengthSpec == 5) {
                length = 4;
            } else if (lCtx.getPreviousLength() != 0 && lCtx.getPreviousLength() >= 2 && lengthSpec == 0) {
                lengthSpec = 1;
            }

            if (lengthSpec == 0
                    && (lCtx.getSameLengthNoteSequenceCount() == 0 || lCtx.getSameLengthNoteType() == JMC.SIXTEENTH_NOTE)) {
                length = JMC.SIXTEENTH_NOTE;
            } else if (lengthSpec == 1
                    && (lCtx.getSameLengthNoteSequenceCount() == 0 || lCtx.getSameLengthNoteType() == JMC.EIGHTH_NOTE)) {
                length = JMC.EIGHTH_NOTE;
            } else if (lengthSpec == 2
                    && (lCtx.getSameLengthNoteSequenceCount() == 0 || lCtx.getSameLengthNoteType() == JMC.QUARTER_NOTE)) {
                length = JMC.QUARTER_NOTE;
            } else if (lengthSpec == 3
                    && (lCtx.getSameLengthNoteSequenceCount() == 0 || lCtx.getSameLengthNoteType() == JMC.DOTTED_QUARTER_NOTE)) {
                length = JMC.DOTTED_QUARTER_NOTE;
            } else if (lengthSpec == 4) {
                length = JMC.HALF_NOTE;
            } else if (lengthSpec == 5) {
                length = JMC.WHOLE_NOTE;
            }

            // handle sequences of notes with the same length
            if (lCtx.getSameLengthNoteSequenceCount() == 0 && Chance.test(17)
                    && length <= JMC.DOTTED_QUARTER_NOTE) {
                lCtx.setSameLengthNoteSequenceCount(3 + random.nextInt(7));
                lCtx.setSameLengthNoteType(length);
            }
            if (lCtx.getSameLengthNoteSequenceCount() > 0) {
                lCtx.setSameLengthNoteSequenceCount(lCtx.getSameLengthNoteSequenceCount() - 1);
            }

            return length;
        }

        private static int getNextNotePitchIndex(ScoreContext ctx, MainPartContext lCtx, Double[] frequencies) {
            int notePitchIndex;
            if (lCtx.getPitches().isEmpty()) {
                // avoid excessively high and low notes.
                notePitchIndex = 0;
                lCtx.getPitchRange()[0] = 0;
                lCtx.getPitchRange()[1] = frequencies.length;
            } else {
                int previousNotePitch = lCtx.getPitches().get(lCtx.getPitches().size() - 1);
                boolean shouldResolveToStableTone = shouldResolveToStableTone(lCtx.getPitches(), frequencies);

                if (!lCtx.getCurrentChordInMelody().isEmpty()) {
                    notePitchIndex = lCtx.getCurrentChordInMelody().get(0);
                    lCtx.getCurrentChordInMelody().remove(0);
                } else if (shouldResolveToStableTone) {
                    notePitchIndex = resolve(previousNotePitch, frequencies);
                    if (lCtx.getPitches().size() > 1 && notePitchIndex == previousNotePitch) {
                        // in that case, make a step to break the repetition
                        // pattern
                        int pitchChange = getStepPitchChange(frequencies, lCtx.isDirectionUp(),
                                previousNotePitch);
                        notePitchIndex = previousNotePitch + pitchChange;
                    }
                } else {
                    // try getting a pitch. if the pitch range is exceeded, get
                    // a
                    // new consonant tone, in the opposite direction, different
                    // progress type and different interval
                    int attempt = 0;

                    // use a separate variable in order to allow change only for
                    // this particular note, and not for the direction of the
                    // melody
                    boolean directionUp = lCtx.isDirectionUp();
                    do {
                        int progressType = Chance.choose(PROGRESS_TYPE_PERCENTAGES);
                        // in some cases change the predefined direction (for
                        // this pitch only), for a more interesting melody
                        if ((progressType == 1 || progressType == 2) && Chance.test(15)) {
                            directionUp = !directionUp;
                        }

                        // always follow big jumps with a step back
                        int needsStepBack = needsStepBack(lCtx.getPitches());
                        if (needsStepBack != 0) {
                            progressType = 1;
                            directionUp = needsStepBack == 1;
                        }
                        if (progressType == 1) { // step
                            int pitchChange = getStepPitchChange(frequencies, directionUp, previousNotePitch);
                            notePitchIndex = previousNotePitch + pitchChange;
                        } else if (progressType == 0) { // unison
                            notePitchIndex = previousNotePitch;
                        } else { // 2 - intervals
                            // for a melodic sequence, use only a "jump" of up
                            // to 6 pitches in current direction
                            int change = 2 + random.nextInt(frequencies.length - 2);
                            notePitchIndex = (previousNotePitch + change) % frequencies.length;
                        }

                        if (attempt > 0) {
                            directionUp = !directionUp;
                        }
                        // if there are more than 3 failed attempts, simply
                        // assign a random in-scale, in-range pitch
                        if (attempt > 3) {
                            int start = lCtx.getPitchRange()[1] - random.nextInt(6);
                            for (int i = start; i > lCtx.getPitchRange()[0]; i--) {
                                if (Arrays.binarySearch(lCtx.getCurrentScale().getDefinition(), i % 12) > -1) {
                                    notePitchIndex = i;
                                }
                            }
                        }
                        attempt++;
                    } while (!ToneResolver.isInRange(notePitchIndex, lCtx.getPitchRange()));
                }
            }
            lCtx.getPitches().add(notePitchIndex);
            return notePitchIndex;
        }

        private static int resolve(int previousNotePitch, Double[] frequencies) {
            int idx = previousNotePitch + 1;
            int step = 1;
            while (idx >= 0 && idx < frequencies.length) {
                if (fractionCache.get(frequencies[idx])[0] <= 9) {
                    return idx;
                }
                if (step > 0) {
                    step = -step;
                } else {
                    step = -step;
                    step++;
                }
                idx += step;
                idx = idx % frequencies.length;
            }
            return 0;
        }

        private static int needsStepBack(List<Integer> pitches) {
            if (pitches.size() < 2) {
                return 0;
            }
            int previous = pitches.get(pitches.size() - 1);
            int prePrevious = pitches.get(pitches.size() - 2);

            int diff = previous - prePrevious;
            if (Math.abs(diff) > 6) {
                return (int) -Math.signum(diff); // the opposite direction of
                                                 // the previous interval
            }
            return 0;
        }

        private static int getStepPitchChange(Double[] frequencies, boolean directionUp, int previousNotePitch) {
            int pitchChange = 0;

            int[] steps = new int[] { -1, 1 };
            if (directionUp) {
                steps = new int[] { 1, -1, };
            }
            for (int i : steps) {
                // if the pitch is in the predefined direction and it is within
                // the scale - use it.
                if (previousNotePitch + i < frequencies.length && previousNotePitch + i > 0) {
                    pitchChange = i;
                }
                // in case no other matching tone is found that is common, the
                // last appropriate one will be retained in "pitchChange"
            }
            return pitchChange;
        }

        private static boolean shouldResolveToStableTone(List<Integer> pitches, Double[] frequencies) {
            // if the previous two pitches are unstable
            int previousNotePitch = pitches.get(pitches.size() - 1);
            int prePreviousNotePitch = 0;
            if (pitches.size() >= 2) {
                prePreviousNotePitch = pitches.get(pitches.size() - 2);
            }
            long[] previousRatio = fractionCache.get(frequencies[previousNotePitch]);
            long[] prePreviousRatio = fractionCache.get(frequencies[prePreviousNotePitch]);
            if (prePreviousNotePitch != 0 && previousRatio[0] > 9 && prePreviousRatio[0] > 9) {
                return true;
            }
            return false;
        }
    }
}