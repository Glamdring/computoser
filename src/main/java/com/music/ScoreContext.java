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
import java.util.Map;

import jm.music.data.Part;
import jm.music.data.Score;

import com.google.common.collect.Maps;
import com.music.MainPartGenerator.Variation;
import com.music.model.PartType;
import com.music.model.Scale;

public class ScoreContext {
    private Score score;
    private Scale scale;
    private Scale alternativeScale;
    private int keyNote;
    private int[] metre;
    private int measures;
    private double normalizedMeasureSize;
    private double upBeatLength;
    private double noteLengthCoefficient = 1;
    private double variation;
    private Map<PartType, Part> parts = Maps.newHashMap();

    // storing intermediate decisions for improved analysis later
    private boolean fourToTheFloor;
    private boolean dullBass;
    private boolean dissonant;
    private boolean simplePhrases;
    private boolean electronic;
    private boolean ornamented;
    private List<Variation> variations = new ArrayList<>();

    public Scale getScale() {
        return scale;
    }

    public void setScale(Scale scale) {
        this.scale = scale;
    }

    public int getKeyNote() {
        return keyNote;
    }

    public void setKeyNote(int keyNote) {
        this.keyNote = keyNote;
    }

    public int[] getMetre() {
        return metre;
    }

    public void setMetre(int[] metre) {
        this.metre = metre;
    }

    public int getMeasures() {
        return measures;
    }

    public void setMeasures(int beats) {
        this.measures = beats;
    }

    public double getNormalizedMeasureSize() {
        return normalizedMeasureSize;
    }

    public void setNormalizedMeasureSize(double normalizedBeatSize) {
        this.normalizedMeasureSize = normalizedBeatSize;
    }

    public Score getScore() {
        return score;
    }

    public void setScore(Score score) {
        this.score = score;
    }

    public double getUpBeatLength() {
        return upBeatLength;
    }

    public void setUpBeatLength(double upBeatLength) {
        this.upBeatLength = upBeatLength;
    }

    public Scale getAlternativeScale() {
        return alternativeScale;
    }

    public void setAlternativeScale(Scale alternativeScale) {
        this.alternativeScale = alternativeScale;
    }

    public double getNoteLengthCoefficient() {
        return noteLengthCoefficient;
    }

    public void setNoteLengthCoefficient(double noteLengthCoefficient) {
        this.noteLengthCoefficient = noteLengthCoefficient;
    }

    public Map<PartType, Part> getParts() {
        return parts;
    }

    public boolean isFourToTheFloor() {
        return fourToTheFloor;
    }

    public void setFourToTheFloor(boolean fourToTheFloor) {
        this.fourToTheFloor = fourToTheFloor;
    }

    public boolean isDullBass() {
        return dullBass;
    }

    public void setDullBass(boolean dullBass) {
        this.dullBass = dullBass;
    }

    public boolean isDissonant() {
        return dissonant;
    }

    public void setDissonant(boolean dissonant) {
        this.dissonant = dissonant;
    }

    public boolean isSimplePhrases() {
        return simplePhrases;
    }

    public void setSimplePhrases(boolean simplePhraes) {
        this.simplePhrases = simplePhraes;
    }

    public double getVariation() {
        return variation;
    }

    public void setVariation(double variation) {
        this.variation = variation;
    }

    public boolean isElectronic() {
        return electronic;
    }

    public void setElectronic(boolean electronic) {
        this.electronic = electronic;
    }

    public boolean isOrnamented() {
        return ornamented;
    }

    public void setOrnamented(boolean ornamented) {
        this.ornamented = ornamented;
    }

    public List<Variation> getVariations() {
        return variations;
    }

    public void setVariations(List<Variation> variations) {
        this.variations = variations;
    }

    @Override
    public String toString() {
        return "ScoreContext [scale=" + scale + ", altScale=" + alternativeScale + ", startingNote=" + keyNote
                + ", metre=" + Arrays.toString(metre) + ", beats=" + measures + ", normalizedBeatSize=" + normalizedMeasureSize + ", upBeatLength="
                + upBeatLength + ", tempo=" + score.getTempo() + ", noteLengthCoefficient=" + noteLengthCoefficient + ", parts=" + parts.keySet() + "]";
    }
}
