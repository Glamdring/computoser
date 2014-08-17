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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameResults {
    private Map<String, Integer> scores = new LinkedHashMap<>();
    private List<String> ranking = new ArrayList<>();

    public Map<String, Integer> getScores() {
        return scores;
    }
    public void setScores(Map<String, Integer> scores) {
        this.scores = scores;
    }
    public List<String> getRanking() {
        return ranking;
    }
    public void setRanking(List<String> ranking) {
        this.ranking = ranking;
    }
}