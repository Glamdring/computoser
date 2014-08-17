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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import com.music.web.websocket.dto.Answer;
import com.music.web.websocket.dto.GameEvent;
import com.music.web.websocket.dto.GameEventType;
import com.music.web.websocket.dto.GameResults;
import com.music.web.websocket.dto.PlayerResult;
import com.music.web.websocket.dto.PossibleAnswers;

public class Player {

    private static final Logger logger = LoggerFactory.getLogger(Player.class);

    private WebSocketSession session;
    private String name;
    private ConcurrentMap<Long, Answer> answers = new ConcurrentHashMap<>();

    public Player(WebSocketSession session) {
        this.session = session;
    }

    public void gameStarted() {
        GameEvent event = new GameEvent(GameEventType.GAME_STARTED);
        event.setPieceCount(Game.PIECES_PER_GAME);
        GameHandler.sendMessage(event, session);
    }

    public void gameFinished(GameResults results, Game game) {
        GameEvent event = new GameEvent(GameEventType.GAME_FINISHED);
        List<PlayerResult> playerResults = new ArrayList<PlayerResult>();

        for (int i = 0; i < results.getRanking().size(); i ++) {
            PlayerResult result = new PlayerResult();
            result.setRank(i + 1);
            // the scores map is a name-to-score mapping. We get the name for the player, currently iterated on via the list of players of the game
            String name = game.getPlayers().get(results.getRanking().get(i)).getName();
            result.setScore(results.getScores().get(name));
            result.setName(name);
            playerResults.add(result);
        }
        PlayerResult currentPlayerResult = new PlayerResult();
        int rank = results.getRanking().indexOf(session.getId()) + 1;
        int score = results.getScores().get(name);
        currentPlayerResult.setRank(rank);
        currentPlayerResult.setScore(score);

        event.setPlayerResults(playerResults);
        event.setCurrentPlayerResult(currentPlayerResult);
        GameHandler.sendMessage(event, session);
    }

    public void playerLeft(String id) {
        GameEvent event = new GameEvent(GameEventType.PLAYER_LEFT);
        event.setPlayerId(id);
        try {
            GameHandler.sendMessage(event, session);
        } catch (Exception e) {
            logger.warn("Problem sending PLAYER_LEFT message", e);
        }
    }

    public void playerJoined(Player player) {
        GameEvent event = new GameEvent(GameEventType.PLAYER_JOINED);
        event.setPlayerId(player.getSession().getId());
        event.setPlayerName(player.getName());
        GameHandler.sendMessage(event, session);
    }

    public void sendNextPiece(Long pieceId, PossibleAnswers possibleAnswers, int seconds) {
        GameEvent event = new GameEvent(GameEventType.NEW_PIECE);
        event.setPieceId(pieceId);
        event.setSeconds(seconds);
        event.setPossibleAnswers(possibleAnswers);
        GameHandler.sendMessage(event, session);
    }

    public void answer(Game game, Answer answer) {
        if (answers.putIfAbsent(game.getCurrentPieceId(), answer) == null) {
            if (answer != Game.DUMMY_ANSWER) {
                game.playerHasAnswered();
            }

            GameEvent event = new GameEvent(GameEventType.ANSWER_ACCEPTED);
            event.setCorrectAnswer(game.getCurrentCorrectAnswer());
            GameHandler.sendMessage(event, session);
        }
    }

    public WebSocketSession getSession() {
        return session;
    }
    public void setSession(WebSocketSession session) {
        this.session = session;
    }

    public Map<Long, Answer> getAnswers() {
        return answers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
