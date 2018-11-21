package org.oucho.musicplayer.update;

import android.os.AsyncTask;

import org.oucho.musicplayer.MusiqueApplication;


class CheckAsync {

    static class LatestAppVersion extends AsyncTask<Void, Void, Update> {
        private final String xmlUrl;
        private final AppUpdate.LibraryListener listener;

        LatestAppVersion(String xmlUrl, AppUpdate.LibraryListener listener) {
            this.xmlUrl = xmlUrl;
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (UtilsLibrary.isNetworkAvailable(MusiqueApplication.getInstance())) {

                if (xmlUrl == null || !UtilsLibrary.isStringAnUrl(xmlUrl)) {
                    listener.onFailed();
                    cancel(true);
                }

            } else {
                listener.onFailed();
                cancel(true);
            }
        }

        @Override
        protected Update doInBackground(Void... voids) {

            try {

                return UtilsLibrary.getLatestAppVersionXml(xmlUrl);

            } catch (Exception e) {
                cancel(true);
            }

            return null;

        }

        @Override
        protected void onPostExecute(Update update) {
            super.onPostExecute(update);
            if (UtilsLibrary.isStringAVersion(update.getLatestVersion())) {
                listener.onSuccess(update);
            }
        }
    }

}
