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

package org.oucho.musicplayer.widget;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.oucho.musicplayer.services.PlayerService;


public class WidgetService extends Service {

    public WidgetService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent player = new Intent(getApplicationContext(), PlayerService.class);
        getApplicationContext().startService(player);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
