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

package com.music.tools;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import jm.constants.ProgramChanges;
import jm.music.data.Part;
import jm.music.data.Score;
import jm.util.Read;

import org.apache.commons.lang3.StringUtils;

public class InstrumentExtractor {

    public static void main(String[] args) {
        Map<Integer, String> instrumentNames = new HashMap<>();
        Field[] fields = ProgramChanges.class.getDeclaredFields();
        try {
            for (Field field : fields) {
                Integer value = (Integer) field.get(null);
                if (!instrumentNames.containsKey(value)) {
                    instrumentNames.put(value,
                            StringUtils.capitalize(field.getName().toLowerCase()).replace('_', ' '));
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        Score score = new Score();
        Read.midi(score, "C:\\Users\\bozho\\Downloads\\7938.midi");
        for (Part part : score.getPartArray()) {
            System.out.println(part.getChannel() + " : " + part.getInstrument() + ": " + instrumentNames.get(part.getInstrument()));
        }
    }
}
