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

package com.music.model;

import jm.constants.Scales;

import org.apache.commons.lang3.text.WordUtils;

public enum Scale {
    MAJOR(Scales.MAJOR_SCALE),
    MINOR(Scales.MINOR_SCALE),
    HARMONIC_MINOR(Scales.HARMONIC_MINOR_SCALE),
    MELODIC_MINOR(Scales.MELODIC_MINOR_SCALE),
    NATURAL_MINOR(Scales.NATURAL_MINOR_SCALE),
    DORIAN(Scales.DORIAN_SCALE),
    LYDIAN(Scales.LYDIAN_SCALE),
    MIXOLYDIAN(Scales.MIXOLYDIAN_SCALE),
    //TODO Locrian and Phrygian
    TURKISH(Scales.TURKISH_SCALE),
    INDIAN(Scales.INDIAN_SCALE),
    BLUES(Scales.BLUES_SCALE),
    MAJOR_PENTATONIC(Scales.PENTATONIC_SCALE),
    MINOR_PENTATONIC(new int[] {0, 3, 5, 7, 10});

    private final int[] definition;

    private Scale(int[] definition) {
        this.definition = definition;
    }
    public int[] getDefinition() {
        return definition;
    }

    public String getDisplayName() {
        return WordUtils.capitalizeFully(name().replace("_", " "));
    }
}
