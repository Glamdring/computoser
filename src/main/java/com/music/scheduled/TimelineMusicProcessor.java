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

package com.music.scheduled;

import javax.inject.Inject;

import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.music.dao.UserDao;
import com.music.model.persistent.TimelineMusic;
import com.music.model.persistent.TimelineMusicRequest;
import com.music.service.EmailService;
import com.music.service.EmailService.EmailDetails;
import com.music.service.text.TimelineToMusicService;

@Component
public class TimelineMusicProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TimelineMusicProcessor.class);

    @Inject
    private TimelineToMusicService service;

    @Inject
    private UserDao dao;

    @Inject
    private EmailService emailService;

    @Value("${information.email.sender}")
    private String from;

    @Value("${base.url}")
    private String baseUrl;

    @Scheduled(fixedDelay=10 * DateTimeConstants.MILLIS_PER_SECOND)
    public void run() {
        try {
            TimelineMusicRequest request = dao.getUnprocessedTimelineMusicRequest();
            if (request == null) {
                return;
            }
            long start = System.currentTimeMillis();
            TimelineMusic music = service.storeUserTimelinePiece(request.getUser());
            service.completeRequest(request, start);
            sendEmail(music);
        } catch (Exception ex) {
            logger.error("Problem processing twitter music request", ex);
        }
    }

    private void sendEmail(TimelineMusic music) {
        EmailDetails details = new EmailDetails();
        String url = baseUrl + "/twitterMusic/" + music.getId();
        details.setMessage("Hello.<br />Your twitter music is ready - check it out at <a href=\"" + url + "\">" + url + "</a>");
        details.setSubject("Your twitter music is ready");
        details.setFrom(from);
        details.setTo(music.getUser().getEmail());
        details.setHtml(true);
        emailService.send(details);
    }
}
