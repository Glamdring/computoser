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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.feed.AbstractRssFeedView;

import com.music.model.persistent.Piece;
import com.music.model.prefs.UserPreferences;
import com.music.service.PieceService;
import com.music.service.PurchaseService;
import com.music.util.SecurityUtils;
import com.music.util.music.Chance;
import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Item;

@Controller
public class MusicController {
    private static final Logger logger = LoggerFactory.getLogger(MusicController.class);

    @Inject
    private PieceService pieceService;
    @Inject
    private PurchaseService purchaseService;
    @Inject
    private UserContext userContext;
    @Value("${base.url}")
    private String baseUrl;
    @Value("${hmac.key}")
    private String hmacKey;
    @Value("${cloudfront.root}")
    private String cloudfrontRoot;

    @RequestMapping("/music/get")
    @ResponseBody
    public long getMusic(UserPreferences preferences, BindingResult bindingResult) {
        // report and ignore errors in binding the UserPreferences object
        if (bindingResult.hasErrors()) {
            logger.error("Binding errors: " + bindingResult.getAllErrors());
        }
        if (Chance.test(7)){
            return pieceService.getRandomTopPieceId();
        } else {
            return pieceService.getNextPieceId(preferences);
        }
    }

    @RequestMapping("/music/get/{id}")
    public void getPiece(@PathVariable long id,
            @RequestParam(required = false, defaultValue = "false") boolean mobileApp,
            @RequestParam(required = false, defaultValue = "false") boolean download, OutputStream os,
            HttpServletRequest request, HttpServletResponse response) throws IOException {

        Long userId = null;
        if (userContext.getUser() != null) {
            userId = userContext.getUser().getId();
        }
        if (download) {
            pieceService.incrementDownloads(id, false);
        } else {
            pieceService.storePlay(id, userId, request.getRemoteAddr(), mobileApp);
        }
        response.setHeader("Content-Disposition", "attachment; filename=\"" + id + ".mp3\"");
        setNotCacheable(response);
        response.sendRedirect(cloudfrontRoot + "/" + id + ".mp3");
    }

    @RequestMapping("/music/getMidi/{id}")
    public void getPieceMidi(@PathVariable long id, OutputStream os,
            HttpServletRequest request, HttpServletResponse response) throws IOException {

        pieceService.incrementDownloads(id, true);

        response.setHeader("Content-Disposition", "attachment; filename=\"" + id + ".midi\"");
        setNotCacheable(response);
        response.setContentType("audio/midi");
        InputStream is = pieceService.getPieceMidiFile(id);
        IOUtils.copy(is, os);
    }

    @RequestMapping("/music/getXml/{id}")
    public void getPieceMusicXml(@PathVariable long id, OutputStream os,
            HttpServletRequest request, HttpServletResponse response) {

        response.setHeader("Content-Disposition", "attachment; filename=\"" + id + ".xml\"");
        response.setHeader("X-Robot-Tag", "noindex");

        setNotCacheable(response);
        response.setContentType("text/xml");
        try {
            InputStream is = pieceService.getPieceMusicXml(id);
            IOUtils.copy(is, os);
        } catch (IOException ex) {
            logger.warn("Problem getting musicXML", ex.getMessage());
        }
    }

    @RequestMapping("/music/getTitle/{id}")
    @ResponseBody
    public String getPieceTitle(@PathVariable long id) {
        return pieceService.getPiece(id).getTitle();
    }

    @RequestMapping("/track/{id}")
    public String getTrack(@PathVariable long id, Model model) {
        model.addAttribute("pieceId", id);
        Piece piece = pieceService.getPiece(id);
        if (piece != null) {
            model.addAttribute("title", piece.getTitle());
            model.addAttribute("likes", piece.getLikes());
            return "track";
        } else {
            logger.warn("No track with id=" + id + " found");
            return "redirect:/";
        }
    }

    @RequestMapping("/mytracks")
    public String getMyTracks(@RequestParam(required=false, defaultValue="0") int page, Model model) {
        if (userContext.getUser() != null) {
            List<Piece> pieces = pieceService.getUserPieces(userContext.getUser().getId(), page);
            model.addAttribute("title", "Tracks that you have liked");
            model.addAttribute("currentPage", "mytracks");
            model.addAttribute("pieces", pieces);
            return "tracks";
        } else {
            return "redirect:/";
        }
    }

