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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

@Service
public class LocalFileStorageService implements FileStorageService {

    @Override
    public void storeFile(String path, byte[] content) throws IOException {
         FileUtils.writeByteArrayToFile(new File(path), content);
    }

    @Override
    public void storeFile(String path, InputStream inputStream, long size)
            throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(path));
        IOUtils.copy(inputStream, os);
        os.close();
    }

    @Override
    public InputStream getFile(String path) throws IOException {
        return new FileInputStream(path);
    }

    @Override
    public void moveFile(String sourcePath, String targetPath)
            throws IOException {
        FileUtils.moveFile(new File(sourcePath), new File(targetPath));

    }

    @Override
    public void delete(String path) throws IOException {
        boolean result = new File(path).delete();
        if (!result) {
            throw new IOException("Failed to delete file " + path);
        }
    }
}
