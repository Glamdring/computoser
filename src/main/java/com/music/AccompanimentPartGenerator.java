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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.music.model.Chord;
import com.music.model.ExtendedPhrase;
import com.music.model.InstrumentGroups;
import com.music.model.PartType;
import com.music.model.Scale;
import com.music.model.SpecialNoteType;
import com.music.model.ToneGroups;
import com.music.model.ToneType;
import com.music.model.prefs.UserPreferences;
import com.music.util.music.Chance;
import com.music.util.music.ChordUtils;
import com.music.util.music.NoteFactory;
import com.music.util.music.ToneResolver;

import jm.constants.Instruments;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Rest;
import jm.music.data.Score;
import jm.music.tools.Mod;

public class AccompanimentPartGenerator implements ScoreManipulator {
    private static final Logger logger = LoggerFactory.getLogger(AccompanimentPartGenerator.class);

    private Random random = new Random();

    @Override
    public void handleScore(Score score, ScoreContext ctx, UserPreferences prefs) {
        Part accompanimentPart = ctx.getParts().get(PartType.ACCOMPANIMENT);
        if (accompanimentPart == null) {
            return;
        }
        //TODO disable this part for pentatonic scales, or make it work

        Part mainPart = ctx.getParts().get(PartType.MAIN);
        double currentMeasureSize = 0;
        double normalizedMeasureSize = ctx.getNormalizedMeasureSize();
        SpecialNoteType specialNoteType = null;

        boolean preferChordNotesOffset = Chance.test(20);
        // small offset of all chords from the start of the measure
        // misaligning it slightly with the main part
        double measureOffset = Chance.test(5) ? 0.15 : 0;

        for (Phrase phrase : mainPart.getPhraseArray()) {
            Phrase accompanimentPhrase = new Phrase();
            accompanimentPhrase.setTitle("Accompaniment phrase");
            Scale currentScale = ((ExtendedPhrase) phrase).getScale();
            // get copies of the static ones, so that we can shuffle them without affecting the original
            List<Chord> scaleChords = new ArrayList<Chord>(ChordUtils.chords.get(currentScale));
            List<Chord> scaleSeventhChords = new ArrayList<Chord>(ChordUtils.seventhChords.get(currentScale));
            List<Chord> scaleOtherChords = new ArrayList<Chord>(ChordUtils.otherChords.get(currentScale));

            Note[] notes = phrase.getNoteArray();
            List<ToneType> firstToneTypes = new ArrayList<>();
            boolean interMeasureChord = false;
            double measureChordLength = 0;
            boolean canHaveInterMeasureChords = ctx.getMetre()[0] > 3 && ctx.getMetre()[0] % 4 == 0;

            Chord chord = null;
            for (int i = 0; i < notes.length; i++) {
                // shuffle every time, so that we don't always get the same chord for a given note
                Collections.shuffle(scaleChords, random);
                Collections.shuffle(scaleSeventhChords, random);
                Collections.shuffle(scaleOtherChords, random);

                Note currentNote = notes[i];
                if (currentNote.getRhythmValue() == 0) {
                    continue; // rhythm value is 0 for the first notes of a (main-part) chord. So progress to the next
                }
                // inter-measure chords only for even-numbered, compound metres
                if (canHaveInterMeasureChords && currentMeasureSize == 0) {
                    interMeasureChord = Chance.test(18);
                }
                double chordLength = interMeasureChord ? normalizedMeasureSize / 2 : normalizedMeasureSize;
                boolean isHalfMeasure = currentMeasureSize == normalizedMeasureSize / 2;

                if (currentNote.getPitch() == 0) {
                    logger.warn("Pitch is 0 in main part.");
                    continue;
                }
                if (!currentNote.isRest() && (currentMeasureSize == 0 || (interMeasureChord && isHalfMeasure))) {
                    boolean preferStable = ToneResolver.needsContrastingChord(firstToneTypes, ToneGroups.UNSTABLE);
                    boolean preferUnstable = ToneResolver.needsContrastingChord(firstToneTypes, ToneGroups.STABLE);
                    Chord previous = chord;
                    chord = ChordUtils.getChord(ctx, currentNote.getPitch(), previous, scaleChords, scaleSeventhChords, scaleOtherChords, preferStable, preferUnstable);
                    if (chord != null && Chance.test(90)) {
                        if (Chance.test(20)) { // change the special note type
                            if (Chance.test(60)) { // to a new value
                                specialNoteType = SpecialNoteType.values()[random.nextInt(SpecialNoteType.values().length)];
                            } else { // reset
                                specialNoteType = null;
                            }
                        }
                        firstToneTypes.add(chord.getFirstToneType());
                        int[] chordPitches = chord.getPitches();
                        logger.debug(Arrays.toString(chordPitches) + " : " + currentNote.getPitch());

                        postProcessPitches(accompanimentPart, chordPitches);

                        int dynamics = InstrumentGroups.getInstrumentSpecificDynamics(65 + random.nextInt(20), accompanimentPart.getInstrument());
                        // in some cases repeat the chord
                        if (Chance.test(82)) {
                            if (Chance.test(75)) { //full chord
                                addChord(chordPitches, chordLength, dynamics, accompanimentPhrase,
                                        specialNoteType, preferChordNotesOffset, measureOffset);
                            } else { //partial chord + rest
                                // make the filling rest between 1/16 and 1/4
                                double restLength = 0.125 * Math.pow(2, random.nextInt(4));
                                if (restLength >= chordLength) {
                                    restLength = chordLength / 2;
                                }
                                addChord(chordPitches, chordLength - restLength, dynamics,
                                        accompanimentPhrase, specialNoteType, preferChordNotesOffset,
                                        measureOffset);
                                accompanimentPhrase.addRest(new Rest(restLength));
                            }
                        } else {
                            addChord(chordPitches, chordLength / 2, dynamics, accompanimentPhrase,
                                    specialNoteType, preferChordNotesOffset, measureOffset);
                            addChord(chordPitches, chordLength / 2, dynamics, accompanimentPhrase,
                                    specialNoteType, preferChordNotesOffset, measureOffset);
                        }
                    } else {
                        accompanimentPhrase.addRest(new Rest(chordLength));
                    }

                    measureChordLength += chordLength;
                }

                if (currentNote.isRest() && (currentMeasureSize == 0 || (interMeasureChord && isHalfMeasure))) {
                    accompanimentPhrase.addRest(new Rest(chordLength));
                    measureChordLength += chordLength;
                }

                currentMeasureSize += currentNote.getRhythmValue();
                if (currentMeasureSize >= normalizedMeasureSize) {
                    // when there's a long note and so no inter-measure chord is possible, fill the measure with a rest
                    if (measureChordLength != currentMeasureSize) {
                        double fillingSize = normalizedMeasureSize - measureChordLength;
                        accompanimentPhrase.addRest(new Rest(fillingSize));
                    }
                    currentMeasureSize = 0;
                    measureChordLength = 0;
                }
            }
            accompanimentPart.add(accompanimentPhrase);
        }
        //Mod.transpose(phrase, -12); // an octave lower;

        // transpose to the desired key
        Mod.transpose(accompanimentPart, ctx.getKeyNote());
    }

