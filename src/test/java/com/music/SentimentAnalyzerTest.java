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

package com.music;

import org.junit.Test;
import static org.junit.Assert.*;

import com.google.common.collect.Sets;
import com.music.model.persistent.TimelineMusic;
import com.music.service.text.SentimentAnalyzer;
import com.music.service.text.SentimentAnalyzer.SentimentResult;

public class SentimentAnalyzerTest {

    @Test
    public void testSentimentAnalyzer() {
        SentimentAnalyzer analyzer = new SentimentAnalyzer();
        analyzer.init();

        TimelineMusic meta = new TimelineMusic();
        SentimentResult result = analyzer.getSentiment(Sets.newHashSet("This sucks", "Too bad it didn't happen", "Not cool, man"), meta);
        assertEquals(SentimentResult.NEGATIVE, result);

        result = analyzer.getSentiment(Sets.newHashSet("Whatever", "I'm coming tomorrow by bus", "Three cities participated in the campagin"), meta);
        assertEquals(SentimentResult.NEUTRAL, result);

        result = analyzer.getSentiment(Sets.newHashSet("Brilliant job", "Nice one, mate", "I really appreciate your effort"), meta);
        assertEquals(SentimentResult.POSITIVE, result);
    }
}
