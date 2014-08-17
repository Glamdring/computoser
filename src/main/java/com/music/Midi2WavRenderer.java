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

package com.music;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.media.sound.AudioSynthesizer;

public class Midi2WavRenderer {
    private static final Logger logger = LoggerFactory.getLogger(Midi2WavRenderer.class);

    //TODO pool synthesizers

    public static void midi2wav(InputStream is, OutputStream os) {
        try {
            Sequence sequence = MidiSystem.getSequence(is);
            render(sequence, os);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Render sequence using selected or default soundbank into wave audio file.
     */
    public static void render(Sequence sequence, OutputStream outStream) {
        try {
            // Find available AudioSynthesizer.
            AudioSynthesizer synth = findAudioSynthesizer();
            if (synth == null) {
                logger.warn("No AudioSynhtesizer was found!");
                return;
            }

            // Open AudioStream from AudioSynthesizer.
            AudioInputStream stream = synth.openStream(null, null);

            Generator.loadSoundbankInstruments(synth);
            // Play Sequence into AudioSynthesizer Receiver.
            double total = send(sequence, synth.getReceiver());

            // Calculate how long the WAVE file needs to be.
            long len = (long) (stream.getFormat().getFrameRate() * (total + 4));
            stream = new AudioInputStream(stream, stream.getFormat(), len);

            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, outStream);

            synth.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Find available AudioSynthesizer.
     */
    public static AudioSynthesizer findAudioSynthesizer() throws MidiUnavailableException, IOException, InvalidMidiDataException {
        // First check if default synthesizer is AudioSynthesizer.
        Synthesizer synth = MidiSystem.getSynthesizer();
        if (synth instanceof AudioSynthesizer) {
            return (AudioSynthesizer) synth;
        }

        // If default synhtesizer is not AudioSynthesizer, check others.
        Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < infos.length; i++) {
            MidiDevice dev = MidiSystem.getMidiDevice(infos[i]);
            if (dev instanceof AudioSynthesizer) {
                return (AudioSynthesizer) dev;
            }
        }

        // No AudioSynthesizer was found, return null.
        return null;
    }

    /*
     * Send entiry MIDI Sequence into Receiver using timestamps.
     */
    public static double send(Sequence seq, Receiver recv) {
        float divtype = seq.getDivisionType();
        assert (seq.getDivisionType() == Sequence.PPQ);
        Track[] tracks = seq.getTracks();
        int[] trackspos = new int[tracks.length];
        int mpq = 500000;
        int seqres = seq.getResolution();
        long lasttick = 0;
        long curtime = 0;
        while (true) {
            MidiEvent selevent = null;
            int seltrack = -1;
            for (int i = 0; i < tracks.length; i++) {
                int trackpos = trackspos[i];
                Track track = tracks[i];
                if (trackpos < track.size()) {
                    MidiEvent event = track.get(trackpos);
                    if (selevent == null || event.getTick() < selevent.getTick()) {
                        selevent = event;
                        seltrack = i;
                    }
                }
            }
            if (seltrack == -1)
                break;
            trackspos[seltrack]++;
            long tick = selevent.getTick();
            if (divtype == Sequence.PPQ)
                curtime += ((tick - lasttick) * mpq) / seqres;
            else
                curtime = (long) ((tick * 1000000.0 * divtype) / seqres);
            lasttick = tick;
            MidiMessage msg = selevent.getMessage();
            if (msg instanceof MetaMessage) {
                if (divtype == Sequence.PPQ)
                    if (((MetaMessage) msg).getType() == 0x51) {
                        byte[] data = ((MetaMessage) msg).getData();
                        mpq = ((data[0] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
                    }
            } else {
                if (recv != null)
                    recv.send(msg, curtime);
            }
        }

        return curtime / 1000000.0;
    }

}
