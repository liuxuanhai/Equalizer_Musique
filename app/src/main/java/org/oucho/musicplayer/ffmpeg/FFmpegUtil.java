package org.oucho.musicplayer.ffmpeg;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class FFmpegUtil {

    static String convertInputStreamToString(InputStream inputStream) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            String str;
            StringBuilder sb = new StringBuilder();
            while ((str = r.readLine()) != null) {
                sb.append(str);
            }
            return sb.toString();
        } catch (IOException e) {
            Log.e("ffmpeg", "error converting input stream to string", e);
        }
        return null;
    }

    static void destroyProcess(Process process) {
        if (process != null)
            process.destroy();
    }

    static boolean isProcessCompleted(Process process) {
        try {
            if (process == null) return true;
            process.exitValue();
            return true;
        } catch (IllegalThreadStateException e) {
            // do nothing
        }
        return false;
    }
}
