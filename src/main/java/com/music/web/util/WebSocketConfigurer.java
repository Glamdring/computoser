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

package com.music.web.util;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;

import com.music.web.websocket.GameHandler;

@Configuration
public class WebSocketConfigurer {

    @Inject
    private GameHandler gameHandler;

    @Bean
    public SimpleUrlHandlerMapping handlerMapping() {

        SockJsService sockJsService = new DefaultSockJsService(taskScheduler());

        Map<String, Object> urlMap = new HashMap<String, Object>();
        urlMap.put("/game/websocket/**", new SockJsHttpRequestHandler(sockJsService, gameHandler));

        SimpleUrlHandlerMapping hm = new SimpleUrlHandlerMapping();
        hm.setUrlMap(urlMap);
        return hm;
    }

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
      ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
      taskScheduler.setThreadNamePrefix("SockJS-");
      return taskScheduler;
    }

}