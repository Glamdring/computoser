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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="score-partwise")
@XmlAccessorType(XmlAccessType.FIELD)
public class ScorePartwise {

    @XmlAttribute
    private String version = "3.0";

    @XmlElement(name="movement-title")
    private String movementTitle;

    @XmlElementWrapper(name="part-list")
    @XmlElement(name="score-part")
    private List<ScorePartDefinition> partDefinitionList = new ArrayList<>();

    @XmlElement(name="part")
    private List<ScorePart> partList = new ArrayList<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMovementTitle() {
        return movementTitle;
    }

    public void setMovementTitle(String movementTitle) {
        this.movementTitle = movementTitle;
    }

    public List<ScorePartDefinition> getPartDefinitionList() {
        return partDefinitionList;
    }

    public void setPartDefinitionList(List<ScorePartDefinition> partDefinitionList) {
        this.partDefinitionList = partDefinitionList;
    }

    public List<ScorePart> getPartList() {
        return partList;
    }

    public void setPartList(List<ScorePart> partList) {
        this.partList = partList;
    }
}
