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

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import org.oucho.musicplayer.view.Notification;

public class StopReceive  extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        String etat = intent.getAction();
        String halt = intent.getStringExtra("halt");

        if ("org.oucho.musicplayer.STOP".equals(etat) && "stop".equals(halt)) {

            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                NotificationManager notificationManager;
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                assert notificationManager != null;
                notificationManager.cancel(Notification.NOTIFY_ID);

            }, 500);
        }
    }
}
