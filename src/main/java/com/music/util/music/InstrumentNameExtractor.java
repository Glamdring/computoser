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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import jm.constants.ProgramChanges;

import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstrumentNameExtractor {
    private static final Logger logger = LoggerFactory.getLogger(InstrumentNameExtractor.class);

    private static Map<Integer, String> names = new HashMap<>();
    static {
        try {
            Field[] fields = ProgramChanges.class.getDeclaredFields();
            for (Field field : fields) {
                Integer id = (Integer) field.get(null);
                if (!names.containsKey(id)) {
                    names.put(id, WordUtils.capitalizeFully(field.getName()).replace('_', ' '));
                }
            }

        } catch (Exception ex) {
            logger.warn("Failed to extract instrument names", ex);
        }
    }
    public static String getInstrumentName(int instrument) {
        return names.get(instrument);
    }
}
