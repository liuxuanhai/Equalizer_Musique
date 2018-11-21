package org.oucho.musicplayer.update;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.oucho.musicplayer.R;

class AppUpdate {

    private static final String TAG_LOG = "AppUpdate";

    private final Context context;

    private Display display;
    private String xmlUrl;
    private Boolean showAppUpdated;
    private final String titleUpdate;
    private final String btnDismiss;
    private final String btnUpdate;
    private final String titleNoUpdate;

    AppUpdate(Context context) {
        this.context = context;
        this.display = Display.DIALOG;
        this.showAppUpdated = false;

        // Dialog
        this.titleUpdate = context.getResources().getString(R.string.appupdater_update_available);
        this.titleNoUpdate = context.getResources().getString(R.string.appupdater_update_not_available);
        this.btnUpdate = context.getResources().getString(R.string.appupdater_btn_update);
        this.btnDismiss = context.getResources().getString(R.string.appupdater_btn_dismiss);
    }

    AppUpdate setDisplay(Display display) {
        this.display = display;
        return this;
    }

    AppUpdate setUpdateXML() {
        this.xmlUrl = CheckUpdate.updateURL;
        return this;
    }

    AppUpdate showAppUpdated() {
        this.showAppUpdated = true;
        return this;
    }

    @SuppressWarnings("unused")
    public AppUpdate init() {
        start();
        return this;
    }


    public void start() {
        CheckAsync.LatestAppVersion latestAppVersion = new CheckAsync.LatestAppVersion(xmlUrl, new LibraryListener() {
            @Override
            public void onSuccess(Update update) {
                if (UtilsLibrary.isUpdateAvailable(UtilsLibrary.getAppInstalledVersion(context), update.getLatestVersion())) {

                        switch (display) {
                            case DIALOG:
                                UtilsDisplay.showUpdateAvailableDialog(context, titleUpdate, getDescriptionUpdate(context, update, Display.DIALOG), btnDismiss, btnUpdate, update.getUrlToDownload());
                                break;
                            case SNACKBAR:
                                UtilsDisplay.showUpdateAvailableSnackbar(context, getDescriptionUpdate(context, update, Display.SNACKBAR), update.getUrlToDownload());
                                break;
                        }

                } else if (showAppUpdated) {
                    switch (display) {
                        case DIALOG:
                            UtilsDisplay.showUpdateNotAvailableDialog(context, titleNoUpdate, getDescriptionNoUpdate(context));
                            break;
                        case SNACKBAR:
                            UtilsDisplay.showUpdateNotAvailableSnackbar(context, getDescriptionNoUpdate(context));
                            break;
                    }
                }
            }

            @Override
            public void onFailed() {

                try {
                    throw new IllegalArgumentException("XML file is not valid!");
                } catch (IllegalArgumentException e) {
                    Log.e(TAG_LOG, String.valueOf(e));
                }
            }
        });

        latestAppVersion.execute();
    }

    interface LibraryListener {
        void onSuccess(Update update);

        void onFailed();
    }

    private String getDescriptionUpdate(Context context, Update update, Display display) {

            switch (display) {
                case DIALOG:
                    if (!TextUtils.isEmpty(update.getReleaseNotes()))
                        return String.format(context.getResources().getString(R.string.appupdater_update_available_description_dialog_before_release_notes), update.getLatestVersion(), update.getReleaseNotes());
                    else
                        return String.format(context.getResources().getString(R.string.appupdater_update_available_description_dialog), update.getLatestVersion(), UtilsLibrary.getAppName(context));

                case SNACKBAR:
                    return String.format(context.getResources().getString(R.string.appupdater_update_available_description_snackbar), update.getLatestVersion());
            }

        return null;
    }

    private String getDescriptionNoUpdate(Context context) {

            return String.format(context.getResources().getString(R.string.appupdater_update_not_available_description), UtilsLibrary.getAppName(context));
    }

}
