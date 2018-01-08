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

import static com.music.model.InstrumentGroups.ACCOMPANIMENT_INSTRUMENTS;
import static com.music.model.InstrumentGroups.ARPEGGIO_INSTRUMENTS;
import static com.music.model.InstrumentGroups.BASS_INSTRUMENTS;
import static com.music.model.InstrumentGroups.DRONE_INSTRUMENTS;
import static com.music.model.InstrumentGroups.ELECTRONIC_MAIN_PART_ONLY_INSTRUMENTS;
import static com.music.model.InstrumentGroups.MAIN_PART_ONLY_INSTRUMENTS;
import static com.music.model.InstrumentGroups.PAD_INSTRUMENTS;
import static com.music.model.InstrumentGroups.SIMPLE_BEAT_INSTRUMENTS;
import static com.music.model.InstrumentGroups.SPORADIC_EFFECTS_INSTRUMENTS;

import java.util.Random;

import com.music.model.InstrumentGroups;
import com.music.model.PartType;
import com.music.model.prefs.Ternary;
import com.music.model.prefs.UserPreferences;
import com.music.util.music.Chance;

import jm.constants.Instruments;
import jm.music.data.Part;
import jm.music.data.Score;

public class PartConfigurer implements ScoreManipulator {
    private Random random = new Random();

