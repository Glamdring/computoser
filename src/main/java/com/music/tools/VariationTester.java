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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jm.JMC;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Score;
import jm.util.Play;

import com.music.MainPartGenerator;
import com.music.MainPartGenerator.MainPartContext;
import com.music.ScoreContext;
import com.music.model.ExtendedPhrase;
import com.music.model.Scale;

public class VariationTester {

    public static void main(String[] args) {
        Note[] melody = new Note[] { new Note(JMC.E5, JMC.EIGHTH_NOTE), new Note(JMC.EF5, JMC.EIGHTH_NOTE),
                new Note(JMC.E5, JMC.EIGHTH_NOTE), new Note(JMC.EF5, JMC.EIGHTH_NOTE),
                new Note(JMC.E5, JMC.EIGHTH_NOTE), new Note(JMC.B4, JMC.EIGHTH_NOTE),
                new Note(JMC.D5, JMC.EIGHTH_NOTE), new Note(JMC.C5, JMC.EIGHTH_NOTE),
                new Note(JMC.A4, JMC.EIGHTH_NOTE) };

        MainPartGenerator generator = new MainPartGenerator();
        Score score = new Score();
        Part mainPart = new Part("Main", 1);
        score.addPart(mainPart);
        ScoreContext ctx = new ScoreContext();
        ctx.setMetre(new int[] {3, 8});
        ctx.setScale(Scale.MINOR);
        ctx.setKeyNote(5);
        ctx.setNoteLengthCoefficient(1);
        double normalizedMeasureSize = 1d * ctx.getMetre()[0] * 4 / ctx.getMetre()[1];
        ctx.setNormalizedMeasureSize(normalizedMeasureSize);
        MainPartContext lCtx = generator.initLocalContext(score, ctx);
        lCtx.setPitches(Arrays.asList(60, 80, 80));
        ExtendedPhrase p = new ExtendedPhrase();
        p.setBaseVelocity(40);
        p.setScale(ctx.getScale());
        mainPart.add(p);
        lCtx.setCurrentPhrase(p);
        for (Note note : melody) {
            p.addNote(note);
        }
        List<Note> modified = new ArrayList<>(Arrays.asList(melody));
        generator.varyBaseStructure(lCtx, modified);
        System.out.println(modified);
        for (Note note : modified) {
            p.addNote(note);
        }

        Play.midi(score);
    }
}
