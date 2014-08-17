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

package com.music.dao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import com.google.common.base.Joiner;
import com.music.model.persistent.Piece;
import com.music.model.persistent.PieceEvaluation;
import com.music.model.prefs.Mood;
import com.music.model.prefs.Tempo;
import com.music.model.prefs.Ternary;
import com.music.model.prefs.UserPreferences;
import com.music.model.prefs.Variation;

@Repository
public class PieceDao extends Dao {
    private Joiner andJoiner = Joiner.on(" AND ");

    public PieceEvaluation getEvaluation(long pieceId, Long userId, String ip) {
        TypedQuery<PieceEvaluation> query;
        if (userId != null) {
            query = getEntityManager().createQuery("SELECT ev FROM PieceEvaluation ev where ev.piece.id=:pieceId AND ev.user.id=:userId", PieceEvaluation.class);
            query.setParameter("userId", userId);
        } else {
            query = getEntityManager().createQuery("SELECT ev FROM PieceEvaluation ev where ev.piece.id=:pieceId AND ev.ip=:ip", PieceEvaluation.class);
            query.setParameter("ip", ip);
        }
        query.setParameter("pieceId", pieceId);

        try {
            return query.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    public List<Piece> getUserPieces(Long userId, int page, int pageSize) {
        TypedQuery<Piece> query = getEntityManager().createQuery("SELECT pe.piece FROM PieceEvaluation pe WHERE pe.user.id=:userId AND pe.positive = true ORDER BY pe.dateTime DESC", Piece.class);
        query.setParameter("userId", userId);
        query.setFirstResult(page * pageSize);
        query.setMaxResults(pageSize);

        return query.getResultList();
    }

    public List<Piece> getTopPieces(int page, int pageSize) {
        TypedQuery<Piece> query = getEntityManager().createQuery("SELECT piece FROM Piece piece WHERE piece.likes > 0 ORDER BY likes DESC, generationTime DESC", Piece.class);
        query.setFirstResult(page * pageSize);
        query.setMaxResults(pageSize);

        return query.getResultList();
    }

    public long getMaxPieceId() {
        Query query = getEntityManager().createQuery("SELECT MAX(id) FROM Piece");
        return (Long) query.getSingleResult();
    }

    public Piece getNewlyCreatedPiece(UserPreferences preferences) {
        String queryString = "SELECT piece FROM Piece piece WHERE piece.newlyCreated = true ORDER BY generationTime DESC";
        if (!preferences.isDefault()) {
            queryString = addPreferenceCriteria(preferences, "SELECT piece FROM Piece piece WHERE piece.newlyCreated = true AND ") + " ORDER BY generationTime DESC";
        }
        TypedQuery<Piece> query = getEntityManager().createQuery(queryString, Piece.class);
        query.setMaxResults(1);

        try {
            return query.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    public List<Piece> getByPreferences(UserPreferences preferences) {
        String queryStr = "SELECT piece FROM Piece piece ORDER BY likes DESC, generationTime DESC";
        if (!preferences.isDefault()) {
            queryStr = addPreferenceCriteria(preferences, "SELECT piece FROM Piece piece WHERE ") + " ORDER BY likes DESC, generationTime DESC";
        }
        TypedQuery<Piece> query = getEntityManager().createQuery(queryStr, Piece.class);
        query.setMaxResults(30);

        return query.getResultList();
    }

    private String addPreferenceCriteria(UserPreferences prefs, String query) {
        List<String> criteria = new ArrayList<>();
        if (prefs.isClassical()) {
            criteria.add("intermediateDecisions.classical=true");
        }
        if (prefs.getAccompaniment() != Ternary.OPTIONAL) {
            criteria.add("intermediateDecisions.accompaniment=" + (prefs.getAccompaniment() == Ternary.YES));
        }
        if (prefs.getDrums() != Ternary.OPTIONAL) {
            criteria.add("intermediateDecisions.drums=" + (prefs.getDrums() == Ternary.YES));
        }
        if (prefs.getTempo() != Tempo.ANY) {
            criteria.add("tempoType='" + prefs.getTempo() + "'");
        }
        if (prefs.getElectronic() == Ternary.YES) {
            criteria.add("intermediateDecisions.electronic=true");
        }
        if (prefs.isPreferDissonance()) {
            criteria.add("intermediateDecisions.dissonant=true");
        }
        if (prefs.getInstrument() != -1) {
            criteria.add("mainInstrument='" + prefs.getInstrument() + "'");
        }
        if (prefs.getVariation() != Variation.ANY) {
            criteria.add("variation >= " + prefs.getVariation().getFrom());
            criteria.add("variation < " + prefs.getVariation().getTo());
        }

        if (prefs.getMood() != Mood.ANY && prefs.getScale() == null) {
            if (prefs.getMood() == Mood.MAJOR) {
                criteria.add("scale IN ('MAJOR', 'MAJOR_PENTATONIC')");
            } else {
                criteria.add("scale IN ('MINOR', 'MINOR_PENTATONIC', 'HARMONIC_MINOR', 'MELODIC_MINOR', 'NATURAL_MINOR')");
            }
        }
        if (prefs.getScale() != null) {
            criteria.add("scale='" + prefs.getScale().toString() + "'");
        }

        query += andJoiner.join(criteria);
        return query;
    }

    public List<Piece> getPiecesInRange(DateTime start, DateTime end) {
        TypedQuery<Piece> query = getEntityManager().createQuery("SELECT p FROM Piece p WHERE p.generationTime > :start AND p.generationTime < :end", Piece.class);
        query.setParameter("start", start);
        query.setParameter("end", end);

        return query.getResultList();
    }

    public List<Piece> getFeedEntryPiecesInRange(DateTime start, DateTime end) {
        TypedQuery<Piece> query = getEntityManager().createQuery("SELECT e.piece FROM FeedEntry e WHERE e.inclusionTime > :start AND e.inclusionTime < :end ORDER BY e.piece.generationTime DESC", Piece.class);
        query.setParameter("start", start);
        query.setParameter("end", end);

        return query.getResultList();
    }

    @SuppressWarnings("rawtypes")
    public Map<Piece, Integer> getTopRecentPieces(int page, int pageSize, DateTime minusWeeks) {
        TypedQuery<List> query = getEntityManager().createQuery("SELECT new list(ev.piece, COUNT(ev) AS cnt) FROM PieceEvaluation ev WHERE ev.dateTime > :threshold AND ev.piece.likes > 0 GROUP BY ev.piece ORDER BY cnt DESC, ev.dateTime DESC", List.class);
        query.setParameter("threshold", minusWeeks);
        query.setFirstResult(page * pageSize);
        query.setMaxResults(pageSize);

        Map<Piece, Integer> result = new LinkedHashMap<>();
        for (List<?> list : query.getResultList()) {
            result.put((Piece) list.get(0), ((Long) list.get(1)).intValue());
        }
        return result;
    }
}
