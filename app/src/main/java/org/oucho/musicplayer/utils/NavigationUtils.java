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

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.search.SearchActivity;
import org.oucho.musicplayer.equalizer.EqualizerActivity;

public class NavigationUtils {

    private static final String TAG_LOG = "NavigationUtils";

    public static void showSearchActivity(Activity activity) {

        Log.i(TAG_LOG, "showSearchActivity");

        Intent i = new Intent(activity, SearchActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(i);
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public static void showEqualizer(Activity activity) {

        Log.i(TAG_LOG, "showSearchActivity");

        Intent i = new Intent(activity, EqualizerActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(i);
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public static void showMainActivity(Activity activity) {

        Log.i(TAG_LOG, "showSearchActivity");

        Intent i = new Intent(activity, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(i);
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
