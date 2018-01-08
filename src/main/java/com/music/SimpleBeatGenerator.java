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

import java.util.Random;

import com.music.model.PartType;
import com.music.model.prefs.UserPreferences;
import com.music.util.music.Chance;
import com.music.util.music.NoteFactory;

import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Rest;
import jm.music.data.Score;

public class SimpleBeatGenerator implements ScoreManipulator {
    private Random random = new Random();

    @Override
    public void handleScore(Score score, ScoreContext ctx, UserPreferences prefs) {
        Part drumPart = ctx.getParts().get(PartType.SIMPLE_BEAT);
        if (drumPart == null) {
            return;
        }
        Phrase phrase = new Phrase();
        boolean needsBass = Chance.test(80);
        boolean hasBass = ctx.getParts().get(PartType.BASS) != null;
        // "four on the floor" in case of faster pieces that (in most cases) have a bass
        if (PartConfigurer.isRegularMetre(ctx) && Chance.test(50) && ctx.getParts().get(PartType.PERCUSSIONS) == null && score.getTempo() > 115 && (needsBass ? hasBass : true)) {
            ctx.setFourToTheFloor(true);
            drumPart.setChannel(9);
            for (int i = 0; i < ctx.getMeasures(); i++) {
                for (int k = 0; k < 4; k++) {
                    Note note = NoteFactory.createNote(35 + k % 2, ctx.getNormalizedMeasureSize() / 4);
                    phrase.add(note);
                }
            }
        } else {
            for (int i = 0; i < ctx.getMeasures(); i++) {
                Note note = NoteFactory.createNote(40 + random.nextInt(30), ctx.getNormalizedMeasureSize() / 4);
                phrase.add(note);
                phrase.add(new Rest(ctx.getNormalizedMeasureSize() - note.getRhythmValue()));
            }
        }

        drumPart.add(phrase);

    }

}
