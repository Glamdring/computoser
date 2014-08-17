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

package com.music.service;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import jm.music.data.Score;
import jm.util.Read;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.music.MainPartGenerator;
import com.music.dao.PieceDao;
import com.music.model.InstrumentGroups;
import com.music.model.PartType;
import com.music.model.persistent.Piece;
import com.music.model.prefs.Tempo;
import com.music.scheduled.BackupJob;
import com.music.scheduled.FeedGenerator;
import com.music.scheduled.PieceDigestSendingJob;

@ManagedResource
@Component
public class ManagementService {
    private static final Logger logger = LoggerFactory.getLogger(ManagementService.class);

    @Inject
    private BackupJob backupJob;

    @Inject
    private PieceDigestSendingJob digestSendingJob;

    @Inject
    private FeedGenerator feedGenerator;

    @Inject
    private PieceService pieceService;

    @Inject
    private PurchaseService purchaseService;

    @Inject
    private PieceDao dao;

    @Async
    @ManagedOperation
    public void backup() {
        backupJob.run();
    }

    @ManagedOperation
    @Async
    public void sendDailyEmails() {
        digestSendingJob.sendEmails();
    }

    @ManagedOperation
    @Async
    public void generateFeed() {
        feedGenerator.triggerFeedGeneration();
    }

    @ManagedOperation
    @Async
    public void resendPurchaseEmail(long purchaseId) {
        purchaseService.resentPurchaseEmail(purchaseId);
    }

    @ManagedOperation
    @Async
    @Transactional
    public void fillTempo() {
        List<Piece> pieces = dao.listOrdered(Piece.class, "id");
        for (Piece piece : pieces) {
            piece.getIntermediateDecisions().setTempoType(Tempo.forValue(piece.getTempo()));
            dao.persist(piece);
        }
    }

    @ManagedOperation
    @Async
    @Transactional
    public void fillElectronic() {
        List<Piece> pieces = dao.listOrdered(Piece.class, "id");
        for (Piece piece : pieces) {
            piece.getIntermediateDecisions().setElectronic(
                    piece.getParts().contains(PartType.PAD1.toString())
                            || Arrays.binarySearch(InstrumentGroups.ELECTRONIC_MAIN_PART_ONLY_INSTRUMENTS,
                                    piece.getMainInstrument()) > -1);
            dao.persist(piece);
        }
    }


    @ManagedOperation
    @Async
    public void fillVariation() {
        List<Piece> pieces = dao.listOrdered(Piece.class, "id");
        for (Piece piece : pieces) {
            try {
                InputStream is = pieceService.getPieceMidiFile(piece.getId());
                byte[] midi = IOUtils.toByteArray(is);
                File tmp = File.createTempFile("tmp", "mid");
                FileUtils.writeByteArrayToFile(tmp, midi);
                Score score = new Score();
                Read.midi(score, tmp.getAbsolutePath());
                double variation = MainPartGenerator.calculateVariation(score.getPart(0));
                piece.setVariation(variation);
                pieceService.save(piece);
                tmp.delete();
            } catch (Exception ex) {
                logger.error("Problem seting variety", ex);
            }

        }
    }
}