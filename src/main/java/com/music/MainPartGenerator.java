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

import static com.music.util.music.ToneResolver.isInScale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.music.model.Chord;
import com.music.model.Contour;
import com.music.model.ExtendedPhrase;
import com.music.model.InstrumentGroups;
import com.music.model.PartType;
import com.music.model.Scale;
import com.music.model.SpecialNoteType;
import com.music.model.ToneType;
import com.music.model.prefs.UserPreferences;
import com.music.tools.SongDBAnalyzer;
import com.music.util.music.Cadences;
import com.music.util.music.Chance;
import com.music.util.music.ChordUtils;
import com.music.util.music.NoteFactory;
import com.music.util.music.ToneResolver;

import jm.JMC;
import jm.constants.Pitches;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Rest;
import jm.music.data.Score;
import jm.music.tools.Mod;

public class MainPartGenerator implements ScoreManipulator {
    private static final Logger logger = LoggerFactory.getLogger(MainPartGenerator.class);

    private Random random = new Random();
    private static final int[] PROGRESS_TYPE_PERCENTAGES = new int[] {25, 48, 25, 2};
    private static final int[] INTERVAL_SPEC_PERCENTAGES = new int[] {20, 20, 35, 10, 15};
    private static final int[] NOTE_LENGTH_PERCENTAGES = new int[] {10, 31, 40, 7, 9, 3};

    @Override
    public void handleScore(Score score, ScoreContext ctx, UserPreferences prefs) {
        // TODO http://en.wikipedia.org/wiki/Tonicization (+ secondary dominant)
        // http://en.wikipedia.org/wiki/Nonchord_tone (All types of tones below)
        // http://en.wikipedia.org/wiki/Counterpoint allow for a secondary main part instead of accompaniment (counterpoint part should follow main part somehow)
        // http://www.solomonsmusic.net/vartech.htm
        // http://dolmetsch.com/form.pdf

        logger.debug("Generating piece with ctx=" + ctx);

        Part mainPart = ctx.getParts().get(PartType.MAIN);

        MainPartContext lCtx = initLocalContext(score, ctx);

        // used for representing the structure of the phrase, for debugging purposes
        StringBuilder structureItem = new StringBuilder();

        // some empty initial measures
        addInitialMeasures(lCtx, mainPart);

        // TODO add upBeat
        while(lCtx.getTotalMeasures() + lCtx.getCurrentPhraseMeasuresCounter() < ctx.getMeasures()) {

            if (lCtx.getCurrentPhrase() == null) {
                createNewPhrase(lCtx, mainPart);
            }
            initializeMeasure(lCtx);

            boolean downBeat = lCtx.isNextNoteDownBeat(); //getting here, because it will get overridden when we calculate the length

            if (lCtx.getCurrentPhraseMeasuresCounter() >= lCtx.getPhraseMeasures() && lCtx.getCurrentMeasureSize() == 0 && lCtx.getTotalLength() > 0) {
                //stop repeating the theme and end the phrase
                lCtx.getCurrentPhrase().setNotes(lCtx.getPitches().size());
                resetPhrase(lCtx);
                continue;
            }

            // in-phrase repetitions (motifs)
            if (lCtx.getCurrentPhraseMeasuresCounter() > 0 && lCtx.getCurrentPhraseMeasuresCounter() < lCtx.getPhraseMeasures()) {
                if (!lCtx.getCurrentPhraseMotifs().isEmpty() && Chance.test(40) && lCtx.getCurrentMeasureSize() == 0) {
                    lCtx.getCurrentPhrase().getStructure().add(structureItem.toString());
                    structureItem = new StringBuilder();

                    repeatExistingMotifWithinPhrase(lCtx);
                    lCtx.setMeasuresSinceLastMotif(0);
                } else if (shouldRepeatNewMotif(lCtx)) {
                    lCtx.getCurrentPhrase().getStructure().add(structureItem.toString());
                    structureItem = new StringBuilder();

                    repeatNewMotifWithinPhrase(lCtx);
                    lCtx.setMeasuresSinceLastMotif(0);
                } else {
                    // count the number of measures since the last motif repetition
                    if (lCtx.getCurrentMeasureSize() == 0) {
                        lCtx.setMeasuresSinceLastMotif(lCtx.getMeasuresSinceLastMotif() + 1);
                    }
                    if (Chance.test(17)) {
                        repeatNotesWithinPhrase(lCtx);
                        structureItem.append("--Repeat--");
                    }
                }
            }

            handleSpecialNotes(lCtx);

            handleContour(lCtx);

            boolean useInterval = false;
            boolean terraceContourChange = lCtx.getContour() == Contour.TERRACE && lCtx.getCurrentMeasureSize() == 0 && lCtx.getCurrentPhraseMeasuresCounter() % 2 == 0 && lCtx.getCurrentPhraseMeasuresCounter() != 0;
            if (terraceContourChange) {
                useInterval = true;
                lCtx.setDirectionUp(!lCtx.isDirectionUp());
            }

            //TODO add two-note chords (simultaneous intervals). The logic should contain resolving dissonant and unstable sim.intervals to consonant and stable.
            lCtx.setNotePitch(getNextNotePitch(lCtx, useInterval));

            // restore the direction after the terrace jump
            if (terraceContourChange) {
                lCtx.setDirectionUp(!lCtx.isDirectionUp());
            }
            //TODO climax - around 2/3 and 3/4

            double length = getLength(lCtx);
            lCtx.setLength(length);
            lCtx.getPitches().add(lCtx.getNotePitch());
            lCtx.getUniquePitches().add(lCtx.getNotePitch());

            // use pause in a couple of cases at random, and also in the middle and at the end of the theme
            boolean usePause = Chance.test(11)
                    || (lCtx.isEndOfMeasure() == true
                            && lCtx.getCurrentPhraseMeasuresCounter() == lCtx.getPhraseMeasures() / 2 && Chance
                                .test(70))
                    || (lCtx.getCurrentPhraseMeasuresCounter() == lCtx.getPhraseMeasures() && Chance
                            .test(75));

            if (usePause) {
                lCtx.getCurrentPhrase().addRest(new Rest(length));
                structureItem.append("-R-");
            } else {
                Note note = createNote(lCtx, downBeat, length);

                if (lCtx.isAllowOrnaments() && Chance.test(6)) {
                    addOrnamentedNote(lCtx, length, note);
                } else {
                    lCtx.getCurrentPhrase().addNote(note);
                }

                structureItem.append("-" + note.getPitch() + "-");
            }
        }

        // add the cadences to the final phrase, if the above loop has been interrupted due to the end of the piece
        if (lCtx.getCurrentPhrase() != null) {
            lCtx.getCurrentPhrase().getStructure().add(structureItem.toString());
            int endingMeasures = addEnding(lCtx.getCurrentPhrase(), ctx);
            lCtx.setCurrentPhraseMeasuresCounter(lCtx.getCurrentPhraseMeasuresCounter() + endingMeasures);
            lCtx.setTotalMeasures(lCtx.getTotalMeasures() + lCtx.getCurrentPhraseMeasuresCounter());
            lCtx.getCurrentPhrase().setMeasures(lCtx.getCurrentPhraseMeasuresCounter());
        }

        List<ExtendedPhrase> phrases = repeatPhrases(lCtx);

        // override with the possibly changed list of phrases
        lCtx.setPhrases(phrases);

        // if any (slight) modification has been carried out to the measure count, update it here
        ctx.setMeasures(lCtx.getTotalMeasures());

        // add the phrases to the main part
        mainPart.addPhraseList(lCtx.getPhrases().toArray(new ExtendedPhrase[lCtx.getPhrases().size()]));
        // transpose to the desired key
        Mod.transpose(mainPart, ctx.getKeyNote());

        if (ctx.getParts().containsKey(PartType.MAIN_DUPLICATE)) {
            Part duplicateMainPart = ctx.getParts().get(PartType.MAIN_DUPLICATE);
            duplicateMainPart.addPhraseList(mainPart.getPhraseArray());
            if (Chance.test(45) || duplicateMainPart.getInstrument() == mainPart.getInstrument()) {
                Mod.transpose(duplicateMainPart, -12); // an octave lower
            }
        }

        applyExtras(ctx);
        ctx.setVariation(calculateVariation(mainPart));
        //printDegreePercentages(mainPart, currentScale); used only for statistics
        if (logger.isDebugEnabled()) {
            printStructure(lCtx);
        }
    }

    private void initializeMeasure(MainPartContext lCtx) {
        if (lCtx.getCurrentMeasureSize() == lCtx.getNormalizedMeasureSize()) {
            lCtx.setCurrentMeasureSize(0);
            lCtx.setEndOfMeasure(false); // also set in getProperLengthAnd..., but it logically fits here as well
            lCtx.setCurrentPhraseMeasuresCounter(lCtx.getCurrentPhraseMeasuresCounter() + 1);
        } else if (lCtx.getCurrentMeasureSize() > lCtx.getNormalizedMeasureSize()) {
            logger.warn("Detected measure longer than the allowed value");
        }
    }

    public MainPartContext initLocalContext(Score score, ScoreContext ctx) {
        MainPartContext lCtx = new MainPartContext();
        lCtx.setScoreContext(ctx);
        lCtx.setNormalizedMeasureSize(ctx.getNormalizedMeasureSize());
        lCtx.setDirectionUp(Chance.test(65));
        lCtx.setContour(chooseContour());
        lCtx.setCurrentScale(ctx.getScale());
        lCtx.setSimplePhrases(Chance.test(12));
        ctx.setSimplePhrases(lCtx.isSimplePhrases());
        lCtx.setContourChangeNotes(8 + random.nextInt(5));
        lCtx.setAllowOrnaments(score.getTempo() > 75 && score.getTempo() < 120 && Chance.test(19));
        ctx.setOrnamented(lCtx.isAllowOrnaments());

        lCtx.setAllowSyncopation(!ctx.isElectronic() && Chance.test(25));

        if (Chance.test(2)) {
            lCtx.setAllowMoreDissonance(true);
            ctx.setDissonant(true);
        }
        if (Chance.test(5)) {
            lCtx.setDominantSpecialNoteType(SpecialNoteType.values()[random.nextInt(SpecialNoteType.values().length)]);
        }
        return lCtx;
    }

