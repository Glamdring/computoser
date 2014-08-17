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

public class Chance {
    private static final Random random = new Random();

    public static boolean test(int percentage) {
        if (percentage >= random.nextInt(100) + 1) {
            return true;
        } else {
            return false;
        }
    }

    public static int choose(int[] percentages) {
        int rand = random.nextInt(100) + 1;
        int sum = 0;
        for (int i = 0; i < percentages.length; i++) {
            sum += percentages[i];
            if (sum > 100) {
                throw new IllegalStateException("Percentages exceed 100");
            }
            if (rand <= sum) {
                return i;
            }
        }
        throw new IllegalStateException("Percentages passed do not go up to 100");
    }
}
