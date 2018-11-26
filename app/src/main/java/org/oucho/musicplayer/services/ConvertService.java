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

package org.oucho.musicplayer.services;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.MusiqueApplication;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.ffmpeg.ExecuteBinaryResponseHandler;
import org.oucho.musicplayer.ffmpeg.FFmpeg;
import org.oucho.musicplayer.utils.PreferenceUtil;
import org.oucho.musicplayer.utils.StorageHelper;

import java.io.File;
import java.util.ArrayList;

import static org.oucho.musicplayer.MusiqueKeys.STORAGE_ACCESS_FRAMEWORK;


public class ConvertService extends Service {

    private static final String TAG = "ConvertService";

    public static final int NOTIFID = 10;

    private AsyncTask<String, Integer, Boolean> convertTask;
    private final ArrayList<String> mSongList = new ArrayList<>();

    private Song mSong;
    private Album mAlbum;
    private boolean isAlbum;
    private boolean albumFlag;
    private static boolean isCancel;
    private static boolean isEncode = false;
    private int time;
    private int trackCount;
    private int currentTrack;

    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    private String format;
    private String encode;
    private String extension;

    private byte[] cover;

    private boolean hq;

    private static boolean waitPermExt = false;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();


        if (action != null && action.equals("cancel")) {

            isCancel = true;
            Toast.makeText(MusiqueApplication.getInstance(), R.string.stop_convert, Toast.LENGTH_LONG).show();

        } else {

            String type = intent.getStringExtra("type");

            if (type != null) {

                if (type.equals("album")) {

                    isCancel = false;

                    isAlbum = true;
                    mAlbum = intent.getParcelableExtra("album");
                    hq = intent.getBooleanExtra("hq", false);

                    createSongPathList();
                    trackCount = mSongList.size();
                    convertTask = new Convert().execute();

                } else if (type.equals("song")) {

                    isCancel = false;

                    isAlbum = false;
                    mSong = intent.getParcelableExtra("song");
                    hq = intent.getBooleanExtra("hq", false);

                    Log.d(TAG, "song = " + mSong.getTitle());

                    convertTask = new Convert().execute();
                }
            }

            format = intent.getStringExtra("format");

        }

