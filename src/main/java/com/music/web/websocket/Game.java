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

package com.music.web.websocket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.TreeMultimap;
import com.music.MetreConfigurer;
import com.music.model.InstrumentGroups;
import com.music.model.persistent.Piece;
import com.music.model.prefs.UserPreferences;
import com.music.service.PieceService;
import com.music.util.music.InstrumentNameExtractor;
import com.music.web.websocket.dto.Answer;
import com.music.web.websocket.dto.GameResults;
import com.music.web.websocket.dto.Instrument;
import com.music.web.websocket.dto.Metre;
import com.music.web.websocket.dto.PossibleAnswers;

public class Game {

    private static final int SECONDS = 60;
    public static final int PIECES_PER_GAME = 3;
    private static final int ANSWERS_PER_QUESTION = 4;
    private Random random = new Random();

    private static final UserPreferences prefs = new UserPreferences();

    private String id;
    private Map<String, Player> players = new ConcurrentHashMap<>();
    private Long currentPieceId;
    private Answer currentCorrectAnswer;
    private List<Piece> pieces = new ArrayList<>(); // only the host fills this collection, no need for it to be concurrent
    private boolean started;
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private PieceService pieceService;
    private Future<?> nextExecution;

    private Map<Piece, PossibleAnswers> possibleAnswersHistory = new LinkedHashMap<>(); //no need to be concurrent
    private GameResults results = new GameResults();
    public static final Answer DUMMY_ANSWER = new Answer();
    private int secondsBeforeNextPiece = 7;

    public Game(String id, PieceService pieceService) {
        this.id = id;
        this.pieceService = pieceService;
    }

    public void start() {
        started = true;
        for (Player player : players.values()) {
            player.gameStarted();
        }
        sendNextPiece();
    }

    public void playerHasAnswered() {
        // if all players have provided answers for all pieces so far, proceed
        for (Player player : players.values()) {
            if (player.getAnswers().size() < pieces.size()) {
                return;
            }
        }
        if (nextExecution.cancel(false)) {
            scheduleNext(secondsBeforeNextPiece); // schedule next in X seconds, so that players have the time to see the correct answer
        }
    }

    private void scheduleNext(int seconds) {
        nextExecution = executor.schedule(new Runnable() {
            @Override
            public void run() {
                sendNextPiece();
            }
        }, seconds, TimeUnit.SECONDS);
    }

    public void sendNextPiece() {

        // first send blank answers for players who haven't answered (checked in a thread-safe manner in the Player class)
        if (pieces.size() > 0) {
            for (Player player : players.values()) {
                player.answer(this, DUMMY_ANSWER);
            }
        }

        // if this has been the last question, end the game
        if (pieces.size() >= PIECES_PER_GAME) {
            stop();
            return;
        }

        Long pieceId = pieceService.getNextPieceId(prefs);
        Piece piece = pieceService.getPiece(pieceId);
        pieces.add(piece);
        currentPieceId = pieceId;
        fillCurrentCorrectAnswer(piece);

        PossibleAnswers possibleAnswers = generatePossibleAnswers(piece);
        possibleAnswersHistory.put(piece, possibleAnswers);

        for (Player player : players.values()) {
            player.sendNextPiece(pieceId, possibleAnswers, SECONDS);
        }
        scheduleNext(SECONDS + 1); //+1 sec to account for latency
    }

    private void fillCurrentCorrectAnswer(Piece piece) {
        currentCorrectAnswer = new Answer();
        currentCorrectAnswer.setMainInstrument(piece.getMainInstrument());
        currentCorrectAnswer.setMetreNumerator(piece.getMetreNumerator());
        currentCorrectAnswer.setMetreDenominator(piece.getMetreDenominator());
        currentCorrectAnswer.setTempo(piece.getTempo());
    }


    private void stop() {
        nextExecution.cancel(false);
        started = false;
        calculateResults();
        for (Player player : players.values()) {
            player.gameFinished(this.results, this);
        }
    }

