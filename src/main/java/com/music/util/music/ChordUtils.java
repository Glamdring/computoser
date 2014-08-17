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

package com.music.util.music;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import jm.constants.Chords;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.music.ScoreContext;
import com.music.model.Chord;
import com.music.model.ChordType;
import com.music.model.Scale;
import com.music.model.ToneGroups;
import com.music.model.ToneType;

public class ChordUtils {
    private static final Logger logger = LoggerFactory.getLogger(ChordUtils.class);

    public static final Map<Scale, List<Chord>> chords = new HashMap<Scale, List<Chord>>();
    public static final Map<Scale, List<Chord>> seventhChords = new HashMap<Scale, List<Chord>>();
    public static final Map<Scale, List<Chord>> otherChords = new HashMap<Scale, List<Chord>>();

    public static final Random random = new Random();

    static {
        initializeChords();
    }

    public static Chord getChord(ScoreContext ctx, int pitch, Chord previousChord, List<Chord> chords,
            List<Chord> alternativeChords, List<Chord> otherChords, boolean preferStable,
            boolean preferUnstable) {
        //TODO check http://mugglinworks.com/chordmaps/mapC.htm

        // first transpose to C-scale, as the pre-initialized chords are there
        pitch = pitch - ctx.getKeyNote();
        boolean alternative = Chance.test(13);
        boolean other = !alternative && Chance.test(7);
        boolean invert = Chance.test(15);

        List<Chord> listToIterate = chords;
        if (alternative && alternativeChords != null) {
            listToIterate = alternativeChords;
        } else if (other && otherChords != null) {
            listToIterate = otherChords;
        }
        int note = pitch % 12;
        boolean trimMiddleNote = Chance.test(5);
        List<Chord> eligibleChords = new ArrayList<>();
        List<Chord> alternativeEligibleChords = new ArrayList<>();
        for (Chord chordDef : listToIterate) {
            int[] chordDefPitches = chordDef.getPitches();
            int currentNoteIdx = Arrays.binarySearch(chordDefPitches, note);
            // if the note in the main part is found in the chord, mark as
            // eligible. In 2% of the cases, allow unharmonizing chords (the
            // unharmonic chord may not ultimately be selected, hence the high
            // percentage)
            if (currentNoteIdx > -1 || Chance.test(2)) {
                int[] chord = new int[chordDefPitches.length];
                if (currentNoteIdx < 0) {
                    currentNoteIdx = 0;
                }
                chord[currentNoteIdx] = pitch;
                // transpose back to the current key (+ctx.getKeyNote())
                int root = pitch - chordDefPitches[currentNoteIdx] + chordDefPitches[0] + ctx.getKeyNote();
                for (int i = 0; i < chordDefPitches.length; i++) {
                    chord[i] = root + (chordDefPitches[i] - chordDefPitches[0]) + ctx.getKeyNote();
                }
                Chord eligibleChord = new Chord();
                eligibleChord.setPitches(chord);
                eligibleChord.setFirstToneType(chordDef.getFirstToneType());
                eligibleChord.setChordType(chordDef.getChordType());

                if (invert) {
                    // sometimes invert all but the root, other times - only the final note(s)
                    for (int i = (Chance.test(50) ? 1 : 2); i < eligibleChord.getPitches().length; i ++) {
                        eligibleChord.getPitches()[i] = eligibleChord.getPitches()[i] - 12;
                    }
                }

                if (trimMiddleNote) {
                    ArrayUtils.remove(eligibleChord.getPitches(), 1);
                }

                // if the current chord doesn't match the preferences, store it as a temp result and continue
                if (preferStable && !ToneGroups.STABLE.getToneTypes().contains(eligibleChord.getFirstToneType())) {
                    alternativeEligibleChords.add(eligibleChord);
                    continue;
                }

                if (preferUnstable && !ToneGroups.UNSTABLE.getToneTypes().contains(eligibleChord.getFirstToneType())) {
                    alternativeEligibleChords.add(eligibleChord);
                    continue;
                }
                eligibleChords.add(eligibleChord);
            }
        }
        for (Iterator<Chord> it = eligibleChords.iterator(); it.hasNext();) {
            Chord chord = it.next();
            if (isDisallowedInProgression(chord, previousChord) && Chance.test(95)) {
                it.remove();
                if (alternativeEligibleChords.isEmpty()) {
                    alternativeEligibleChords.add(chord);
                }
            }
        }

        Chord result = null;
        if (!eligibleChords.isEmpty()) {
            result = eligibleChords.get(random.nextInt(eligibleChords.size()));
        } else  if (eligibleChords.isEmpty() && !alternativeEligibleChords.isEmpty()) { // if no suitable chord is found that matches the preferences, but there's one that's otherwise suitable, return it
            result = alternativeEligibleChords.get(random.nextInt(alternativeEligibleChords.size()));
        }
        if (result == null) {
            logger.debug("Failed to find chord for " + pitch + " in scale " + ctx.getScale());
            return null;
        }

        return result;
    }

