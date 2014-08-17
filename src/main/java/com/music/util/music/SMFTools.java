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

package com.music.util.music;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

import jm.JMC;
import jm.midi.MidiUtil;
import jm.midi.Track;
import jm.midi.event.EndTrack;
import jm.midi.event.Event;
import jm.midi.event.NoteOn;
import jm.midi.event.PChange;
import jm.midi.event.VoiceEvt;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Score;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Code copied from SMF and MidiParser, but without System.exit(..)
 *
 * @author bozho
 *
 */
public final class SMFTools implements JMC {

    private static final Logger logger = LoggerFactory.getLogger(SMFTools.class);

    // --------------------------------------
    // attributes
    // --------------------------------------
    /** The standard MIDI file type to be read or written */
    @SuppressWarnings("unused")
    private short fileType;
    /** The current number of tracks stored by this Class */
    private short numOfTracks;
    /** Pulses per quarter note value */
    private short ppqn;
    /** list of Tracks contained within this SMF */
    private Vector<Track> trackList;

    public SMFTools() {
        this((short) 1, (short) 480);
    }

    public SMFTools(short fileType, short ppqn) {
        this.fileType = fileType;
        this.ppqn = ppqn;
        this.numOfTracks = 0;
        this.trackList = new Vector<>();
    }

    // -----------------------------------------------------------
    // Converts a SMF into jMusic Score data
    // -----------------------------------------------------------
    /**
     * Convert a SMF into the jMusic data type
     */
    @SuppressWarnings("rawtypes")
    public static void SMFToScore(Score score, SMFTools smf) {
        Enumeration<Track> en = smf.getTrackList().elements();
        // Go through tracks
        while (en.hasMoreElements()) {
            Part part = new Part();
            Track smfTrack = en.nextElement();
            Vector evtList = smfTrack.getEvtList();
            Vector phrVct = new Vector();
            sortEvents(evtList, phrVct, smf, part);
            for (int i = 0; i < phrVct.size(); i++) {
                part.addPhrase((Phrase) phrVct.elementAt(i));
            }
            score.addPart(part);
            score.clean();
        }
    }

