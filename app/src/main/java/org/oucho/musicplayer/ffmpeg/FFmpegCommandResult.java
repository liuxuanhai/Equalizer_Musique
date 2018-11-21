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