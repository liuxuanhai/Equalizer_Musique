package org.oucho.musicplayer.update;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.support.design.widget.Snackbar;

import org.oucho.musicplayer.R;

import java.net.URL;

class UtilsDisplay {


    static void showUpdateAvailableDialog(final Context context, String title, String content, String btnNegative, String btnPositive, final URL apk) {

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(content)

        .setPositiveButton(btnPositive, (dialog, id) -> UtilsLibrary.goToUpdate(context, apk))

        .setNegativeButton(btnNegative, null).show();

    }

    static void showUpdateNotAvailableDialog(final Context context, String title, String content) {


        new AlertDialog.Builder(context)
                .setTitle(title)

                .setMessage(content)
                .setPositiveButton(context.getResources().getString(android.R.string.ok), null)
                .show();
    }

    static void showUpdateAvailableSnackbar(final Context context, String content, final URL apk) {
        Activity activity = (Activity) context;


        Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), content, Snackbar.LENGTH_LONG);
        snackbar.setAction(context.getResources().getString(R.string.appupdater_btn_update), view -> UtilsLibrary.goToUpdate(context, apk)).show();
    }

    static void showUpdateNotAvailableSnackbar(final Context context, String content) {
        Activity activity = (Activity) context;


        Snackbar.make(activity.findViewById(android.R.id.content), content, Snackbar.LENGTH_LONG).show();
    }

}
