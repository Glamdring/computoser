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

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GameEvent {

    @JsonIgnore
    private static final ObjectMapper mapper = new ObjectMapper();

    private GameEventType type;
    private String playerName;
    private String playerId;
    private String gameId;
    private Long pieceId;
    private Integer seconds;
    private PossibleAnswers possibleAnswers;
    private List<PlayerResult> playerResults;
    private PlayerResult currentPlayerResult;
    private Answer correctAnswer;
    private int pieceCount;

    public GameEvent(GameEventType type) {
        this.type = type;
    }

    public GameEvent() {
        // needed for deserialization
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public GameEventType getType() {
        return type;
    }
    public void setType(GameEventType type) {
        this.type = type;
    }
    public String getPlayerName() {
        return playerName;
    }
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    public String getGameId() {
        return gameId;
    }
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public Long getPieceId() {
        return pieceId;
    }
    public void setPieceId(Long pieceId) {
        this.pieceId = pieceId;
    }
    public Integer getSeconds() {
        return seconds;
    }
    public void setSeconds(Integer seconds) {
        this.seconds = seconds;
    }
    public PossibleAnswers getPossibleAnswers() {
        return possibleAnswers;
    }
    public void setPossibleAnswers(PossibleAnswers possibleAnswers) {
        this.possibleAnswers = possibleAnswers;
    }

    public Answer getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(Answer correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public int getPieceCount() {
        return pieceCount;
    }

    public void setPieceCount(int trackCount) {
        this.pieceCount = trackCount;
    }

    public List<PlayerResult> getPlayerResults() {
        return playerResults;
    }

    public void setPlayerResults(List<PlayerResult> playerResults) {
        this.playerResults = playerResults;
    }

    public PlayerResult getCurrentPlayerResult() {
        return currentPlayerResult;
    }

    public void setCurrentPlayerResult(PlayerResult currentPlayerResult) {
        this.currentPlayerResult = currentPlayerResult;
    }

    public String toJson() {
        try {
            return mapper.writeValueAsString(this);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
