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

import org.springframework.stereotype.Component;

@Component
public class LikeCountingJob {

    //@Scheduled(fixedRate=20 * DateTimeConstants.MILLIS_PER_MINUTE)
    public void setLikes() {
        //TODO implement

        /**
            UPDATE Piece p JOIN (SELECT count(id) AS cnt, piece_id from PieceEvaluation WHERE positive=true GROUP BY piece_id) as ev ON p.id = ev.piece_id SET p.likes = ev.cnt;
            UPDATE Piece p JOIN (SELECT count(id) AS cnt, piece_id from PieceEvaluation WHERE positive=false GROUP BY piece_id) as ev ON p.id = ev.piece_id SET p.likes = p.likes - ev.cnt;
         */
    }
}