    private void calculateResults() {
        TreeMultimap<Integer, String> rankings = TreeMultimap.create();
        for (Player player : players.values()) {

            int score = 0;
            List<Answer> playerAnswers = new ArrayList<>();
            //cannot simply copy the values() of player.getAnswers(), because it is an unordered map (as it needs to be concurrent)
            for (Piece piece : pieces) {
                Answer answer = player.getAnswers().get(piece.getId());
                if (answer.getTempo() > -1) {
                    int diff = Math.abs(answer.getTempo() - piece.getTempo());
                    if (diff < 3) {
                        score += 15;
                    } else {
                        score += 5 / Math.log10(diff);
                    }
                }
                if (answer.getMainInstrument() == piece.getMainInstrument()) {
                    score += 10;
                }
                if (answer.getMetreNumerator() == piece.getMetreNumerator() && answer.getMetreDenominator() == piece.getMetreDenominator()) {
                    score += 10;
                }
                playerAnswers.add(answer);
            }
            results.getScores().put(player.getName(), score);
            rankings.put(score, player.getSession().getId());
        }
        // the ordered player ids
        results.setRanking(new ArrayList<>(rankings.values()));
        Collections.reverse(results.getRanking());
    }

    public boolean playerJoined(Player player) {
        for (Player otherPlayer : players.values()) {
            if (otherPlayer.getName().equals(player.getName())) {
                return false;
            }
        }
        for (Player otherPlayer : players.values()) {
            otherPlayer.playerJoined(player);
            player.playerJoined(otherPlayer); // let the newly joined player know of all the others
        }
        players.put(player.getSession().getId(), player);
        return true;
    }

    public void playerLeft(String playerId) {
        players.remove(playerId);
        for (Player player : players.values()) {
            if (!player.getSession().getId().equals(playerId)) {
                player.playerLeft(playerId);
            }
        }
    }

    private PossibleAnswers generatePossibleAnswers(Piece piece) {
        PossibleAnswers answers = new PossibleAnswers();

        // first select the instrument distractors
        int instrument = piece.getMainInstrument();
        List<Instrument> instruments = new ArrayList<>(ANSWERS_PER_QUESTION);
        instruments.add(new Instrument(instrument, InstrumentNameExtractor.getInstrumentName(instrument)));
        while (instruments.size() < ANSWERS_PER_QUESTION) {
            int[] set = random.nextBoolean() ? InstrumentGroups.MAIN_PART_INSTRUMENTS : InstrumentGroups.MAIN_PART_ONLY_INSTRUMENTS;
            int selectedId = set[random.nextInt(set.length)];
            Instrument newInstrument = new Instrument(selectedId, InstrumentNameExtractor.getInstrumentName(selectedId));
            if (selectedId != instrument && !instruments.contains(newInstrument)) {
                instruments.add(newInstrument);
            }
        }
        Collections.shuffle(instruments, random);

        answers.setInstruments(instruments);

        // fill the metre distractors
        List<Metre> metres = new ArrayList<>(ANSWERS_PER_QUESTION);
        metres.add(new Metre(piece.getMetreNumerator(), piece.getMetreDenominator()));
        while (metres.size() < ANSWERS_PER_QUESTION) {
            int[] metre = MetreConfigurer.getRandomMetre(random);
            // disallow metres that have the same ratios as the correct answer
            Metre newMetre = new Metre(metre[0], metre[1]);
            if (!metres.contains(newMetre) && isNotDerivedMetre(piece, metre)) {
                metres.add(newMetre);
            }
        }
        Collections.shuffle(metres, random);

        answers.setMetres(metres);
        return answers;
    }

    public boolean isNotDerivedMetre(Piece piece, int[] metre) {
        return ((double) metre[0]) / piece.getMetreNumerator() != ((double) metre[1]) / piece.getMetreDenominator();
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public Map<String, Player> getPlayers() {
        return players;
    }

    public void setPlayers(Map<String, Player> players) {
        this.players = players;
    }

    public Long getCurrentPieceId() {
        return currentPieceId;
    }
    public void setCurrentPieceId(Long currentPieceId) {
        this.currentPieceId = currentPieceId;
    }
    public boolean isStarted() {
        return started;
    }
    public void setStarted(boolean started) {
        this.started = started;
    }

    public Answer getCurrentCorrectAnswer() {
        return currentCorrectAnswer;
    }

    public GameResults getResults() {
        return results;
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    public void setSecondsBeforeNextPiece(int secondsBeforeNextPiece) {
        this.secondsBeforeNextPiece = secondsBeforeNextPiece;
    }

}
