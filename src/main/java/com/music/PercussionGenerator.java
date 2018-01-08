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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import com.music.model.ExtendedPhrase;
import com.music.model.PartType;
import com.music.model.prefs.UserPreferences;
import com.music.util.music.Chance;
import com.music.util.music.NoteFactory;

import jm.music.data.CPhrase;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Rest;
import jm.music.data.Score;

public class PercussionGenerator implements ScoreManipulator {
    private Random random = new Random();

    private int[] PRIMARY_DRUM_PERCENTAGES = { 90, 10, 25, 50, 80, 10, 25, 50 };
    private int[] SECONDARY_DRUM_PERCENTAGES = { 15, 5, 80, 5, 20, 15, 80, 15 };
    private int[] AVAILABLE_PERCUSSIONS = {36, 38, 42, 43, 44, 46, 48, 49, 51};

    @Override
    public void handleScore(Score score, ScoreContext ctx, UserPreferences prefs) {
        Part drumPart = ctx.getParts().get(PartType.PERCUSSIONS);
        if (drumPart == null) {
            return;
        }

        Part mainPart = ctx.getParts().get(PartType.MAIN);
        Phrase[] phrases = mainPart.getPhraseArray();
        for (Phrase phrase : phrases) {
            if (!(phrase instanceof ExtendedPhrase)) {
                continue;
            }

            int[] percussions = getRandomPercussionPattern();

            // TODO allow for -..-..-., and generally allow more strictly accented beats
            boolean useMiddleBeats = Chance.test(5);

            ExtendedPhrase ePhrase = (ExtendedPhrase) phrase;
            double beatNoteLength = ctx.getNormalizedMeasureSize() / 8;
            if (useMiddleBeats) {
                beatNoteLength = ctx.getNormalizedMeasureSize() / 16;
            }
            for (int i = 0; i < ePhrase.getMeasures(); i++) {
                List<Phrase> fullKit = new ArrayList<Phrase>();

                // sometimes alternate the pattern per measure
                if (Chance.test(8)) {
                    percussions = getRandomPercussionPattern();
                }
                // don't use the drums in some measures
                if (Chance.test(6)) {
                    Phrase phr = new Phrase(0.0);
                    phr.addRest(new Rest(ctx.getNormalizedMeasureSize()));
                    fullKit.add(phr);
                    continue;
                }

                for (int j = 0; j < percussions.length; j++) {
                    Phrase phr = new Phrase(0.0);
                    for (short k = 0; k < 16; k++) {
                        int[] chances = PRIMARY_DRUM_PERCENTAGES;
                        if (j % 2 == 1 || percussions[j] == 46) {
                            chances = SECONDARY_DRUM_PERCENTAGES;
                        }
                        if ((useMiddleBeats && k % 2 == 1 && Chance.test(6)) || Chance.test(chances[k / 2])) {
                            Note note = NoteFactory.createNote(percussions[j], beatNoteLength);
                            if (k % 2 == 1) {
                                note.setDynamic(35 + random.nextInt(8));
                            } else {
                                note.setDynamic(45 + random.nextInt(10));
                            }
                            phr.addNote(note);
                        } else {
                            phr.addRest(new Rest(beatNoteLength));
                        }
                    }
                    fullKit.add(phr);
                }

                // add phrases to the instrument (part)
                CPhrase cp = new CPhrase(0.0);
                cp.setPhraseList(new Vector<Phrase>(fullKit));

                cp.setStartTime(i * ctx.getNormalizedMeasureSize());
                drumPart.addCPhrase(cp);
            }
        }
    }

    private int[] getRandomPercussionPattern() {
        int[] percussions = new int[2 + random.nextInt(3)];
        for (int i = 0; i < percussions.length; i++) {
            percussions[i] = AVAILABLE_PERCUSSIONS[random.nextInt(AVAILABLE_PERCUSSIONS.length)];
        }
        return percussions;
    }
}
