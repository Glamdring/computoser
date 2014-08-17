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

import jm.JMC;

import com.music.MainPartGenerator.MainPartContext;
import com.music.ScoreContext;
import com.music.util.music.Chance;
import com.music.util.music.ToneResolver;

public class ScaleTester {
    private static Random random = new Random();

    private static int sampleRate = 8000;

    private static Map<Double, long[]> fractionCache = new HashMap<>();

//    public static void main(String[] args) {
//        double[] scale = new double[]{261.63, 294.33375, 302.5096875, 335.2134375, 343.389375, 384.2690625, 392.445, 425.14875, 433.3246875, 490.55625, 506.908125};
//        double ff = 261.63;
//        //double[] scale = new double[] {261.63, ff * 9 / 8, ff * 5 / 4, ff * 4 / 3, ff * 3 / 2, ff * 5 / 3, ff * 15 / 8};
//        for (int i = 0; i < scale.length; i++) {
//            for (int k = 0; k < scale.length; k++) {
//                long[] fraction = findCommonFraction(scale[k] / scale[i]);
//                System.out.print(fraction[0] + "/" + fraction[1] + "; ");
//            }
//            System.out.println();
//        }
//    }
    public static void main(String[] args) {
        final AudioFormat af = new AudioFormat(sampleRate, 16, 1, true, true);
        try {
            double fundamentalFreq = 261.63;
            Set<Double> frequences = new HashSet<Double>();
            frequences.add(fundamentalFreq);
            int octaveMultiplier = 2;
            int harmonics = 27;
            // TODO exclude the 7th harmonic? And the 11th? Or not..
            for (int i = 3; i < 100; i++) {
                double actualFreq = fundamentalFreq * i;
                double closestTonicRatio = actualFreq / (fundamentalFreq * octaveMultiplier);
                if (closestTonicRatio < 1 || closestTonicRatio > 2) {
                    octaveMultiplier *= 2;
                }
                double closestTonic = actualFreq - actualFreq % (fundamentalFreq * octaveMultiplier);
                double normalizedFreq = fundamentalFreq * (actualFreq / closestTonic);

                System.out.println(actualFreq);
                System.out.println(normalizedFreq);
                System.out.println("_---");
                frequences.add(normalizedFreq);
                if (frequences.size() == harmonics) {
                    break;
                }
            }

            List<Double> chromaticScale = new ArrayList<>(frequences);
            Collections.sort(chromaticScale);

            int perfectIntervalIndex = 0;
            int idx = 0;
            for (Iterator<Double> it = chromaticScale.iterator(); it.hasNext();) {
                Double noteFreq = it.next();
                long[] fraction = findCommonFraction(noteFreq / fundamentalFreq);
                fractionCache.put(noteFreq, fraction);
                if (fraction[0] == 3 && fraction[1] == 2) {
                    perfectIntervalIndex = idx;
                }
                idx++;
                System.out.println(fraction);
            }
            System.out.println(chromaticScale);
            System.out.println(chromaticScale.size());

            Set<Double> scaleSet = new HashSet<Double>();
            scaleSet.add(chromaticScale.get(0));
            idx = perfectIntervalIndex;
            while (scaleSet.size() < 11) {
                scaleSet.add(chromaticScale.get(idx));
                idx += perfectIntervalIndex;
                idx = idx % chromaticScale.size();
                //System.out.println(scaleSet.size());
            }
            List<Double> scale = new ArrayList<Double>(scaleSet);
            Collections.sort(scale);

            System.out.println("Scale: " + scale);

            SourceDataLine line = AudioSystem.getSourceDataLine(af);
            line.open(af);
            line.start();
            //int[] scale = Scale.MAJOR.getDefinition();
            //int[] scale = {0, 4, 8, 10, 14, 18, 22};
            //int[] scale = {0, 2, 4, 6, 8, 9, 10, 12, 14, 16, 18, 20, 22, 23};
            int position = 0;
            double ratio = Math.pow(2, 1/24d);

            Random random = new Random();
            Double[] scaleFrequencies = scale.toArray(new Double[scale.size()]);

//            for (int i = 0; i < scaleFrequencies.length; i++) {
//                play(line, generateSineWavefreq(scaleFrequencies[i], 1));
//            }
            MainPartContext lCtx = new MainPartContext();
            lCtx.setDirectionUp(true);
            ScoreContext ctx = new ScoreContext();
            double[] melody = new double[30];
            double[] lengths = new double[30];
            for (int i = 0; i < 30; i ++) {
                position = getNextNotePitchIndex(ctx, lCtx, scaleFrequencies);
                double freq = scaleFrequencies[position];
                double length = getNoteLength(lCtx);
                melody[i] = freq;
                lengths[i] = length ;
            }

            for (int i = 0; i < melody.length; i++) {
                play(line, generateSineWavefreq(melody[i], lengths[i]));
            }

            line.drain();
            line.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] generateSineWavefreq(double frequencyOfSignal, double seconds) {
        // total samples = (duration in second) * (samples per second)
        byte[] sin = new byte[(int) (seconds * sampleRate)];
        double samplingInterval = (double) (sampleRate / frequencyOfSignal);
//        System.out.println("Frequency of Signal : " + frequencyOfSignal);
//        System.out.println("Sampling Interval   : " + samplingInterval);
        for (int i = 0; i < sin.length; i++) {
            double angle = (2.0 * Math.PI * i) / samplingInterval;
            sin[i] = (byte) (Math.sin(angle) * 127);
            // System.out.println("" + sin[i]);
        }
        return sin;
    }

    private static void play(SourceDataLine line, byte[] array) {
        int length = sampleRate * array.length / 1000;
        line.write(array, 0, array.length);
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
                divisor ++;
            }
        }