        return START_NOT_STICKY;
    }


    private void onStop() {

        Log.w(TAG, "stop");
        if (convertTask.getStatus() == AsyncTask.Status.FINISHED) {

            isEncode = false;
            mNotifyManager.cancel(NOTIFID);

        } else {
            new Handler().postDelayed(this::onStop, 500);
        }
    }


    @SuppressLint("StaticFieldLeak")
    class Convert extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            PendingIntent cancel = PendingIntent.getService(getApplicationContext(), 0,
                    new Intent(getApplicationContext(), ConvertService.class).setAction("cancel"), 0);

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

            Bitmap bigIcon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher);

            mNotifyManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(getApplicationContext(), "ID");
            mBuilder.setContentTitle(getText(R.string.converting))
                    .setSmallIcon(R.drawable.tape_drive)
                    .setLargeIcon(bigIcon)
                    .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                    .setOngoing(true)
                    .setShowWhen(false)
                    .setProgress(100, 0, false)
                    .setContentIntent(pendingIntent)
                    .addAction(R.drawable.ic_cancel_red_900_24dp, getString(R.string.cancel), cancel);

            mNotifyManager.notify(NOTIFID, mBuilder.build());

            Toast.makeText(getApplicationContext(), R.string.converting, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Boolean doInBackground(String... arg0) {
            boolean success = false;

            long freespaceSource;

            if (isAlbum) {

                File song = new File(mSongList.get(0));

                String size1 = String.valueOf(StorageHelper.getSourceFreeBytes(song.getPath())).replace("-", "");
                freespaceSource = Long.parseLong(size1);
            } else {
                String size1 = String.valueOf(StorageHelper.getSourceFreeBytes(mSong.getPath())).replace("-", "");
                freespaceSource = Long.parseLong(size1);
            }

            String size0 = String.valueOf(StorageHelper.getInternalFreeBytes()).replace("-", "");
            long freespaceIntern = Long.parseLong(size0);

            if (freespaceIntern < 52428800 && freespaceSource < 52428800) {
                String msg = getApplicationContext().getString(R.string.stockage_space);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

                onStop();

            } else {

                if (isAlbum)
                    success = convertAlbum();
                else
                    success = convertSong();

                isEncode = true;
            }

            return success;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d(TAG, "onPostExecute()");
        }

    }

    private  boolean convertAlbum() {

        albumFlag = true;

        Thread albumThread = new Thread(() -> {

            for (int i = 0; i < mSongList.size(); i++) {

                while (!albumFlag) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (isCancel)
                    break;

                currentTrack = i + 1;

                File song = new File(mSongList.get(i));

                File test = new File(song.getPath() + ".test");

                waitPermExt = false;
                if (!StorageHelper.isWritable(test) && PreferenceUtil.getTreeUris() == null) {

                    Intent intent = new Intent();
                    intent.setAction(STORAGE_ACCESS_FRAMEWORK);
                    sendBroadcast(intent);

                    while (!waitPermExt) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    if (isCancel)
                        break;
                }


                try {

                    albumFlag = false;
                    singleSong(mSongList.get(i));
                } catch (Exception e) {
                    Log.w(TAG, "Warning = " + e);
                }

            }
        });

        albumThread.start();

        return true;
    }


    private  boolean convertSong() {

        File song = new File(mSong.getPath());

        File test = new File(song.getPath() + ".test");
        waitPermExt = false;
        if (!StorageHelper.isWritable(test) && PreferenceUtil.getTreeUris() == null) {

            Intent intent = new Intent();
            intent.setAction(STORAGE_ACCESS_FRAMEWORK);
            sendBroadcast(intent);

            while (!waitPermExt) {

                Log.w(TAG, "!waitPermExt = " + !waitPermExt);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!isCancel)
            singleSong(mSong.getPath());

        return true;
    }


    private void singleSong(String path) {

        int sampleRate = 0;
        int bitRate = 0;
        int bit_depth = 0;

        AudioFile audioFile = null;

        try {
            File file = new File(path);
            audioFile = AudioFileIO.read(file);

            Tag tag = audioFile.getTag();
            cover = tag.getFirstArtwork().getBinaryData();
            time = audioFile.getAudioHeader().getTrackLength();
            sampleRate = Integer.parseInt(audioFile.getAudioHeader().getSampleRate());
            bitRate = Integer.parseInt(audioFile.getAudioHeader().getBitRate().replace("~", ""));


        } catch (Exception ignore) {}

        Log.i(TAG, "bitrate = " + bitRate);

        switch (format) {
            case "mp3":

                if (hq) {

                    if (sampleRate >= 48000 && bitRate >= 500)
                        encode = "-hide_banner -map a -acodec mp3 -ab 320k -ar 48000 -ac 2";
                    else if (sampleRate >= 44100 && bitRate >= 500)
                        encode = "-hide_banner -map a -acodec mp3 -ab 320k -ar 44100 -ac 2";
                    else if (sampleRate >= 44100 && bitRate >= 320)
                        encode = "-hide_banner -map a -acodec mp3 -q 0 -ar 44100 -ac 2";
                    else if (sampleRate >= 44100 && bitRate >= 256)
                        encode = "-hide_banner -map a -acodec mp3 -q 1 -ar 44100 -ac 2";
                    else
                        encode = "-hide_banner -map a -acodec mp3 -ac 2";
                } else {

                     if (sampleRate >= 44100 && bitRate >= 320)
                        encode = "-hide_banner -map a -acodec mp3 -q 0 -ar 44100 -ac 2";
                    else if (sampleRate >= 44100 && bitRate >= 256)
                        encode = "-hide_banner -map a -acodec mp3 -q 1 -ar 44100 -ac 2";
                    else
                        encode = "-hide_banner -map a -acodec mp3 -ac 2";
                }

                extension = ".mp3";
                break;

            case "aac":

                if (hq) {
                    if (sampleRate >= 48000 && bitRate >= 500)
                        encode = "-hide_banner -map a -acodec aac -aprofile aac_low -ab 384k -ar 48000 -ac 2";
                    else if (sampleRate >= 44100 && bitRate >= 500)
                        encode = "-hide_banner -map a -acodec aac -aprofile aac_low -ab 384k -ar 44100 -ac 2";
                    else if (sampleRate >= 44100 && bitRate >= 384)
                        encode = "-hide_banner -map a -acodec aac -aprofile aac_low -ab 320k -ar 44100 -ac 2";
                    else if (sampleRate >= 44100 && bitRate > 256)
                        encode = "-hide_banner -map a -acodec aac -aprofile aac_low -ab 256k -ar 44100 -ac 2";
                    else if (sampleRate >= 44100 && bitRate > 192)
                        encode = "-hide_banner -map a -acodec aac -aprofile aac_low -ab 192k -ar 44100 -ac 2";
                    else
                        encode = "-hide_banner -map a -acodec aac -aprofile aac_low -ac 2";

                } else {
                    if (sampleRate >= 44100 && bitRate >= 320)
                        encode = "-hide_banner -map a -acodec aac -aprofile aac_low -ab 256k -ar 44100 -ac 2";
                    else if (sampleRate >= 44100 && bitRate >= 256)
                        encode = "-hide_banner -map a -acodec aac -aprofile aac_low -ab 192k -ar 44100 -ac 2";
                    else
                        encode = "-hide_banner -map a -acodec aac -aprofile aac_low -ac 2";
                }

                extension = ".m4a";
                break;

            case "flac":

                if (audioFile != null)
                    bit_depth = audioFile.getAudioHeader().getBitsPerSample();

                if (bit_depth < 24)
                    encode = "-hide_banner -map a -acodec flac -sample_fmt s16 -compression_level 3";
                else
                    encode = "-hide_banner -map a -acodec flac -sample_fmt s32 -compression_level 3";

                extension = ".flac";
                break;

            default:
                break;
        }

        try {

            String inputFile = new File(path).getName();
            String inputPath = new File(path).getParent();
            String filenameWithoutExt = inputFile.replace(inputFile.substring(inputFile.lastIndexOf(".")), "");
            String pathCache = MusiqueApplication.getInstance().getCacheDir().getPath() + "/";

            File fileOutput = new File(pathCache + filenameWithoutExt + " - convert" + extension);

            if (fileOutput.exists())
                StorageHelper.deleteFile(fileOutput);

            String[] a = {"-i"};
            String[] b = {path};
            String[] c = encode.split(" ");
            String[] d = {pathCache + filenameWithoutExt + " - convert" + extension};

            String[] aaa = concat(a,b);
            String[] bbb = concat(aaa,c);

            String[] command = concat(bbb,d);

            execFFmpegBinary(command, fileOutput, new File(inputPath) );

        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private void execFFmpegBinary(final String[] command, File source, File target) {

        FFmpeg ffmpeg = new FFmpeg(MusiqueApplication.getInstance());

        ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
            @Override
            public void onFailure(String s) {
                Log.e(TAG,"FAILED with output : "+s);
            }

            @Override
            public void onSuccess(String s) {
                Log.d(TAG,"Success with output : "+s);
            }

            @Override
            public void onProgress(String s) {

                if (s.contains("kB time=")) {
                    String tic = s.substring(s.lastIndexOf("time=") + 5);
                    String tac = tic.substring(0, tic.indexOf("."));

                    String[] h1 = tac.split(":");

                    int hour = Integer.parseInt(h1[0]);
                    int minute = Integer.parseInt(h1[1]);
                    int second = Integer.parseInt(h1[2]);

                    int temps = second + (60 * minute) + (3600 * hour);

                    String track = "";

                    if (isAlbum)
                        track = "(" + currentTrack + "/" + trackCount + ")" + " - ";

                    mBuilder.setProgress(time, temps, false)
                            .setContentText(track + source.getName().replace(" - convert", ""));

                    mNotifyManager.notify(NOTIFID, mBuilder.build());

                }
            }

            @Override
            public void onStart() {
                Log.d(TAG, "Start ffmpeg");
            }

            @Override
            public void onFinish() {


                try {
                    AudioFile audioFile = AudioFileIO.read(source);
                    Tag tag = audioFile.getTag();

                    Artwork art = ArtworkFactory.createArtworkFromByte(cover);
                    tag.setField(art);
                    audioFile.commit();

                } catch (Exception e) {
                    Log.e(TAG, "Insert arwork error: " + e);
                }


                if (StorageHelper.copyFile(source, target, true))
                    StorageHelper.deleteFile(source);

                if (isAlbum) {

                    albumFlag = true;

                    if (currentTrack == trackCount || isCancel)
                        onStop();

                } else {
                    onStop();
                }

                Log.d(TAG, "Finished command : ffmpeg");
            }
        });

    }


    private void createSongPathList() {
        String selection = "album_id = " + mAlbum.getId();

        String[] projection = {MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.DATA};
        final String sortOrder = MediaStore.Audio.AudioColumns.TRACK + " ASC";

        Cursor cursor = null;
        try {
            Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            cursor = getApplicationContext().getContentResolver().query(uri, projection, selection, null, sortOrder);
            if (cursor != null) {
                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {

                    mSongList.add(cursor.getString(1));
                    cursor.moveToNext();
                }
            }

        } catch (Exception e) {
            Log.e("ConvertService", e.toString());

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String[] concat(String[] s1, String[] s2) {
        String[] erg = new String[s1.length + s2.length];

        System.arraycopy(s1, 0, erg, 0, s1.length);
        System.arraycopy(s2, 0, erg, s1.length, s2.length);

        return erg;
    }

    public static boolean isEncode() {
        return isEncode;
    }

    public static void setPermExt() {
        waitPermExt = true;
    }

    public static void cancel() {
        isCancel = true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
