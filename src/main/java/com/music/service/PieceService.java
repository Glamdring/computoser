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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Resource;
import javax.inject.Inject;

import jm.constants.Instruments;
import jm.music.data.Score;
import jm.util.Write;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Joiner;
import com.music.Generator;
import com.music.MusicXmlRenderer;
import com.music.ScoreContext;
import com.music.dao.PieceDao;
import com.music.model.PartType;
import com.music.model.persistent.Piece;
import com.music.model.persistent.PieceEvaluation;
import com.music.model.persistent.PiecePlay;
import com.music.model.persistent.User;
import com.music.model.prefs.Tempo;
import com.music.model.prefs.UserPreferences;
import com.music.util.MutingPrintStream;
import com.music.util.RetryableOperation;
import com.music.util.SharedData;
import com.music.util.music.SMFTools;

@Service
@ManagedResource
public class PieceService {

    private static final Logger logger = LoggerFactory.getLogger(PieceService.class);

    private Random random = new Random();

    @Inject
    private Generator generator;

    @Inject
    private SharedData sharedData;

    @Inject
    private PieceDao dao;

    @Resource(name = "${filesystem.implementation}")
    private FileStorageService fileStorageService;

    @Value("${storage.dir}")
    private String storageDir;

    @Value("${algorithm.version}")
    private String algorithmVersion;

    private Joiner joiner = Joiner.on(",");

    @Transactional
    public long generatePiece() {
        long start = System.currentTimeMillis();
        ScoreContext ctx = generator.generatePiece();
        logger.info("Piece generation took: " + (System.currentTimeMillis() - start) + " millis");
        byte[] mp3 = null;
        byte[] midi = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                start = System.currentTimeMillis();
                MutingPrintStream.ignore.set(true);
                Write.midi(ctx.getScore(), baos);
                logger.info("Writing MIDI in memory took: " + (System.currentTimeMillis() - start) + " millis");
            } finally {
                MutingPrintStream.ignore.set(null);
            }
            midi = baos.toByteArray();
            mp3 = generator.toMp3(midi);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Piece piece = savePiece(ctx, mp3, midi);

