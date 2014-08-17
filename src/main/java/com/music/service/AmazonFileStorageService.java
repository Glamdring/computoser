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
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.model.S3Object;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AmazonFileStorageService implements FileStorageService {

    @Inject
    private S3Service service;

    @Value("${s3.bucket.name}")
    private String bucketName;

    @Override
    public void storeFile(String path, byte[] content) throws IOException {
        storeFile(path, new ByteArrayInputStream(content), content.length);
    }

    @Override
    public void storeFile(String path, InputStream is, long size) throws IOException {
        path = stripLeadingSlash(path);
        S3Object fileObject = new S3Object(path);
        fileObject.setDataInputStream(is);
        if (size != -1) {
            fileObject.setContentLength(size);
        }
        try {
            service.putObject(bucketName, fileObject);
        } catch (S3ServiceException e) {
            throw new IOException(e);
        }
    }

    @Override
    public InputStream getFile(String path) throws IOException {
        try {
            path = stripLeadingSlash(path);
            S3Object object = service.getObject(bucketName, path);
            return object.getDataInputStream();
        } catch (ServiceException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void moveFile(String sourcePath, String targetPath)
            throws IOException {
        try {
            sourcePath = stripLeadingSlash(sourcePath);
            targetPath = stripLeadingSlash(targetPath);
            S3Object targetObject = new S3Object(targetPath);
            service.moveObject(bucketName, sourcePath, bucketName, targetObject, true);
        } catch (ServiceException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void delete(String path) throws IOException {
        path = stripLeadingSlash(path);
        try {
            service.deleteObject(bucketName, path);
        } catch (ServiceException ex) {
            throw new IOException(ex);
        }
    }
    private String stripLeadingSlash(String path) {
        if (path.charAt(0) == '/') {
            return path.substring(1);
        }
        return path;
    }
}
