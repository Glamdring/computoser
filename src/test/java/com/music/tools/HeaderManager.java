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

package com.music.tools;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

public class HeaderManager {

    private static final String LS = System.getProperty("line.separator");
    public static void main(String[] args) throws Exception {
        String path = args[0];
        String header = FileUtils.readFileToString(new File(path, "src/main/resources/license/AGPL-3-header.txt"), Charsets.UTF_8);

        File sourceRoot = new File(path, "src");
        System.out.println("Source root is: " + sourceRoot);
        Collection<File> files = FileUtils.listFiles(sourceRoot, new String[] {"java"}, true);
        System.out.println("Ammending " + files.size() + " source files");
        for (File file : files) {
            System.out.println("Checking file " + file);
            String content = FileUtils.readFileToString(file, Charsets.UTF_8);
            if (content.contains("Copyright")) {
                System.out.println("Skipping file " + file);
                continue;
            }
            content = header + LS + LS + content;
            FileUtils.write(file, content);
        }
    }
}
