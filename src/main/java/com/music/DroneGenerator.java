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
import java.util.List;
import java.util.Random;

import com.music.model.ExtendedPhrase;
import com.music.model.InstrumentGroups;
import com.music.model.PartType;
import com.music.model.Scale;
import com.music.model.prefs.UserPreferences;
import com.music.util.music.Chance;
import com.music.util.music.NoteFactory;

import jm.constants.Pitches;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Score;

public class DroneGenerator implements ScoreManipulator {
    private final Random random = new Random();

    @Override
    public void handleScore(Score score, ScoreContext ctx, UserPreferences prefs) {
        Part dronePart = ctx.getParts().get(PartType.DRONE);
        if (dronePart == null) {
            return;
        }
        Note[] droneNotes = null;
        Part mainPart = ctx.getParts().get(PartType.MAIN);
        Phrase[] phrases = mainPart.getPhraseArray();
        for (Phrase phrase : phrases) {
            Phrase dronePhrase = new Phrase();
            if (!(phrase instanceof ExtendedPhrase) || Chance.test(17)) { // skip the drone for some phrases
                continue;
            }

            ExtendedPhrase ePhrase = (ExtendedPhrase) phrase;

            // change the drone type
            if (droneNotes == null || ePhrase.getScale() == ctx.getAlternativeScale() || Chance.test(10)) {
                droneNotes = getDroneNotes(ePhrase.getScale(), ctx.getKeyNote(), ctx.getNormalizedMeasureSize(), ePhrase.getMeasures(), dronePart.getInstrument());
            }

            // if one note is held the entire time;
            if (droneNotes.length == 1) {
                dronePhrase.addNote(droneNotes[0]);
            } else {
                for (int i = 0; i < ePhrase.getMeasures(); i++) {
                    dronePhrase.addNoteList(droneNotes);
                }
            }
            dronePart.add(dronePhrase);
        }
    }

    private Note[] getDroneNotes(Scale scale, int keyNote, double normalizedMeasureSize, int measures, int instrument) {
        boolean singleNote = Chance.test(30);
        boolean hold = Chance.test(20);
        int notesPerMeasure = (int) Math.pow(2, 0 + random.nextInt(4));
        List<Note> notes = new ArrayList<>();
        double noteLength = normalizedMeasureSize / notesPerMeasure;
        if (singleNote && hold) {
            Note note = NoteFactory.createNote(Pitches.C5 + keyNote, normalizedMeasureSize * measures);
            note.setDynamic(InstrumentGroups.getInstrumentSpecificDynamics(55, instrument));
            notes.add(note);
        } else if (singleNote) {
            for (int i = 0; i < notesPerMeasure; i++) {
                Note note = NoteFactory.createNote(Pitches.C5 + keyNote, noteLength);
                note.setDynamic(InstrumentGroups.getInstrumentSpecificDynamics(55, instrument));
                notes.add(note);
            }
        } else {
            int pitch = Pitches.C5 + keyNote;
            for (int i = 0; i < notesPerMeasure; i++) {
                Note note = NoteFactory.createNote(pitch, noteLength);
                note.setDynamic(InstrumentGroups.getInstrumentSpecificDynamics(50, instrument));
                notes.add(note);
                boolean directionUp = random.nextBoolean();
                int step = random.nextInt(3);
                int toneDegree = Arrays.binarySearch(scale.getDefinition(), pitch % 12);
                if (toneDegree > 0) {
                    pitch = Pitches.C5 + keyNote + (directionUp ? 1 : -1) * scale.getDefinition()[(toneDegree + step) % 7];
                } else {
                    pitch = Pitches.c5 + keyNote;
                }
            }
        }

        return notes.toArray(new Note[notes.size()]);
    }

}
