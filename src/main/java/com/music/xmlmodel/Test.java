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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

public class Test {

    public static void main(String[] args) throws Exception {
        ScorePartwise score = new ScorePartwise();
        score.setMovementTitle("Foo");
        ScorePartDefinition def = new ScorePartDefinition();
        def.setId("1");
        def.setPartName("Part 1");
        score.getPartDefinitionList().add(def);

        ScorePart part = new ScorePart();
        part.setId("1");
        Measure measure = new Measure();
        measure.setNumber(1);
        NoteElement note1 = new NoteElement();
        note1.setDuration(2);
        note1.setType("half");
        note1.setVoice(1);
        note1.getPitch().setOctave(5);
        note1.getPitch().setStep("D");
        measure.getNotes().add(note1);
        measure.getNotes().add(note1);
        part.getMeasures().add(measure);
        score.getPartList().add(part);

        Marshaller m = JAXBContext.newInstance(ScorePartwise.class).createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(score, System.out);
    }
}
