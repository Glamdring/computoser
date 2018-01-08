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
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;

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

import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Rest;
import jm.music.data.Score;

public class Arpeggiator implements ScoreManipulator {
    private Random random = new Random();

    @Override
    public void handleScore(Score score, ScoreContext ctx, UserPreferences prefs) {
        Part part = ctx.getParts().get(PartType.ARPEGGIO);
        if (part == null) {
            return;
        }
        Part mainPart = ctx.getParts().get(PartType.MAIN);
        double currentMeasureSize = 0;
        double normalizedMeasureSize = ctx.getNormalizedMeasureSize();
        SpecialNoteType specialNoteType = null;
        for (Phrase phrase : mainPart.getPhraseArray()) {
            if (Chance.test(20)) { // change the special note type
                if (Chance.test(60)) { // to a new value
                    specialNoteType = SpecialNoteType.values()[random.nextInt(SpecialNoteType.values().length)];
                } else { // reset
                    specialNoteType = null;
                }
            }

            Phrase arpeggioPhrase = new Phrase();
            arpeggioPhrase.setTitle("Arpeggio phrase");
            Scale currentScale = ((ExtendedPhrase) phrase).getScale();
            // get copies of the static ones, so that we can shuffle them without affecting the original
            List<Chord> scaleChords = new ArrayList<Chord>(ChordUtils.chords.get(currentScale));
            Collections.shuffle(scaleChords, random);
            Note[] notes = phrase.getNoteArray();
            List<ToneType> firstToneTypes = new ArrayList<>();

            int measures = 0;
            Note[] currentNotes = null;
            boolean useTwoNoteChords = Chance.test(14);
            Chord chord = null;
            for (int i = 0; i < notes.length; i++) {
                Note currentNote = notes[i];
                if (currentNote.getRhythmValue() == 0) {
                    continue; // rhythm value is 0 for the first notes of a (main-part) chord. So progress to the next
                }
                boolean lastMeasure = measures == ctx.getMeasures() - 1;
                if (currentMeasureSize == 0 && !currentNote.isRest() && !lastMeasure) {
                    boolean preferStable = ToneResolver.needsContrastingChord(firstToneTypes, ToneGroups.UNSTABLE);
                    boolean preferUnstable = ToneResolver.needsContrastingChord(firstToneTypes, ToneGroups.STABLE);
                    // change the chord only in 1/4 of the cases
                    if (currentNotes == null || Chance.test(25)) {
                        // no alternatives for now - only 3-note chords
                        Chord previous = chord;
                        chord = ChordUtils.getChord(ctx, currentNote.getPitch(), previous, scaleChords, scaleChords, scaleChords, preferStable, preferUnstable);
                        if (chord != null) {
                            int[] pitches = chord.getPitches();
                            //remove the middle note in some cases (but make it possible to have three-note chords in a generally two-note phrase)
                            if (pitches.length == 3 && useTwoNoteChords && Chance.test(90)) {
                                pitches = ArrayUtils.remove(pitches, 1);
                            }
                            int count = Chance.test(90) ? (Chance.test(80) ? 4 : 2) : (Chance.test(80) ? 3 : 5);

                            currentNotes = new Note[count];
                            double length = normalizedMeasureSize / count;
                            for (int k = 0; k < count; k++) {
                                Note note = NoteFactory.createNote(pitches[random.nextInt(pitches.length)], length);
                                note.setDynamic(InstrumentGroups.getInstrumentSpecificDynamics(65 + random.nextInt(10), part.getInstrument()));
                                if (specialNoteType != null) {
                                    note.setDuration(note.getRhythmValue() * specialNoteType.getValue());
                                }
                                currentNotes[k] = note;
                            }

                        }
                    }
                    if (Chance.test(85) && currentNotes != null) {
                        for (Note note : currentNotes) {
                            arpeggioPhrase.addNote(note);
                        }
                    } else {
                        arpeggioPhrase.addRest(new Rest(normalizedMeasureSize));
                    }
                } else if (currentMeasureSize == 0 && (currentNote.isRest() || lastMeasure)) {
                    arpeggioPhrase.addRest(new Rest(normalizedMeasureSize));
                }

                currentMeasureSize += currentNote.getRhythmValue();
                if (currentMeasureSize >= normalizedMeasureSize) {
                    currentMeasureSize = 0;
                    measures++;
                }
            }
            part.add(arpeggioPhrase);
        }

    }
}
