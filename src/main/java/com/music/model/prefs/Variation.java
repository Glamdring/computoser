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

package com.music.model.prefs;

public enum Variation {
    ANY(0, 22), MONOTONOUS(0, 2), AVERAGE(2, 5), MOVING(5, 7.5), VERY_VARIATIVE(7.5, 10), EXTREMELY_VARIATIVE(10, 22);
    private final double from;
    private final double to;
    private Variation(double from, double to) {
        this.from = from;
        this.to = to;
    }
    public double getFrom() {
        return from;
    }
    public double getTo() {
        return to;
    }
}
