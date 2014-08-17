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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.music.service.PieceService;
import com.music.util.SharedData;

@Component
public class MusicGenerationJob {

    private static final Logger logger = LoggerFactory.getLogger(MusicGenerationJob.class);

    @Inject
    private PieceService service;

    @Inject
    private SharedData sharedData;

    @Scheduled(fixedDelay = 12 * DateTimeConstants.MILLIS_PER_MINUTE)
    public void generate() {
        if (sharedData.isGenerateMusic()) {
            logger.info("Starting piece generation");
            //TODO more precise adjustment this according to users' listening activity
            int piecesNeeded = sharedData.isAdaptGenerationQuantity() && sharedData.getListeningRequests().get() > 10 ? 3 : 1;
            for (int i = 0; i < piecesNeeded; i ++) {
                long id = service.generatePiece();
                sharedData.setMaxId(id);
            }
            // reset
            sharedData.getListeningRequests().set(0);
        }
    }
}