    private void applyExtras(ScoreContext ctx) {
        if (Chance.test(18) && !ctx.isElectronic()) {
            //rubato(ctx.getScore());
        }
    }

    // slight random variation in note lengths
    private void rubato(Score score) {
        for (Part part : score.getPartArray()) {
            for (Phrase phrase : part.getPhraseArray()) {
                double lengthChange = 0;
                int rubatoNoteCount = 0;
                int rubatoNotesRemaining = 0;
                int i = 0;
                Note[] notes = phrase.getNoteArray();
                int total = notes.length;
                for (Note note : notes) {
                    if (rubatoNotesRemaining == 0 && Chance.test(10)) {
                        rubatoNoteCount = (1 + random.nextInt(5)) * 2;
                        // only apply rubato if there are enough notes till the end of the phrase
                        rubatoNoteCount = Math.min(rubatoNoteCount, (total - i) * 2);
                        rubatoNotesRemaining = rubatoNoteCount;
                        lengthChange = (random.nextBoolean() ? -1 : 1) * random.nextInt(20) / 100d;
                    }
                    if (rubatoNotesRemaining > 0) {
                        double change = lengthChange;
                        if (rubatoNotesRemaining <= rubatoNoteCount / 2) {
                            change = -change;
                        }
                        note.setRhythmValue(note.getRhythmValue() - change);
                        rubatoNotesRemaining--;
                    }
                    i++;
                }
            }
        }
    }

    private Note createNote(MainPartContext lCtx, boolean downBeat, double length) {
        Note note = NoteFactory.createNote(lCtx.getNotePitch(), length);
        note.setDynamic(getNextNoteDynamics(lCtx, downBeat));

        if (lCtx.getSpecialNoteType() != null) {
            note.setDuration(note.getRhythmValue() * lCtx.getSpecialNoteType().getValue());
        } else if (Chance.test(8)) { // in 8% of the cases, choose this note to be special by its own
            note.setDuration(note.getRhythmValue() * SpecialNoteType.values()[random.nextInt(SpecialNoteType.values().length)].getValue());
        } else if (lCtx.getDominantSpecialNoteType() != null) {
            note.setDuration(note.getRhythmValue() * lCtx.getDominantSpecialNoteType().getValue());
        }
        return note;
    }

    private List<ExtendedPhrase> repeatPhrases(MainPartContext lCtx) {
        // repeat some phrases (and place them at random positions)
        List<ExtendedPhrase> phrases = new ArrayList<>(lCtx.getPhrases());
        for (ExtendedPhrase phrase : lCtx.getPhrases()) {
            if (Chance.test(35)) {
                logger.debug("Repeating phrase");
                int repetitions = 1 + random.nextInt(lCtx.isSimplePhrases() ? 3 : 2);
                for (int i = 0; i < repetitions; i ++) {
                    ExtendedPhrase copy = phrase.copy();
                    if (Chance.test(15)) {
                        copy.setInstrument(InstrumentGroups.MAIN_PART_INSTRUMENTS[random.nextInt(InstrumentGroups.MAIN_PART_INSTRUMENTS.length)]);
                    }
                    phrases.add(random.nextInt(phrases.size()), copy);
                    lCtx.setTotalMeasures(lCtx.getTotalMeasures() + phrase.getMeasures());
                }
            } else {
                logger.debug("Not repeating phrase");
            }
        }
        return phrases;
    }

    private void addOrnamentedNote(MainPartContext lCtx, double length, Note note) {
        int count = (int) (length / JMC.THIRTYSECOND_NOTE);
        boolean up = Chance.test(60);
        boolean inScale = Chance.test(75);
        int pitchChange = up ? 1 : -1;
        int secondPitchChange = 2 * pitchChange;
        if (inScale) {
            pitchChange = getStepPitchChange(lCtx.getCurrentScale(), up, note.getPitch());
            secondPitchChange = pitchChange + getStepPitchChange(lCtx.getCurrentScale(), up, note.getPitch() + pitchChange);
        }

        if (length >= JMC.QUARTER_NOTE && Chance.test(60)) { // trill or tremolo
            boolean tremolo = Chance.test(40); // vary the velocity of the sound
            boolean mordents = Chance.test(20); // trill alternates 2 notes, mordents is alternation of 3 notes
            boolean changePitch = Chance.test(85);
            for (int i = 0; i < count; i++) {
                Note ornamentNote = cloneNote(note);
                ornamentNote.setLength(JMC.THIRTYSECOND_NOTE);
                if (i % 2 == 1) {
                    if (changePitch) {
                        ornamentNote.setPitch(ornamentNote.getPitch() + pitchChange);
                    }
                    if (tremolo) {
                        ornamentNote.setDynamic(ornamentNote.getDynamic() - 20);
                    }

                }
                if (mordents && i % 3 == 1 && changePitch) {
                    ornamentNote.setPitch(ornamentNote.getPitch() + pitchChange);
                } else if (mordents && changePitch && i % 3 == 2) {
                    ornamentNote.setPitch(ornamentNote.getPitch() + secondPitchChange);
                }

                lCtx.getCurrentPhrase().addNote(ornamentNote);
            }
        } else { //appoggiatura
            Note ornament = cloneNote(note);
            note.setLength(note.getRhythmValue() - JMC.THIRTYSECOND_NOTE);
            ornament.setLength(JMC.THIRTYSECOND_NOTE);
            lCtx.getCurrentPhrase().addNote(ornament);
            lCtx.getCurrentPhrase().addNote(note);
        }

        //TODO "turn"
    }

    private boolean shouldRepeatNewMotif(MainPartContext lCtx) {
        // if the motif limit is not reached, repeat and add new more often
        if (lCtx.getCurrentPhraseMeasuresCounter() >= 2 && lCtx.getCurrentMeasureSize() == 0) {
             if (lCtx.getCurrentPhraseMotifs().size() < lCtx.getCurrentPhraseMotifsCount()) {
                 int threshold = 2 + random.nextInt(4); //25% chance for 4-measure motif.
                 if (Chance.test(15)) { //rarely allow longer motifs
                     threshold += 1 + random.nextInt(3);
                 }
                 if (lCtx.getMeasuresSinceLastMotif() > threshold) {
                     return true;
                 }
             } else if (Chance.test(25)) { //even if the limit is reached, also possibly introduce new motifs
                 return true;
             }
        }
        return false;
    }

    private void addInitialMeasures(MainPartContext lCtx, Part part) {
        if (Chance.test(43)) {
            int pitch = Pitches.C4 + lCtx.getScoreContext().getKeyNote();
            // initial note is needed, so that accompaniment can "hook" to it.
            Note note = NoteFactory.createNote(pitch, JMC.QUARTER_NOTE);
            double restLength = lCtx.getScoreContext().getNormalizedMeasureSize() - JMC.QUARTER_NOTE;
            ExtendedPhrase phrase = new ExtendedPhrase();
            phrase.setScale(lCtx.getScoreContext().getScale());
            phrase.setMeasures(2 + random.nextInt(3));
            phrase.add(note);
            if (restLength > 0) {
                phrase.add(new Rest(restLength));
            }
            for (int i = 0; i < phrase.getMeasures() - 1; i++) {
                phrase.addRest(new Rest(lCtx.getScoreContext().getNormalizedMeasureSize()));
            }
            lCtx.setTotalMeasures(lCtx.getTotalMeasures() + phrase.getMeasures());
            part.add(phrase);
        }
    }

    private int getNextNoteDynamics(MainPartContext lCtx, boolean downBeat) {
        int result = 0;
        if (lCtx.getDynamicSequenceRemainingNotes() > 0) {
            result = lCtx.getPreviousDynamics() + (3 + random.nextInt(3)) * (lCtx.isDynamicsDirectionForte() ? 1 : -1);
            lCtx.setDynamicSequenceRemainingNotes(lCtx.getDynamicSequenceRemainingNotes() - 1);
        } else if (lCtx.getSilentSequenceRemainingNotes() > 0){
            result = lCtx.getCurrentPhrase().getBaseVelocity() + random.nextInt(8);
            lCtx.setSilentSequenceRemainingNotes(lCtx.getSilentSequenceRemainingNotes() - 1);
        } else {
            if (Chance.test(15)) {
                lCtx.setDynamicSequenceRemainingNotes(5 + random.nextInt(9));
                lCtx.setDynamicsDirectionForte(random.nextBoolean());
                if (lCtx.isDynamicsDirectionForte()) {
                    result = 80;
                } else {
                    result = 95;
                }
            } else if ((lCtx.isFirstNoteInMeasure() || downBeat) && Chance.test(60)) { //first notes in measures and down-beats in some cases are louder
                result = lCtx.getCurrentPhrase().getBaseVelocity() + 10 + random.nextInt(35);
            } else {
                result = lCtx.getCurrentPhrase().getBaseVelocity() + random.nextInt(20); // (75) to 94
            }
        }
        if (result < lCtx.getCurrentPhrase().getBaseVelocity() + 8 && lCtx.getSilentSequenceRemainingNotes() == 0) {
            lCtx.setSilentSequenceRemainingNotes(2 + random.nextInt(3));
        }

        lCtx.setPreviousDynamics(result);
        return result;
    }

    public static double calculateVariation(Part part) {
        double variation = 0;
        Phrase[] phrases = part.getPhraseArray();
        for (Phrase phrase : phrases) {
            variation += getVariationPerNote(phrase);
        }
        return variation / phrases.length;
    }

    private static double getVariationPerNote(Phrase phrase) {
        double variation = 0;
        int noteCount = 0;
        Note[] notes = phrase.getNoteArray();
        Note previousNote = null;
        for (Note note : notes) {
            if (note.isRest()) {
                continue;
            }
            if (previousNote != null) {
                variation += Math.abs(note.getPitch() - previousNote.getPitch());
            }
            previousNote = note;
            noteCount++;
        }
        if (noteCount == 0) {
            return 0;
        }
        double result = variation / noteCount;
        return result;
    }

