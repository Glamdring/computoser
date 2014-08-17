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

/**
 * A class used for both chord definitions ("pitches" holds the degrees) and actual chords ("pitches" holds the actual chord definition)
 * @author bozho
 *
 */
public class Chord {

    private int[] pitches;
    private ToneType firstToneType;
    private ChordType chordType;

    public int[] getPitches() {
        return pitches;
    }
    public void setPitches(int[] pitches) {
        this.pitches = pitches;
    }
    public ToneType getFirstToneType() {
        return firstToneType;
    }
    public void setFirstToneType(ToneType type) {
        this.firstToneType = type;
    }
    public ChordType getChordType() {
        return chordType;
    }
    public void setChordType(ChordType chordType) {
        this.chordType = chordType;
    }

    public static Chord createDefinition(ToneType toneType, ChordType type) {
        Chord chord = new Chord();
        chord.setFirstToneType(toneType);
        chord.setChordType(type);
        return chord;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((chordType == null) ? 0 : chordType.hashCode());
        result = prime * result + ((firstToneType == null) ? 0 : firstToneType.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Chord other = (Chord) obj;
        if (chordType != other.chordType)
            return false;
        if (firstToneType != other.firstToneType)
            return false;
        return true;
    }
}
