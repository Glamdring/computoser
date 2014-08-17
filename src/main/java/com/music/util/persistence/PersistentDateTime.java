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

package com.music.util.persistence;

import java.util.Properties;

import org.joda.time.DateTimeZone;


/**
 * Class simply used to support switching to multiple types without changing mapping.
 * Current type implementation: jadira.usertype
 * @author bozho
 */
public class PersistentDateTime extends org.jadira.usertype.dateandtime.joda.PersistentDateTime {

    private static final long serialVersionUID = 6517203034160316166L;

    @Override
    public void setParameterValues(Properties parameters) {
        // the type doesn't use the default joda-time timezone, but uses the JVM one instead.
        if (parameters == null) {
            parameters = new Properties();
        }
        parameters.setProperty("databaseZone", DateTimeZone.getDefault().getID());
        super.setParameterValues(parameters);
    }
}
