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

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.music.model.Scale;
import com.music.model.ToneGroups;
import com.music.model.ToneType;

public class ToneResolver {
    private static final Logger logger = LoggerFactory.getLogger(ToneResolver.class);

    public static int resolve(int pitch, Scale scale) {
        int resolvedTone = pitch; // initially assume it's a stable or unresolvable tone
        int toneDegree = Arrays.binarySearch(scale.getDefinition(), pitch % 12);
        int resolvedToneDegree = 0;
        if (toneDegree > -1) {
            // resolve tendency scale tones to stable scale tones
            int previousDegree = toneDegree - 1;
            if (previousDegree == ToneType.TONIC.getDegree() || previousDegree == ToneType.DOMINANT.getDegree() || previousDegree == ToneType.MEDIANT.getDegree()) {
                // get the difference in pitch between the two degrees in this scale. Then subtract them from the passed tone
                resolvedTone = getResolvedTone(pitch, scale, toneDegree, previousDegree);
                resolvedToneDegree = previousDegree;
            } else if ((toneDegree + 1) % 7 == ToneType.TONIC.getDegree()) {
                resolvedTone = pitch + (12 - scale.getDefinition()[toneDegree]);
                resolvedToneDegree = (toneDegree + 1) % 7;
            }
        } else {
            // resolve non-scale tones to unstable (tendency) scale tones

            // TODO http://www2.siba.fi/muste1/index.php?id=63&la=en
        }

        // in rare cases resolve to an unexpected stable tone (for the sake of "surprise")
        if (toneDegree > -1 && Chance.test(7)) {
            if (resolvedToneDegree == ToneType.TONIC.getDegree()) {
                resolvedTone = getResolvedTone(pitch, scale, toneDegree,
                        Chance.test(60) ? ToneType.MEDIANT.getDegree() : ToneType.DOMINANT.getDegree());
            }
            if (resolvedToneDegree == ToneType.MEDIANT.getDegree()) {
                resolvedTone = getResolvedTone(pitch, scale, toneDegree,
                        Chance.test(60) ? ToneType.DOMINANT.getDegree() : ToneType.TONIC.getDegree());
            }
            if (resolvedToneDegree == ToneType.DOMINANT.getDegree()) {
                resolvedTone = getResolvedTone(pitch, scale, toneDegree,
                        Chance.test(60) ? ToneType.TONIC.getDegree() : ToneType.MEDIANT.getDegree());
            }
        }

        return resolvedTone;
    }

    public static int resolveToTonic(int pitch, Scale scale) {
        int toneDegree = Arrays.binarySearch(scale.getDefinition(), pitch % 12);
        if (toneDegree < 0) {
            logger.warn("Tone not found in scale: " + pitch + " in " + scale);
            return pitch;
        }
        return getResolvedTone(pitch, scale, toneDegree, ToneType.TONIC.getDegree());
    }

    private static int getResolvedTone(int tone, Scale scale, int toneDegree, int resolutionDegree) {
        return tone - (scale.getDefinition()[toneDegree] - scale.getDefinition()[resolutionDegree]);
    }

    public static boolean isStable(int pitch, Scale scale) {
        if (scale == Scale.MELODIC_MINOR) {
            return true; //this scale has more tones than a regular diatonic scale, so skip that check
        }
        int toneDegree = Arrays.binarySearch(scale.getDefinition(), pitch % 12);
        if (toneDegree == ToneType.TONIC.getDegree() || toneDegree == ToneType.MEDIANT.getDegree() || toneDegree == ToneType.DOMINANT.getDegree()) {
            return true;
        }
        return false;
    }

    public static boolean isCommonTone(int tone, Scale scale) {
        boolean isStable = isStable(tone, scale);
        if (isStable) {
            return true;
        }

        int toneDegree = Arrays.binarySearch(scale.getDefinition(), tone % 12);
        // supertonic is also common according to statistics
        if (toneDegree == ToneType.SUPERTONIC.getDegree()) {
            return true;
        }

        return false;
    }

    public static boolean isInRange(int pitch, int[] pitchRange) {
        return pitch >= pitchRange[0] && pitch <= pitchRange[1];
    }

    public static boolean isInScale(int[] scaleDef, int previous, int interval, boolean up) {
        if (up) {
            return Arrays.binarySearch(scaleDef, (previous + interval) % 12) > -1;
        } else {
            return Arrays.binarySearch(scaleDef, (previous - interval) % 12) > -1;
        }
    }

    public static boolean isInScale(int[] scaleDef, int pitch) {
        return Arrays.binarySearch(scaleDef, pitch % 12) > -1;
    }

    /**
     * @param firstToneTypes
     * @param contrastTo
     * @return true if the previous X tones are all of the same type and need changing to the contrasting type
     */
    public static boolean needsContrastingChord(List<ToneType> firstToneTypes, ToneGroups contrastTo) {
        if (firstToneTypes.size() < 4) {
            return false;
        }
        for (int i = firstToneTypes.size() - 1; i >= firstToneTypes.size() - 4; i--) {
            if (!contrastTo.getToneTypes().contains(firstToneTypes.get(i))) {
                return false;
            }
        }
        return true;
    }
}
