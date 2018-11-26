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

package org.oucho.musicplayer.utils;


import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;

import org.oucho.musicplayer.services.PlayerService;
import org.oucho.musicplayer.MusiqueKeys;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class VolumeTimer implements MusiqueKeys {

    private CountDownTimer minuteurVolume;

    public void setVolume(float volume) {

        if (PlayerService.isPlaying() || PlayerService.isPaused()) {

            PlayerService.setVolume(volume);
        }
    }

    public void baisser(final Context context, final ScheduledFuture task, final int delay) {

        // définir si le delay est supérieur ou inférieur à 10mn

        final short minutes = (short) ( ( (delay / 1000) % 3600) / 60);

        final boolean tempsMinuterie = minutes > 10;

        int cycle;

        if (tempsMinuterie) {
            cycle = 60000;
        } else {
            cycle = 1000;
        }


        minuteurVolume = new CountDownTimer(delay, cycle) {
            @Override
            public void onTick(long mseconds) {

                long temps1 = ((task.getDelay(TimeUnit.MILLISECONDS) / 1000) % 3600) / 60 ;

                long temps2 = task.getDelay(TimeUnit.MILLISECONDS) / 1000;

                if (tempsMinuterie) {

                    if (temps1 < 1) {
                        setVolume(0.1f);
                    } else if (temps1 < 2) {
                        setVolume(0.2f);
                    } else if (temps1 < 3) {
                        setVolume(0.3f);
                    } else if (temps1 < 4) {
                        setVolume(0.4f);
                    } else if (temps1 < 5) {
                        setVolume(0.5f);
                    } else if (temps1 < 6) {
                        setVolume(0.6f);
                    } else if (temps1 < 7) {
                        setVolume(0.7f);
                    } else if (temps1 < 8) {
                        setVolume(0.8f);
                    } else if (temps1 < 9) {
                        setVolume(0.9f);
                    } else if (temps1 < 10) {
                        setVolume(1.0f);
                    }

                } else {

                    if (temps2 < 6) {
                        setVolume(0.1f);
                    } else if (temps2 < 12) {
                        setVolume(0.2f);
                    } else if (temps2 < 18) {
                        setVolume(0.3f);
                    } else if (temps2 < 24) {
                        setVolume(0.4f);
                    } else if (temps2 < 30) {
                        setVolume(0.5f);
                    } else if (temps2 < 36) {
                        setVolume(0.6f);
                    } else if (temps2 < 42) {
                        setVolume(0.7f);
                    } else if (temps2 < 48) {
                        setVolume(0.8f);
                    } else if (temps2 < 54) {
                        setVolume(0.9f);
                    } else if (temps2 < 60) {
                        setVolume(1.0f);
                    }
                }
            }

            @Override
            public void onFinish() {

                Intent intent = new Intent();
                intent.setAction(INTENT_QUIT);
                intent.putExtra("halt", "exit");
                context.sendBroadcast(intent);
            }
        }.start();

    }

    public CountDownTimer getMinuteur() {
        return minuteurVolume;
    }
}