    private void handleContour(MainPartContext lCtx) {
        lCtx.setCurrentContourNotes(lCtx.getCurrentContourNotes() + 1);
        // change direction of music according the the predefine contour.
        if (lCtx.getContour() == Contour.ARCH && lCtx.getCurrentContourNotes() == lCtx.getContourChangeNotes() / 2) {
            lCtx.setDirectionUp(!lCtx.isDirectionUp());
        }
        if (lCtx.getCurrentContourNotes() >= lCtx.getContourChangeNotes()) {
            // invert the direction if the current contour is expected to have reached the 'border' of the pitch range
            if (lCtx.getContour() == Contour.RAMP) {
                lCtx.setDirectionUp(!lCtx.isDirectionUp());
            }
            lCtx.setContour(chooseContour());
            lCtx.setCurrentContourNotes(0);
            lCtx.setContourChangeNotes(8 + random.nextInt(5));
        }
    }

    private double getLength(MainPartContext lCtx) {
        double length = 0;

        if (lCtx.isUsePreviousMeasureLengths() && lCtx.getMeasureLengths().size() > lCtx.getNoteIdxInMeasure()) {
            length = lCtx.getMeasureLengths().get(lCtx.getNoteIdxInMeasure());
        } else {
            length = getNextNoteLength(lCtx);
        }
        length = getProperLengthAndUpdateContext(lCtx, length);

        return length;
    }

    public double getProperLengthAndUpdateContext(MainPartContext lCtx, double desiredLength) {
        double result = desiredLength;
        double lengthToDownbeat = getLengthToNextDownBeat(lCtx, desiredLength);
        boolean syncopate = lCtx.isAllowSyncopation() && Chance.test(2);
        if (!syncopate && lengthToDownbeat != -1 && lengthToDownbeat <= desiredLength) {
            result = lengthToDownbeat;
            lCtx.setNextNoteDownBeat(true);
        } else {
            lCtx.setNextNoteDownBeat(false);
        }
        lCtx.setFirstNoteInMeasure(lCtx.getCurrentMeasureSize() == 0);
        if (lCtx.getCurrentMeasureSize() + result >= lCtx.getNormalizedMeasureSize()) {
            result = lCtx.getNormalizedMeasureSize() - lCtx.getCurrentMeasureSize();
            lCtx.setEndOfMeasure(true);
            lCtx.setNextNoteDownBeat(true);
            // if this is the last note in the phrase, resolve to a stable tone
            if (lCtx.getCurrentPhraseMeasuresCounter() == lCtx.getPhraseMeasures()) {
                lCtx.setNotePitch(ToneResolver.resolve(lCtx.getNotePitch(), lCtx.getCurrentScale()));
            }
        } else {
            lCtx.setNoteIdxInMeasure(lCtx.getNoteIdxInMeasure() + 1);
            lCtx.setEndOfMeasure(false);
        }
        lCtx.setCurrentMeasureSize(lCtx.getCurrentMeasureSize() + result);

        if (!lCtx.isUsePreviousMeasureLengths()) {
            lCtx.getMeasureLengths().add(result);
        }
        lCtx.setPreviousLength(result);
        if (lCtx.isEndOfMeasure()) {
            lCtx.setNoteIdxInMeasure(0);
            // repeat the lengths of the notes in the next measure in >50% of the cases
            lCtx.setUsePreviousMeasureLengths(lCtx.getMeasureLengths().size() > 2 && Chance.test(55));
            if (!lCtx.isUsePreviousMeasureLengths()) {
                lCtx.getMeasureLengths().clear();
            }
        }

        lCtx.setTotalLength(lCtx.getTotalLength() + result);
        return result;
    }

    // handle complex metres - simple metres (2/x and 3/x have only 1 upbeat. Complex metres have 2 or more upbeats)
    private double getLengthToNextDownBeat(MainPartContext lCtx, double desiredLength) {
        ScoreContext ctx = lCtx.getScoreContext();
        if (ctx.getMetre()[0] == 2 || ctx.getMetre()[0] == 3) {
            return -1; // simple metres don't have inter-measure upbeats
        }
        if (ctx.getMetre()[0] % 4 == 0 && ctx.getMetre()[0] != 12) { // 4/x, 8/x, 16/x
            return getLengthToNextDownBeat(lCtx, lCtx.getNormalizedMeasureSize(), lCtx.getCurrentMeasureSize(), desiredLength, 2);
        }

        if (ctx.getMetre()[0] % 3 == 0) { // 6/x, 9/x, 12/x, 15/x
            return getLengthToNextDownBeat(lCtx, lCtx.getNormalizedMeasureSize(), lCtx.getCurrentMeasureSize(), desiredLength, 3);
        }

        // unequal measures (numerator is prime)
        if (ctx.getMetre()[0] == 5 || ctx.getMetre()[0] == 7 || ctx.getMetre()[0] == 11 || ctx.getMetre()[0] == 13) {
            //TODO more combinations (3-2-2, 2-3-2?)

            // we divide the metre into 2-2-2...-3, and initially it looks like a regular
            // metre. The final group is longer, and that's handled by the end
            // of the measure code, outside this method.
            int groupCount = ctx.getMetre()[0] / 2;
            if (lCtx.getCurrentMeasureSize() > groupCount * 4d / ctx.getMetre()[1]) {
                return -1;
            }
            return getLengthToNextDownBeat(lCtx, lCtx.getNormalizedMeasureSize() - 4d / ctx.getMetre()[1],
                    lCtx.getCurrentMeasureSize(), desiredLength, groupCount);
        }

        return -1;
    }

    private double getLengthToNextDownBeat(MainPartContext lCtx, double normalizedMeasureSize, double currentMeasureSize, double length, int groupCount) {
        double groupSize = normalizedMeasureSize / groupCount;
        for (int i = 1; i <= groupCount; i ++) {
            if (currentMeasureSize < i * groupSize && currentMeasureSize + length >= i * groupSize) {
                return i * groupSize - currentMeasureSize;
            }
        }
        return -1;
    }

    private void createNewPhrase(MainPartContext lCtx, Part part) {
        lCtx.setCurrentPhrase(new ExtendedPhrase());
        lCtx.getCurrentPhrase().setTitle("Phrase " + lCtx.getPhrases().size());
        lCtx.getCurrentPhrase().setScale(lCtx.getCurrentScale());
        lCtx.getCurrentPhrase().setContour(lCtx.getContour());
        lCtx.setCurrentPhraseMotifsCount(2 + random.nextInt(4));
        //currentPhrase.setTempo(score.getTempo() + random.nextInt(20) - 10); TODO synchronize with other parts
        lCtx.getPhrases().add(lCtx.getCurrentPhrase());
        lCtx.getCurrentPhrase().setBaseVelocity(InstrumentGroups.getInstrumentSpecificDynamics(65 + random.nextInt(20), part.getInstrument()));
        if (lCtx.isSimplePhrases()) {
            lCtx.setPhraseMeasures((int) Math.round((15 + random.nextInt(15)) / lCtx.getNormalizedMeasureSize()));
        } else {
            lCtx.setPhraseMeasures((int) Math.round((26 + random.nextInt(26)) / lCtx.getNormalizedMeasureSize()));
        }
    }

    private void resetPhrase(MainPartContext lCtx) {
        int cadenceMeasures = addEnding(lCtx.getCurrentPhrase(), lCtx.getScoreContext());
        lCtx.setCurrentPhraseMeasuresCounter(lCtx.getCurrentPhraseMeasuresCounter() + cadenceMeasures);
        lCtx.getCurrentPhrase().setMeasures(lCtx.getCurrentPhraseMeasuresCounter());
        lCtx.setTotalMeasures(lCtx.getTotalMeasures() + lCtx.getCurrentPhraseMeasuresCounter());
        lCtx.setCurrentPhrase(null); // end of phrase, start a new one
        lCtx.setCurrentPhraseMeasuresCounter(0);
        lCtx.setPreviousLength(0);

        // one-or-two-measure rest before the next phrase
        ExtendedPhrase pausePhrase = new ExtendedPhrase();
        pausePhrase.setTitle("Phrase " + lCtx.getPhrases().size());
        pausePhrase.setScale(lCtx.getCurrentScale());
        int measures = Chance.test(85) || lCtx.getScoreContext().getScore().getTempo() < 90 ? 1 : 2;
        pausePhrase.setMeasures(measures);
        for (int i = 0; i < measures; i ++) {
            pausePhrase.addRest(new Rest(lCtx.getNormalizedMeasureSize())); //TODO upbeat here as well?
        }
        lCtx.getPhrases().add(pausePhrase);
        lCtx.setTotalMeasures(lCtx.getTotalMeasures() + measures);

        lCtx.setUsePreviousMeasureLengths(false);
        lCtx.getMeasureLengths().clear();
        lCtx.setNoteIdxInMeasure(0);
        lCtx.getPitches().clear();
        lCtx.getCurrentPhraseMotifs().clear();
        lCtx.setCurrentScale(Chance.test(20) ? lCtx.getScoreContext().getAlternativeScale() : lCtx.getScoreContext().getScale());
        lCtx.setCurrentContourNotes(0);
        lCtx.setContour(chooseContour());
        lCtx.setContourChangeNotes(8 + random.nextInt(5));
    }

    private void handleSpecialNotes(MainPartContext lCtx) {
        // choose if this measure is going to be of a special type (stacatto, legato, tenuto)
        if (lCtx.getCurrentMeasureSize() == 0) {
            if (lCtx.getSpecialNoteType() == null && Chance.test(33)) {
                lCtx.setSpecialNoteMeasures(1);
                lCtx.setSpecialNoteType(SpecialNoteType.values()[random.nextInt(4)]);
                lCtx.setMaxSpecialMeasures(2 + random.nextInt(4));
            }
            if (lCtx.getSpecialNoteType() != null) {
                lCtx.setSpecialNoteMeasures(lCtx.getSpecialNoteMeasures() + 1);
            }
            if (lCtx.getSpecialNoteMeasures() > lCtx.getMaxSpecialMeasures()) {
                lCtx.setSpecialNoteMeasures(0);
                lCtx.setSpecialNoteType(null);
            }
        }
    }

    private Contour chooseContour() {
        return Contour.values()[Chance.choose(new int[]{50, 25, 25})];
    }

