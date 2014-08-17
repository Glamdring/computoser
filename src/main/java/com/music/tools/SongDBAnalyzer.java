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

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.google.common.collect.Sets;

public class SongDBAnalyzer {

    private static final DecimalFormat df = new DecimalFormat("#.##");

    public static void main(String[] args) throws Exception {
        File dir = new File("C:\\workspace\\music\\analysis\\db");
        File[] files = dir.listFiles();

        XMLReader xr = XMLReaderFactory.createXMLReader();
        NoteContentHandler handler = new NoteContentHandler();
        xr.setContentHandler(handler);
        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                try {
                    xr.parse(new InputSource(reader));
                    handler.getNotes().add(null);
                } catch (Exception ex) {
                    System.out.println(file.getName());
                    throw ex;
                }
            }
        }
        List<NoteElement> noteList = handler.getNotes();

        int[] degreeValue = new int[] {0,0,0,0,0,0,0};
        int[] degreeBeforePause = new int[] {0,0,0,0,0,0,0};
        int[] lengths = new int[1000];
        int[] unisonLengths = new int[1000];
        int[] intervals = new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0};
        int[] firstInMeasure = new int[] {0,0,0,0,0,0,0};
        int[] lastInMeasure = new int[] {0,0,0,0,0,0,0};
        int[][] lengthsPerDegree = new int[7][1000];
        int[][] nextNotes = new int[7][7];
        int measures = 0;
        int notes = 0;
        int stable = 0;
        int unstable = 0;
        NoteElement previousNote = null;
        int currentNoteCount = 0;
        List<Integer> noteCounts = new ArrayList<Integer>();
        for (NoteElement note : noteList) {
            if (note == null) {
                previousNote = null;
                noteCounts.add(currentNoteCount);
                currentNoteCount = 0;
                continue;
            } else {
                currentNoteCount++;
            }

            if (note.isRest() && previousNote != null) {
                degreeBeforePause[previousNote.getScaleDegree()-1]++;
            }
            if (!note.isRest() && !note.isFlat() && !note.isSharp()) {
                notes++;
                degreeValue[note.getScaleDegree()-1]++;
                lengths[(int) (note.getNoteLength() * 100)]++;
                lengthsPerDegree[note.getScaleDegree()-1][(int)(note.getNoteLength() * 100)]++;
                if (previousNote != null) {
                    nextNotes[previousNote.getScaleDegree()-1][note.getScaleDegree()-1]++;
                    int currentNormalized = note.getScaleDegree() + note.getOctave() * 7;
                    int previousNormalized = previousNote.getScaleDegree() + previousNote.getOctave() * 7;
                    int interval = currentNormalized - previousNormalized;
                    int idx = Math.abs(interval);
                    if (intervals.length > idx) {
                        intervals[idx]++;
                    }
                    if (interval == 0) {
                        unisonLengths[(int) (note.getNoteLength() * 100)]++;
                    }
                }

                if (previousNote == null || previousNote.getStartMeasure() != note.getStartMeasure()) {
                    if (previousNote != null) {
                        lastInMeasure[previousNote.getScaleDegree() - 1]++;
                    }
                    firstInMeasure[note.getScaleDegree() - 1] ++;
                    measures++;
                }
                if (note.getScaleDegree() == 1 || note.getScaleDegree() == 3 || note.getScaleDegree() == 5) {
                    stable ++;
                } else {
                    unstable ++;
                }
                previousNote = note;
            }
        }

        System.out.println("Degrees: " + Arrays.toString(percentages(degreeValue)));
        System.out.println("Degrees before pause: " + Arrays.toString(percentages(degreeBeforePause)));
        System.out.println("Intervals: " + Arrays.toString(percentages(intervals)));
        System.out.println("First note in measure: " + Arrays.toString(percentages(firstInMeasure)));
        System.out.println("Last note in measure: " + Arrays.toString(percentages(lastInMeasure)));
        System.out.println("Lengths: " + Arrays.toString(percentages(lengths, true, true)));
        System.out.println("Unison lengths: " + Arrays.toString(percentages(unisonLengths, true, true)));
        System.out.println("Stable:Unstable = " + stable + ":" + unstable);
        System.out.println("Notes/measure = " + (1d*notes/measures));
        for (int i = 0; i < 7; i++) {
            System.out.println("Lengths for degree " + (i+1));
            double degreeTotal = 0;
            for (int length = 0; length < lengthsPerDegree[i].length; length++) {
                degreeTotal += lengthsPerDegree[i][length];
            }
            for (int length = 0; length < lengthsPerDegree[i].length; length++) {
                if (lengthsPerDegree[i][length] > 0) {
                    System.out.print(length + "=" + df.format((lengthsPerDegree[i][length]) / degreeTotal * 100));
                    System.out.print("; ");
                }
            }
            System.out.println();
            System.out.println();
        }
        System.out.println("-----------------------");
        System.out.println();

        for (int i = 0; i < 7; i++) {
            System.out.println("Next notes for degree " + (i+1));
            double degreeTotal = 0;
            for (int k = 0; k < nextNotes[i].length; k++) {
                degreeTotal += nextNotes[i][k];
            }
            for (int k = 0; k < nextNotes[i].length; k++) {
                System.out.print((k+1) + "=" + df.format((nextNotes[i][k]) / degreeTotal * 100));
                System.out.print("; ");
            }
            System.out.println();
            System.out.println();
        }

        double sum = 0;
        int noteCountSize = 0;
        for (int count : noteCounts) {
            sum += count;
            if (count != 0) {
                noteCountSize ++;
            }
        }
        System.out.println(noteCounts);
        System.out.println("Average notes per piece: " + df.format(sum / noteCountSize));
    }

    public static String[] percentages(int[] array) {
        return percentages(array, false, false);
    }
    public static String[] percentages(int[] array, boolean excludeZero, boolean includeIdx) {
        double total = 0;
        List<String> result = new ArrayList<>();
        for (int val : array) {
            total += val;
        }
        for (int i = 0; i < array.length; i++) {
            double value = array[i] / total * 100;
            if (excludeZero && value == 0) {
                continue;
            }
            String prefix = "";
            if (includeIdx) {
                prefix += i + "=";
            }
            result.add(prefix + df.format(value));
        }
        return result.toArray(new String[result.size()]);
    }

    public static class NoteContentHandler extends DefaultHandler {
        private List<NoteElement> notes = new ArrayList<>();
        private NoteElement currentNote;
        private Set<String> noteProperties = Sets.newHashSet("start_beat_abs", "start_measure", "start_beat",
                "note_length", "scale_degree", "octave", "isRest");
        private String currentProperty;
        private StringBuilder currentPropertyValue = new StringBuilder();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if (localName.equals("note")) {
                currentNote = new NoteElement();
            }
            if (noteProperties.contains(localName)) {
                currentProperty = localName;
            }

        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (currentProperty != null && currentNote != null) {
                currentPropertyValue.append(Arrays.copyOfRange(ch, start, start+length));
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (localName.equals("note") && currentNote != null) {
                notes.add(currentNote);
                currentNote = null;
            }
            if (noteProperties.contains(localName) && currentNote != null) {
                //System.out.println(currentProperty + " : " + currentPropertyValue.toString().trim());

                if (currentProperty.equals("start_beat_abs")) {
                    currentNote.setStartBeatAbs(Double.parseDouble(currentPropertyValue.toString().trim()));
                }
                if (currentProperty.equals("start_measure")) {
                    currentNote.setStartMeasure(Double.parseDouble(currentPropertyValue.toString().trim()));
                }
                if (currentProperty.equals("start_beat")) {
                    currentNote.setStartBeat(Double.parseDouble(currentPropertyValue.toString().trim()));
                }
                if (currentProperty.equals("note_length")) {
                    currentNote.setNoteLength(Double.parseDouble(currentPropertyValue.toString().trim()));
                }
                if (currentProperty.equals("scale_degree")) {
                    String value = currentPropertyValue.toString().trim();
                    if (value.equalsIgnoreCase("rest")) {
                        currentNote.setScaleDegree(-1);
                    } else {
                        if (value.endsWith("f")) {
                            currentNote.setFlat(true);
                            value = value.substring(0, value.length() - 1);
                        } else if (value.endsWith("s")) {
                            currentNote.setSharp(true);
                            value = value.substring(0, value.length() - 1);
                        }
                        currentNote.setScaleDegree(Integer.parseInt(value));
                    }
                }
                if (currentProperty.equals("octave")) {
                    currentNote.setOctave(Integer.parseInt(currentPropertyValue.toString().trim()));
                }
                if (currentProperty.equals("isRest")) {
                    currentNote.setRest(currentPropertyValue.toString().trim().equals("1") ? true : false);
                }
                currentPropertyValue = new StringBuilder();
                currentProperty = null;
            }
        }

        public List<NoteElement> getNotes() {
            return notes;
        }
    }
}