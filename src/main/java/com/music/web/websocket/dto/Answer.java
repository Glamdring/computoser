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

package com.music.web.websocket.dto;

public class Answer {
    private int tempo = -1;
    private int mainInstrument;
    private int metreNumerator;
    private int metreDenominator;

    public int getTempo() {
        return tempo;
    }
    public void setTempo(int tempo) {
        this.tempo = tempo;
    }
    public int getMainInstrument() {
        return mainInstrument;
    }
    public void setMainInstrument(int mainInstrument) {
        this.mainInstrument = mainInstrument;
    }
    public int getMetreNumerator() {
        return metreNumerator;
    }
    public void setMetreNumerator(int metreNumerator) {
        this.metreNumerator = metreNumerator;
    }
    public int getMetreDenominator() {
        return metreDenominator;
    }
    public void setMetreDenominator(int metreDenominator) {
        this.metreDenominator = metreDenominator;
    }
}