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

import com.music.model.SpecialNoteType;
import com.music.util.music.Chance;
import com.music.util.music.NoteFactory;

import jm.JMC;
import jm.constants.Instruments;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Rest;
import jm.music.data.Score;
import jm.util.Play;

public class InstrumentTester {
    public static void main(String[] args) {

        //BELLS, CRYSTAL, ATMOSPHERE, DROPS, STAR_THEME, SOUNDEFFECTS, BIRD, HELICOPTER, APPLAUSE, GUNSHOT, FRET_NOISE, TOM, SOUNDTRACK

        Score score = new Score();
        score.addPart(new Part(Instruments.CLARINET));
        Phrase phrase = new Phrase();
        score.getPart(0).addPhrase(phrase);

//        addChord(new int[] {60, 63, 65}, 1d, 90, phrase, null, true);
        for (int i = 50; i < 70; i++) {
            Note note = new Note(i, JMC.EIGHTH_NOTE);
            phrase.add(note);
        }

        Play.midi(score);
    }

    private static void addChord(int[] chordPitches, double chordLength, int dynamics, Phrase phrase, SpecialNoteType noteType, boolean preferOffset) {

        // in some cases add each subsequent note with a slight offset
        double offset = 0.4;

        System.out.println("OFF: " + offset);
        // all but the first note are set with length = 0, so that they all sound as a chord
        for (int i = 1; i < chordPitches.length; ++i) {
            Note localNote = NoteFactory.createNote(chordPitches[i], 0.0d);
            localNote.setOffset(i * offset);
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
