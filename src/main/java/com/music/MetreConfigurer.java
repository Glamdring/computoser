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

import com.music.model.prefs.Tempo;
import com.music.model.prefs.UserPreferences;
import com.music.util.music.Chance;

import jm.JMC;
import jm.music.data.Score;

public class MetreConfigurer implements ScoreManipulator {
    private Random random = new Random();

    @Override
    public void handleScore(Score score, ScoreContext ctx, UserPreferences prefs) {
        int[] metre = getRandomMetre(random);

        if (prefs != null && prefs.getTempo() != null) {
            score.setTempo(prefs.getTempo().getFrom() 
                    + random.nextInt(prefs.getTempo().getTo() - prefs.getTempo().getFrom()));
        } else {
            score.setTempo(Tempo.ANY.getFrom() + random.nextInt(Tempo.ANY.getTo() - Tempo.ANY.getFrom()));
        }

        ctx.setMetre(metre);
        score.setNumerator(metre[0]);
        score.setDenominator(metre[1]);

        double normalizedMeasureSize = getNormalizedMeasureSize(ctx.getMetre());
        ctx.setNormalizedMeasureSize(normalizedMeasureSize);
        if (prefs != null && prefs.getMeasures() != 0) {
            ctx.setMeasures(prefs.getMeasures());
        } else {
            ctx.setMeasures((int) ((10 + random.nextInt(10)) * score.getTempo() / 40));
        }

        if (Chance.test(20) && metre[1] < 16) {
            ctx.setUpBeatLength(normalizedMeasureSize - Math.max(1, metre[0] - random.nextInt(3) - 1) * JMC.EIGHTH_NOTE);
        }

        // possibly have longer notes for faster tempo?
        ctx.setNoteLengthCoefficient(1);
    }

    public static double getNormalizedMeasureSize(int[] metre) {
        // jMusic's whole note = 4, so 4 is the dividend.
        double normalizedMeasureSize = 1d * metre[0] * 4 / metre[1];
        return normalizedMeasureSize;
    }

    public static int[] getRandomMetre(Random random) {
        int[] metre = new int[2];
        metre[1] = (int) Math.pow(2, 2 + random.nextInt(3));
        int metreSpec = random.nextInt(3);
        if (metreSpec == 0) {
            metre[0] = 2 + random.nextInt(metre[1]);
        } else if (metreSpec == 1){
            metre[0] = metre[1] / 2;
        } else if (metreSpec == 2) {
            metre[0] = metre[1];
        }
        return metre;
    }
}