    @SuppressWarnings("unused")
    private void printDegreePercentages(Part mainPart, Scale currentScale) {
        int[] counts = new int[] {0,0,0,0,0,0,0};
        for (Phrase phrase : mainPart.getPhraseArray()) {
            for (Note note : phrase.getNoteArray()) {
                int scaleIdx = Arrays.binarySearch(currentScale.getDefinition(), note.getPitch() % 12);
                if (scaleIdx > -1) {
                    counts[scaleIdx]++;
                }
            }
        }
        System.out.println(Arrays.toString(SongDBAnalyzer.percentages(counts)));
    }

    private int addEnding(ExtendedPhrase phrase, ScoreContext ctx) {

        if (Chance.test(12) && ctx.getScale().getDefinition().length == 7 && !ctx.isElectronic()) {
            int[] cadence = Cadences.getRandomCadence();

            for (int i = 0; i < cadence.length; i++) {
                double length = 0;
                if (i < 2) {
                    length = ctx.getNormalizedMeasureSize() / 2;
                } else {
                    length = ctx.getNormalizedMeasureSize(); //the 3rd chord (if exists) takes up a whole measure
                }

                int degree = cadence[i];
                int pitch = Pitches.C4 + ctx.getKeyNote() + phrase.getScale().getDefinition()[degree];
                int[] chord = getRandomChord(phrase, ctx, pitch);
                if (chord != null) {
                    phrase.addChord(chord, length);
                }
            }

            return cadence.length - 1;
        } else {
            // sometimes repeat the last measure
            if (Chance.test(70)) {
                Note[] notes = phrase.getNoteArray();
                List<Note> lastMeasure = new ArrayList<>();
                double measureSize = 0;
                for (int i = notes.length - 1; i >= 0; i--) {
                    lastMeasure.add(notes[i]);
                    measureSize += notes[i].getRhythmValue();
                    if (measureSize >= ctx.getNormalizedMeasureSize()) {
                        break;
                    }
                }
                Collections.reverse(lastMeasure);
                phrase.addNoteList(lastMeasure.toArray(new Note[lastMeasure.size()]));
            }

            phrase.addNote(Pitches.C4 + ctx.getKeyNote(), ctx.getNormalizedMeasureSize());
            return 1;
        }
    }

    private int[] getRandomChord(ExtendedPhrase phrase, ScoreContext ctx, int pitch) {
        // making a copy, so that the original is not shuffled
        List<Chord> list = new ArrayList<>(ChordUtils.chords.get(phrase.getScale()));
        Collections.shuffle(list, random);
        Chord chord = ChordUtils.getChord(ctx, pitch, null, list, null, null, false, false);
        if (chord != null) {
            return chord.getPitches();
        } else {
            return null;
        }
    }

    private double getNextNoteLength(MainPartContext lCtx) {
        double length = 0;
        int lengthSpec = Chance.choose(NOTE_LENGTH_PERCENTAGES);

        // don't allow drastic changes in note length
        if (lCtx.getPreviousLength() != 0 && lCtx.getPreviousLength() < 1 && lengthSpec == 5) {
            length = 4;
        } else if (lCtx.getPreviousLength() != 0 && lCtx.getPreviousLength() >= 2 && lengthSpec == 0) {
            lengthSpec = 1;
        }

        // make unisons for long notes less-likely
        if (lengthSpec >= 3 && lCtx.getPitches().size() > 1 && lCtx.getPitches().get(lCtx.getPitches().size() - 2) == lCtx.getNotePitch()
                && length >= JMC.DOTTED_QUARTER_NOTE && Chance.test(70)) {
            lengthSpec = Chance.choose(NOTE_LENGTH_PERCENTAGES);
        }

        // use a given length either if the spec mandates so, or if there is an ongoing sequence of that length
        if (shouldUseNoteLength(lCtx, lengthSpec, 0, JMC.SIXTEENTH_NOTE)) {
            length = JMC.SIXTEENTH_NOTE;
        } else if (shouldUseNoteLength(lCtx, lengthSpec, 1, JMC.EIGHTH_NOTE)) {
            length = JMC.EIGHTH_NOTE;
        } else if (shouldUseNoteLength(lCtx, lengthSpec, 2, JMC.QUARTER_NOTE)) {
            length = JMC.QUARTER_NOTE;
        } else if (shouldUseNoteLength(lCtx, lengthSpec, 3, JMC.DOTTED_QUARTER_NOTE)) {
            length = JMC.DOTTED_QUARTER_NOTE;
        } else if (lengthSpec == 4) {
            length = JMC.HALF_NOTE;
        } else if (lengthSpec == 5){
            length = JMC.WHOLE_NOTE;
        }

        // handle sequences of notes with the same length
        if (lCtx.getSameLengthNoteSequenceCount() == 0 && Chance.test(17) && length <= JMC.DOTTED_QUARTER_NOTE) {
            lCtx.setSameLengthNoteSequenceCount(3 + random.nextInt(7));
            lCtx.setSameLengthNoteType(length);
        }
        if (lCtx.getSameLengthNoteSequenceCount() > 0) {
            lCtx.setSameLengthNoteSequenceCount(lCtx.getSameLengthNoteSequenceCount() - 1);
        }

        length = length * lCtx.getScoreContext().getNoteLengthCoefficient();

        return length;
    }

    private boolean shouldUseNoteLength(MainPartContext lCtx, int lengthSpec, int desiredLengthSpec, double lengthType) {
        return (lengthSpec == desiredLengthSpec && lCtx.getSameLengthNoteSequenceCount() == 0) || (lCtx.getSameLengthNoteSequenceCount() > 0 && lCtx.getSameLengthNoteType() == lengthType);
    }

    private int getNextNotePitch(MainPartContext lCtx, boolean forceInterval) {
        // http://www.tpub.com/harmony/2.htm
        // http://www2.siba.fi/muste1/index.php?id=63&la=en
        // http://smu.edu/totw/melody.htm

        // TODO long notes should be stable tones?

        int notePitch;
        if (lCtx.getPitches().isEmpty()) {
            // avoid excessively high and low notes.
            notePitch = random.nextInt(15) + (lCtx.isDirectionUp() ? 55 : 60);
            notePitch = ToneResolver.resolve(notePitch, lCtx.getCurrentScale());
            if (lCtx.isDirectionUp()) {
                lCtx.getPitchRange()[0] = notePitch - 7;
                lCtx.getPitchRange()[1] = notePitch + 12;
            } else {
                lCtx.getPitchRange()[0] = notePitch - 12;
                lCtx.getPitchRange()[1] = notePitch + 7;
            }
        } else {
            int previousNotePitch = lCtx.getPitches().get(lCtx.getPitches().size() - 1);
            boolean shouldResolveToStableTone = shouldResolveToStableTone(lCtx.getPitches(), lCtx.getCurrentScale());

            if (!lCtx.getCurrentChordInMelody().isEmpty()) {
                notePitch = lCtx.getCurrentChordInMelody().get(0);
                lCtx.getCurrentChordInMelody().remove(0);
            } else  if (shouldResolveToStableTone) {
                notePitch = ToneResolver.resolve(previousNotePitch, lCtx.getCurrentScale());
                if (lCtx.getPitches().size() > 1 && notePitch == previousNotePitch) {
                    logger.warn("Resolving two equivalent tones to the same tone: " + notePitch + ", scale=" + lCtx.getCurrentScale());
                    // in that case, make a step to break the repetition pattern
                    int pitchChange = getStepPitchChange(lCtx.getCurrentScale(), lCtx.isDirectionUp(), previousNotePitch);
                    notePitch = previousNotePitch + pitchChange;
                }

                // after resolving to a stable tone, optionally use that tone to start a sequence of notes that correspond to a chord (arpeggio)
                if (Chance.test(12)) {
                    int[] chord = getRandomChord(lCtx.getCurrentPhrase(), lCtx.getScoreContext(), notePitch);
                    if (chord != null) {
                        for (int i = 1; i < chord.length; i ++) { //starting from the 2nd - the first pitch is added to the melody in this iteration
                            lCtx.getCurrentChordInMelody().add(chord[i]);
                        }
                    }
                }
            } else if (lCtx.getCircleOfFifthsSequence() > 0) {
                lCtx.setCircleOfFifthsSequence(lCtx.getCircleOfFifthsSequence() - 1);
                if (!ToneResolver.isInScale(lCtx.getCurrentScale().getDefinition(), previousNotePitch, 7, lCtx.isDirectionUp())) {
                    lCtx.setDirectionUp(!lCtx.isDirectionUp());
                }
                notePitch = previousNotePitch + 7 * (lCtx.isDirectionUp() ? 1 : -1);
                if (notePitch > lCtx.getPitchRange()[1] || (Chance.test(37) && notePitch - 12 > lCtx.getPitchRange()[0])) {
                    notePitch = notePitch - 12;
                }
                if (!isInScale(lCtx.getCurrentScale().getDefinition(), notePitch)) {
                    notePitch = ToneResolver.resolve(notePitch, lCtx.getCurrentScale());
                }
            } else {
                // in rare cases, use out-of-scale notes on upbeats that create tension and dissonance
                if (!lCtx.isNextNoteDownBeat() && ToneResolver.isStable(previousNotePitch, lCtx.getCurrentScale()) && ((lCtx.isAllowMoreDissonance() && Chance.test(5)) || Chance.test(3))) {
                    // Note: "change should happen only for a tone that's a major 2nd away from a stable tone"
                    // (that's either a passing tone or a neighbouring tone)
                    // More: http://www2.siba.fi/muste1/index.php?id=66&la=en

                    int step = lCtx.isDirectionUp() ? 1 : -1;
                    notePitch = previousNotePitch += step; // default value
                    // get up or down from the previous pitch until an out-of-scale note is found
                    for (int pitch = previousNotePitch; Math.abs(previousNotePitch - pitch) < 3; pitch += step) {
                        if (Arrays.binarySearch(lCtx.getCurrentScale().getDefinition(), pitch % 12) < 0) {
                            notePitch = pitch;
                            break;
                        }
                    }
                } else {
                    int interval = 0;
                    // try getting a pitch. if the pitch range is exceeded, get a
                    // new consonant tone, in the opposite direction, different progress type and different interval
                    int attempt = 0;

                    // use a separate variable in order to allow change only for this particular note, and not for the direction of the melody
                    boolean directionUp = lCtx.isDirectionUp();
                    do {
                        int progressType = Chance.choose(PROGRESS_TYPE_PERCENTAGES); // TODO consider using compound intervals
                        // in some cases change the predefined direction (for this pitch only), for a more interesting melody
                        if ((progressType == 1 || progressType == 2) && Chance.test(15)) {
                            directionUp = !directionUp;
                        }

                        // always follow big jumps with a step back
                        int needsStepBack = needsStepBack(lCtx.getPitches());
                        if (needsStepBack != 0) {
                            progressType = 1;
                            directionUp = needsStepBack == 1;
                        }
                        if (forceInterval) { //if an interval is preferred (usually by the contour)
                            progressType = 2;
                        }
                        if (progressType == 1) { // step
                            int pitchChange = getStepPitchChange(lCtx.getCurrentScale(), directionUp, previousNotePitch);
                            notePitch = previousNotePitch + pitchChange;
                        } else if (progressType == 0) { //unison
                            notePitch = previousNotePitch;
                        } else if (progressType == 3) { //octave
                            notePitch = previousNotePitch + 12;
                        } else { // 2 - intervals
                            if ((lCtx.isNextNoteDownBeat() && Chance.test(95)) || (lCtx.isAllowMoreDissonance() && Chance.test(75)) || (!lCtx.isAllowMoreDissonance() && Chance.test(92))) {
                                // for a melodic sequence, use only a "jump" of up to 6 pitches in current direction
                                int intervalSpec = Chance.choose(INTERVAL_SPEC_PERCENTAGES);
                                interval = getSemiTonesForConsonance(lCtx.getCurrentScale(), previousNotePitch, intervalSpec, directionUp, lCtx.getPitchRange());
                            } else {
                                interval = getSemiTonesForDissonance(lCtx.getCurrentScale(), previousNotePitch, directionUp, lCtx.getPitchRange());
                            }
                            notePitch = previousNotePitch + interval;
                        }

                        if (attempt > 0) {
                            directionUp = !directionUp;
                        }
                        // if there are more than 3 failed attempts, simply assign a random in-scale, in-range pitch
                        if (attempt > 3) {
                            int start = lCtx.getPitchRange()[1] - random.nextInt(lCtx.getPitchRange()[1] - lCtx.getPitchRange()[0]);
                            for (int i = start; i > lCtx.getPitchRange()[0]; i--) {
                                if (ToneResolver.isInScale(lCtx.getCurrentScale().getDefinition(), i)) {
                                    notePitch = i;
                                    break;
                                }
                            }
                        }
                        attempt++;
                    } while (!ToneResolver.isInRange(notePitch, lCtx.getPitchRange()));
                }
            }
        }

        // circle of fifths in melody
        if (Chance.test(6) && ToneResolver.isStable(notePitch, lCtx.getCurrentScale())) {
            lCtx.setCircleOfFifthsSequence(4 + random.nextInt(6));
        }
        return notePitch;
    }

