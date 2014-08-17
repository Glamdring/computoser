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

package com.music.websocket;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.model.persistent.Piece;
import com.music.model.prefs.UserPreferences;
import com.music.service.PieceService;
import com.music.web.websocket.Game;
import com.music.web.websocket.GameHandler;
import com.music.web.websocket.dto.GameAction;
import com.music.web.websocket.dto.GameEvent;
import com.music.web.websocket.dto.GameEventType;
import com.music.web.websocket.dto.GameMessage;

public class GameTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Mock
    private PieceService pieceServiceMock;

    private Piece piece;
    private Map<String, Deque<WebSocketMessage<?>>> messages = new ConcurrentHashMap<>();

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        piece = new Piece();
        piece.setId(1L);
        piece.setTempo(60);
        piece.setMetreNumerator(4);
        piece.setMetreDenominator(8);
        piece.setMainInstrument(1);
        when(pieceServiceMock.getPiece(anyLong())).thenReturn(piece);
        when(pieceServiceMock.getNextPieceId(Mockito.<UserPreferences>any())).thenReturn(piece.getId());
    }

    private WebSocketSession getSession(final String id) {
        messages.put(id, new LinkedList<WebSocketMessage<?>>());
        WebSocketSession session = mock(WebSocketSession.class);
        try {
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    messages.get(id).offer((WebSocketMessage<?>) invocation.getArguments()[0]);
                    return null;
                }
            }).when(session).sendMessage(Mockito.<WebSocketMessage<?>>any());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        when(session.getId()).thenReturn(id);
        return session;
    }

    @Test
    public void gameInitializedTest() throws Exception {
        GameHandler handler = new GameHandler();
        initializeGame(handler);

        GameEvent response = mapper.readValue(messages.get("1").removeLast().getPayload().toString(), GameEvent.class);
        Assert.assertEquals(GameEventType.GAME_INITIALIZED, response.getType());

        Assert.assertEquals(1, handler.getGames().size());

        Game game = handler.getGames().values().iterator().next();
        Assert.assertEquals(game.getId(), response.getGameId());
    }

    private void initializeGame(GameHandler handler) {
        try {
            GameMessage msg = new GameMessage();
            msg.setAction(GameAction.INITIALIZE);
            msg.setPlayerName("Game starter");
            TextMessage message = getTextMessage(msg);
            WebSocketSession session = getSession("1");

            handler.afterConnectionEstablished(session);
            handler.handleMessage(session, message);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private TextMessage getTextMessage(GameMessage msg) {
        String payload;
        try {
            payload = mapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        TextMessage message = new TextMessage(payload);
        return message;
    }

    @Test
    public void gameStartedTest() throws Exception {

        GameHandler handler = new GameHandler();
        handler.setPieceService(pieceServiceMock);

        initializeGame(handler);
        Game game = handler.getGames().values().iterator().next();

        GameMessage startMsg = new GameMessage();
        startMsg.setAction(GameAction.START);
        startMsg.setGameId(game.getId());

        WebSocketSession session = getSession("1");
        handler.handleMessage(session, getTextMessage(startMsg));

        GameEvent responseNewPiece = mapper.readValue(messages.get("1").removeLast().getPayload().toString(), GameEvent.class);
        GameEvent responseGameStarted = mapper.readValue(messages.get("1").removeLast().getPayload().toString(), GameEvent.class);
        Assert.assertEquals(GameEventType.GAME_STARTED, responseGameStarted.getType());

        Assert.assertEquals(GameEventType.NEW_PIECE, responseNewPiece.getType());
        Assert.assertEquals(piece.getId(), responseNewPiece.getPieceId());
    }

    @Test
    public void playerJoinedTest() throws Exception {

        GameHandler handler = new GameHandler();
        handler.setPieceService(pieceServiceMock);

        initializeGame(handler);
        Game game = handler.getGames().values().iterator().next();

        GameMessage playerJoinMsg = new GameMessage();
        playerJoinMsg.setAction(GameAction.JOIN);
        playerJoinMsg.setPlayerName("AAA");
        playerJoinMsg.setGameId(game.getId());

        getSession("1");
        WebSocketSession player2Session = getSession("2");

        handler.afterConnectionEstablished(player2Session);

        handler.handleMessage(player2Session, getTextMessage(playerJoinMsg));

        GameEvent playerJoinedEvent = mapper.readValue(messages.get("1").removeLast().getPayload().toString(), GameEvent.class);
        Assert.assertEquals(GameEventType.PLAYER_JOINED, playerJoinedEvent.getType());
        Assert.assertEquals(playerJoinedEvent.getPlayerId(), "2");

        Assert.assertTrue(messages.get("2").size() == 1); // player received a 'player_joined' event for each other player in the game
    }

    @Test
    public void derivedMetreTest() {
        Game game = new Game("1", null);
        Piece piece = new Piece();
        piece.setMetreNumerator(4);
        piece.setMetreDenominator(4);
        Assert.assertFalse(game.isNotDerivedMetre(piece, new int[] {8, 8}));
        Assert.assertTrue(game.isNotDerivedMetre(piece, new int[] {4, 8}));

        piece.setMetreNumerator(8);
        piece.setMetreDenominator(8);
        Assert.assertFalse(game.isNotDerivedMetre(piece, new int[] {4, 4}));
        Assert.assertTrue(game.isNotDerivedMetre(piece, new int[] {2, 4}));

        piece.setMetreNumerator(2);
        piece.setMetreDenominator(4);
        Assert.assertFalse(game.isNotDerivedMetre(piece, new int[] {4, 8}));
        Assert.assertFalse(game.isNotDerivedMetre(piece, new int[] {2, 4}));
        Assert.assertTrue(game.isNotDerivedMetre(piece, new int[] {4, 4}));

    }

    @Test
    public void gameFinishedTest() throws Exception {
        GameHandler handler = new GameHandler();
        handler.setPieceService(pieceServiceMock);

        initializeGame(handler);
        Game game = handler.getGames().values().iterator().next();


        GameMessage playerJoinMsg = new GameMessage();
        playerJoinMsg.setAction(GameAction.JOIN);
        playerJoinMsg.setPlayerName("AAA");
        playerJoinMsg.setGameId(game.getId());
        WebSocketSession session = getSession("1");

        handler.handleMessage(session, getTextMessage(playerJoinMsg));
        game.setSecondsBeforeNextPiece(0);

        // Answer all three questions
        for (int i = 0; i < 3; i ++) {
            game.sendNextPiece();
            GameMessage answerMsg = new GameMessage();
            answerMsg.setAction(GameAction.ANSWER);
            com.music.web.websocket.dto.Answer answer = new com.music.web.websocket.dto.Answer();
            answer.setMainInstrument(0);
            answer.setMetreNumerator(2);
            answer.setMetreDenominator(4);
            answer.setTempo(80);
            answerMsg.setAnswer(answer);
            answerMsg.setGameId(game.getId());
            handler.handleMessage(session, getTextMessage(answerMsg));
        }
        Assert.assertFalse(game.isStarted());
        Assert.assertTrue(game.getResults().getRanking().contains("1"));
        Assert.assertTrue(game.getResults().getScores().containsKey(playerJoinMsg.getPlayerName()));
    }
    //TODO two async players test, automatic new piece test, timeout new piece test

}
