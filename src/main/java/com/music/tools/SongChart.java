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

import java.awt.Color;
import java.awt.Graphics;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.music.tools.SongDBAnalyzer.NoteContentHandler;


public class SongChart {

    public static void main(String[] args) throws Exception {
        File file = new File("C:\\workspace\\music\\analysis\\db\\" + "21.xml");

        XMLReader xr = XMLReaderFactory.createXMLReader();
        NoteContentHandler handler = new NoteContentHandler();
        xr.setContentHandler(handler);
        try (Reader reader = new FileReader(file)) {
            try {
                xr.parse(new InputSource(reader));
            } catch (Exception ex) {
                System.out.println(file.getName());
                throw ex;
            }
        }
        List<NoteElement> noteList = handler.getNotes();
        List<Integer> pitches = new ArrayList<Integer>();
        for (NoteElement element : noteList) {
            if (element != null && !element.isRest()) {
                pitches.add(60 + element.getScaleDegree() + element.getOctave() * 7);
            }
        }

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(0, 0, 500, 500);
        frame.setVisible(true);
        GraphicsPanel gp = new GraphicsPanel(pitches);
        frame.setContentPane(gp);
    }

    public static class GraphicsPanel extends JPanel {
        private List<Integer> notes;
        public GraphicsPanel(List<Integer> notes) {
            this.notes = notes;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Integer previousNote = null;
            int currentX = 20;
            g.setColor(Color.RED);
            for (Integer note : notes) {
                if (previousNote == null) {
                    previousNote = note;
                    continue;
                }
                g.drawLine(currentX - 20, (80 - previousNote) * 4, currentX, (80 - note) * 4);
                previousNote = note;
                currentX += 20;
            }
        }
    }
}
