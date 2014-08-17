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

public enum Tempo {
    ANY(45, 185), VERY_SLOW(45,70), SLOW(71,95), MEDIUM(96,120), FAST(121, 145), VERY_FAST(146, 185);
    private final int from;
    private final int to;

    private Tempo(int from, int to) {
        this.from = from;
        this.to = to;
    }
    public int getFrom() {
        return from;
    }
    public int getTo() {
        return to;
    }
    public static Tempo forValue(int tempo) {
        for (Tempo t : values()) {
            if (t != ANY && t.getFrom() <= tempo && tempo <= t.getTo()) {
                return t;
            }
        }
        return null;
    }
}
