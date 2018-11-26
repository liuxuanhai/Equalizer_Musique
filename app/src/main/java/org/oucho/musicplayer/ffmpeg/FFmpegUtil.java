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