    private int needsStepBack(List<Integer> pitches) {
        if (pitches.size() < 2) {
            return 0;
        }
        int previous = pitches.get(pitches.size() - 1);
        int prePrevious = pitches.get(pitches.size() - 2);

        int diff = previous - prePrevious;
        if (Math.abs(diff) > 7) {
            return (int) -Math.signum(diff); //the opposite direction of the previous interval
        }
        // force another step back after the initial step back. E.g. x -> x + 7, x+7-1, x+7-2
        if (pitches.size() >= 3) {
            int prePrePrevious = pitches.get(pitches.size() - 3);
            int previousDiff = prePrevious - prePrePrevious;
            if (Math.abs(previousDiff) > 7) {
                return (int) -Math.signum(previousDiff); //the opposite direction of the pre-previous interval
            }
        }
        return 0;
    }

    private int getStepPitchChange(Scale currentScale, boolean directionUp, int previousNotePitch) {
        int pitchChange = 0;
        // prefer common tones in case the previous note is not the dominant. Dominant is surrounded by non-common tones, so no choice there
        boolean preferCommonTone = Chance.test(30) && Arrays.binarySearch(currentScale.getDefinition(), previousNotePitch % 12) != ToneType.DOMINANT.getDegree();
        boolean avoidSubdominant = Chance.test(60); //statistics show that steps rarely go to the subdominant

        int[] steps = new int[] {-2, -1 , 1 ,2};
        if (directionUp) {
            steps = new int[] {2, 1, -1, -2};
        }
        for (int i: steps) {
            int idx = Arrays.binarySearch(currentScale.getDefinition(), (previousNotePitch + i) % 12);
            // if the pitch is in the predefined direction and it is within the scale - use it.
            if (idx > -1 && !(avoidSubdominant && idx == ToneType.SUBDOMINANT.getDegree())) {
                pitchChange = i;
            }
            // if we found a pitch change which matches the direction, and there's no preference for common tone, end the loop
            if (pitchChange != 0 && !preferCommonTone && (directionUp && i > 0 || !directionUp && i < 0) ) {
                break;
            }
            // if, however, common tone is preferred, break only is the selected on is common - otherwise continue.
            if (preferCommonTone && ToneResolver.isCommonTone(previousNotePitch + pitchChange, currentScale)) {
                break;
            }

            // in case no other matching tone is found that is common, the last appropriate one will be retained in "pitchChange"
        }
        return pitchChange;
    }

    private boolean shouldResolveToStableTone(List<Integer> pitches, Scale currentScale) {
        // if the previous two pitches are unstable
        int previousNotePitch = pitches.get(pitches.size() - 1);
        int prePreviousNotePitch = 0;
        if (pitches.size() >= 2) {
            prePreviousNotePitch = pitches.get(pitches.size() - 2);
        }
        if (prePreviousNotePitch != 0 && !ToneResolver.isStable(prePreviousNotePitch, currentScale) && !ToneResolver.isStable(previousNotePitch, currentScale)) {
            return true;
        }
        return false;
    }

    private int getSemiTonesForConsonance(Scale scale, int previous, int intervalSpec, boolean up, int[] pitchRange) {
        int semiTones = 0;
        int[] scaleDef = scale.getDefinition();
        // Note - depending on the direction, we are getting regular or inverted intervals
        if (intervalSpec == 0 && isInScale(scaleDef, previous, 7, up)) { // perfect fifth
            semiTones = 7;
        } else  if (intervalSpec == 1 && isInScale(scaleDef, previous, 5, up)) { // perfect fourth
            semiTones = 5;
        } else  if (intervalSpec == 2 && isInScale(scaleDef, previous, 4, up)) { // major third
            semiTones = 4;
        } else if (intervalSpec == 2 && isInScale(scaleDef, previous, 3, up)) { // minor third
            semiTones = 3;
        } else if (intervalSpec == 3 && isInScale(scaleDef, previous, 9, up)) { // major sixth
            semiTones = 9;
        } else if (intervalSpec == 3 && isInScale(scaleDef, previous, 8, up)) { // minor sixth
            semiTones = 8;
        }

        // if no interval works, try whatever works

        if (semiTones == 0 || !ToneResolver.isInRange(previous + (up ? 1 : -1) * semiTones, pitchRange)) {
            intervalSpec = 4;
        }

        // use whatever interval resolves to a common tone
        if (intervalSpec == 4) {
            List<Integer> intervals = Lists.newArrayList(3,4,5,7,8,9);
            Collections.shuffle(intervals, random);
            for (int interval : intervals) {
                int adjusted = (up ? 1 : -1) * interval; //adjusted according to direction
                if (ToneResolver.isCommonTone(previous + adjusted, scale) && ToneResolver.isInRange(previous + adjusted, pitchRange)) {
                    semiTones = adjusted;
                    break;
                }
            }
        }

        // if, for some reason, no interval worked, resolve to stable
        if (semiTones == 0) {
            semiTones = previous - ToneResolver.resolve(previous, scale);
        }

        if (intervalSpec != 4 && !up) {
            semiTones = -semiTones;
        }

        return semiTones;
    }

    private int getSemiTonesForDissonance(Scale scale, int previous, boolean up,
            int[] pitchRange) {
        int semiTones = 0;
        int[] scaleDef = scale.getDefinition();
        List<Integer> availableSemiTones = Lists.newArrayList();
        // Note - depending on the direction, we are getting regular or inverted intervals
        if (isInScale(scaleDef, previous, 6, up)) { // augmented fourth (tritone)
            availableSemiTones.add(6);
        }
        if (isInScale(scaleDef, previous, 10, up)) { // minor seventh
            availableSemiTones.add(10);
        }
        if (isInScale(scaleDef, previous, 11, up)) { // major seventh
            availableSemiTones.add(11);
        }

        // if, for some reason, no interval worked, resolve to stable
        if (availableSemiTones.isEmpty()) {
            semiTones = previous - ToneResolver.resolve(previous, scale);
        } else {
            semiTones = availableSemiTones.get(random.nextInt(availableSemiTones.size()));
        }
        if (!up) {
            semiTones = -semiTones;
        }

        return semiTones;

    }

    private void repeatNewMotifWithinPhrase(MainPartContext lCtx) {
        int repetitions = 2 + random.nextInt(3);
        int repeatedMeasures = Chance.test(50) ? lCtx.getMeasuresSinceLastMotif() : 2 + random.nextInt(4);

        boolean chiasmus = Chance.test(35) && repeatedMeasures % 2 == 0 && repetitions == 1;
        List<Note> repeatedNotes = new ArrayList<>();
        Note[] notes = lCtx.getCurrentPhrase().getNoteArray();

        int measures = 0;
        int i = notes.length - 1;
        double currentMeasureSize = 0;
        int chiasmusAdditionIdx = 0; // needed for inversion of the halves
        while (measures <= repeatedMeasures && i >= 0) {
            if (chiasmus && measures >= repeatedMeasures / 2) {
                repeatedNotes.add(chiasmusAdditionIdx++, notes[i]);
            } else {
                repeatedNotes.add(notes[i]);
            }
            currentMeasureSize += notes[i].getRhythmValue();
            i--;
            if (currentMeasureSize == lCtx.getNormalizedMeasureSize()) {
                measures ++;
                currentMeasureSize = 0;
            }
        }
        Collections.reverse(repeatedNotes);
        if (lCtx.getCurrentPhraseMotifs().size() <= lCtx.getCurrentPhraseMotifsCount()) {
            Motif motif = new Motif();
            motif.setNotes(repeatedNotes);
            motif.setMeasures(measures);
            lCtx.getCurrentPhraseMotifs().add(motif);
        }
        repeatMotif(lCtx, repetitions, measures, chiasmus, repeatedNotes);
    }

