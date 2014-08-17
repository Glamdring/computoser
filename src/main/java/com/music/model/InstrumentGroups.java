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

package com.music.model;

import static jm.constants.ProgramChanges.*;

public class InstrumentGroups {
    // before adding instruments, check the stats with select sum(likes) as sm, mainInstrument from Piece group by mainInstrument order by sm desc;
    public static final int[] MAIN_PART_INSTRUMENTS = new int[]{PIANO, GUITAR, STEEL_GUITAR, FRENCH_HORN, STRING_ENSEMBLE_1, CELLO};
    public static final int[] MAIN_PART_ONLY_INSTRUMENTS = new int[]{PIANO, ORGAN2, HONKYTONK_PIANO, EPIANO, EPIANO2, CLAV, MUSIC_BOX, VIBRAPHONE, GUITAR, STEEL_GUITAR, EL_GUITAR, DGUITAR, SYNTH_BRASS, CELLO};
    public static final int[] ELECTRONIC_MAIN_PART_ONLY_INSTRUMENTS = new int[]{OBOE, ORGAN2, HONKYTONK_PIANO, EPIANO, DX_EPIANO, CLAV, MUSIC_BOX, VIBRAPHONE, STEEL_GUITAR, EL_GUITAR, DGUITAR, ELECTRIC_GRAND, SYNTH_STRINGS, SYNTH_STRINGS_2, SYNTH_BRASS};
    public static final int[] ACCOMPANIMENT_INSTRUMENTS = new int[]{PIANO, GUITAR, STRING_ENSEMBLE_1};
    public static final int[] ARPEGGIO_INSTRUMENTS = new int[]{PIANO, GUITAR, ELECTRIC_GRAND, ELECTRIC_ORGAN, FANTASIA, SPACE_VOICE, BOWED_GLASS, HALO_PAD, SWEEP_PAD, ECHO, STRING_ENSEMBLE_1, STRING_ENSEMBLE_2, HARPSICHORD, ATMOSPHERE};
    public static final int[] SIMPLE_BEAT_INSTRUMENTS = new int[]{WOODBLOCK , DRUM, SYNTH_DRUM};
    public static final int[] SPORADIC_EFFECTS_INSTRUMENTS = new int[]{BELLS, CRYSTAL, SOUNDEFFECTS, BIRD, HELICOPTER, APPLAUSE, FRET_NOISE, TOM};
    public static final int[] PAD_INSTRUMENTS = new int[]{FANTASIA, SPACE_VOICE, BOWED_GLASS, HALO_PAD, SWEEP_PAD, ECHO, STRING_ENSEMBLE_1, DROPS};
    public static final int[] DRONE_INSTRUMENTS = new int[]{PIANO, GUITAR, ORGAN, HONKYTONK_PIANO, VIBRAPHONE};
    public static final int[] BASS_INSTRUMENTS = new int[]{PICKED_BASS, PICKED_BASS, BASS, DOUBLE_BASS, CELLO};

    public static int getInstrumentSpecificDynamics(int baseDynamics, int instrument) {
        double modifier = 1;
        switch (instrument) {
        case SWEEP_PAD: case HALO_PAD: modifier = 0.9d; break;
        case STRING_ENSEMBLE_1: modifier = 0.75d; break;
        }
        return (int) (modifier * baseDynamics);
    }
}
