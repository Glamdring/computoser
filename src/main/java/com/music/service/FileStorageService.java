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
import java.io.InputStream;

public interface FileStorageService {
    /**
     * Stores a file on a given path
     * @param path
     * @param content
     */
    void storeFile(String path, byte[] content) throws IOException;

    void moveFile(String sourcePath, String targetPath) throws IOException;

    InputStream getFile(String path) throws IOException;

    void delete(String path) throws IOException;

    void storeFile(String path, InputStream inputStream, long size) throws IOException;
}
