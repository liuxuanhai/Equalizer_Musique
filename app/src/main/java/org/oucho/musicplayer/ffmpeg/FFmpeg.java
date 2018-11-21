package org.oucho.musicplayer.ffmpeg;

import android.content.Context;
import android.widget.Toast;

import java.io.File;

public class FFmpeg implements FFmpegInterface {

    private final Context context;
    private FFmpegExecuteAsyncTask ffmpegExecuteAsyncTask;

    private static final long MINIMUM_TIMEOUT = 10 * 1000;
    private long timeout = Long.MAX_VALUE;

    public FFmpeg(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void execute(String[] cmd, FFmpegExecuteResponseHandler ffmpegExecuteResponseHandler) {

        if (ffmpegExecuteAsyncTask != null && !ffmpegExecuteAsyncTask.isProcessCompleted()) {
            Toast.makeText(context, "FFmpeg command is already running, you are only allowed to run single command at a time", Toast.LENGTH_LONG).show();
        }

        if (cmd.length != 0) {

            String[] ffmpegBinary = new String[] { getFFmpeg() };
            String[] command = concat(ffmpegBinary, cmd);
            ffmpegExecuteAsyncTask = new FFmpegExecuteAsyncTask(command , timeout, ffmpegExecuteResponseHandler);
            ffmpegExecuteAsyncTask.execute();
        } else {
            throw new IllegalArgumentException("shell command cannot be empty");
        }
    }


    private String[] concat(String[] s1, String[] s2) {
        String[] erg = new String[s1.length + s2.length];

        System.arraycopy(s1, 0, erg, 0, s1.length);
        System.arraycopy(s2, 0, erg, s1.length, s2.length);

        return erg;
    }

    @Override
    public boolean isFFmpegCommandRunning() {
        return ffmpegExecuteAsyncTask != null && !ffmpegExecuteAsyncTask.isProcessCompleted();
    }


    @Override
    public void setTimeout(long timeout) {
        if (timeout >= MINIMUM_TIMEOUT) {
            this.timeout = timeout;
        }
    }

    private File getFilesDirectory() {
        return context.getDir("bin", 0);
    }

    private String getFFmpeg() {
        return getFilesDirectory().getAbsolutePath() + File.separator + "ffmpeg";
    }
}
