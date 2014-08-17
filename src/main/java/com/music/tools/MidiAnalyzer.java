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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import jm.constants.Scales;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Score;
import jm.music.tools.PhraseAnalysis;
import jm.util.Play;
import jm.util.Read;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class MidiAnalyzer {

    public static void main(String[] args) {
        Score score = new Score();
        Read.midi(score, "C:\\workspace\\music\\analysis\\midi\\jarre\\EQUINOX3.MID");
        for (Part part : score.getPartArray()) {
            System.out.println(part.getTitle() + " : " + part.getInstrument());
        }
        Part part = score.getPart(1);

        System.out.println(part.getInstrument());
        part.setTempo(160);
        int previousPitch = 0;
        int prePreviousPitch = 0;
        System.out.println(score.getTimeSignature());
        Multiset<Integer> uniqueIntervals = HashMultiset.create();
        int directionChanges = 0;
        int directionRetentions = 0;

        LinkedList<Double> noteLengths = new LinkedList<>();
        for (Note note : part.getPhrase(0).getNoteArray()) {
            System.out.println(note.getPitch());
            if (!note.isRest()) {
                if (prePreviousPitch != 0) {
                    int previousDiff = previousPitch - prePreviousPitch;
                    int diff = note.getPitch() - previousPitch;
                    if (Math.signum(previousDiff) != Math.signum(diff) && diff != 0 && previousDiff != 0) {
                        directionChanges++;
                        System.out.println(prePreviousPitch + ":" + previousPitch + ":" + note.getPitch());
                    } else if (diff != 0 && previousDiff != 0) {
                        directionRetentions++;
                    }
                }
                if (note.getPitch() - previousPitch != 0) {
                    prePreviousPitch = previousPitch;
                }

                uniqueIntervals.add(previousPitch - note.getPitch());
                previousPitch = note.getPitch();
            }
            noteLengths.add(note.getRhythmValue());
        }

        double normalizedBeatSize = 1d * score.getNumerator() * 4 / score.getDenominator();
        System.out.println("Beat size: " + normalizedBeatSize);
        double currentBeatSize = 0;
        int beats = 0;
        int beatsWithPerfectHalves = 0;
        // reverse, to avoid off-beats
        for (Iterator<Double> it = noteLengths.descendingIterator(); it.hasNext();) {
            currentBeatSize += it.next();;
            if (currentBeatSize >= normalizedBeatSize) {
                currentBeatSize = 0;
                beats++;
            }
            if (currentBeatSize == normalizedBeatSize / 2) {
                beatsWithPerfectHalves++;
            }
        }

        System.out.println("Beats:beats with perfect halves -- " + beats + ":" + beatsWithPerfectHalves);

        Hashtable<String, Object> table = PhraseAnalysis.getAllStatistics(score.getPart(1).getPhrase(0), 1, 0, Scales.MAJOR_SCALE);
        for (Entry<String, Object> entry : table.entrySet()) {
            System.out.println(entry.getKey() + "=" + entry.getValue());
        }
        for (Integer interval : uniqueIntervals.elementSet()) {
            System.out.println(interval + " : " + uniqueIntervals.count(interval));
        }

        System.out.println("---");

        System.out.println(directionChanges + " : " + directionRetentions);
        Play.midi(part);
    }
}
