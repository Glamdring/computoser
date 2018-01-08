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

import com.music.model.Scale;
import com.music.model.prefs.UserPreferences;
import com.music.util.music.Chance;

import jm.music.data.Score;

public class ScaleConfigurer implements ScoreManipulator {
    private Random random = new Random();

    private int[] SCALE_PERCENTAGES = new int[] {25,23,9,3,4,4,4,4,3,3,3,7,8};
    //private int[] MINOR_SCALE_PERCENTAGES = new int[] {0,45,24,7,14,0,0,0,0,0,0,0,10};

    @Override
    public void handleScore(Score score, ScoreContext ctx, UserPreferences prefs) {
        ctx.setScale(prefs != null && prefs.getScale() != null 
                ? prefs.getScale() 
                : getRandomScale());
        Scale alternativescale = getRandomScale();
        while (alternativescale == ctx.getScale()) {
            alternativescale = getRandomScale();
        }
        ctx.setAlternativeScale(alternativescale);

        @SuppressWarnings("unused")
        int keyNote = random.nextInt(12);

        //ctx.setKeyNote(keyNote);
    }

    private Scale getRandomScale() {
        Scale[] scales = Scale.values();
        return scales[Chance.choose(SCALE_PERCENTAGES)];
    }
}
