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

package com.music.xmlmodel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
public class NoteElement {

    private Pitch pitch;
    private RestElement rest;
    private int duration;
    private int voice;
    private String type;
    private String dot;
    private String stem = "up";

    public Pitch getPitch() {
        return pitch;
    }
    public void setPitch(Pitch pitch) {
        this.pitch = pitch;
    }
    public int getDuration() {
        return duration;
    }
    public void setDuration(int duration) {
        this.duration = duration;
    }
    public int getVoice() {
        return voice;
    }
    public void setVoice(int voice) {
        this.voice = voice;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getStem() {
        return stem;
    }
    public void setStem(String stem) {
        this.stem = stem;
    }
    public RestElement getRest() {
        return rest;
    }
    public void setRest(RestElement rest) {
        this.rest = rest;
    }
    public String getDot() {
        return dot;
    }
    public void setDot(String dot) {
        this.dot = dot;
    }
}