    @SuppressWarnings("rawtypes")
    private static void sortEvents(Vector evtList, Vector phrVct, SMFTools smf, Part part) {
        double startTime = 0.0;
        double[] currentLength = new double[100];
        Note[] curNote = new Note[100];
        int phrIndex = 0;
        // Go through evts
        for (int i = 0; i < evtList.size(); i++) {
            Event evt = (Event) evtList.elementAt(i);
            startTime += (double) evt.getTime() / (double) smf.getPPQN();
            if (evt.getID() == 007) {
                PChange pchg = (PChange) evt;
                part.setInstrument(pchg.getValue());
                // if this event is a NoteOn event go on
            } else if (evt.getID() == 005) {
                NoteOn noteOn = (NoteOn) evt;
                part.setChannel(noteOn.getMidiChannel());
                short pitch = noteOn.getPitch();
                int dynamic = noteOn.getVelocity();
                short midiChannel = noteOn.getMidiChannel();
                // if you're a true NoteOn
                if (dynamic > 0) {
                    noteOn(phrIndex, curNote, smf, i, currentLength, startTime, phrVct, midiChannel, pitch,
                            dynamic, evtList);
                }
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void noteOn(int phrIndex, Note[] curNote, SMFTools smf, int i, double[] currentLength,
            double startTime, Vector phrVct, short midiChannel, short pitch, int dynamic, Vector evtList) {

        phrIndex = -1;
        // work out what phrase is ready to accept a note
        for (int p = 0; p < phrVct.size(); p++) {
            // Warning 0.02 should really be fixed
            if (currentLength[p] <= (startTime + 0.08)) {
                phrIndex = p;
                break;
            }
        }
        // need to create new phrase for a new voice?
        if (phrIndex == -1) {
            phrIndex = phrVct.size();
            phrVct.addElement(new Phrase(startTime));
            currentLength[phrIndex] = startTime;
        }
        // Do we need to add a rest ?
        if ((startTime > currentLength[phrIndex]) && (curNote[phrIndex] != null)) {
            double newTime = startTime - currentLength[phrIndex];
            // perform a level of quantisation first
            if (newTime < 0.25) {
                double length = curNote[phrIndex].getRhythmValue();
                curNote[phrIndex].setRhythmValue(length + newTime);
            } else {
                Note restNote = new Note(REST, newTime, 0);
                restNote.setPan(midiChannel);
                restNote.setDuration(newTime);
                restNote.setOffset(0.0);
                ((Phrase) phrVct.elementAt(phrIndex)).addNote(restNote);
            }
            currentLength[phrIndex] += newTime;
        }
        // get end time
        double time = MidiUtil.getEndEvt(pitch, evtList, i) / (double) smf.getPPQN();
        // create the new note
        Note tempNote = new Note(pitch, time, dynamic);
        tempNote.setDuration(time);
        curNote[phrIndex] = tempNote;
        ((Phrase) phrVct.elementAt(phrIndex)).addNote(curNote[phrIndex]);
        currentLength[phrIndex] += curNote[phrIndex].getRhythmValue();
    }

    public Vector<Track> getTrackList() {
        return this.trackList;
    }

    public short getPPQN() {
        return this.ppqn;
    }

    public void clearTracks() {
        if (!this.trackList.isEmpty()) {
            // remove any previous tracks
            this.trackList.removeAllElements();
        }
    }

    // ------------------------------------------
    // Read SMF
    // ------------------------------------------
    /**
     * Read from a standard MIDI file
     *
     * @params InputStream - the datasource to read from
     * @params Score score - the score to place jMusic data translation into
     * @exception IOException
     *                - any IO problems
     */
    public void read(InputStream is) throws IOException {
        // Given the small size of MIDI files read all
        // data into a ByteArrayStream for further processing
        byte[] fileData = new byte[is.available()];
        is.read(fileData);
        ByteArrayInputStream bais = new ByteArrayInputStream(fileData);
        DataInputStream dis = new DataInputStream(bais);
        // clear any SMF data
        if (!this.trackList.isEmpty()) {
            this.trackList.removeAllElements(); // remove any previous tracks
        }
        // check header for MIDIfile validity
        if (dis.readInt() != 0x4D546864) {// Check for MIDI track validity
            throw new IOException("This is NOT a MIDI file !!!");
        } else {// If MIDI file passes skip length bytes
            dis.readInt(); // skip over Length info
        }
        // get SMF Class data
        try {
            fileType = dis.readShort();
            this.numOfTracks = dis.readShort();
            this.ppqn = dis.readShort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        // skip the tempo track fro type 1 files
        /*
         * if(fileType == 1) { skipATrack(dis); numOfTracks--; }
         */
        // Read all track chunks
        for (int i = 0; i < numOfTracks; i++) {
            readTrackChunk(dis);
        }
        is.close();
        dis.close();
    }

    // ------------------------------------
    // Write a SMF
    // -------------------------------------
    /**
     * Write to a standard MIDI file
     *
     * @param OutputStream
     *            the datasource to write to
     * @param Score
     *            score the Score to get data from
     * @exception IOException
     *                did the write go ok
     */
    public void write(OutputStream os) throws IOException {
        // IO Stream stuff
        DataOutputStream dos = new DataOutputStream(os);
        // find number of tracks
        this.numOfTracks = (short) trackList.size();
        // write header chunk
        try {
            dos.writeInt(0x4D546864); // MThd
            dos.writeInt(6); // Length
            dos.writeShort(1); // Midi File Type
            dos.writeShort(numOfTracks); // Number of tracks
            dos.writeShort(ppqn); // Pulses Per Quarter Note
        } catch (Exception e) {
            e.printStackTrace();
        }
        // write all tracks
        Enumeration<Track> en = trackList.elements();
        while (en.hasMoreElements()) {
            Track smfTrack = en.nextElement();
            writeTrackChunk(dos, smfTrack);
        }
        os.flush();
        os.close();
        dos.flush();
        dos.close();
    }

    /**
     * Print all MIDI tracks and MIDI events
     */
    public void print() {
        Enumeration<Track> en = trackList.elements();
        while (en.hasMoreElements()) {
            Track track = en.nextElement();
            track.print();
        }
    }

    // ----------------------------------------
    // SMF Track Reads and Writes
    // ----------------------------------------
    /**
     * Reads a MIDI track chunk
     *
     * @param DataInputStream
     *            dis - the input stream to read from
     * @exception IOException
     */
    private void readTrackChunk(DataInputStream dis) throws IOException {
        // local variables for Track class
        Track track = new Track();
        // Insert new Track into a list of tracks
        this.trackList.addElement(track);
        int deltaTime = 0;
        // Read track header
        if (dis.readInt() != 0x4D54726B) {// If MTrk read is wrong
            throw new IOException("Track started in wrong place!!!!  ABORTING");
        } else {// If MTrk read ok get bytesRemaining
            dis.readInt();
        }
        // loop variables
        int status, oldStatus = 0, eventLength = 0;
        // Start gathering event data
        Event event = null;
        while (true) {
            try {
                // get variable length timestamp
                deltaTime = MidiUtil.readVarLength(dis);
                // mark stream so we can return if we need running status
                dis.mark(2);
                status = dis.readUnsignedByte();
                // decide on running status
                if (status < 0x80) { // set running status
                    status = oldStatus;
                    // return stream to before status read
                    dis.reset();
                }
                // create default event of correct type
                if (status >= 0xFF) { // Meta Event
                    int type = dis.readUnsignedByte();
                    eventLength = MidiUtil.readVarLength(dis);
                    event = jm.midi.MidiUtil.createMetaEvent(type);
                } else if (status >= 0xF0) { // System Exclusive --- NOT
                                             // SUPPORTED
                    eventLength = MidiUtil.readVarLength(dis);
                } else if (status >= 0x80) { // MIDI voice event
                    short selection = (short) (status / 0x10);
                    short midiChannel = (short) (status - (selection * 0x10));
                    VoiceEvt evt = (VoiceEvt) MidiUtil.createVoiceEvent(selection);
                    if (evt == null) {
                        throw new IOException("MIDI file read error: invalid voice event type!");
                    }
                    evt.setMidiChannel(midiChannel);
                    event = evt;
                }
                oldStatus = status;
            } catch (EOFException ex) {
                logger.warn("EOFException (" + ex.getMessage() + ") encountered in SMFTools");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            if (event != null) {
                // read data into the new event and
                // add the new event to the Track object
                event.setTime(deltaTime);
                event.read(dis);
                track.addEvent(event);
                // event.print();
                if (event instanceof EndTrack)
                    break;
            } else {
                // skip the stream ahead to next valid event
                dis.skipBytes(eventLength);
            }
        }
    }

    /**
     * Write the Track Chunk
     *
     * @param DataOutputStream
     *            dos
     * @param Track
     *            track - track to write
     * @exception IOException
     */
    @SuppressWarnings("rawtypes")
    private void writeTrackChunk(DataOutputStream odos, Track track) throws IOException {
        // Write to temporary stream to buffer disk writes and
        // calculate the number of bytes written to the stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        int header = 0x4D54726B;
        Enumeration en = track.getEvtList().elements();
        en = track.getEvtList().elements();
        // At this stage Except that all events are NoteOn events
        while (en.hasMoreElements()) {
            Event evt = (Event) en.nextElement();
            evt.write(dos);
            if (DEBUG)
                evt.print();
        }
        // Write to the real stream
        odos.writeInt(header);
        odos.writeInt(baos.size());
        odos.write(baos.toByteArray(), 0, baos.size());
    }
}