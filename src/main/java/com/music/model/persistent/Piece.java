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
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import com.music.model.Scale;

@Entity
public class Piece {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "INT(11) UNSIGNED")
    private Long id;

    @Type(type="com.music.util.persistence.PersistentDateTime")
    private DateTime generationTime;

    @Column(nullable=false)
    private int likes;

    @Column
    private String title;

    @Column
    @Enumerated(EnumType.STRING)
    private Scale scale;

    @Column
    @Enumerated(EnumType.STRING)
    private Scale alternativeScale;

    @Column(nullable=false)
    private int keyNote;

    @Column(nullable=false)
    private int metreNumerator;

    @Column(nullable=false)
    private int metreDenominator;

    @Column(nullable=false)
    private int measures;

    @Column(nullable=false)
    private int tempo;

    @Column(nullable=false)
    private double normalizedMeasureSize;

    @Column(nullable=false)
    private double upBeatLength;

    @Column(nullable=false)
    private double noteLengthCoefficient;

    @Column(nullable=false)
    private boolean newlyCreated;

    @Column
    private String parts;

    @Embedded
    private IntermediateDecisions intermediateDecisions = new IntermediateDecisions();

    @Column(nullable=false)
    private int mainInstrument;

    @Column(nullable=false)
    private double variation;

    @Column
    private String encodedUserPreferences;

    @Column(nullable=false)
    private int midiDownloads;

    @Column(nullable=false)
    private int mp3Downloads;

    @Column
    private String algorithmVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Scale getScale() {
        return scale;
    }

    public void setScale(Scale scale) {
        this.scale = scale;
    }

    public Scale getAlternativeScale() {
        return alternativeScale;
    }

    public void setAlternativeScale(Scale alternativeScale) {
        this.alternativeScale = alternativeScale;
    }

    public int getKeyNote() {
        return keyNote;
    }

    public void setKeyNote(int keyNote) {
        this.keyNote = keyNote;
    }

    public int getMetreNumerator() {
        return metreNumerator;
    }

    public void setMetreNumerator(int metreNumerator) {
        this.metreNumerator = metreNumerator;
    }

    public int getMetreDenominator() {
        return metreDenominator;
    }

    public void setMetreDenominator(int metreDenominator) {
        this.metreDenominator = metreDenominator;
    }

    public int getMeasures() {
        return measures;
    }

    public void setMeasures(int measures) {
        this.measures = measures;
    }

    public double getNormalizedMeasureSize() {
        return normalizedMeasureSize;
    }

    public void setNormalizedMeasureSize(double normalizedMeasureSize) {
        this.normalizedMeasureSize = normalizedMeasureSize;
    }

    public double getUpBeatLength() {
        return upBeatLength;
    }

    public void setUpBeatLength(double upBeatLength) {
        this.upBeatLength = upBeatLength;
    }

    public double getNoteLengthCoefficient() {
        return noteLengthCoefficient;
    }

    public void setNoteLengthCoefficient(double noteLengthCoefficient) {
        this.noteLengthCoefficient = noteLengthCoefficient;
    }

    public String getParts() {
        return parts;
    }

    public void setParts(String parts) {
        this.parts = parts;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getTempo() {
        return tempo;
    }

    public void setTempo(int tempo) {
        this.tempo = tempo;
    }

    public IntermediateDecisions getIntermediateDecisions() {
        return intermediateDecisions;
    }

    public void setIntermediateDecisions(IntermediateDecisions intermediateDecisions) {
        this.intermediateDecisions = intermediateDecisions;
    }

    public String getEncodedUserPreferences() {
        return encodedUserPreferences;
    }

    public void setEncodedUserPreferences(String encodedUserPreferences) {
        this.encodedUserPreferences = encodedUserPreferences;
    }

    public boolean isNewlyCreated() {
        return newlyCreated;
    }

    public void setNewlyCreated(boolean newlyCreated) {
        this.newlyCreated = newlyCreated;
    }

    public DateTime getGenerationTime() {
        return generationTime;
    }

    public void setGenerationTime(DateTime generationTime) {
        this.generationTime = generationTime;
    }

    public int getMainInstrument() {
        return mainInstrument;
    }

    public void setMainInstrument(int mainInstrument) {
        this.mainInstrument = mainInstrument;
    }

    public int getMidiDownloads() {
        return midiDownloads;
    }

    public void setMidiDownloads(int downloads) {
        this.midiDownloads = downloads;
    }

    public double getVariation() {
        return variation;
    }

    public void setVariation(double variation) {
        this.variation = variation;
    }

    public String getAlgorithmVersion() {
        return algorithmVersion;
    }

    public void setAlgorithmVersion(String algorithmVersion) {
        this.algorithmVersion = algorithmVersion;
    }

    public int getMp3Downloads() {
        return mp3Downloads;
    }

    public void setMp3Downloads(int mp3Downloads) {
        this.mp3Downloads = mp3Downloads;
    }
}