        return piece.getId();
    }

    @Transactional(readOnly = true)
    public long getRandomTopPieceId() {
        // return the if of a random existing top 200 track
        List<Piece> topPieces = dao.getTopPieces(0, 200);
        return topPieces.get(random.nextInt(topPieces.size())).getId();
    }

    @Transactional
    public void evaluate(long pieceId, Long userId, boolean positive, String ip) {
        if (dao.getEvaluation(pieceId, userId, ip) == null) {
            PieceEvaluation evaluation = new PieceEvaluation();
            evaluation.setDateTime(new DateTime());
            if (userId != null) {
                evaluation.setUser(dao.getById(User.class, userId));
            }
            Piece piece = dao.getById(Piece.class, pieceId);
            evaluation.setPiece(piece);
            evaluation.setPositive(positive);
            evaluation.setIp(ip);
            dao.persist(evaluation);

            // not exact due to race condition - a job should count likes every
            // X minutes
            if (positive) {
                piece.setLikes(piece.getLikes() + 1);
            } else {
                piece.setLikes(piece.getLikes() - 1);
            }
        }
    }

    private Piece savePiece(ScoreContext ctx, byte[] mp3, byte[] midi) {
        Piece piece = new Piece();
        piece.setNewlyCreated(true);
        piece.setGenerationTime(new DateTime());
        piece.setTitle(ctx.getScore().getTitle());
        piece.setKeyNote(ctx.getKeyNote());
        piece.setMeasures(ctx.getMeasures());
        piece.setNormalizedMeasureSize(ctx.getNormalizedMeasureSize());
        piece.setTempo((int) ctx.getScore().getTempo());
        piece.setNoteLengthCoefficient(ctx.getNoteLengthCoefficient());
        piece.setScale(ctx.getScale());
        piece.setAlternativeScale(ctx.getAlternativeScale());
        piece.setUpBeatLength(ctx.getUpBeatLength());
        piece.setMetreNumerator(ctx.getMetre()[0]);
        piece.setMetreDenominator(ctx.getMetre()[1]);
        piece.setMainInstrument(ctx.getParts().get(PartType.MAIN).getInstrument());
        piece.setVariation(ctx.getVariation());
        piece.setParts(joiner.join(ctx.getParts().keySet()));
        piece.setAlgorithmVersion(algorithmVersion);

        piece.getIntermediateDecisions().setDullBass(ctx.isDullBass());
        piece.getIntermediateDecisions().setFourToTheFloor(ctx.isFourToTheFloor());
        piece.getIntermediateDecisions().setSimplePhrases(ctx.isSimplePhrases());
        piece.getIntermediateDecisions().setAccompaniment(ctx.getParts().containsKey(PartType.ACCOMPANIMENT));
        piece.getIntermediateDecisions().setDrums(ctx.getParts().containsKey(PartType.PERCUSSIONS));
        piece.getIntermediateDecisions().setClassical(
                ctx.getParts().get(PartType.MAIN).getInstrument() == Instruments.PIANO
                        && piece.getIntermediateDecisions().isAccompaniment());
        piece.getIntermediateDecisions().setTempoType(Tempo.forValue(piece.getTempo()));
        piece.getIntermediateDecisions().setElectronic(ctx.isElectronic());
        piece.getIntermediateDecisions().setDissonant(ctx.isDissonant());
        piece.getIntermediateDecisions().setOrnamented(ctx.isOrnamented());
        piece.getIntermediateDecisions().setVariations(StringUtils.left(joiner.join(ctx.getVariations()), 2000));

        Piece result = dao.persist(piece);

        try {
            fileStorageService.storeFile(getMp3FilePath(result.getId()), mp3);
            fileStorageService.storeFile(getMidiFilePath(result.getId()), midi);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    @Transactional
    public void storePlay(long pieceId, Long userId, String ip, boolean mobileApp) {
        PiecePlay play = new PiecePlay();
        play.setDateTime(new DateTime());
        play.setMobileApp(mobileApp);
        if (userId != null) {
            play.setUser(dao.getById(User.class, userId));
        }
        Piece piece = dao.getById(Piece.class, pieceId);
        play.setPiece(piece);
        play.setIp(ip);
        dao.persist(play);

        if (piece.isNewlyCreated()) {
            // we don't care about isolation here - 2 or more users can get (and
            // set new=false to) the same piece and that's OK
            piece.setNewlyCreated(false);
            dao.persist(piece);
        }
    }

    @Transactional(readOnly = true)
    public List<Piece> getUserPieces(Long id, int page) {
        return dao.getUserPieces(id, page, 30);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "topPiecesCache")
    public List<Piece> getTopPieces(int page) {
        return dao.getTopPieces(page, 30);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "topRecentPiecesCache")
    public Map<Piece, Integer> getTopRecentPieces(int page) {
        return dao.getTopRecentPieces(page, 30, new DateTime().minusWeeks(2));
    }

    public InputStream getPieceFile(final long id) {
        RetryableOperation<InputStream> op = RetryableOperation.create(new Callable<InputStream>() {
            @Override
            public InputStream call() throws Exception {
                try (InputStream is = fileStorageService.getFile(getMp3FilePath(id))) {
                    return new ByteArrayInputStream(IOUtils.toByteArray(is));
                }
            }

        });
        try {
            return op.retry(3, IOException.class);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public InputStream getPieceMidiFile(long id) {
        try (InputStream is = fileStorageService.getFile(getMidiFilePath(id))) {
            return new ByteArrayInputStream(IOUtils.toByteArray(is));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public InputStream getPieceMusicXml(long id) throws IOException {
        Piece piece = dao.getById(Piece.class, id);
        try (InputStream is = fileStorageService.getFile(getMidiFilePath(id))) {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            Score score = new Score();
            SMFTools localSMF = new SMFTools();
            localSMF.read(is);
            SMFTools.SMFToScore(score, localSMF);
            score.setTitle(piece.getTitle());
            score.setNumerator(piece.getMetreNumerator());
            score.setDenominator(piece.getMetreDenominator());
            MusicXmlRenderer.render(score, result);
            return new ByteArrayInputStream(result.toByteArray());
        }
    }

    private String getMp3FilePath(long id) {
        return storageDir + id + ".mp3";
    }

    private String getMidiFilePath(long id) {
        return storageDir + id + ".mid";
    }

    @Transactional(readOnly = true)
    public Piece getPiece(long id) {
        return dao.getById(Piece.class, id);
    }

    @Transactional
    public long getNextPieceId(UserPreferences preferences) {
        Piece piece = dao.getNewlyCreatedPiece(preferences);
        long id = 1;
        if (piece == null && preferences.isDefault()) {
            id = getRandomPieceId();
        } else if (piece == null && !preferences.isDefault()) {
            List<Piece> pieces = dao.getByPreferences(preferences);
            if (!pieces.isEmpty()) {
                id = pieces.get(random.nextInt(pieces.size())).getId();
            } else {
                id = getRandomPieceId();
            }
        } else {
            id = piece.getId();
        }
        sharedData.getListeningRequests().incrementAndGet();
        return id;
    }

    private long getRandomPieceId() {
        Piece piece;
        long maxId = sharedData.getMaxId();
        if (maxId == 0) {
            maxId = dao.getMaxPieceId();
        }
        long id = 0;
        int attempts = 0;
        while (attempts < 5) {
            id = (long) (1 + (Math.random() * maxId));
            piece = dao.getById(Piece.class, id);
            if (piece != null && piece.getLikes() >= -3) {
                break;
            }
            attempts++;
        }
        return id;
    }

    @ManagedOperation
    @Async
    public void generatePieces(int count) {
        for (int i = 0; i < count; i++) {
            generatePiece();
        }
    }

    @ManagedOperation
    @Async
    public void rerenderMp3(@ManagedOperationParameter(description = "", name = "fromId") int fromId,
            @ManagedOperationParameter(description = "", name = "toId") int toId) {
        for (int i = fromId; i <= toId; i++) {
            try (InputStream is = fileStorageService.getFile(getMidiFilePath(i))) {
                byte[] midi = IOUtils.toByteArray(is);
                byte[] mp3 = generator.toMp3(midi);
                fileStorageService.storeFile(getMp3FilePath(i), mp3);
            } catch (Exception ex) {
                logger.warn("Error rerendering midi with id=" + i);
            }
        }
    }

    @Cacheable(value = "rssCache")
    public List<Piece> getRssFeed() {
        DateTime now = new DateTime();
        DateTime tenDaysAgo = now.minusDays(10);
        return dao.getFeedEntryPiecesInRange(tenDaysAgo, now);
    }

    @Transactional(readOnly = true)
    public List<Piece> getPieces(List<Long> pieceIds) {
        return dao.getByIds(Piece.class, pieceIds);
    }

    @Transactional
    public void incrementDownloads(long id, boolean midi) {
        Piece piece = dao.getById(Piece.class, id);
        if (midi) {
            piece.setMidiDownloads(piece.getMidiDownloads() + 1);
        } else {
            piece.setMp3Downloads(piece.getMp3Downloads() + 1);
        }
        dao.persist(piece);
    }

    @Transactional(readOnly = true)
    public List<Piece> search(UserPreferences preferences) {
        List<Piece> result = dao.getByPreferences(preferences);
        return result;
    }

    @Transactional
    public void save(Piece piece) {
        dao.persist(piece);
    }

    public void downloadPieces(OutputStream out, Collection<Piece> pieces) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out);
        for (Piece piece : pieces) {
            ZipEntry entry = new ZipEntry(piece.getTitle() + "-" + piece.getId() + ".mp3");
            zip.putNextEntry(entry);
            zip.write(IOUtils.toByteArray(getPieceFile(piece.getId())));
            zip.closeEntry();

            entry = new ZipEntry(piece.getTitle() + "-" + piece.getId() + ".midi");
            zip.putNextEntry(entry);
            zip.write(IOUtils.toByteArray(getPieceMidiFile(piece.getId())));
            zip.closeEntry();
        }

        ZipEntry license = new ZipEntry("license.txt");
        zip.putNextEntry(license);
        zip.write(IOUtils.toByteArray(getClass().getResourceAsStream("/emailTemplates/license.txt")));
        zip.closeEntry();
        zip.finish();
    }
}
