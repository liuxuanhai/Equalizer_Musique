/*
 * Musique - Music player/converter for android
 * Copyright (C) 2017  Old-Geek
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.oucho.musicplayer.equalizer;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;

import org.oucho.musicplayer.MusiqueApplication;
import org.oucho.musicplayer.R;


public class AudioEffects {

    static final short BASSBOOST_MAX_STRENGTH = 1000;
    private static final String PREF_EQ_ENABLED = "enabled";
    private static final String PREF_BAND_LEVEL = "level";
    private static final String PREF_PRESET = "preset";
    private static final String PREF_BASSBOOST = "bassboost";
    private static final String AUDIO_EFFECTS_PREFS = "audioeffects";

    private static final BassBoostValues sBassBoostValues = new BassBoostValues();
    private static final EqualizerValues sEqualizerValues = new EqualizerValues();
    private static BassBoost sBassBoost;
    private static Equalizer sEqualizer;
    private static boolean sCustomPreset;

    public static void init(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(AUDIO_EFFECTS_PREFS, Context.MODE_PRIVATE);

        initBassBoostValues(prefs);
        initEqualizerValues(prefs);
    }

    static void openAudioEffectSession(Context context, int audioSessionId) {
        SharedPreferences prefs = context.getSharedPreferences(AUDIO_EFFECTS_PREFS, Context.MODE_PRIVATE);

        initBassBoost(audioSessionId);
        initEqualizer(prefs, audioSessionId);
    }

    static void closeAudioEffectSession() {
        if (sBassBoost != null) {
            sBassBoost.release();
            sBassBoost = null;
        }

        if (sEqualizer != null) {
            sEqualizer.release();
            sEqualizer = null;
        }
    }

    private static void initBassBoostValues(SharedPreferences prefs) {
        sBassBoostValues.enabled = prefs.getBoolean(PREF_EQ_ENABLED, false);
        sBassBoostValues.strength = (short) prefs.getInt(PREF_BASSBOOST, 0);
    }

    private static void initBassBoost(int audioSessionId) {
        if (sBassBoost != null) {
            sBassBoost.release();
            sBassBoost = null;
        }
        sBassBoost = new BassBoost(0, audioSessionId);
        sBassBoost.setEnabled(sBassBoostValues.enabled);

        short strength = sBassBoostValues.strength;

        if (strength >= 0 && strength <= BASSBOOST_MAX_STRENGTH) {
            sBassBoost.setStrength(strength);
        }
    }

    private static void initEqualizerValues(SharedPreferences prefs) {


        sEqualizerValues.enabled = prefs.getBoolean(PREF_EQ_ENABLED, false);

        sEqualizerValues.preset = (short) prefs.getInt(PREF_PRESET, -1);

        if (sEqualizerValues.preset == -1) {
            sCustomPreset = true;
        }
    }

    private static void initEqualizer(SharedPreferences prefs, int audioSessionId) {

        if (sEqualizer != null) {
            sEqualizer.release();
            sEqualizer = null;
        }
        sEqualizer = new Equalizer(0, audioSessionId);
        sEqualizer.setEnabled(sEqualizerValues.enabled);

        if (!sCustomPreset) {
            usePreset(sEqualizerValues.preset);

        }

        sEqualizerValues.numberOfBands = sEqualizer.getNumberOfBands();


        if(!sEqualizerValues.levelsSet)
        {
            sEqualizerValues.bandLevels = new short[sEqualizerValues.numberOfBands];
        }
        for (short b = 0; b < sEqualizerValues.numberOfBands; b++) {
            if(!sEqualizerValues.levelsSet) {
                short level = (short) prefs.getInt(PREF_BAND_LEVEL + b, sEqualizer.getBandLevel(b));
                sEqualizerValues.bandLevels[b] = level;
                if (sCustomPreset) {

                    sEqualizer.setBandLevel(b, level);
                }
            }
            else
            {
                sEqualizer.setBandLevel(b, sEqualizerValues.bandLevels[b]);
            }
        }

        sEqualizerValues.levelsSet = true;


    }

    static short getBassBoostStrength() {
        return sBassBoostValues.strength;
    }

    static void setBassBoostStrength(short strength) {
        sBassBoostValues.strength = strength;
        if (sBassBoost != null) {
            sBassBoost.setStrength(strength);
        }

    }

    static short[] getBandLevelRange() {
        if (sEqualizer == null) {
            return null;
        }
        return sEqualizer.getBandLevelRange();
    }

    static short getBandLevel(short band) {
        if (sEqualizer == null && sEqualizerValues.levelsSet && sEqualizerValues.bandLevels.length > band) {
            return sEqualizerValues.bandLevels[band];
        }
        assert sEqualizer != null;
        return sEqualizer.getBandLevel(band);
    }

    static boolean areAudioEffectsEnabled() {
        if (sEqualizer == null) {
            return sEqualizerValues.enabled;
        }

        return sEqualizer.getEnabled();
    }

    static void setAudioEffectsEnabled(boolean enabled) {
        if (sEqualizer == null || sBassBoost == null) {
            return;
        }
        sBassBoost.setEnabled(true);
        sEqualizer.setEnabled(enabled);
    }

    static void setBandLevel(short band, short level) {
        sCustomPreset = true;

        if(sEqualizerValues.bandLevels.length > band)
        {
            sEqualizerValues.preset = -1;
            sEqualizerValues.bandLevels[band] = level;
        }

        if(sEqualizer != null) {
            sEqualizer.setBandLevel(band, level);
        }
    }

    static String[] getEqualizerPresets(Context context) {
        if (sEqualizer == null) {
            return new String[]{};
        }
        short numberOfPresets = sEqualizer.getNumberOfPresets();

        String[] presets = new String[numberOfPresets + 1];

        presets[0] = context.getResources().getString(R.string.custom);

        for (short n = 0; n < numberOfPresets; n++) {

            switch (sEqualizer.getPresetName(n)) {
                case "Classical":
                    presets[n + 1] = MusiqueApplication.getInstance().getString(R.string.classical);
                    break;
                case "Flat":
                    presets[n + 1] = MusiqueApplication.getInstance().getString(R.string.flat);
                    break;
                default:
                    presets[n + 1] = sEqualizer.getPresetName(n);
                    break;
            }
        }

        return presets;
    }

    static int getCurrentPreset() {
        if (sEqualizer == null || sCustomPreset) {
            return 0;
        }

        return sEqualizer.getCurrentPreset() + 1;
    }

    static void usePreset(short preset) {
        if (sEqualizer == null) {
            return;
        }
        sCustomPreset = false;

        try {
            sEqualizer.usePreset(preset);
        } catch (Exception ignore) {

        }

    }


    static short getNumberOfBands() {
        if (sEqualizer == null) {
            return 0;
        }
        return sEqualizer.getNumberOfBands();
    }

    static int getCenterFreq(short band) {
        if (sEqualizer == null) {
            return 0;
        }
        return sEqualizer.getCenterFreq(band);
    }

    static void savePrefs(Context context) {

        if (sEqualizer == null || sBassBoost == null) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(AUDIO_EFFECTS_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(PREF_BASSBOOST, sBassBoostValues.strength);

        short preset = sCustomPreset ? -1 : sEqualizer.getCurrentPreset();
        editor.putInt(PREF_PRESET, preset);


        short bands = sEqualizer.getNumberOfBands();

        for (short b = 0; b < bands; b++) {
            short level = sEqualizer.getBandLevel(b);

            editor.putInt(PREF_BAND_LEVEL + b, level);
        }

        editor.putBoolean(PREF_EQ_ENABLED, sEqualizer.getEnabled());

        editor.apply();
    }

    private static class BassBoostValues {
        boolean enabled;
        short strength;
    }

    private static class EqualizerValues {
        boolean enabled;
        short preset;
        short numberOfBands;
        short[] bandLevels;

        boolean levelsSet = false;
    }


}
