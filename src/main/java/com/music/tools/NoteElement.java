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

public class NoteElement {

    private double startBeatAbs;
    private double startMeasure;
    private double startBeat;
    private double noteLength;
    private int scaleDegree;
    private int octave;
    private boolean isRest;
    private boolean flat;
    private boolean sharp;

    public double getStartBeatAbs() {
        return startBeatAbs;
    }
    public void setStartBeatAbs(double startBeatAbs) {
        this.startBeatAbs = startBeatAbs;
    }
    public double getStartMeasure() {
        return startMeasure;
    }
    public void setStartMeasure(double startMeasure) {
        this.startMeasure = startMeasure;
    }
    public double getStartBeat() {
        return startBeat;
    }
    public void setStartBeat(double startBeat) {
        this.startBeat = startBeat;
    }
    public double getNoteLength() {
        return noteLength;
    }
    public void setNoteLength(double noteLength) {
        this.noteLength = noteLength;
    }
    public int getScaleDegree() {
        return scaleDegree;
    }
    public void setScaleDegree(int scaleDegree) {
        this.scaleDegree = scaleDegree;
    }
    public int getOctave() {
        return octave;
    }
    public void setOctave(int octave) {
        this.octave = octave;
    }
    public boolean isRest() {
        return isRest;
    }
    public void setRest(boolean isRest) {
        this.isRest = isRest;
    }

    public boolean isFlat() {
        return flat;
    }
    public void setFlat(boolean flat) {
        this.flat = flat;
    }
    public boolean isSharp() {
        return sharp;
    }
    public void setSharp(boolean sharp) {
        this.sharp = sharp;
    }
    @Override
    public String toString() {
        return "NoteElement [startBeatAbs=" + startBeatAbs + ", startMeasure=" + startMeasure
                + ", startBeat=" + startBeat + ", noteLength=" + noteLength + ", scaleDegree=" + scaleDegree
                + ", octave=" + octave + ", isRest=" + isRest + "]";
    }
}