    private void repeatExistingMotifWithinPhrase(MainPartContext lCtx) {
        if (lCtx.getCurrentPhraseMotifs().isEmpty()) {
            return;
        }
        int repetitions = 3 + random.nextInt(3);
        Motif motif = lCtx.getCurrentPhraseMotifs().get(random.nextInt(lCtx.getCurrentPhraseMotifs().size()));
        repeatMotif(lCtx, repetitions, motif.getMeasures(), false, motif.getNotes());
    }

    private void repeatMotif(MainPartContext lCtx, int repetitions, int repeatedMeasures, boolean chiasmus,
            List<Note> repeatedNotes) {
        boolean sequences = Chance.test(28);
        int sequenceTranspositionValue = random.nextBoolean() ? 1 : 2;
        lCtx.getCurrentPhrase().getStructure().add("Starting repetition");
        for (int k = 0; k < repetitions; k++) {
            List<Note> currentRepetitionNotes = new ArrayList<>();
            for (Note note : repeatedNotes) {
                currentRepetitionNotes.add(cloneNote(note));
            }
            if (!chiasmus) {
                if (sequences) {
                    transpose(lCtx.getCurrentScale(), repeatedNotes, lCtx, sequenceTranspositionValue + (random.nextBoolean() ? 1 : 2));
                } else if (Chance.test(52)){
                    makeVariations(lCtx, currentRepetitionNotes, k, repetitions);
                }
            }
            lCtx.getCurrentPhrase().getStructure().add(getNoteString(currentRepetitionNotes, lCtx.getNormalizedMeasureSize()));
            for (Note note : currentRepetitionNotes) {
                lCtx.getCurrentPhrase().add(note);
                if (!note.isRest()) {
                    lCtx.getPitches().add(note.getPitch());
                }
            }
        }
        lCtx.getCurrentPhrase().getStructure().add("Repetition over");
        lCtx.setCurrentPhraseMeasuresCounter(lCtx.getCurrentPhraseMeasuresCounter() + repetitions * repeatedMeasures);
    }

    private void repeatNotesWithinPhrase(MainPartContext lCtx) {
        int repetitions = 1 + random.nextInt(4);
        int repeatedCount = 3 + random.nextInt(9);
        int offset = random.nextBoolean() ? 0 : random.nextInt(3); //sometimes, but rarely have an offset
        List<Note> repeatedNotes = new ArrayList<>(repeatedCount);
        Note[] notes = lCtx.getCurrentPhrase().getNoteArray();
        if (notes.length - offset - repeatedCount < 0 || notes.length - offset < 0) {
            return;
        }
        for (int i = notes.length - offset - repeatedCount; i < notes.length - offset; i++) {
            repeatedNotes.add(notes[i]);
        }
        if (repeatedNotes.isEmpty()) {
            return;
        }
        for (int k = 0; k < repetitions; k++) {
            List<Note> currentRepetitionNotes = new ArrayList<>();
            for (Note note : repeatedNotes) {
                currentRepetitionNotes.add(cloneNote(note));
            }
            // rarely make variations for the first repetition
            if ((k != 0 && Chance.test(70)) || Chance.test(25)) {
                makeVariations(lCtx, currentRepetitionNotes, k, repetitions);
            }
            for (Note note : currentRepetitionNotes) {
                // new note needed again, because otherwise duration may be changed after repeating it multiple times

                // each time make sure no attempt to use previous lengths is made
                lCtx.setUsePreviousMeasureLengths(false);
                // Try the current length. If it's not appropriate (measure-wise), then it will be shortened
                // All context fields will be updated by this call to getProperLengthAndUpdateContext
                note.setLength(getProperLengthAndUpdateContext(lCtx, note.getRhythmValue()));
                lCtx.getCurrentPhrase().add(note);

                if (!note.isRest()) {
                    lCtx.getPitches().add(note.getPitch());
                }
            }
        }
    }

    private Note cloneNote(Note note) {
        Note newNote = NoteFactory.createNote(note.getPitch(), note.getRhythmValue(), note.getDynamic());
        newNote.setDuration(note.getDuration());
        return newNote;
    }

    private void makeVariations(MainPartContext lCtx, List<Note> repeatedNotes, int repetitionIndex, int totalRepetitions) {
        if (repeatedNotes.size() < 2) {
            return;
        }
        // only one type of variation allowed - otherwise the relation to the original motif is hard to detect
        boolean varyBaseStructure = (repetitionIndex == 0 && Chance.test(3)) || Chance.test(8);
        boolean transpose = Chance.test(30);
        boolean invert = Chance.test(24) && repeatedNotes.size() > 2;
        boolean retrograde = Chance.test(17) && repeatedNotes.size() > 2;
        boolean changeKey = !transpose && Chance.test(10);
        boolean notesToRests = Chance.test(15);
        boolean truncate = Chance.test(8);
        boolean varyEnding = Chance.test(7) && repeatedNotes.size() > 3;
        boolean multiplyPitches = Chance.test(4);
        boolean stabilize = repetitionIndex + 1 == totalRepetitions && Chance.test(90);

        if (Chance.test(15)) {
            changeDynamics(repeatedNotes);
            lCtx.getScoreContext().getVariations().add(Variation.CHANGE_DYNAMICS);
        }

        if (truncate) {
            truncate(repeatedNotes, lCtx);
        }

        if (transpose) {
            transpose(lCtx.getCurrentScale(), repeatedNotes, lCtx, 1 + random.nextInt(4));
            lCtx.getScoreContext().getVariations().add(Variation.TRANSPOSE);
        } else if (invert) {
            invert(lCtx.getCurrentScale(), repeatedNotes);
            lCtx.getScoreContext().getVariations().add(Variation.INVERT);
        } else if (varyEnding) {
            varyEnding(repeatedNotes, lCtx);
            lCtx.getScoreContext().getVariations().add(Variation.VARY_ENDING);
        } else if (varyBaseStructure) {
            varyBaseStructure(lCtx, repeatedNotes);
            lCtx.getScoreContext().getVariations().add(Variation.VARY_BASE_STRUCTURE);
        } else if (retrograde) {
            retrograde(repeatedNotes);
            lCtx.getScoreContext().getVariations().add(Variation.RETROGRADE);
        } else if (changeKey) {
            changeKey(repeatedNotes, 1 + random.nextInt(3));
            lCtx.getScoreContext().getVariations().add(Variation.CHANGE_KEY);
        } else if (notesToRests) {
            replaceNotesWithRests(repeatedNotes);
            lCtx.getScoreContext().getVariations().add(Variation.NOTES_TO_RESTS);
        } else if (multiplyPitches) {
            multiplyPitches(repeatedNotes, lCtx);
            lCtx.getScoreContext().getVariations().add(Variation.MULTIPLY_PITCHES);
        }

        if (stabilize && !repeatedNotes.isEmpty()) { // some modifications may remove notes, so check again for size
            stabilize(repeatedNotes, lCtx);
            lCtx.getScoreContext().getVariations().add(Variation.STABILIZE);
        }

        //TODO half or double the size of the notes - diminution and augmentation
    }

    /**
     * Ends the motif in a stable tone
     * @param repeatedNotes
     * @param lCtx
     */
    private void stabilize(List<Note> repeatedNotes, MainPartContext lCtx) {
        Note lastNote = repeatedNotes.get(repeatedNotes.size() - 1);
        // if already stable, make it the tonic. If not stable - resolve.
        if (ToneResolver.isStable(lastNote.getPitch(), lCtx.getCurrentScale())) {
            lastNote.setPitch(ToneResolver.resolveToTonic(lastNote.getPitch(), lCtx.getCurrentScale()));
        } else {
            lastNote.setPitch(ToneResolver.resolve(lastNote.getPitch(), lCtx.getCurrentScale()));
        }
    }

    private void multiplyPitches(List<Note> repeatedNotes, MainPartContext lCtx) {
        double multiplier = 0.7 + random.nextInt(60) / 100d;
        boolean round = Chance.test(50);
        boolean down = Chance.test(50);
        for (Note note : repeatedNotes) {
            if (note.isRest()) {
                continue;
            }
            double newPitch = note.getPitch() / 12 * 12 + multiplier * (note.getPitch() % 12);
            int pitch;
            if (round) {
                pitch = (int) Math.round(newPitch);
            } else {
                pitch = (int) Math.floor(newPitch);
            }
            while (!ToneResolver.isInScale(lCtx.getCurrentScale().getDefinition(), pitch)) {
                if (down) {
                    pitch --;
                } else {
                    pitch ++;
                }
            }
            pitch = Math.max(pitch, lCtx.getPitchRange()[0]);
            pitch = Math.min(lCtx.getPitchRange()[1], pitch);
            note.setPitch(pitch);
        }
    }

    private void varyEnding(List<Note> repeatedNotes, MainPartContext lCtx) {
        int variedNotes = 1 + random.nextInt(3);
        if (repeatedNotes.size() - variedNotes < 2) {
            return; // either some notes might have been removed in other variations
        }

        List<Note> ending = new ArrayList<>(repeatedNotes.subList(
                repeatedNotes.size() - variedNotes, repeatedNotes.size()));
        makeVariations(lCtx, ending, 1, 1);
        //variation may change the number of notes
        if (ending.size() == 0) {
            return;
        }
        if (ending.size() != variedNotes) {
            variedNotes = ending.size();
        }
        int k = 0;
        for (int i = repeatedNotes.size() - variedNotes; i < repeatedNotes.size(); i++) {
            repeatedNotes.set(i, ending.get(k++));
        }
    }