    // some forbidden progressions based on experience
    public static Map<Chord, List<Chord>> disallowedProgressions = new HashMap<>();
    static {
        Chord ab = Chord.createDefinition(ToneType.SUBMEDIANT, ChordType.MINOR);
        Chord am = Chord.createDefinition(ToneType.SUBMEDIANT, ChordType.MAJOR);
        Chord bb = Chord.createDefinition(ToneType.LEADING_OR_SUBTONIC, ChordType.MINOR);
        Chord c = Chord.createDefinition(ToneType.TONIC, ChordType.MAJOR);
        Chord dm = Chord.createDefinition(ToneType.SUPERTONIC, ChordType.MAJOR);
        Chord em = Chord.createDefinition(ToneType.MEDIANT, ChordType.MAJOR);
        Chord eb = Chord.createDefinition(ToneType.MEDIANT, ChordType.MINOR);
        Chord f = Chord.createDefinition(ToneType.SUBDOMINANT, ChordType.MAJOR);
        Chord g = Chord.createDefinition(ToneType.DOMINANT, ChordType.MAJOR);

        disallowedProgressions.put(am, Arrays.asList(f));
        disallowedProgressions.put(ab, Arrays.asList(am, c, dm, em, f, g));
        disallowedProgressions.put(bb, Arrays.asList(c, dm, em));
        disallowedProgressions.put(c, Arrays.asList(dm, am));
        disallowedProgressions.put(dm, Arrays.asList(ab, c, em, eb, f, g));
        disallowedProgressions.put(em, Arrays.asList(ab, bb, dm, eb, g));
        disallowedProgressions.put(eb, Arrays.asList(dm, em, f, g));
        disallowedProgressions.put(f, Arrays.asList(ab, bb, dm, em, eb));
    }

    private static boolean isDisallowedInProgression(Chord chord, Chord previousChord) {
        if (previousChord == null) {
            return false;
        }
        List<Chord> disallowed = disallowedProgressions.get(previousChord);
        if (disallowed == null) {
            return false;
        }
        return disallowed.contains(chord);
    }

    private static void initializeChords() {
        for (Scale scale : Scale.values()) {
            List<Chord> scaleChords = new ArrayList<Chord>();
            List<Chord> scaleSeventhChords = new ArrayList<Chord>();
            List<Chord> scaleOtherChords = new ArrayList<Chord>();
            int[][] chordDefs = new int[][] {Chords.MAJOR, Chords.MINOR, Chords.DIMINISHED, Chords.AUGMENTED};
            for (int note : scale.getDefinition()) {
                int chordType = -1;
                int[] chordDef = null;
                int[] seventhChordDef = null;
                int[] otherChordDef = null;
                for (int i = 0; i < chordDefs.length; i ++) {
                    if (isScaleChord(chordDefs[i], scale, note)) {
                        chordType = i;
                        if (chordType == 0) {
                            chordDef = Chords.MAJOR;
                            seventhChordDef = Chords.MAJOR_SEVENTH;
                            otherChordDef = Chords.SIXTH;
                        }
                        if (chordType == 1) {
                            chordDef = Chords.MINOR;
                            seventhChordDef = Chords.MINOR_SEVENTH;
                            otherChordDef = Chords.MINOR_SIXTH;
                        }
                        if (chordType == 2) {
                            chordDef = Chords.DIMINISHED;
                            seventhChordDef = Chords.DIMINISHED_SEVENTH;
                            otherChordDef = Chords.DIMINISHED; // nothing specific - use default
                        }
                        if (chordType == 3) {
                            chordDef = Chords.AUGMENTED;
                            seventhChordDef = Chords.SEVENTH_SHARP_FIFTH;
                            otherChordDef = Chords.AUGMENTED; // nothing specific - use default
                        }

                        if (chordDef != null) { // chords in some scales (e.g. Turkish) may not be classified in the above 4 groups. Skip those
                            ToneType firstToneType = ToneType.forDegree(Arrays.binarySearch(scale.getDefinition(), note));
                            Chord chord = getChordDef(note, chordDef);
                            chord.setChordType(ChordType.values()[chordType]);
                            chord.setFirstToneType(firstToneType);
                            scaleChords.add(chord);

                            Chord seventhChord = getChordDef(note, seventhChordDef);
                            seventhChord.setFirstToneType(firstToneType);
                            chord.setChordType(ChordType.values()[chordType]);
                            scaleSeventhChords.add(seventhChord);

                            Chord otherChord = getChordDef(note, otherChordDef);
                            otherChord.setFirstToneType(firstToneType);
                            chord.setChordType(ChordType.values()[chordType]);
                            scaleOtherChords.add(otherChord);
                        }
                    }
                }
            }
            chords.put(scale, scaleChords);
            seventhChords.put(scale, scaleSeventhChords);
            otherChords.put(scale, scaleOtherChords);
        }
    }

    private static Chord getChordDef(int note, int[] chordDef) {
        int[] chordPitches = new int[chordDef.length + 1];
        chordPitches[0] = note;
        for (int k = 0; k < chordDef.length; k++) {
            chordPitches[k + 1] = chordPitches[0] + chordDef[k];
        }
        Chord chord = new Chord();
        chord.setPitches(chordPitches);
        return chord;
    }

    // checks if all notes in the chord will be notes from the scale
    private static boolean isScaleChord(int[] chordType, Scale scale, int root) {
        int[] supposedChord = new int[3];
        fillChord(supposedChord, chordType, root);
        for (int note : supposedChord) {
            if (Arrays.binarySearch(scale.getDefinition(), note) < 0) {
                return false;
            }
        }
        return true;
    }

    private static void fillChord(int[] supposedChord, int[] chordType, int root) {
        supposedChord[0] = root;
        for (int i = 0; i < chordType.length; i++) {
            supposedChord[i + 1] = (root + chordType[i]) % 12;
        }
    }
}
