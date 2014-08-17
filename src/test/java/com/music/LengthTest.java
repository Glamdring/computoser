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

import java.util.ArrayList;
import java.util.List;

import jm.JMC;
import jm.constants.Pitches;
import jm.constants.RhythmValues;
import jm.music.data.Note;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.music.MainPartGenerator.MainPartContext;
import com.music.model.Scale;

public class LengthTest {

    MainPartGenerator generator = new MainPartGenerator();
    MainPartContext lCtx = new MainPartContext();

    @Before
    public void init() {
        lCtx.setScoreContext(new ScoreContext());
        lCtx.setCurrentScale(Scale.MAJOR);
    }

    @Test
    public void lengthToNextDownBeatSimpleMetreTest() {
        lCtx.getScoreContext().setMetre(new int[] {2, 4});
        lCtx.setNormalizedMeasureSize(MetreConfigurer.getNormalizedMeasureSize(lCtx.getScoreContext().getMetre()));

        lCtx.setCurrentMeasureSize(0);
        assertProperLength(2, 2);
        lCtx.setCurrentMeasureSize(0);
        assertProperLength(2, 3);

        lCtx.setCurrentMeasureSize(0.5);
        assertProperLength(1.5, 2);
        lCtx.setCurrentMeasureSize(0.5);
        assertProperLength(1, 1);
        lCtx.setCurrentMeasureSize(0.5);
        assertProperLength(1.5, 1.5);
    }

    @Test
    public void lengthToNextDownBeatFourQuartersTest() {
        lCtx.getScoreContext().setMetre(new int[] {4, 4});
        lCtx.setNormalizedMeasureSize(MetreConfigurer.getNormalizedMeasureSize(lCtx.getScoreContext().getMetre()));

        lCtx.setCurrentMeasureSize(0);
        assertProperLength(2, 3);
        lCtx.setCurrentMeasureSize(0);
        assertProperLength(1.5, 1.5);

        lCtx.setCurrentMeasureSize(1.5);
        assertProperLength(0.5, 3);
        lCtx.setCurrentMeasureSize(1.5);
        assertProperLength(0.25, 0.25);

        lCtx.setCurrentMeasureSize(2);
        assertProperLength(2, 2.5);

        lCtx.setCurrentMeasureSize(2.5);
        assertProperLength(1.5, 1.75);

        lCtx.setCurrentMeasureSize(2.5);
        assertProperLength(1, 1);
    }

    @Test
    public void lengthToNextDownBeatEightSixteenthsTest() {
        lCtx.getScoreContext().setMetre(new int[] {8, 16});
        lCtx.setNormalizedMeasureSize(MetreConfigurer.getNormalizedMeasureSize(lCtx.getScoreContext().getMetre()));

        lCtx.setCurrentMeasureSize(0);
        assertProperLength(2 * JMC.SIXTEENTH_NOTE, 2 * JMC.SIXTEENTH_NOTE);
        lCtx.setCurrentMeasureSize(0);
        assertProperLength(4 * JMC.SIXTEENTH_NOTE, 5 * JMC.SIXTEENTH_NOTE);

        lCtx.setCurrentMeasureSize(5 * JMC.SIXTEENTH_NOTE);
        assertProperLength(3 * JMC.SIXTEENTH_NOTE, 5 * JMC.SIXTEENTH_NOTE);
        lCtx.setCurrentMeasureSize(5 * JMC.SIXTEENTH_NOTE);
        assertProperLength(JMC.EIGHTH_NOTE, JMC.EIGHTH_NOTE);
    }

    @Test
    public void lengthToNextDownBeatSixEightsTest() {
        lCtx.getScoreContext().setMetre(new int[] {6, 8});
        lCtx.setNormalizedMeasureSize(MetreConfigurer.getNormalizedMeasureSize(lCtx.getScoreContext().getMetre()));

        lCtx.setCurrentMeasureSize(0);
        assertProperLength(2 * JMC.EIGHTH_NOTE, 3 * JMC.EIGHTH_NOTE);

        lCtx.setCurrentMeasureSize(0);
        assertProperLength(JMC.EIGHTH_NOTE, JMC.EIGHTH_NOTE);

        lCtx.setCurrentMeasureSize(1 * JMC.EIGHTH_NOTE);
        assertProperLength(JMC.EIGHTH_NOTE, 2 * JMC.EIGHTH_NOTE);

        lCtx.setCurrentMeasureSize(3 * JMC.EIGHTH_NOTE);
        assertProperLength(JMC.EIGHTH_NOTE, 2 * JMC.EIGHTH_NOTE);

        lCtx.setCurrentMeasureSize(5 * JMC.EIGHTH_NOTE + JMC.SIXTEENTH_NOTE);
        assertProperLength(JMC.SIXTEENTH_NOTE, 2 * JMC.EIGHTH_NOTE);
    }

    @Test
    public void lengthToNextDownBeatSevenEightsTest() {
        lCtx.getScoreContext().setMetre(new int[] {7, 8});
        lCtx.setNormalizedMeasureSize(MetreConfigurer.getNormalizedMeasureSize(lCtx.getScoreContext().getMetre()));

        lCtx.setCurrentMeasureSize(0);
        assertProperLength(2 * JMC.EIGHTH_NOTE, 3 * JMC.EIGHTH_NOTE);

        lCtx.setCurrentMeasureSize(1 * JMC.EIGHTH_NOTE);
        assertProperLength(JMC.EIGHTH_NOTE, 2 * JMC.EIGHTH_NOTE);

        lCtx.setCurrentMeasureSize(3 * JMC.EIGHTH_NOTE);
        assertProperLength(JMC.EIGHTH_NOTE, 2 * JMC.EIGHTH_NOTE);

        lCtx.setCurrentMeasureSize(5 * JMC.EIGHTH_NOTE);
        assertProperLength(2 * JMC.EIGHTH_NOTE, 2 * JMC.QUARTER_NOTE);
    }

    @Test
    public void retrogradeTest() {
        MainPartGenerator gen = new MainPartGenerator();
        List<Note> list = new ArrayList<>();
        list.add(new Note(Pitches.C4, RhythmValues.EIGHTH_NOTE));
        list.add(new Note(Pitches.C5, RhythmValues.QUARTER_NOTE));
        list.add(new Note(Pitches.C6, RhythmValues.HALF_NOTE));
        gen.retrograde(list);
        Assert.assertEquals(RhythmValues.EIGHTH_NOTE, list.get(0).getRhythmValue(), 0);
        Assert.assertEquals(RhythmValues.QUARTER_NOTE, list.get(1).getRhythmValue(), 0);
        Assert.assertEquals(RhythmValues.HALF_NOTE, list.get(2).getRhythmValue(), 0);

        Assert.assertEquals(Pitches.C6, list.get(0).getPitch());
        Assert.assertEquals(Pitches.C5, list.get(1).getPitch());
        Assert.assertEquals(Pitches.C4, list.get(2).getPitch());
    }

    private void assertProperLength(double expected, double desired) {
        Assert.assertEquals(expected, generator.getProperLengthAndUpdateContext(lCtx, desired), 0);
    }
}
