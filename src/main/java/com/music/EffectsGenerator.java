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

import jm.constants.Pitches;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Rest;
import jm.music.data.Score;

public class EffectsGenerator implements ScoreManipulator {
    private Random random = new Random();

    @Override
    public void handleScore(Score score, ScoreContext ctx, UserPreferences prefs) {
        Part effectsPart = ctx.getParts().get(PartType.EFFECTS);
        if (effectsPart == null) {
            return;
        }
        Phrase phrase = new Phrase();
        int restPercentage = Chance.test(65) ? 75 : 15; //sporadic vs regular
        for (int i = 0; i < ctx.getMeasures(); i++) {
            handleEffects(ctx, phrase, restPercentage);
        }
        effectsPart.add(phrase);
    }


    private void handleEffects(ScoreContext ctx, Phrase phrase, int restPercentages) {
        if (Chance.test(restPercentages)) {
            phrase.addRest(new Rest(ctx.getNormalizedMeasureSize()));
        } else {
            int pitch = getRandomPitch(ctx);
            double effectLength = random.nextDouble() * ctx.getNormalizedMeasureSize();
            effectLength = Math.min(effectLength, ctx.getNormalizedMeasureSize());
            Note note = NoteFactory.createNote(pitch, effectLength);
            phrase.add(note);
            double lastRest = ctx.getNormalizedMeasureSize() - effectLength;
            if (lastRest > 0) {
                phrase.add(new Rest(lastRest));
            }
        }
    }

    private int getRandomPitch(ScoreContext ctx) {
        int pitch = Pitches.C4
                + ctx.getKeyNote()
                + ctx.getScale().getDefinition()[random.nextInt(ctx.getScale().getDefinition().length)];
        return pitch;
    }
}
