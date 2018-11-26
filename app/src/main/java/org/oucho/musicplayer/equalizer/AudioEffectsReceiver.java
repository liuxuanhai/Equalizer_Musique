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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AudioEffectsReceiver extends BroadcastReceiver {

    public static final String EXTRA_AUDIO_SESSION_ID = "org.oucho.musicplayer.EXTRA_AUDIO_SESSION_ID";

    public static final String ACTION_OPEN_AUDIO_EFFECT_SESSION = "org.oucho.musicplayer.OPEN_AUDIO_EFFECT_SESSION";
    public static final String ACTION_CLOSE_AUDIO_EFFECT_SESSION = "org.oucho.musicplayer.CLOSE_AUDIO_EFFECT_SESSION";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int audioSessionId = intent.getIntExtra(EXTRA_AUDIO_SESSION_ID, 0);

        if(ACTION_OPEN_AUDIO_EFFECT_SESSION.equals(action)) {
            AudioEffects.openAudioEffectSession(context, audioSessionId);
        }

        else if(ACTION_CLOSE_AUDIO_EFFECT_SESSION.equals(action)) {
            AudioEffects.closeAudioEffectSession();
        }
    }
}
