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

import java.util.ArrayList;
import java.util.List;

import jm.music.data.Note;
import jm.music.data.Phrase;

public class ExtendedPhrase extends Phrase {
    private static final long serialVersionUID = 7582636612785500769L;

    private Scale scale;
    private int measures;
    private int notes;
    private Contour contour;
    private int baseVelocity;
    private List<String> structure = new ArrayList<>();

    public Scale getScale() {
        return scale;
    }

    public void setScale(Scale scale) {
        this.scale = scale;
    }

    public int getMeasures() {
        return measures;
    }

    public void setMeasures(int measures) {
        this.measures = measures;
    }

    public Contour getContour() {
        return contour;
    }

    public void setContour(Contour contour) {
        this.contour = contour;
    }

    public int getNotes() {
        return notes;
    }

    public void setNotes(int themeNotes) {
        this.notes = themeNotes;
    }

    public int getBaseVelocity() {
        return baseVelocity;
    }

    public void setBaseVelocity(int baseVelocity) {
        this.baseVelocity = baseVelocity;
    }

    public List<String> getStructure() {
        return structure;
    }

    public void setStructure(List<String> structure) {
        this.structure = structure;
    }

    public ExtendedPhrase copy() {
        ExtendedPhrase newPhrase = new ExtendedPhrase();
        for (Note note : getNoteArray()) {
            newPhrase.add(note.copy());
        }
        newPhrase.setDenominator(getDenominator());
        newPhrase.setTitle(getTitle() + " copy");
        newPhrase.setInstrument(getInstrument());
        newPhrase.setAppend(getAppend());
        newPhrase.setPan(getPan());
        newPhrase.setLinkedPhrase(getLinkedPhrase());
        newPhrase.setMyPart(getMyPart());
        newPhrase.setTempo(getTempo());
        newPhrase.setNumerator(getNumerator());
        newPhrase.setDenominator(getDenominator());
        newPhrase.setScale(getScale());
        newPhrase.setContour(getContour());
        newPhrase.setMeasures(getMeasures());
        newPhrase.setNotes(getNotes());
        newPhrase.setBaseVelocity(getBaseVelocity());
        newPhrase.setStructure(getStructure());
        return newPhrase;
    }
}
