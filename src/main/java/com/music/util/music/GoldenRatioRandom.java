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

package com.music.util.music;

import java.util.Random;

/**
 * Naive experiment - currently not in use
 *
 * @author bozho
 *
 */
public class GoldenRatioRandom extends Random {
    private static final long serialVersionUID = -837489222113296455L;
    private double GOLDEN_RATIO = 1.6180339887;
    private int previous = -1;

    public int nextInt(int max) {
        if (previous == -1) {
            return super.nextInt(max);
        } else {
            int next = 0;
            if (nextBoolean()) {
                next = (int) Math.round(previous / GOLDEN_RATIO);
            } else {
                next = (int) Math.round(previous * GOLDEN_RATIO);
            }

            return next % max;
        }
    }
}
