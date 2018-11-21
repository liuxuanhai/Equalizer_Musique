package org.oucho.musicplayer.ffmpeg;

interface FFmpegExecuteResponseHandler extends FFmpegResponseHandler {

    void onSuccess(String message);

    void onProgress(String message);

    void onFailure(String message);

}
