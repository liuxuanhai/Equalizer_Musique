/*
 * Radio - Internet radio for android
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

package org.oucho.radio2.update;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;

import org.oucho.radio2.R;

import static org.oucho.radio2.radio.RadioKeys.APK_DOWNLOAD_URL;

class UpdateSnackBar {

    static void show(final Context context, final String downloadUrl) {
        if (isContextValid(context)) {

            Activity activity = (Activity) context;

            Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), context.getResources().getString(R.string.android_auto_update_dialog_title), Snackbar.LENGTH_LONG);
            snackbar.setAction(context.getResources().getString(R.string.appupdater_btn_update), view -> goToDownload(context, downloadUrl));
            snackbar.show();
       }
    }

    private static boolean isContextValid(Context context) {
        return context instanceof Activity && !((Activity) context).isFinishing();
    }


    private static void goToDownload(Context context, String downloadUrl) {
        Intent intent = new Intent(context.getApplicationContext(), DownloadService.class);
        intent.putExtra(APK_DOWNLOAD_URL, downloadUrl);
        context.startService(intent);
    }
}
