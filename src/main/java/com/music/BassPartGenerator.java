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
import com.music.model.SpecialNoteType;
import com.music.model.prefs.UserPreferences;
import com.music.util.music.Chance;
import com.music.util.music.NoteFactory;

import jm.JMC;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Rest;
import jm.music.data.Score;

public class BassPartGenerator implements ScoreManipulator {

    private static Random random = new Random();

    @Override
    public void handleScore(Score score, ScoreContext ctx, UserPreferences prefs) {
        Part bassPart = ctx.getParts().get(PartType.BASS);
        if (bassPart == null) {
            return;
        }

        Part mainPart = ctx.getParts().get(PartType.MAIN);

        int degreePercentages[] = new int[]{27, 13, 21, 6, 19, 10, 4};
        int dullBassPercentages[] = new int[]{45, 11, 22, 0, 22, 0, 0};

        boolean dullBass = Chance.test(38);
        ctx.setDullBass(dullBass);

        Phrase[] phrases = mainPart.getPhraseArray();

        double durationModifier = Chance.test(30) ? SpecialNoteType.STACCATO.getValue() : 0;

        for (Phrase phrase : phrases) {
            ExtendedPhrase extPhrase = ((ExtendedPhrase) phrase);
            Scale currentScale = extPhrase.getScale();
            if (currentScale.getDefinition().length != 7) { //no bass for irregular scales, for now
                continue;
            }
            Phrase bassPhrase = new Phrase();
            for (int i = 0; i < extPhrase.getMeasures(); i++) {
                for (int k = 0; k < 2; k++) {
                    if (Chance.test(70)) {
                        int degreeIdx = 0;
                        if (dullBass) {
                            degreeIdx = Chance.choose(dullBassPercentages);
                        } else {
                            degreeIdx = Chance.choose(degreePercentages);
                        }
                        Note note = NoteFactory.createNote(JMC.C3 + ctx.getKeyNote() + currentScale.getDefinition()[degreeIdx], ctx.getNormalizedMeasureSize() / 2);
                        note.setDynamic(InstrumentGroups.getInstrumentSpecificDynamics(60 + random.nextInt(15), bassPart.getInstrument()));
                        if (durationModifier > 0) {
                            note.setDuration(note.getRhythmValue() * durationModifier);
                        }
                        bassPhrase.addNote(note);
                    } else {
                        bassPhrase.addRest(new Rest(ctx.getNormalizedMeasureSize() / 2));
                    }
                }
            }
            bassPart.addPhrase(bassPhrase);
        }

    }
}
