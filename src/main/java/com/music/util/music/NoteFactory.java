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

import jm.JMC;
import jm.music.data.Note;

/**
 * Needed to intercept note creation, due to the System.exit(..) in the note constructor
 * @author bozho
 *
 */
public class NoteFactory {

    public static Note createNote(int pitch, double length) {
        validatePitch(pitch);
        return new Note(pitch, length);
    }


    public static Note createNote(int pitch, double length, int dynamics) {
        validatePitch(pitch);
        return new Note(pitch, length, dynamics);
    }

    private static void validatePitch(int pitch) {
        if (pitch < 0 && pitch != JMC.REST) {
            throw new IllegalArgumentException("Pitch cannot be less than zero, but " + pitch + " is passed");
        }
    }
}
