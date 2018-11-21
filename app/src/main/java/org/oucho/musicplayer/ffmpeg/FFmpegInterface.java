package org.oucho.musicplayer.ffmpeg;

@SuppressWarnings("unused")
interface FFmpegInterface {

    void execute(String[] cmd, FFmpegExecuteResponseHandler ffmpegExecuteResponseHandler);

    boolean isFFmpegCommandRunning();

    void setTimeout(long timeout);

}