        return new long[] {numerator, denominator};
    }


    /**
     * Pieces copied from MainPartGenerator
     */
    private static final int[] PROGRESS_TYPE_PERCENTAGES = new int[] {25, 48, 25, 2};
    private static final int[] NOTE_LENGTH_PERCENTAGES = new int[] {10, 31, 40, 7, 9, 3};

    public static double getNoteLength(MainPartContext lCtx) {
        double length = 0;
        int lengthSpec = Chance.choose(NOTE_LENGTH_PERCENTAGES);

        // don't allow drastic changes in note length
        if (lCtx.getPreviousLength() != 0 && lCtx.getPreviousLength() < 1 && lengthSpec == 5) {
            length = 4;
        } else if (lCtx.getPreviousLength() != 0 && lCtx.getPreviousLength() >= 2 && lengthSpec == 0) {
            lengthSpec = 1;
        }

        if (lengthSpec == 0 && (lCtx.getSameLengthNoteSequenceCount() == 0 || lCtx.getSameLengthNoteType() == JMC.SIXTEENTH_NOTE)) {
            length = JMC.SIXTEENTH_NOTE;
        } else if (lengthSpec == 1 && (lCtx.getSameLengthNoteSequenceCount() == 0 || lCtx.getSameLengthNoteType() == JMC.EIGHTH_NOTE)) {
            length = JMC.EIGHTH_NOTE;
        } else if (lengthSpec == 2 && (lCtx.getSameLengthNoteSequenceCount() == 0 || lCtx.getSameLengthNoteType() == JMC.QUARTER_NOTE)) {
            length = JMC.QUARTER_NOTE;
        } else if (lengthSpec == 3 && (lCtx.getSameLengthNoteSequenceCount() == 0 || lCtx.getSameLengthNoteType() == JMC.DOTTED_QUARTER_NOTE)) {
            length = JMC.DOTTED_QUARTER_NOTE;
        } else if (lengthSpec == 4) {
            length = JMC.HALF_NOTE;
        } else if (lengthSpec == 5){
            length = JMC.WHOLE_NOTE;
        }

        // handle sequences of notes with the same length
        if (lCtx.getSameLengthNoteSequenceCount() == 0 && Chance.test(17) && length <= JMC.DOTTED_QUARTER_NOTE) {
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
            } else  if (shouldResolveToStableTone) {
                notePitchIndex = resolve(previousNotePitch, frequencies);
                if (lCtx.getPitches().size() > 1 && notePitchIndex == previousNotePitch) {
                    // in that case, make a step to break the repetition pattern
                    int pitchChange = getStepPitchChange(frequencies, lCtx.isDirectionUp(), previousNotePitch);
                    notePitchIndex = previousNotePitch + pitchChange;
                }
            } else {
                // try getting a pitch. if the pitch range is exceeded, get a
                // new consonant tone, in the opposite direction, different progress type and different interval
                int attempt = 0;

                // use a separate variable in order to allow change only for this particular note, and not for the direction of the melody
                boolean directionUp = lCtx.isDirectionUp();
                do {
                    int progressType = Chance.choose(PROGRESS_TYPE_PERCENTAGES);
                    // in some cases change the predefined direction (for this pitch only), for a more interesting melody
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
                    } else if (progressType == 0) { //unison
                        notePitchIndex = previousNotePitch;
                    } else { // 2 - intervals
                        // for a melodic sequence, use only a "jump" of up to 6 pitches in current direction
                        int change = 2 + random.nextInt(frequencies.length - 2);
                        notePitchIndex = (previousNotePitch + change) % frequencies.length;
                    }

                    if (attempt > 0) {
                        directionUp = !directionUp;
                    }
                    // if there are more than 3 failed attempts, simply assign a random in-scale, in-range pitch
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
                step = - step;
            } else {
                step = - step;
                step ++;
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
            return (int) -Math.signum(diff); //the opposite direction of the previous interval
        }
        return 0;
    }

    private static int getStepPitchChange(Double[] frequencies, boolean directionUp, int previousNotePitch) {
        int pitchChange = 0;

        int[] steps = new int[] {-1 , 1};
        if (directionUp) {
            steps = new int[] {1, -1,};
        }
        for (int i: steps) {
            // if the pitch is in the predefined direction and it is within the scale - use it.
            if (previousNotePitch + i < frequencies.length && previousNotePitch + i > 0) {
                pitchChange = i;
            }
            // in case no other matching tone is found that is common, the last appropriate one will be retained in "pitchChange"
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
