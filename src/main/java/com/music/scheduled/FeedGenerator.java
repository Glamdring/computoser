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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.music.dao.PieceDao;
import com.music.model.persistent.FeedEntry;
import com.music.model.persistent.Piece;

@Component
public class FeedGenerator {
    private static final Logger logger = LoggerFactory.getLogger(FeedGenerator.class);

    @Inject
    private PieceDao dao;

    @Scheduled(cron = "0 0 0 * * ?") //every midnight
    @Transactional
    public void generateFeed() {
        logger.info("Generating feed");
        DateTime now = new DateTime();
        DateTime oneDayAgo = now.minusDays(1);
        List<Piece> pieces = dao.getPiecesInRange(oneDayAgo, now);
        Collections.shuffle(pieces);
        logger.info("Found " + pieces.size() + " eligible pieces");
        int persisted = 0;
        for (Piece piece : pieces) {
            if (piece.getLikes() < 3 && piece.getLikes() > -3) {
                FeedEntry entry = new FeedEntry();
                entry.setInclusionTime(now);
                entry.setPiece(piece);
                dao.persist(entry);
                persisted++;
            }
            if (persisted == 2) {
                break;
            }
        }
        logger.info("Inserted " + persisted + " entries into the feed");
    }

    @Transactional
    @Async
    public void triggerFeedGeneration() {
        try {
            generateFeed();
        } catch (Exception ex) {
            logger.warn("Problem generating feed", ex);
        }
    }

}
