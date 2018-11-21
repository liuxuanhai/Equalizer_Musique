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
