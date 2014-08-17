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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.music.dao.PieceDao;
import com.music.model.persistent.Piece;
import com.music.model.persistent.PiecePack;

@Service
public class PiecePackService {

    @Inject
    private PieceDao dao;

    @Inject
    private PieceService pieceService;

    @Transactional(readOnly=true)
    public List<PiecePack> getPiecePacks() {
        List<PiecePack> packs = dao.listOrdered(PiecePack.class, "priority");
        // order pieces in memory, by likes
        for (PiecePack pack : packs) {
            TreeSet<Piece> pieces = new TreeSet<>(pieceComparator);
            pieces.addAll(pack.getPieces());
            pack.setPieces(pieces);
        }
        return packs;
    }

    public static final Comparator<Piece> pieceComparator = new Comparator<Piece>() {
        @Override
        public int compare(Piece o1, Piece o2) {
            int result = Ints.compare(o2.getLikes(), o1.getLikes());
            if (result == 0) {
                result = Longs.compare(o2.getId(), o1.getId());
            }
            return result;
        }
    };

    @Transactional(readOnly=true)
    public List<PiecePack> getPiecePacks(ArrayList<Long> ids) {
        return dao.getByIds(PiecePack.class, ids);
    }

    @Transactional
    public void download(long packId, OutputStream out) throws IOException {
        PiecePack pack = getPack(packId);
        pack.setDownloads(pack.getDownloads() + 1);
        dao.persist(pack);
        pieceService.downloadPieces(out, pack.getPieces());
    }

    @Transactional(readOnly=true)
    public PiecePack getPack(long id) {
        return dao.getById(PiecePack.class, id);
    }
}