    // retain the notes on down (strong) beats, and change everything else
    public void varyBaseStructure(MainPartContext lCtx, List<Note> repeatedNotes) {

        lCtx.setUsePreviousMeasureLengths(false); //varying the structure negates the usage of the same note lengths in the next measure

        int groupSize = 1;
        ScoreContext ctx = lCtx.getScoreContext();
        if (ctx.getMetre()[0] == 2 || ctx.getMetre()[0] == 3) {
            groupSize = 1; // simple metres don't have inter-measure upbeats
        }
        if (ctx.getMetre()[0] % 4 == 0 && ctx.getMetre()[0] != 12) { // 4/x, 8/x, 16/x
            groupSize = 2;
        } else if (ctx.getMetre()[0] != 3 && ctx.getMetre()[0] % 3 == 0) { // 6/x, 9/x, 12/x, 15/x
            groupSize = 3;
        } else if (ctx.getMetre()[0] == 5 || ctx.getMetre()[0] == 7 || ctx.getMetre()[0] == 11 || ctx.getMetre()[0] == 13) { // unequal measures (numerator is prime)
            return; // not supported
        }

        double measureSize = 0;
        List<Note> variation = new ArrayList<>();
        double divisor = ctx.getNormalizedMeasureSize() / groupSize;
        for (Note note : repeatedNotes) {
            if (Chance.test(36) || measureSize == 0 || measureSize % divisor == 0) { // keep a % of the notes, and all down-beat notes
                variation.add(note);
            } else {
                double replacementSize = 0;
                while (measureSize + replacementSize < ctx.getNormalizedMeasureSize() && (measureSize + replacementSize) % divisor != 0) {
                    int pitch = getNextNotePitch(lCtx, false);
                    lCtx.setNotePitch(pitch);
                    double length = getNextNoteLength(lCtx);
                    if (length + replacementSize > ctx.getNormalizedMeasureSize()) {
                        length = ctx.getNormalizedMeasureSize() - replacementSize;
                    }
                    double lengthToBeat = getLengthToNextDownBeat(lCtx, lCtx.getNormalizedMeasureSize(), measureSize + replacementSize, length, groupSize);
                    if (lengthToBeat != -1 && lengthToBeat < length) {
                        length = lengthToBeat;
                    }
                    Note replacementNote = createNote(lCtx, false, length);
                    // keep the last note of each measure
                    if (measureSize + length >= ctx.getNormalizedMeasureSize()) {
                        replacementNote = note;
                    }

                    variation.add(replacementNote);
                    replacementSize += length;
                }
            }
            measureSize += note.getRhythmValue();
            if (measureSize >= ctx.getNormalizedMeasureSize()) {
                measureSize = 0;
            }
        }
        // replace the repeated notes with the variation
        repeatedNotes.clear();
        repeatedNotes.addAll(variation);
    }

    private void truncate(List<Note> repeatedNotes, MainPartContext lCtx) {
        // cut the trailing measure
        double measureSize = 0;
        for (int i = repeatedNotes.size() - 1; i >= 0; i--) {
            measureSize += repeatedNotes.get(i).getRhythmValue();
            repeatedNotes.remove(i);
            if (measureSize >= lCtx.getNormalizedMeasureSize()) {
                break;
            }
        }
    }

    private void replaceNotesWithRests(List<Note> repeatedNotes) {
        boolean firstNotes = Chance.test(60);
        int erasedNotes = 1 + random.nextInt(3);
        int erasedCounter = 0;
        for (int i = 0; i < repeatedNotes.size(); i ++) {
            if (firstNotes || Chance.test(erasedNotes * 100 / repeatedNotes.size())) {
                Note removed = repeatedNotes.remove(i);
                repeatedNotes.add(i, new Rest(removed.getRhythmValue()));
                erasedCounter++;
            }
            if (erasedCounter >= erasedNotes) {
                break;
            }
        }

    }

    private void changeDynamics(List<Note> repeatedNotes) {
        int change = (random.nextBoolean() ? -1 : 1) * (4 + random.nextInt(8));
        for (Note note : repeatedNotes) {
            note.setDynamic(note.getDynamic() + change);
        }
    }

    private void changeKey(List<Note> repeatedNotes, int semitonesUp) {
        for (Note note : repeatedNotes) {
            if (!note.isRest()) {
                note.setPitch(note.getPitch() + semitonesUp);
            }
        }

    }

    void retrograde(List<Note> repeatedNotes) {
        // reverse the pitches
        for (int i = 0; i < repeatedNotes.size() / 2; i ++) {
            Note note = repeatedNotes.get(i);
            Note mirrorNote = repeatedNotes.get(repeatedNotes.size() - 1 - i);
            int pitch = note.getPitch();
            note.setPitch(mirrorNote.getPitch());
            mirrorNote.setPitch(pitch);
        }
    }

    private void invert(Scale scale, List<Note> repeatedNotes) {
        int previousNotePitch = -1;

        int notesToInvert = repeatedNotes.size();
        if (Chance.test(37)) { // in some cases invert only the last X notes
            notesToInvert = 1 + random.nextInt(5);
        }
        int idx = 0;
        for (Note note : repeatedNotes) {
            // skip in case we want to invert only the final notes
            if (idx < repeatedNotes.size() - notesToInvert) {
                continue;
            }
            if (!note.isRest()) {
                if (previousNotePitch != -1) {
                    int intervalInSemitones = previousNotePitch - note.getPitch();
                    boolean invertedDirectionUp = intervalInSemitones > 0;
                    intervalInSemitones = Math.abs(intervalInSemitones);
                    if (!isInScale(scale.getDefinition(), previousNotePitch, intervalInSemitones, invertedDirectionUp)){
                        // if it's a major interval, make it minor, and vice-versa
                        if (intervalInSemitones < 5) { //seconds and thirds
                            if (intervalInSemitones % 2 == 1) {
                                intervalInSemitones ++;
                            } else {
                                intervalInSemitones --;
                            }
                        } else { // sixths and sevenths. (Possibly including fifths and fourths in some scales)
                            if (intervalInSemitones % 2 == 1) {
                                intervalInSemitones --;
                            } else {
                                intervalInSemitones ++;
                            }
                        }
                    }
                    int newPitch = previousNotePitch + (invertedDirectionUp ? 1 : -1) * intervalInSemitones;
                    previousNotePitch = note.getPitch(); // storing 'previous' before changing the pitch, so that the intervals from the original melody are followed
                    if (newPitch == 0) {
                       logger.warn("New pitch is 0. intervalInSemitones=" + intervalInSemitones);
                       newPitch = previousNotePitch;
                    }
                    note.setPitch(newPitch);
                } else {
                    previousNotePitch = note.getPitch();
                }
            }
            idx++;
        }
    }

    private void transpose(Scale currentScale, List<Note> repeatedNotes, MainPartContext lCtx, int transpositionValue) {
        if (!lCtx.isDirectionUp()) {
            transpositionValue = -transpositionValue;
        }
        for (Note note : repeatedNotes) {
            // not changing the key, just moving within the same scale
            int idx = Arrays.binarySearch(currentScale.getDefinition(), note.getPitch() % 12);
            if (note.isRest() || idx < 0) {
                continue;
            }
            // adding the length so that no negative numbers occur
            int newIdx = (idx + currentScale.getDefinition().length + transpositionValue) % currentScale.getDefinition().length;
            int newPitch = note.getPitch() - note.getPitch() % 12 + currentScale.getDefinition()[newIdx];

            if (!ToneResolver.isInRange(newPitch, lCtx.getPitchRange())) {
                // if the note would exceed the pitch range after transposition, just set it to the boundary-pitch
                newPitch = Math.max(newPitch, lCtx.getPitchRange()[0]);
                newPitch = Math.min(newPitch, lCtx.getPitchRange()[1]);
            }
            note.setPitch(newPitch);
        }
    }

    private String getNoteString(List<Note> currentRepetitionNotes, double normalizedMeasureSize) {
        StringBuilder sb = new StringBuilder();
        double duration = 0;
        for (Note note : currentRepetitionNotes) {
            sb.append("-" + note.getPitch() + "-");
            duration += note.getRhythmValue();
            if (duration >= normalizedMeasureSize) {
                sb.append("||");
                duration = 0;
            }
        }
        return sb.toString();
    }

    private void printStructure(MainPartContext lCtx) {
        logger.info("-----Structure-----");
        for (ExtendedPhrase phrase : lCtx.getPhrases()) {
            for (String item : phrase.getStructure()) {
                logger.info(item);
            }
            logger.info("--end-of-phrase--");
        }
        logger.info("-------------");
    }

    public static enum Variation {
        TRUNCATE, CHANGE_DYNAMICS, CHANGE_KEY, RETROGRADE, INVERT, TRANSPOSE, NOTES_TO_RESTS, VARY_BASE_STRUCTURE, VARY_ENDING, MULTIPLY_PITCHES, STABILIZE
    }

    // holder for variables needed in the main part generation
    public static class MainPartContext {
        private ScoreContext scoreContext;
        private double normalizedMeasureSize;
        private int notePitch;
        private double currentMeasureSize;
        private boolean directionUp;
        private Contour contour;
        private int sameLengthNoteSequenceCount;
        private double sameLengthNoteType;
        private int[] pitchRange = new int[2];
        private Set<Integer> uniquePitches = new HashSet<Integer>();
        private List<Double> measureLengths = new ArrayList<Double>();
        private List<Integer> pitches = new ArrayList<>();
        private List<Motif> currentPhraseMotifs = new ArrayList<>();
        private List<ExtendedPhrase> phrases = new ArrayList<>();
        private boolean usePreviousMeasureLengths;
        private int noteIdxInMeasure;
        private double totalLength;
        private int specialNoteMeasures;
        private SpecialNoteType specialNoteType;
        private int maxSpecialMeasures;
        private int totalMeasures;
        private ExtendedPhrase currentPhrase;
        private Scale currentScale;
        private int currentPhraseMotifsCount;
        private int currentPhraseMeasuresCounter;
        private int phraseMeasures;
        private double previousLength;
        private double length;
        private boolean firstNoteInMeasure;
        private boolean endOfMeasure;
        private int contourChangeNotes;
        private int currentContourNotes;
        private boolean simplePhrases;
        private SpecialNoteType dominantSpecialNoteType;
        private List<Integer> currentChordInMelody = new ArrayList<>(3);
        private boolean dynamicsDirectionForte;
        private int dynamicSequenceRemainingNotes;
        private int previousDynamics;
        private boolean nextNoteDownBeat;
        private int circleOfFifthsSequence;
        private int measuresSinceLastMotif;
        private boolean allowOrnaments;
        private boolean allowMoreDissonance;
        private boolean allowSyncopation;
        private int silentSequenceRemainingNotes;

