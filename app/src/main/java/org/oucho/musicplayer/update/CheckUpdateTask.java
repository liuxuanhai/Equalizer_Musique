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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.oucho.radio2.R;
import org.oucho.radio2.dialog.UpdateDialog;
import org.oucho.radio2.radio.RadioKeys;
import org.oucho.radio2.utils.AppUtils;
import org.oucho.radio2.utils.HttpUtils;


class CheckUpdateTask extends AsyncTask<Void, Void, String> implements RadioKeys {

    private ProgressDialog dialog;
    private final Context mContext;
    private final int mType;
    private boolean mShowProgressDialog;
    private static final String url = URL_UPDATE;

    CheckUpdateTask(Context context, int type, boolean showProgressDialog) {

        this.mContext = context;
        this.mType = type;
        this.mShowProgressDialog = showProgressDialog;

    }


    protected void onPreExecute() {
        if (mShowProgressDialog) {
            dialog = new ProgressDialog(mContext);
            dialog.setMessage(mContext.getString(R.string.android_auto_update_dialog_checking));
            dialog.show();
        }
    }


    @Override
    protected void onPostExecute(String result) {

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        if (!TextUtils.isEmpty(result)) {
            parseJson(result);
        }
    }

    private void parseJson(String result) {
        try {

            JSONObject obj = new JSONObject(result);
            String updateMessage = obj.getString(APK_UPDATE_CONTENT);
            String apkUrl = obj.getString(APK_DOWNLOAD_URL);
            int apkCode = obj.getInt(APK_VERSION_CODE);

            int versionCode = AppUtils.getVersionCode(mContext);

            Log.d(UPDATE_TAG, "apkCode= " + apkCode + ", versionCode= " + versionCode);

            if (apkCode > versionCode) {

                if (mType == TYPE_DIALOG) {
                    showDialog(mContext, updateMessage, apkUrl);
                } else if (mType == TYPE_SNACK) {
                    showSnack(mContext, apkUrl);
                }

            } else if (mShowProgressDialog) {
                Toast.makeText(mContext, mContext.getString(R.string.android_auto_update_toast_no_new_update), Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            Log.e(UPDATE_TAG, "parse json error: " + e);
        }
    }


    /**
     * Show dialog
     */
    private void showDialog(Context context, String content, String apkUrl) {
        UpdateDialog.show(context, content, apkUrl);
    }

    private void showSnack(Context context, String apkUrl) {
        UpdateSnackBar.show(context, apkUrl);
    }

    @Override
    protected String doInBackground(Void... args) {
        return HttpUtils.get(url);
    }
}
