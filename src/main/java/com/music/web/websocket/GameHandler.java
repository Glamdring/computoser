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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.service.PieceService;
import com.music.web.websocket.dto.GameEvent;
import com.music.web.websocket.dto.GameEventType;
import com.music.web.websocket.dto.GameMessage;

@Component
public class GameHandler extends AbstractWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(GameHandler.class);

    private ObjectMapper mapper = new ObjectMapper();

    @Inject
    private PieceService pieceService;

    private Map<String, Game> games = new ConcurrentHashMap<>();
    private Map<String, Game> pendingGames = new ConcurrentHashMap<>();
    private Map<String, Player> players = new ConcurrentHashMap<>();
    private Map<String, Game> playerGames = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Player player = new Player(session);
        players.put(session.getId(), player);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        leave(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        try {
            GameMessage message = getMessage(textMessage);
            switch(message.getAction()) {
                case INITIALIZE: initialize(message, session); break;
                case JOIN: join(message.getGameId(), message.getPlayerName(), session); break;
                case LEAVE: leave(session.getId()); break;
                case START: startGame(message); break;
                case ANSWER: answer(message, session.getId()); break;
                case JOIN_RANDOM: joinRandomGame(message.getPlayerName(), session); break;
            }
        } catch (Exception ex) {
            logger.error("Exception occurred while handling message", ex);
        }
    }

    private void startGame(GameMessage message) {
        Game game = games.get(message.getGameId());
        if (game != null) {
            game.start();
        }
        pendingGames.remove(game.getId());
    }

    private void answer(GameMessage message, String playerId) {
        Game game = games.get(message.getGameId());
        Player player = game.getPlayers().get(playerId);
        player.answer(game, message.getAnswer());
    }

    private void leave(String playerId) {
        players.remove(playerId);
        Game game = playerGames.remove(playerId);
        if (game != null) {
            game.playerLeft(playerId);
            // if this is the last player, remove the orphaned game
            if (game.getPlayers().isEmpty()) {
                games.remove(game.getId());
                pendingGames.remove(game.getId());
            }
        }
    }

    private void join(String gameId, String playerName, WebSocketSession session) {
        Game game = games.get(gameId);
        if (game == null) {
            sendMessage(new GameEvent(GameEventType.NO_GAME_AVAILABLE), session);
        }
        if (!game.isStarted()) {
            String playerId = session.getId();
            Player player = players.get(playerId);
            player.setName(playerName);
            playerGames.put(playerId, game);
            if (!game.playerJoined(player)) {
                sendMessage(new GameEvent(GameEventType.PLAYER_NAME_TAKEN), session);
            }
        } else {
            sendMessage(new GameEvent(GameEventType.GAME_ALREADY_STARTED), session);
        }
    }

    public static void sendMessage(GameEvent event, WebSocketSession session) {
        try {
            session.sendMessage(new TextMessage(event.toJson()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void joinRandomGame(String playerName, WebSocketSession session) {
        if (pendingGames.isEmpty()) {
            sendMessage(new GameEvent(GameEventType.NO_GAME_AVAILABLE), session);
            return;
        }
        List<Game> games = new ArrayList<>(this.pendingGames.values());
        Random random = new Random();

        Game game = pendingGames.get(random.nextInt(games.size()));
        join(game.getId(), playerName, session);
        GameEvent event = new GameEvent(GameEventType.RANDOM_GAME_JOINED);
        event.setGameId(game.getId());
        sendMessage(event, session);
    }

    private void initialize(GameMessage message, WebSocketSession session) {
        String gameId = UUID.randomUUID().toString();
        Game game = new Game(gameId, pieceService);
        games.put(gameId, game);
        pendingGames.put(gameId, game);

        join(gameId, message.getPlayerName(), session);
        GameEvent event = new GameEvent(GameEventType.GAME_INITIALIZED);
        event.setGameId(gameId);
        sendMessage(event, session);
    }

    public GameMessage getMessage(TextMessage message) {
        try {
            return mapper.readValue(message.getPayload(), GameMessage.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public Map<String, Game> getGames() {
        return games;
    }
    public void setPieceService(PieceService pieceService) {
        this.pieceService = pieceService;
    }
}
