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

package com.music.model.persistent;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.music.model.prefs.Tempo;

@Embeddable
public class IntermediateDecisions {

    @Column(nullable=false)
    private boolean dullBass;

    @Column(nullable=false)
    private boolean fourToTheFloor;

    @Enumerated(EnumType.STRING)
    private Tempo tempoType;

    @Column(nullable=false)
    private boolean classical;

    @Column(nullable=false)
    private boolean electronic;

    @Column(nullable=false)
    private boolean drums;

    @Column(nullable=false)
    private boolean accompaniment;

    @Column(nullable=false)
    private boolean dissonant;

    @Column(nullable=false)
    private boolean simplePhrases;

    @Column(nullable=false)
    private boolean ornamented;

    @Column(length=2000)
    private String variations;

    public boolean isDullBass() {
        return dullBass;
    }

    public void setDullBass(boolean dullBass) {
        this.dullBass = dullBass;
    }

    public boolean isFourToTheFloor() {
        return fourToTheFloor;
    }

    public void setFourToTheFloor(boolean fourToTheFloor) {
        this.fourToTheFloor = fourToTheFloor;
    }

    public Tempo getTempoType() {
        return tempoType;
    }

    public void setTempoType(Tempo tempoType) {
        this.tempoType = tempoType;
    }

    public boolean isClassical() {
        return classical;
    }

    public void setClassical(boolean classical) {
        this.classical = classical;
    }

    public boolean isDrums() {
        return drums;
    }

    public void setDrums(boolean drums) {
        this.drums = drums;
    }

    public boolean isAccompaniment() {
        return accompaniment;
    }

    public void setAccompaniment(boolean accompaniment) {
        this.accompaniment = accompaniment;
    }

    public boolean isElectronic() {
        return electronic;
    }

    public void setElectronic(boolean electronic) {
        this.electronic = electronic;
    }

    public boolean isDissonant() {
        return dissonant;
    }

    public void setDissonant(boolean preferDissonance) {
        this.dissonant = preferDissonance;
    }

    public boolean isSimplePhrases() {
        return simplePhrases;
    }

    public void setSimplePhrases(boolean simplePhrases) {
        this.simplePhrases = simplePhrases;
    }

    public boolean isOrnamented() {
        return ornamented;
    }

    public void setOrnamented(boolean ornamentation) {
        this.ornamented = ornamentation;
    }

    public String getVariations() {
        return variations;
    }

    public void setVariations(String variations) {
        this.variations = variations;
    }
}
