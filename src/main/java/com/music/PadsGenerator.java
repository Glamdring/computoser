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

import com.music.model.ExtendedPhrase;
import com.music.model.InstrumentGroups;
import com.music.model.PartType;
import com.music.model.Scale;
import com.music.model.prefs.UserPreferences;
import com.music.util.music.Chance;
import com.music.util.music.NoteFactory;

import jm.constants.Pitches;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Rest;
import jm.music.data.Score;

public class PadsGenerator implements ScoreManipulator {
    private Random random = new Random();

    @Override
    public void handleScore(Score score, ScoreContext ctx, UserPreferences prefs) {
        Part pad1 = ctx.getParts().get(PartType.PAD1);
        Part pad2 = ctx.getParts().get(PartType.PAD2);
        if (pad1 == null) {
            return;
        }
        Phrase pad1Phrase = new Phrase();
        pad1Phrase.setInstrument(pad1.getInstrument());
        pad1.add(pad1Phrase);

        Phrase pad2Phrase = null;
        if (pad2 != null) {
            pad2Phrase = new Phrase();
            pad2Phrase.setInstrument(pad2.getInstrument());
            pad2.add(pad2Phrase);
        }

        boolean pad1Long = Chance.test(40);
        boolean pad2Long = Chance.test(40);
        int nextContinuationChangeMeasurePad1 = 0;
        int nextContinuationChangeMeasurePad2 = 0;

        Part mainPart = ctx.getParts().get(PartType.MAIN);
        for (Phrase phrase : mainPart.getPhraseArray()) {
            ExtendedPhrase ePhrase = (ExtendedPhrase) phrase;
            for (int i = 0; i < ePhrase.getMeasures(); i++) {
                nextContinuationChangeMeasurePad1 = handlePads(ctx, ePhrase.getScale(), pad1Phrase, i, nextContinuationChangeMeasurePad1, pad1Long);
                if (pad2 != null) {
                    nextContinuationChangeMeasurePad2 = handlePads(ctx, ePhrase.getScale(), pad2Phrase, i, nextContinuationChangeMeasurePad2, pad2Long);
                }
            }
        }
    }

    private int handlePads(ScoreContext ctx, Scale scale, Phrase phrase, int measure, int nextContinuationChangeMeasure, boolean longPads) {
        if (measure == nextContinuationChangeMeasure) {
            if (Chance.test(80)) {
                int holdFor = 0;
                if ((longPads && Chance.test(80)) || (!longPads && Chance.test(10))) {
                    holdFor = 2 + random.nextInt(10);
                } else {
                    holdFor = 1;
                }
                Note note = NoteFactory.createNote(getRandomPitch(ctx, scale), holdFor * ctx.getNormalizedMeasureSize());
                note.setDynamic(InstrumentGroups.getInstrumentSpecificDynamics(60, phrase.getInstrument()));
                phrase.addNote(note);
                return nextContinuationChangeMeasure + holdFor;
            } else {
                phrase.addRest(new Rest(ctx.getNormalizedMeasureSize()));
                return nextContinuationChangeMeasure + 1;
            }
        } else {
            return nextContinuationChangeMeasure;
        }
    }

    private int getRandomPitch(ScoreContext ctx, Scale scale) {
        int pitch = Pitches.C4
                + ctx.getKeyNote()
                + scale.getDefinition()[random.nextInt(scale.getDefinition().length)];
        return pitch;
    }
}
