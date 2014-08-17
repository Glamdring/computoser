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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.music.service.FileStorageService;

@Component
public class BackupJob {

    private static final Logger log = LoggerFactory.getLogger(BackupJob.class);
    private static final int ZIP_BUFFER = 2048;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormat.forPattern("dd-MM-yyyy-HH-mm");

    @Resource(name="${filesystem.implementation}")
    private FileStorageService fileStorageService;

    @Value("${database.url}")
    private String jdbcUrl;

    @Value("${database.username}")
    private String username;

    @Value("${database.password}")
    private String password;

    @Value("${backup.dir}")
    private String finalBackupDir;

    @Value("${database.perform.backup}")
    private boolean performBackup;

    private String baseDir;

    @PostConstruct
    public void init() {
         if (!finalBackupDir.endsWith("/")) {
             throw new IllegalArgumentException("backup.dir property must end with a slash");
         }

         baseDir = System.getProperty("java.io.tmpdir") + finalBackupDir;
         new File(baseDir).mkdirs();
    }

    @Scheduled(cron = "0 0 0 * * ?") //every midnight
    public void run() {
        if (performBackup) {
            parseAndPerformMySQLBackup(username, password, jdbcUrl);
        }
    }

    private void parseAndPerformMySQLBackup(String user, String password, String jdbcUrl) {
        String port = "3306";

        Pattern hostPattern = Pattern.compile("//((\\w)+)/");
        Matcher m = hostPattern.matcher(jdbcUrl);
        String host = null;
        if (m.find()) {
            host = m.group(1);
        }

        Pattern dbPattern = Pattern.compile("/((\\w)+)\\?");
        m = dbPattern.matcher(jdbcUrl);
        String db = null;
        if (m.find()) {
            db = m.group(1);
        }

        log.debug(host + ":" + port + ":" + user + ":***:" + db);

        try {
            createBackup(host, port, user, password, db);
        } catch (Exception ex) {
            log.error("Error during backup", ex);
        }

    }

    private void createBackup(String host, String port, String user, String password,
            String db) throws Exception {



        String fileName = "backup-" + DATE_TIME_FORMAT.print(new DateTime());
        String baseFilePath = new File(baseDir + fileName).getAbsolutePath();
        String sqlFilePath = baseFilePath + ".sql";

        String execString = "mysqldump --host=" + host + " --port=" + port + " --user="
                + user + (StringUtils.isNotBlank(password) ? " --password=" + password : "")
                + " --compact --complete-insert --extended-insert --single-transaction "
                + "--skip-comments --skip-triggers --default-character-set=utf8 " + db
                + " --result-file=" + sqlFilePath;

        Process process = Runtime.getRuntime().exec(execString);
        if (log.isDebugEnabled()) {
            log.debug("Output: " + IOUtils.toString(process.getInputStream()));
            log.debug("Error: " + IOUtils.toString(process.getErrorStream()));
        }
        if (process.waitFor() == 0) {

            zipBackup(baseFilePath);
        }

        File zipFile = new File(baseFilePath + ".zip");
        InputStream is = new BufferedInputStream(new FileInputStream(zipFile));
        fileStorageService.storeFile(finalBackupDir + fileName + ".zip", is, zipFile.length());

        // result = "SET FOREIGN_KEY_CHECKS = 0;\\n" + result
        // + "\\nSET FOREIGN_KEY_CHECKS = 1;";
    }

    private void zipBackup(String baseFileName) throws IOException {
        FileOutputStream fos = new FileOutputStream(baseFileName + ".zip");
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));

        File entryFile = new File(baseFileName + ".sql");
        FileInputStream fi = new FileInputStream(entryFile);
        InputStream origin = new BufferedInputStream(fi, ZIP_BUFFER);
        ZipEntry entry = new ZipEntry("data.sql");
        zos.putNextEntry(entry);
        int count;
        byte[] data = new byte[ZIP_BUFFER];
        while ((count = origin.read(data, 0, ZIP_BUFFER)) != -1) {
            zos.write(data, 0, count);
        }
        origin.close();
        zos.close();

        entryFile.delete();
    }
}