    @Override
    public void handleScore(Score score, ScoreContext ctx, UserPreferences prefs) {
        boolean accompaniment = Chance.test(70);
        int partIdx = 0;
        int mainInstrument = 0;
        boolean electronic = false;
        boolean classical = (prefs != null && prefs.isClassical()) || Chance.test(5); //ensure some classical-sounding pieces
        boolean counterpoint = false;
        if (classical) {
            accompaniment = true;
        }
        if ((prefs != null && prefs.getAccompaniment() == Ternary.NO) 
                || Chance.test(1) 
                || (classical && Chance.test(12))) {
            counterpoint = true;
            accompaniment = false;
        }

        if (!accompaniment) {
            if ((prefs != null && prefs.getElectronic() == Ternary.YES) || Chance.test(35)) { // electronic
                electronic = true;
                ctx.setElectronic(true);
                mainInstrument = ELECTRONIC_MAIN_PART_ONLY_INSTRUMENTS[random.nextInt(ELECTRONIC_MAIN_PART_ONLY_INSTRUMENTS.length)];
            } else {
                mainInstrument = MAIN_PART_ONLY_INSTRUMENTS[random.nextInt(MAIN_PART_ONLY_INSTRUMENTS.length)];
            }
        } else {
            mainInstrument = InstrumentGroups.MAIN_PART_INSTRUMENTS[random.nextInt(InstrumentGroups.MAIN_PART_INSTRUMENTS.length)];
        }

        if (classical) {
            mainInstrument = Instruments.PIANO;
        }
        
        // override the instrument if it was configured
        if (prefs != null && prefs.getInstrument() != -1) {
            mainInstrument = prefs.getInstrument();
        }

        Part main = new Part(PartType.MAIN.getTitle(), mainInstrument , partIdx++);
        score.add(main);
        ctx.getParts().put(PartType.MAIN, main);

        if (Chance.test(12)) {
            Part duplicateMainPart = new Part(PartType.MAIN_DUPLICATE.getTitle(), MAIN_PART_ONLY_INSTRUMENTS[random.nextInt(MAIN_PART_ONLY_INSTRUMENTS.length)], partIdx++);
            score.add(duplicateMainPart);
            ctx.getParts().put(PartType.MAIN_DUPLICATE, duplicateMainPart);
        }

        if (accompaniment) {
            if (Chance.test(55) && !electronic) {
                Part accompanimentPart = new Part(PartType.ACCOMPANIMENT.getTitle(), ACCOMPANIMENT_INSTRUMENTS[random.nextInt(ACCOMPANIMENT_INSTRUMENTS.length)], partIdx++);
                score.add(accompanimentPart);
                ctx.getParts().put(PartType.ACCOMPANIMENT, accompanimentPart);
            } else if (isRegularMetre(ctx)){
                Part arpegioPart = new Part(PartType.ARPEGGIO.getTitle(), ARPEGGIO_INSTRUMENTS[random.nextInt(ARPEGGIO_INSTRUMENTS.length)], partIdx++);
                score.add(arpegioPart);
                ctx.getParts().put(PartType.ARPEGGIO, arpegioPart);
            }
        }

        boolean hasBass = Chance.test(22) && !classical;
        if (hasBass) {
            Part bassPart = new Part(PartType.BASS.getTitle(), BASS_INSTRUMENTS[random.nextInt(BASS_INSTRUMENTS.length)], partIdx++);
            score.add(bassPart);
            ctx.getParts().put(PartType.BASS, bassPart);
        }

        if (!hasBass && isRegularMetre(ctx) && !classical && Chance.test(16)) {
            Part dronePart = new Part(PartType.DRONE.getTitle(), DRONE_INSTRUMENTS[random.nextInt(DRONE_INSTRUMENTS.length)], partIdx++);
            score.add(dronePart);
            ctx.getParts().put(PartType.DRONE, dronePart);
        }

        boolean useSimpleBeat = Chance.test(33) && !classical;
        if (useSimpleBeat) {
            Part extraPart = new Part(PartType.SIMPLE_BEAT.getTitle(), SIMPLE_BEAT_INSTRUMENTS[random.nextInt(SIMPLE_BEAT_INSTRUMENTS.length)], partIdx++);
            score.add(extraPart);
            ctx.getParts().put(PartType.SIMPLE_BEAT, extraPart);
        }

        boolean usePercussions = (prefs != null && prefs.getDrums() == Ternary.YES) || Chance.test(46);
        if (!useSimpleBeat && usePercussions && isRegularMetre(ctx) && !classical) {
            Part extraPart = new Part(PartType.PERCUSSIONS.getTitle(), 0, 9);
            extraPart.setDynamic(35); //quieter, in the background
            score.add(extraPart);
            ctx.getParts().put(PartType.PERCUSSIONS, extraPart);
        }

        //TODO beats (ala drum & bass). no cymbals?

        if (!classical && Chance.test(30)) {
            Part extraPart = new Part(PartType.EFFECTS.getTitle(), SPORADIC_EFFECTS_INSTRUMENTS[random.nextInt(SPORADIC_EFFECTS_INSTRUMENTS.length)], partIdx++);
            score.add(extraPart);
            ctx.getParts().put(PartType.EFFECTS, extraPart);
        }

        if (Chance.test(30) && !classical) {
            Part padPart1 = new Part(PartType.PAD1.getTitle(), PAD_INSTRUMENTS[random.nextInt(PAD_INSTRUMENTS.length)], partIdx++);
            score.add(padPart1);
            ctx.getParts().put(PartType.PAD1, padPart1);

            if (Chance.test(46)) {
                Part padPart2 = new Part(PartType.PAD2.getTitle(), PAD_INSTRUMENTS[random.nextInt(PAD_INSTRUMENTS.length)], partIdx++);
                score.add(padPart2);
                ctx.getParts().put(PartType.PAD2, padPart2);
            }
        }
        // TODO add more dramatic features other than the timpani
        if (Chance.test(6) && isRegularMetre(ctx) && !classical) {
            Part timpaniPart = new Part(PartType.TIMPANI.getTitle(), Instruments.TIMPANI, partIdx++);
            score.add(timpaniPart);
            ctx.getParts().put(PartType.TIMPANI, timpaniPart);
        }
    }

    public static boolean isRegularMetre(ScoreContext ctx) {
        // check if power of 2
        return ((ctx.getMetre()[0]-1) & ctx.getMetre()[0]) == 0;
    }
}
