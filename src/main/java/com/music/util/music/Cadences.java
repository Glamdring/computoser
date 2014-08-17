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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Cadences {
    private static final List<int[]> cadences = new ArrayList<>();
    private static final Random random = new Random();

    static {
        cadences.add(new int[] {1, 4, 0});
        cadences.add(new int[] {3, 4, 0});
        cadences.add(new int[] {0, 4});
        cadences.add(new int[] {1, 4});
        cadences.add(new int[] {3, 4});
        cadences.add(new int[] {4, 0});
        cadences.add(new int[] {4, 1});
        cadences.add(new int[] {4, 5});
    }

    public static int[] getRandomCadence() {
        return cadences.get(random.nextInt(cadences.size()));
    }
}
