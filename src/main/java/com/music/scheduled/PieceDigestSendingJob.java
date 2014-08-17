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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Maps;
import com.music.dao.PageableOperation;
import com.music.dao.PieceDao;
import com.music.dao.UserDao;
import com.music.model.persistent.Piece;
import com.music.model.persistent.User;
import com.music.service.EmailService;
import com.music.service.EmailService.EmailDetails;
import com.music.util.SecurityUtils;

@Component
public class PieceDigestSendingJob {
    private static final Logger logger = LoggerFactory.getLogger(PieceDigestSendingJob.class);

    @Inject
    private PieceDao pieceDao;
    @Inject
    private UserDao userDao;
    @Inject
    private EmailService emailService;
    @Value("${information.email.sender}")
    private String from;
    @Value("${hmac.key}")
    private String hmacKey;

    @PostConstruct
    public void init() {
        logger.info("Digest job initialized");
    }

    @Scheduled(cron="0 0 9 ? * 1,4")
    @Transactional(readOnly=true)
    public void sendPieceDigestEmails() {
        logger.info("Sending email digests started");
        DateTime now = new DateTime();
        DateTime minusTwoDays = now.minusDays(2);
        List<Piece> pieces = pieceDao.getPiecesInRange(minusTwoDays, now);
        Collections.shuffle(pieces);
        final List<Piece> includedPieces = new ArrayList<>(pieces.subList(0, Math.min(pieces.size(), 3)));
        if (includedPieces.isEmpty()) {
            return;
        }
        // for now - using the same data for all users. TODO send personalized selection
        final EmailDetails baseDetails = new EmailDetails();
        baseDetails.setMessageTemplate("digest.vm");
        baseDetails.setSubject("Computoser-generated tracks digest for you");
        Map<String, Object> model = Maps.newHashMap();
        baseDetails.setMessageTemplateModel(model);
        baseDetails.setFrom(from);
        baseDetails.setHtml(true);
        userDao.performBatched(User.class, 100, new PageableOperation<User>() {
            @Override
            public void execute() {
                for (User user : getData()) {
                    if (user.isReceiveDailyDigest() && StringUtils.isNotBlank(user.getEmail())) {
                        EmailDetails email = SerializationUtils.clone(baseDetails);
                        email.setTo(user.getEmail());
                        email.setCurrentUser(user);
                        String hmac = SecurityUtils.hmac(user.getEmail(), hmacKey);
                        email.getMessageTemplateModel().put("pieces", includedPieces);
                        email.getMessageTemplateModel().put("hmac", hmac);
                        // needed due to SES restrictions
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            throw new IllegalStateException(e);
                        }
                        emailService.send(email);
                    }
                }
            }
        });
    }

    @Transactional(readOnly=true)
    @Async
    public void sendEmails() {
        try {
            sendPieceDigestEmails();
        } catch (Exception ex) {
            logger.warn("Problem sending emails", ex);
        }
    }
}
