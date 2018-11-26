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

import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import org.oucho.musicplayer.MusiqueApplication;
import org.oucho.musicplayer.R;

public class PreferenceUtil {

    private static SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(MusiqueApplication.getInstance());
    }


    public static void setSharedPreferenceUri(@Nullable final Uri uri) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        if (uri == null) {
            editor.putString(MusiqueApplication.getInstance().getString(R.string.key_internal_uri_extsdcard), null);
        }
        else {
            editor.putString(MusiqueApplication.getInstance().getString(R.string.key_internal_uri_extsdcard), uri.toString());
        }
        editor.apply();
    }

    private static Uri getSharedPreferenceUri() {
        String uriString = getSharedPreferences().getString(MusiqueApplication.getInstance().getString(R.string.key_internal_uri_extsdcard), null);

        if (uriString == null)
            return null;
        else
            return Uri.parse(uriString);

    }


    public static Uri getTreeUris() {
        return getSharedPreferenceUri();
    }
}
