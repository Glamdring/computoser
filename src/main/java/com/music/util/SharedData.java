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

package com.music.util;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

@Component
@ManagedResource
public class SharedData {
    private volatile long maxId;
    private AtomicInteger listeningRequests = new AtomicInteger();
    @Value("${generate.pieces}")
    private boolean generateMusic;
    private boolean adaptGenerationQuantity;

    public long getMaxId() {
        return maxId;
    }

    public void setMaxId(long maxId) {
        this.maxId = maxId;
    }

    @ManagedAttribute
    public AtomicInteger getListeningRequests() {
        return listeningRequests;
    }

    @ManagedAttribute
    public void setListeningRequests(AtomicInteger listeningRequests) {
        this.listeningRequests = listeningRequests;
    }

    @ManagedAttribute
    public boolean isGenerateMusic() {
        return generateMusic;
    }

    @ManagedAttribute
    public void setGenerateMusic(boolean generateMusic) {
        this.generateMusic = generateMusic;
    }

    @ManagedAttribute
    public boolean isAdaptGenerationQuantity() {
        return adaptGenerationQuantity;
    }

    @ManagedAttribute
    public void setAdaptGenerationQuantity(boolean adaptGenerationQuantity) {
        this.adaptGenerationQuantity = adaptGenerationQuantity;
    }
}
