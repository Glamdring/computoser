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

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.music.model.Scale;
import com.music.model.prefs.Tempo;
import com.music.model.prefs.Variation;
import com.music.service.text.SentimentAnalyzer.SentimentResult;

@Entity
public class TimelineMusic {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "INT(11) UNSIGNED")
    private Long id;

    @ManyToOne
    private Piece piece;

    @Column
    private Variation variation;

    @Column
    private Scale scale;

    @Column
    private Tempo tempo;

    @Column(nullable=false)
    private int actualTempo;

    @Column(nullable=false)
    private double averageLength;

    @Column
    private SentimentResult sentiment;

    @Column(nullable=false)
    private double averageSpacing;

    @Column(nullable=false)
    private double averageSentiment;

    @ElementCollection(fetch=FetchType.EAGER)
    private Set<String> topKeywords;

    @Column
    private String twitterHandle;

    @ManyToOne
    private User user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Piece getPiece() {
        return piece;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
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

    public Tempo getTempo() {
        return tempo;
    }

    public void setTempo(Tempo tempo) {
        this.tempo = tempo;
    }

    public int getActualTempo() {
        return actualTempo;
    }

    public void setActualTempo(int actualTempo) {
        this.actualTempo = actualTempo;
    }

    public double getAverageLength() {
        return averageLength;
    }

    public void setAverageLength(double averageLength) {
        this.averageLength = averageLength;
    }

    public SentimentResult getSentiment() {
        return sentiment;
    }

    public void setSentiment(SentimentResult sentiment) {
        this.sentiment = sentiment;
    }

    public double getAverageSpacing() {
        return averageSpacing;
    }

    public void setAverageSpacing(double averageSpacing) {
        this.averageSpacing = averageSpacing;
    }

    public Set<String> getTopKeywords() {
        return topKeywords;
    }

    public void setTopKeywords(Set<String> topKeywords) {
        this.topKeywords = topKeywords;
    }

    public String getTwitterHandle() {
        return twitterHandle;
    }

    public void setTwitterHandle(String twitterHandle) {
        this.twitterHandle = twitterHandle;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public double getAverageSentiment() {
        return averageSentiment;
    }

    public void setAverageSentiment(double averageSentiment) {
        this.averageSentiment = averageSentiment;
    }

    @Override
    public String toString() {
        return "TimelineMusic [id=" + id + ", piece=" + piece + ", variation=" + variation + ", scale="
                + scale + ", tempo=" + tempo + ", actualTempo=" + actualTempo + ", averageLength="
                + averageLength + ", sentiment=" + sentiment + ", averageSpacing=" + averageSpacing
                + ", topKeywords=" + topKeywords + "]";
    }
}
