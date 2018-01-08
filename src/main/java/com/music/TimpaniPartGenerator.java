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
import com.music.model.ToneType;
import com.music.model.prefs.UserPreferences;
import com.music.util.music.Chance;
import com.music.util.music.NoteFactory;

import jm.constants.Pitches;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Rest;
import jm.music.data.Score;

public class TimpaniPartGenerator implements ScoreManipulator {

    private Random random = new Random();

    @Override
    public void handleScore(Score score, ScoreContext ctx, UserPreferences prefs) {
        Part timpaniPart = ctx.getParts().get(PartType.TIMPANI);
        if (timpaniPart == null) {
            return;
        }

        Phrase phrase = new Phrase();
        for (int i = 0; i < ctx.getMeasures(); i++) {
            if (Chance.test(20)) {
                phrase.add(getNote(ctx, ToneType.TONIC));
                phrase.add(getNote(ctx, ToneType.MEDIANT));
                if (Chance.test(20)) {
                    phrase.add(getNote(ctx, ToneType.TONIC));
                } else {
                    phrase.addRest(new Rest(ctx.getNormalizedMeasureSize() / 4));
                }
                phrase.addRest(new Rest(ctx.getNormalizedMeasureSize() / 4));
            } else {
                phrase.addRest(new Rest(ctx.getNormalizedMeasureSize()));
            }
        }
        phrase.setDynamic(100);
        timpaniPart.add(phrase);
    }

    private Note getNote(ScoreContext ctx, ToneType tone) {
        int pitch = Pitches.C3 + ctx.getScale().getDefinition()[tone.getDegree()];
        Note note = NoteFactory.createNote(pitch, ctx.getNormalizedMeasureSize() / 4);
        note.setDynamic(52 + random.nextInt(10));
        note.setDuration(1.2);
        return note;
    }

}
