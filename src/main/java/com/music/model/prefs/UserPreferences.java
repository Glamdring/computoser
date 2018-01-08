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

package com.music.model.prefs;

import com.music.model.Scale;

public class UserPreferences {
    // set of instruments

    private boolean classical;
    private Ternary accompaniment = Ternary.OPTIONAL;
    private Ternary drums = Ternary.OPTIONAL;
    private Tempo tempo = Tempo.ANY;
    private Ternary electronic = Ternary.OPTIONAL;
    private Ternary dramatic = Ternary.OPTIONAL;
    private Ternary simplePhrases = Ternary.OPTIONAL;
    private Mood mood = Mood.ANY;
    private Scale scale = null;
    private Variation variation = Variation.ANY;
    private boolean preferDissonance;
    private int instrument = -1;
    private int measures;
    
    public boolean isClassical() {
        return classical;
    }

    public void setClassical(boolean classical) {
        this.classical = classical;
    }

    public Tempo getTempo() {
        return tempo;
    }

    public void setTempo(Tempo tempo) {
        this.tempo = tempo;
    }

    public Ternary getAccompaniment() {
        return accompaniment;
    }

    public void setAccompaniment(Ternary accompaniment) {
        this.accompaniment = accompaniment;
    }

    public Ternary getDrums() {
        return drums;
    }

    public void setDrums(Ternary drums) {
        this.drums = drums;
    }

    public Mood getMood() {
        return mood;
    }

    public void setMood(Mood mood) {
        this.mood = mood;
    }

    public Ternary getElectronic() {
        return electronic;
    }

    public void setElectronic(Ternary electronic) {
        this.electronic = electronic;
    }

    public Ternary getDramatic() {
        return dramatic;
    }

    public void setDramatic(Ternary dramatic) {
        this.dramatic = dramatic;
    }

    public Ternary getSimplePhrases() {
        return simplePhrases;
    }

    public void setSimplePhrases(Ternary simpleMotif) {
        this.simplePhrases = simpleMotif;
    }

    public boolean isPreferDissonance() {
        return preferDissonance;
    }

    public void setPreferDissonance(boolean preferDissonance) {
        this.preferDissonance = preferDissonance;
    }

    public int getInstrument() {
        return instrument;
    }

    public void setInstrument(int instrument) {
        this.instrument = instrument;
    }

    public Variation getVariation() {
        return variation;
    }

    public void setVariation(Variation variation) {
        this.variation = variation;
    }

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

    /**
     * Returns whether there has been a change in preferences
     * @return
     */
    public boolean isDefault() {
        return !classical && accompaniment == Ternary.OPTIONAL
                && drums == Ternary.OPTIONAL && mood == Mood.ANY
                && simplePhrases == Ternary.OPTIONAL
                && electronic == Ternary.OPTIONAL
                && variation == Variation.ANY
                && dramatic == Ternary.OPTIONAL && tempo == Tempo.ANY
                && !preferDissonance && instrument == -1 && scale == null;
    }
}
