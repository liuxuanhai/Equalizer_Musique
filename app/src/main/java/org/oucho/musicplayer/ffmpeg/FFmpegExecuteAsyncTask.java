package org.oucho.musicplayer.ffmpeg;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;


class FFmpegExecuteAsyncTask extends AsyncTask<Void, String, FFmpegCommandResult> {

    private final String[] cmd;
    private final FFmpegExecuteResponseHandler ffmpegExecuteResponseHandler;
    private final long timeout;
    private long startTime;
    private Process process;
    private String output = "";

    FFmpegExecuteAsyncTask(String[] cmd, long timeout, FFmpegExecuteResponseHandler ffmpegExecuteResponseHandler) {
        this.cmd = cmd;
        this.timeout = timeout;
        this.ffmpegExecuteResponseHandler = ffmpegExecuteResponseHandler;
    }

    @Override
    protected void onPreExecute() {
        startTime = System.currentTimeMillis();
        if (ffmpegExecuteResponseHandler != null) {
            ffmpegExecuteResponseHandler.onStart();
        }
    }

    @Override
    protected FFmpegCommandResult doInBackground(Void... params) {

        try {
            process = run(cmd);
            if (process == null) {
                return FFmpegCommandResult.getDummyFailureResponse();
            }
            checkAndUpdateProcess();
            return FFmpegCommandResult.getOutputFromProcess(process);
        } catch (TimeoutException e) {
            Log.e("FFmpeg", "timed out", e);
            return new FFmpegCommandResult(false, e.getMessage());
        } catch (Exception e) {
            Log.e("Error running FFmpeg", "ee", e);
        } finally {
            FFmpegUtil.destroyProcess(process);
        }
        return FFmpegCommandResult.getDummyFailureResponse();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (values != null && values[0] != null && ffmpegExecuteResponseHandler != null) {
            ffmpegExecuteResponseHandler.onProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(FFmpegCommandResult FFmpegCommandResult) {
        if (ffmpegExecuteResponseHandler != null) {
            output += FFmpegCommandResult.output;
            if (FFmpegCommandResult.success) {
                ffmpegExecuteResponseHandler.onSuccess(output);
            } else {
                ffmpegExecuteResponseHandler.onFailure(output);
            }
            ffmpegExecuteResponseHandler.onFinish();
        }
    }

    private void checkAndUpdateProcess() throws TimeoutException {
        while (!FFmpegUtil.isProcessCompleted(process)) {
            // checking if process is completed
            if (FFmpegUtil.isProcessCompleted(process)) {
                return;
            }

            // Handling timeout
            if (timeout != Long.MAX_VALUE && System.currentTimeMillis() > startTime + timeout) {
                throw new TimeoutException("FFmpeg timed out");
            }

            try {
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = reader.readLine()) != null) {
                    if (isCancelled()) {
                        return;
                    }

                    //noinspection StringConcatenationInLoop
                    output += line + "\n";
                    publishProgress(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isProcessCompleted() {
        return FFmpegUtil.isProcessCompleted(process);
    }

    private Process run(String[] commandString) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(commandString);
        } catch (IOException e) {
            Log.e("ShellCommand", "Exception while trying to run: " + Arrays.toString(commandString), e);
        }
        return process;
    }

}
