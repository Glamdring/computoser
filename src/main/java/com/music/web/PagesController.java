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

import java.io.IOException;
import java.util.Arrays;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.music.model.persistent.PiecePack;
import com.music.service.NewsService;
import com.music.service.PiecePackService;

@Controller
public class PagesController {

    @Inject
    private NewsService newsService;

    @Inject
    private PiecePackService packService;

    @RequestMapping("/about")
    public String about() {
        return "about";
    }

    @RequestMapping("/signup")
    public String signup() {
        return "signup";
    }

    @RequestMapping("/info/imprint")
    public String imprint() {
        return "imprint";
    }

    @RequestMapping("/info/terms")
    public String terms() {
        return "terms";
    }

    @RequestMapping("/news")
    public String news(@RequestParam(required=false) Long id, Model model) {
        if (id == null) {
            model.addAttribute("news", newsService.getNews());
        } else {
            model.addAttribute("news", Arrays.asList(newsService.get(id)));
        }
        return "news";
    }

    @RequestMapping("/stockmusic")
    public String stockMusic(Model model) {
        model.addAttribute("packs", packService.getPiecePacks());
        return "stockpacks";
    }

    @RequestMapping("/pack/download")
    public void downloadPack(@RequestParam long id, HttpServletResponse response) throws IOException {
        PiecePack pack = packService.getPack(id);
        if (pack != null) {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=" + pack.getName().replace(' ', '_').replace('/', '_') + ".zip;");
            packService.download(id, response.getOutputStream());
        }
    }
}
