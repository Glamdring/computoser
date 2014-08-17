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

package com.music.model;

import java.util.Map;

import com.google.common.collect.Maps;

public enum ToneType {
    TONIC(0), SUPERTONIC(1), MEDIANT(2), SUBDOMINANT(3), DOMINANT(4), SUBMEDIANT(5), LEADING_OR_SUBTONIC(6);
    private static final Map<Integer, ToneType> BY_DEGREE = Maps.newHashMap();
    static {
        BY_DEGREE.put(0, TONIC);
        BY_DEGREE.put(1, SUPERTONIC);
        BY_DEGREE.put(2, MEDIANT);
        BY_DEGREE.put(3, SUBDOMINANT);
        BY_DEGREE.put(4, DOMINANT);
        BY_DEGREE.put(5, SUBMEDIANT);
        BY_DEGREE.put(6, LEADING_OR_SUBTONIC);
    }
    private final int degree;
    private ToneType(int degree) {
        this.degree = degree;
    }
    public int getDegree() {
        return degree;
    }
    public static ToneType forDegree(int degree) {
        return BY_DEGREE.get(degree);
    }
}