        public double getNormalizedMeasureSize() {
            return normalizedMeasureSize;
        }
        public void setNormalizedMeasureSize(double normalizedMeasureSize) {
            this.normalizedMeasureSize = normalizedMeasureSize;
        }
        public int getNotePitch() {
            return notePitch;
        }
        public void setNotePitch(int notePitch) {
            this.notePitch = notePitch;
        }
        public double getCurrentMeasureSize() {
            return currentMeasureSize;
        }
        public void setCurrentMeasureSize(double currentMeasureSize) {
            this.currentMeasureSize = currentMeasureSize;
        }
        public boolean isDirectionUp() {
            return directionUp;
        }
        public void setDirectionUp(boolean directionUp) {
            this.directionUp = directionUp;
        }
        public Contour getContour() {
            return contour;
        }
        public void setContour(Contour contour) {
            this.contour = contour;
        }
        public int getSameLengthNoteSequenceCount() {
            return sameLengthNoteSequenceCount;
        }
        public void setSameLengthNoteSequenceCount(int eightNoteSequenceCount) {
            this.sameLengthNoteSequenceCount = eightNoteSequenceCount;
        }
        public int[] getPitchRange() {
            return pitchRange;
        }
        public void setPitchRange(int[] pitchRange) {
            this.pitchRange = pitchRange;
        }
        public Set<Integer> getUniquePitches() {
            return uniquePitches;
        }
        public void setUniquePitches(Set<Integer> uniquePitches) {
            this.uniquePitches = uniquePitches;
        }
        public List<Double> getMeasureLengths() {
            return measureLengths;
        }
        public void setMeasureLengths(List<Double> measureLengths) {
            this.measureLengths = measureLengths;
        }
        public List<Integer> getPitches() {
            return pitches;
        }
        public void setPitches(List<Integer> pitches) {
            this.pitches = pitches;
        }
        public List<ExtendedPhrase> getPhrases() {
            return phrases;
        }
        public void setPhrases(List<ExtendedPhrase> phrases) {
            this.phrases = phrases;
        }
        public boolean isUsePreviousMeasureLengths() {
            return usePreviousMeasureLengths;
        }
        public void setUsePreviousMeasureLengths(boolean usePreviousMeasureLengths) {
            this.usePreviousMeasureLengths = usePreviousMeasureLengths;
        }
        public int getNoteIdxInMeasure() {
            return noteIdxInMeasure;
        }
        public void setNoteIdxInMeasure(int noteIdxInMeasure) {
            this.noteIdxInMeasure = noteIdxInMeasure;
        }
        public double getTotalLength() {
            return totalLength;
        }
        public void setTotalLength(double totalLength) {
            this.totalLength = totalLength;
        }
        public int getSpecialNoteMeasures() {
            return specialNoteMeasures;
        }
        public void setSpecialNoteMeasures(int specialNoteMeasures) {
            this.specialNoteMeasures = specialNoteMeasures;
        }
        public SpecialNoteType getSpecialNoteType() {
            return specialNoteType;
        }
        public void setSpecialNoteType(SpecialNoteType specialNoteType) {
            this.specialNoteType = specialNoteType;
        }
        public int getMaxSpecialMeasures() {
            return maxSpecialMeasures;
        }
        public void setMaxSpecialMeasures(int maxSpecialMeasures) {
            this.maxSpecialMeasures = maxSpecialMeasures;
        }
        public int getTotalMeasures() {
            return totalMeasures;
        }
        public void setTotalMeasures(int totalMeasures) {
            this.totalMeasures = totalMeasures;
        }
        public ExtendedPhrase getCurrentPhrase() {
            return currentPhrase;
        }
        public void setCurrentPhrase(ExtendedPhrase currentPhrase) {
            this.currentPhrase = currentPhrase;
        }
        public Scale getCurrentScale() {
            return currentScale;
        }
        public void setCurrentScale(Scale currentScale) {
            this.currentScale = currentScale;
        }
        public int getCurrentPhraseMeasuresCounter() {
            return currentPhraseMeasuresCounter;
        }
        public void setCurrentPhraseMeasuresCounter(int currentPhraseMeasures) {
            this.currentPhraseMeasuresCounter = currentPhraseMeasures;
        }
        public int getPhraseMeasures() {
            return phraseMeasures;
        }
        public void setPhraseMeasures(int phraseMeasures) {
            this.phraseMeasures = phraseMeasures;
        }
        public double getPreviousLength() {
            return previousLength;
        }
        public void setPreviousLength(double previousLength) {
            this.previousLength = previousLength;
        }
        public double getLength() {
            return length;
        }
        public void setLength(double length) {
            this.length = length;
        }
        public boolean isFirstNoteInMeasure() {
            return firstNoteInMeasure;
        }
        public void setFirstNoteInMeasure(boolean firstNoteInMeasure) {
            this.firstNoteInMeasure = firstNoteInMeasure;
        }
        public boolean isEndOfMeasure() {
            return endOfMeasure;
        }
        public void setEndOfMeasure(boolean endOfMeasure) {
            this.endOfMeasure = endOfMeasure;
        }
        public int getContourChangeNotes() {
            return contourChangeNotes;
        }
        public void setContourChangeNotes(int contourChangeNotes) {
            this.contourChangeNotes = contourChangeNotes;
        }
        public int getCurrentContourNotes() {
            return currentContourNotes;
        }
        public void setCurrentContourNotes(int currentContourNotes) {
            this.currentContourNotes = currentContourNotes;
        }
        public boolean isSimplePhrases() {
            return simplePhrases;
        }
        public void setSimplePhrases(boolean simplePhrases) {
            this.simplePhrases = simplePhrases;
        }
        public double getSameLengthNoteType() {
            return sameLengthNoteType;
        }
        public void setSameLengthNoteType(double sameLengthNoteType) {
            this.sameLengthNoteType = sameLengthNoteType;
        }
        public SpecialNoteType getDominantSpecialNoteType() {
            return dominantSpecialNoteType;
        }
        public void setDominantSpecialNoteType(SpecialNoteType dominantSpecialNoteType) {
            this.dominantSpecialNoteType = dominantSpecialNoteType;
        }
        public List<Integer> getCurrentChordInMelody() {
            return currentChordInMelody;
        }
        public void setCurrentChordInMelody(List<Integer> currentChordInMelody) {
            this.currentChordInMelody = currentChordInMelody;
        }
        public boolean isDynamicsDirectionForte() {
            return dynamicsDirectionForte;
        }
        public void setDynamicsDirectionForte(boolean dynamicsDirectionForte) {
            this.dynamicsDirectionForte = dynamicsDirectionForte;
        }
        public int getDynamicSequenceRemainingNotes() {
            return dynamicSequenceRemainingNotes;
        }
        public void setDynamicSequenceRemainingNotes(int dynamicSequenceRemainingNotes) {
            this.dynamicSequenceRemainingNotes = dynamicSequenceRemainingNotes;
        }
        public int getPreviousDynamics() {
            return previousDynamics;
        }
        public void setPreviousDynamics(int previousDynamics) {
            this.previousDynamics = previousDynamics;
        }
        public List<Motif> getCurrentPhraseMotifs() {
            return currentPhraseMotifs;
        }
        public void setCurrentPhraseMotifs(List<Motif> currentPhraseMotifs) {
            this.currentPhraseMotifs = currentPhraseMotifs;
        }
        public int getCurrentPhraseMotifsCount() {
            return currentPhraseMotifsCount;
        }
        public void setCurrentPhraseMotifsCount(int currentPhraseMotifsCount) {
            this.currentPhraseMotifsCount = currentPhraseMotifsCount;
        }
        public boolean isNextNoteDownBeat() {
            return nextNoteDownBeat;
        }
        public void setNextNoteDownBeat(boolean currentNoteUpBeat) {
            this.nextNoteDownBeat = currentNoteUpBeat;
        }
        public int getCircleOfFifthsSequence() {
            return circleOfFifthsSequence;
        }
        public void setCircleOfFifthsSequence(int circleOfFifthsSequence) {
            this.circleOfFifthsSequence = circleOfFifthsSequence;
        }
        public int getMeasuresSinceLastMotif() {
            return measuresSinceLastMotif;
        }
        public void setMeasuresSinceLastMotif(int measuresSinceLastMotif) {
            this.measuresSinceLastMotif = measuresSinceLastMotif;
        }
        public boolean isAllowOrnaments() {
            return allowOrnaments;
        }
        public void setAllowOrnaments(boolean allowOrnaments) {
            this.allowOrnaments = allowOrnaments;
        }
        public boolean isAllowMoreDissonance() {
            return allowMoreDissonance;
        }
        public void setAllowMoreDissonance(boolean allowMoreDissonance) {
            this.allowMoreDissonance = allowMoreDissonance;
        }
        public ScoreContext getScoreContext() {
            return scoreContext;
        }
        public void setScoreContext(ScoreContext scoreContext) {
            this.scoreContext = scoreContext;
        }
        public boolean isAllowSyncopation() {
            return allowSyncopation;
        }
        public void setAllowSyncopation(boolean allowSyncopation) {
            this.allowSyncopation = allowSyncopation;
        }
        public int getSilentSequenceRemainingNotes() {
            return silentSequenceRemainingNotes;
        }
        public void setSilentSequenceRemainingNotes(int silentSequenceRemainingNotes) {
            this.silentSequenceRemainingNotes = silentSequenceRemainingNotes;
        }
    }

    public class Motif {
        private int measures;
        private List<Note> notes;
        public int getMeasures() {
            return measures;
        }
        public void setMeasures(int measures) {
            this.measures = measures;
        }
        public List<Note> getNotes() {
            return notes;
        }
        public void setNotes(List<Note> notes) {
            this.notes = notes;
        }
    }
}