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

import javax.inject.Inject;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.music.model.persistent.TimelineMusic;
import com.music.service.UserService;
import com.music.service.text.TimelineToMusicService;

@Controller
public class TimelineToMusicController {

    @Inject
    private TimelineToMusicService service;

    @Inject
    private UserService userService;

    @Inject
    private UserContext ctx;

    @RequestMapping("/twitterMusic/trigger")
    @ResponseBody
    public Long trigger() {
        return service.makeTimelineMusicRequest(ctx.getUser()).getId();
    }

    @RequestMapping("/twitterMusic/{id}")
    public String get(@PathVariable Long id, Model model) {
        TimelineMusic music = service.getTwitterMusic(id);
        if (music == null) {
            return "redirect:/twitterMusic";
        }
        model.addAttribute("music", music);
        model.addAttribute("currentPage", "twittermusic");
        return "twitterMusic";
    }

    @RequestMapping("/twitterMusic")
    public String home(Model model) {
        boolean hasTwitter = userService.getTwitterAuthentication(ctx.getUser()) != null;
        model.addAttribute("hasTwitter", hasTwitter);
        model.addAttribute("currentPage", "twittermusic");
        return "twitterMusicHome";
    }

}
