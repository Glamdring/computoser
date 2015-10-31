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

import static jm.constants.Durations.EIGHTH_NOTE;
import static jm.constants.Durations.SIXTEENTH_NOTE;
import static jm.constants.Durations.WHOLE_NOTE;
import static jm.constants.Pitches.A1;
import static jm.constants.Pitches.BF4;
import static jm.constants.Pitches.C4;
import static jm.constants.Pitches.CS4;
import static jm.constants.Pitches.DS4;
import static jm.constants.Pitches.F4;
import static jm.constants.Pitches.G4;
import static jm.constants.Pitches.GS4;

import java.io.FileOutputStream;

import jm.constants.Instruments;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Rest;
import jm.music.data.Score;
import jm.util.Play;
import jm.util.Write;

import com.music.model.SpecialNoteType;
import com.music.util.music.Chance;
import com.music.util.music.NoteFactory;

public class InstrumentTester {
    public static void main(String[] args) throws Exception {

        //BELLS, CRYSTAL, ATMOSPHERE, DROPS, STAR_THEME, SOUNDEFFECTS, BIRD, HELICOPTER, APPLAUSE, GUNSHOT, FRET_NOISE, TOM, SOUNDTRACK
        tiNaTinKano();
    }

    private static void tiNaTinKano() throws Exception {
        Score score = new Score();
        score.addPart(new Part(Instruments.PIANO));
        Phrase phrase = new Phrase();
        score.getPart(0).addPhrase(phrase);

        int[] pitches = new int[] {C4, C4, C4, F4, GS4, A1, G4, F4, F4, DS4, DS4, A1, CS4, CS4, CS4, F4, GS4, BF4, GS4, GS4, G4, G4};
        double[] lengths = new double[] {SIXTEENTH_NOTE, SIXTEENTH_NOTE, SIXTEENTH_NOTE, EIGHTH_NOTE, EIGHTH_NOTE,
                SIXTEENTH_NOTE,
                EIGHTH_NOTE, SIXTEENTH_NOTE, SIXTEENTH_NOTE, SIXTEENTH_NOTE, EIGHTH_NOTE, 
                SIXTEENTH_NOTE, 
                SIXTEENTH_NOTE, SIXTEENTH_NOTE, SIXTEENTH_NOTE, EIGHTH_NOTE, EIGHTH_NOTE, EIGHTH_NOTE, EIGHTH_NOTE, SIXTEENTH_NOTE, SIXTEENTH_NOTE, SIXTEENTH_NOTE};
        
        // off-beat
        phrase.addRest(new Rest(SIXTEENTH_NOTE));
        
        int idx = 0;
        for (Integer pitch : pitches) {
            double length = SIXTEENTH_NOTE;
            if (lengths.length > idx) {
                length = lengths[idx];
            }
            if (pitch != A1) {
                Note note = new Note(pitch, length);
                phrase.add(note);
            } else {
                phrase.addRest(new Rest(length));
            }
            idx++;
        }
       
        score.getPart(0).getPhrase(0).addRest(new Rest(SIXTEENTH_NOTE));
        score.getPart(0).addPhrase(new Phrase(phrase.getNoteArray()));
        
        Part accompaniment = new Part(Instruments.PIANO);
        //score.addPart(accompaniment);
        Phrase accPhrase = new Phrase();
        accompaniment.addPhrase(accPhrase);
        accPhrase.addRest(new Rest(SIXTEENTH_NOTE));
        accPhrase.addChord(new int[] {C4, F4, GS4}, WHOLE_NOTE - SIXTEENTH_NOTE);
        accPhrase.addChord(new int[] {C4, F4, GS4}, WHOLE_NOTE);
        Play.midi(score);
        Write.midi(score, new FileOutputStream("c:/tmp/tinatin.mid"));
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
