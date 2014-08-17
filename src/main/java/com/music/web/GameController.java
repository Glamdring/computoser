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

package com.music.web;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.music.web.websocket.Game;

@Controller
@RequestMapping("/game")
public class GameController {

    private Map<String, Game> games = new ConcurrentHashMap<>();

    @RequestMapping({"/", ""})
    public String gameIndex() {
        return "game";
    }

    @MessageMapping("/initialize")
    //@RequestMapping("/initialize")
    public String initialize(Principal principal) {

        return null; //game.getId();
    }

    private void addCurrentUser(Game game, Principal principal) {
        //game.getPlayers().add(principal.getName());
        //game.getAnswers().put(principal.getName(), new ArrayList<Integer>());
    }

    @MessageMapping("/join")
    @ResponseBody
    public void joinGame(String gameId, Principal principal) {
        Game game = games.get(gameId);
        if (game != null && !game.isStarted()) {
            addCurrentUser(game, principal);
        } else {
            // Game doesn't exist or is already started
        }
    }

    @RequestMapping("/nextPiece")
    public Long getNextPiece(String gameId) {
        return 0L;
    }

    @MessageMapping("/answer")
    public void answer(Long userId, String gameId, int bpm) {
        //games.get(gameId).getAnswers().get(userId).add(bpm);
    }
}