    private void postProcessPitches(Part accompanimentPart, int[] chordPitches) {
        // lower tones for string ensemble
        if (accompanimentPart.getInstrument() == Instruments.STRING_ENSEMBLE_1) {
            for (int j = 0; j < chordPitches.length; j++) {
                chordPitches[j] = chordPitches[j] - 12;
            }
        }
    }

    private void addChord(int[] chordPitches, double chordLength, int dynamics, Phrase phrase,
            SpecialNoteType noteType, boolean preferOffset, double measureOffset) {

        // in some cases add each subsequent note with a slight offset
        double offset = preferOffset && Chance.test(30) || Chance.test(5) ? 0.1 + random.nextInt(20) / 100d : 0;

        // all but the first note are set with length = 0, so that they all sound as a chord
        for (int i = 1; i < chordPitches.length; ++i) {
            Note localNote = NoteFactory.createNote(chordPitches[i], 0.0d);
            localNote.setOffset(measureOffset + i * offset);
            if (noteType == null) {
                localNote.setDuration(chordLength * 0.9d);
            } else {
                localNote.setDuration(chordLength * noteType.getValue());
            }
            localNote.setDynamic(dynamics);
            phrase.addNote(localNote);
        }
        // the first note is added with the right length
        Note note = NoteFactory.createNote(chordPitches[0], chordLength);
        note.setDynamic(dynamics);
        note.setOffset(measureOffset);
        phrase.addNote(note);

        // Add a supplementary octave chord in a lower octave
        if (Chance.test(10)) {
            Note startNote = NoteFactory.createNote(chordPitches[0] - 12, 0.0D);
            startNote.setDuration(chordLength * 0.9D);
            phrase.addNote(startNote);
            phrase.addNote(chordPitches[0] - 24, chordLength);
        }
    }
}
