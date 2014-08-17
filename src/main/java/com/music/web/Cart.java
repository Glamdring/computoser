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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component("cartBean")
@Scope(value=WebApplicationContext.SCOPE_SESSION, proxyMode=ScopedProxyMode.TARGET_CLASS)
public class Cart implements Serializable {

    private static final long serialVersionUID = 797321317204271990L;

    private Set<Long> pieceIds = new HashSet<>();
    private Set<Long> piecePackIds = new HashSet<>();

    public Set<Long> getPieceIds() {
        return pieceIds;
    }

    public void setPieceIds(Set<Long> pieceIds) {
        this.pieceIds = pieceIds;
    }

    public Set<Long> getPiecePackIds() {
        return piecePackIds;
    }

    public void setPiecePackIds(Set<Long> piecePackIds) {
        this.piecePackIds = piecePackIds;
    }
}
