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
