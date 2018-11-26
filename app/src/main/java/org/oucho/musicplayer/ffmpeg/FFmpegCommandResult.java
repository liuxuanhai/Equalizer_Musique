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

class FFmpegCommandResult {
    final String output;
    final boolean success;

    FFmpegCommandResult(boolean success, String output) {
        this.success = success;
        this.output = output;
    }

    static FFmpegCommandResult getDummyFailureResponse() {
        return new FFmpegCommandResult(false, "");
    }

    static FFmpegCommandResult getOutputFromProcess(Process process) {
        String output;
        if (success(process.exitValue())) {
            output = FFmpegUtil.convertInputStreamToString(process.getInputStream());
        } else {
            output = FFmpegUtil.convertInputStreamToString(process.getErrorStream());
        }
        return new FFmpegCommandResult(success(process.exitValue()), output);
    }

    private static boolean success(Integer exitValue) {
        return exitValue != null && exitValue == 0;
    }

}