    @RequestMapping("/toptracks")
    public String getTopTracks(@RequestParam(required=false, defaultValue="0") int page, Model model) {
        List<Piece> pieces = pieceService.getTopPieces(page);
        model.addAttribute("title", "Most liked tracks");
        model.addAttribute("currentPage", "toptracks");
        model.addAttribute("pieces", pieces);
        return "tracks";
    }

    @RequestMapping("/toprecent")
    public String getTopRecentTracks(@RequestParam(required=false, defaultValue="0") int page, Model model) {
        List<Piece> pieces = new ArrayList<>();
        // this is outside the session, so it doesn't affect the db. Change when switching to DTOs
        for (Entry<Piece, Integer> entry : pieceService.getTopRecentPieces(page).entrySet()) {
            entry.getKey().setLikes(entry.getValue());
            pieces.add(entry.getKey());
        }

        model.addAttribute("title", "Most liked tracks in the past week");
        model.addAttribute("currentPage", "toprecent");
        model.addAttribute("pieces", pieces);
        return "tracks";
    }

    private void setNotCacheable(HttpServletResponse response) {
        response.setHeader("Pragma", "no-cache");
        // HTTP 1.0 header
        response.setDateHeader("Expires", 1L);
        // HTTP 1.1 header: "no-cache" is the standard value,
        // "no-store" is necessary to prevent caching on FireFox.
        response.setHeader("Cache-Control", "no-cache");
        response.addHeader("Cache-Control", "no-store");
    }

    @RequestMapping("/music/evaluate/{id}")
    @ResponseBody
    public void evaluate(@PathVariable long id, @RequestParam boolean positive, HttpServletRequest request) throws IOException {
        Long userId = null;
        if (userContext.getUser() != null) {
            userId = userContext.getUser().getId();
        }
        pieceService.evaluate(id, userId, positive, request.getRemoteAddr());
    }

    @RequestMapping("/")
    public String index() {
        return "index";
    }

    @ModelAttribute("userContext")
    public UserContext getUserContext() {
        return userContext;
    }

    @RequestMapping("/rss")
    public View userRss(Model model) {
        List<Piece> pieces = pieceService.getRssFeed();
        model.addAttribute("pieces", pieces);
        PieceRssView view = new PieceRssView();
        view.setContentType("application/rss+xml;charset=UTF-8");
        return view;
    }

    @RequestMapping("/purchase/download/{id}/{hmac}")
    public void downloadPurchase(@PathVariable String id, @PathVariable String hmac, HttpServletResponse response) throws IOException {
        if (SecurityUtils.hmac(StringUtils.leftPad(id, 10, '0'), hmacKey).equals(hmac)) {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "Content-Disposition: attachment; filename=computoser-tracks-" + id + ".zip;");
            purchaseService.download(Long.parseLong(id), response.getOutputStream());
        }
    }

    @RequestMapping("/search")
    public String search(UserPreferences preferences, Model model) {
        List<Piece> pieces = pieceService.search(preferences);
        model.addAttribute("searchEnabled", Boolean.TRUE);
        model.addAttribute("pieces", pieces);
        model.addAttribute("preferences", preferences);
        model.addAttribute("title", "Search");
        model.addAttribute("currentPage", "search");
        return "tracks";
    }

    @RequestMapping("/radio")
    public void radio(OutputStream os, HttpServletRequest request, HttpServletResponse response) {
    }

    private class PieceRssView extends AbstractRssFeedView {

        @SuppressWarnings("unchecked")
        @Override
        protected List<Item> buildFeedItems(Map<String, Object> model, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            List<Piece> pieces = (List<Piece>) model.get("pieces");
            List<Item> items = new ArrayList<Item>(pieces.size());

            for (Piece piece : pieces) {
                Item item = new Item();
                item.setAuthor("Computoser");
                item.setPubDate(piece.getGenerationTime().toDate());
                item.setLink(baseUrl + "/track/" + piece.getId());
                item.setTitle(piece.getTitle());
                items.add(item);
            }

            return items;
        }

        @Override
        protected Channel newFeed() {
            Channel channel = super.newFeed();
            channel.setTitle("Computoser - computser-generaces music");
            channel.setDescription(baseUrl);
            channel.setLink(baseUrl + "/rss");
            channel.setEncoding("utf-8");
            return channel;
        }
    }
}
