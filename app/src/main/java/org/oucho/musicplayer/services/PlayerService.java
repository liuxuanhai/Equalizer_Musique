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
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.equalizer.AudioEffectsReceiver;
import org.oucho.musicplayer.db.QueueDbHelper;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.utils.BitmapHelper;
import org.oucho.musicplayer.view.Notification;
import org.oucho.musicplayer.utils.Permissions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class PlayerService extends Service implements MusiqueKeys {


    private static final String TAG_LOG = "PlayerService";


    public static final int NO_REPEAT = 20;
    public static final int REPEAT_ALL = 21;

    private static final int IDLE_DELAY = 60000;

    private final PlaybackBinder mBinder = new PlaybackBinder();

    private static MediaPlayer mediaPlayer1;
    private static MediaPlayer mediaPlayer2;
    private static MediaSessionCompat mMediaSession;

    private List<Song> mOriginalSongList = new ArrayList<>();

    private static final List<Song> mQueuePlayList = new ArrayList<>();

    private static boolean mShuffle = false;
    private static boolean mIsPaused = false;
    private static boolean mIsPlaying = false;
    private static boolean mHasPlaylist = false;

    private boolean mBound = false;
    private boolean firstPlay = true;
    private boolean mPausedByFocusLoss;
    private boolean mAutoPause = false;
    private boolean mPlayImmediately = false;

    private int mStartId;
    private int mCurrentPosition;
    private static int mRepeatMode = NO_REPEAT;

    private Boolean start = false;
    private static Song mCurrentSong;
    private AudioManager mAudioManager;
    private SharedPreferences mStatePrefs;
    private TelephonyManager mTelephonyManager;

    private static int currentPlayer = 1;


    @Override
    public void onCreate() {
        super.onCreate();
        mStatePrefs = getSharedPreferences(STATE_PREFS_NAME, MODE_PRIVATE);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        setMediaPlayer1();
        mediaPlayer1.setOnErrorListener(mOnErrorListener1);
        mediaPlayer1.setOnPreparedListener(mOnPreparedListener1);
        mediaPlayer1.setOnCompletionListener(mOnCompletionListener1);
        mediaPlayer1.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer1.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        setMediaPlayer2();
        mediaPlayer2.setOnErrorListener(mOnErrorListener2);
        mediaPlayer2.setOnPreparedListener(mOnPreparedListener2);
        mediaPlayer2.setOnCompletionListener(mOnCompletionListener2);
        mediaPlayer2.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer2.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        Intent i = new Intent(this, AudioEffectsReceiver.class);
        i.setAction(AudioEffectsReceiver.ACTION_OPEN_AUDIO_EFFECT_SESSION);
        i.putExtra(AudioEffectsReceiver.EXTRA_AUDIO_SESSION_ID, mediaPlayer1.getAudioSessionId());
        sendBroadcast(i);

        IntentFilter receiverFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(mHeadsetStateReceiver, receiverFilter);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mAutoPause = prefs.getBoolean(PREF_AUTO_PAUSE, false);

        restoreState();
        initTelephony();
        setupMediaSession();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mStartId = startId;
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (mQueuePlayList.size() == 0 || action.equals(ACTION_CHOOSE_SONG)) {

                    Intent dialogIntent = new Intent(this, MainActivity.class);
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(dialogIntent);

                } else if (action.equals(ACTION_TOGGLE)) {
                    toggle();
                } else if (action.equals(ACTION_PAUSE)) {
                    pause();
                } else if (action.equals(ACTION_STOP)) {

                    if (!mBound)
                        stopSelf(mStartId);

                } else if (action.equals(ACTION_NEXT)) {
                    playNext();
                } else if (action.equals(ACTION_PREVIOUS)) {
                    playPrev();
                }
            }
        }
        //return START_STICKY;
        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {

        if (mMediaSession != null)
            mMediaSession.release();

        unregisterReceiver(mHeadsetStateReceiver);

        if (mTelephonyManager != null)
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);

        mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);

        mediaPlayer1.stop();
        mediaPlayer1.release();

        mediaPlayer2.stop();
        mediaPlayer2.release();

        Intent i = new Intent(this, AudioEffectsReceiver.class);
        i.setAction(AudioEffectsReceiver.ACTION_CLOSE_AUDIO_EFFECT_SESSION);
        sendBroadcast(i);

        super.onDestroy();
    }


    private void restoreState() {

        if (Permissions.checkPermission(this) && mStatePrefs.getBoolean("stateSaved", false)) {

            int position = mStatePrefs.getInt("currentPosition", 0);

            QueueDbHelper dbHelper = new QueueDbHelper(this);
            List<Song> playList = dbHelper.readAll();
            dbHelper.close();

            mRepeatMode = mStatePrefs.getInt("repeatMode", mRepeatMode);

            setShuffle(mStatePrefs.getBoolean("shuffle", mShuffle));

            setPlayListInternal(playList);
            setPosition(position, false);

            open();
        }
    }


    private void initTelephony() {
        if (mAutoPause) {
            mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

            if (mTelephonyManager != null)
                mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }


    private void setupMediaSession() {
        mMediaSession = new MediaSessionCompat(this, TAG_LOG);
        mMediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                play();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onSkipToNext() {
                playNext();
            }

            @Override
            public void onSkipToPrevious() {
                playPrev();
            }

            @Override
            public void onStop() {
                pause();
            }

            @Override
            public void onSeekTo(long pos) {
                seekTo((int) pos);
            }
        });
    }


    public void setAutoPauseEnabled() {
        if (!mAutoPause) {
            mAutoPause = true;
            initTelephony();
        }
    }


    public void setPlayList(List<Song> songList, int position) {

        setPlayListInternal(songList);

        setPosition(position, true);

        if (mShuffle)
            shuffle();

        notifyChange(QUEUE_CHANGED);
    }


    private void setPlayListInternal(List<Song> songList) {

        if (songList == null || songList.size() <= 0)
            return;

        mOriginalSongList = songList;
        mQueuePlayList.clear();
        mQueuePlayList.addAll(mOriginalSongList);
        mHasPlaylist = true;
    }


    public void addToQueue(Song song) {
        mOriginalSongList.add(song);
        mQueuePlayList.add(song);
        notifyChange(ITEM_ADDED);
    }


    public void notifyChange(String what) {
            updateMediaSession(what);

        boolean saveQueue = (QUEUE_CHANGED.equals(what) || ITEM_ADDED.equals(what) || ORDER_CHANGED.equals(what));

        if (mQueuePlayList.size() > 0) {
            SharedPreferences.Editor editor = mStatePrefs.edit();
            editor.putBoolean("stateSaved", true);

            if (saveQueue) {
                QueueDbHelper dbHelper = new QueueDbHelper(this);
                dbHelper.removeAll();
                dbHelper.add();
                dbHelper.close();
            }

            editor.putInt("currentPosition", mCurrentPosition);
            editor.putInt("repeatMode", mRepeatMode);
            editor.putBoolean("shuffle", mShuffle);
            editor.apply();
        }

        if (PLAYSTATE_CHANGED.equals(what) || META_CHANGED.equals(what)) {
                Notification.updateNotification(getApplicationContext(), this);

            if (isPlaying()) {
                Intent music = new Intent();
                music.setAction("org.oucho.radio2.STOP");
                music.putExtra("halt", "stop");
                sendBroadcast(music);
            }
        }

        sendBroadcast(what, null);
    }


    private void updateMediaSession(String what) {

        if (!mMediaSession.isActive())
            mMediaSession.setActive(true);


        if (what.equals(PLAYSTATE_CHANGED)) {

            int playState = isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
            mMediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(playState, getPlayerPosition(), 1.0F)
                    .setActions(PlaybackStateCompat.ACTION_PLAY
                            | PlaybackStateCompat.ACTION_PAUSE
                            | PlaybackStateCompat.ACTION_PLAY_PAUSE
                            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                    .build());
        }

        if (what.equals(META_CHANGED)) {
            int artSize = (int) getResources().getDimension(R.dimen.notification_art_size);
            Bitmap artwork = null;

            Uri uri = ContentUris.withAppendedId(ARTWORK_URI, getAlbumId());

            try {
                if (uri != null) {
                    ContentResolver res = getApplicationContext().getContentResolver();
                    artwork = BitmapHelper.decode(res.openInputStream(uri), artSize, artSize);
                }
            } catch (IOException ignored) {}

            MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getArtistName())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, getAlbumName())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getSongTitle())
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getTrackDuration())
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork);
            mMediaSession.setMetadata(builder.build());
        }
    }


    private void sendBroadcast(String action, Bundle data) {

        Intent i = new Intent(action);

        if (data != null)
            i.putExtras(data);

        sendBroadcast(i);
    }


    private void play() {

        int result = mAudioManager.requestAudioFocus(mAudioFocusChangeListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);

        if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED && mQueuePlayList.size() >= 1) {

            if (currentPlayer == 1)
                mediaPlayer1.start();
            else
                mediaPlayer2.start();

            mIsPlaying = true;
            mIsPaused = false;

            Log.i(TAG_LOG, "play");
            notifyChange(PLAYSTATE_CHANGED);
        }
    }


    private void pause() {

        if (currentPlayer == 1)
            mediaPlayer1.pause();
        else
            mediaPlayer2.pause();

        mIsPlaying = false;
        mIsPaused = true;
        Log.i(TAG_LOG, "pause");

        notifyChange(PLAYSTATE_CHANGED);
    }


    private void resume() {
        play();
    }


    public void toggle() {

        if (mediaPlayer1.isPlaying() || mediaPlayer2.isPlaying())
            pause();
        else
            resume();

    }


    public void playPrev() {
        int position = getPreviousPosition();

        if (position >= 0 && position < mQueuePlayList.size()) {

            if (mediaPlayer2.isPlaying())
                mediaPlayer2.stop();

            mCurrentPosition = position;
            setCurrentSong(mQueuePlayList.get(position));
            openAndPlay();
        }
    }


    private int getPreviousPosition() {

        updateCurrentPosition();
        int position = mCurrentPosition;

        if ((isPlaying() && getPlayerPosition() >= 1500))
            return position;

        if (position - 1 < 0) {

            if (mRepeatMode == REPEAT_ALL)
                return mQueuePlayList.size() - 1;

            return -1;// NO_REPEAT;
        }

        return position - 1;
    }


    public void playNext() {
        int position = getNextPosition();

        if (position >= 0 && position < mQueuePlayList.size()) {

            if (mediaPlayer2.isPlaying())
                mediaPlayer2.stop();

            mCurrentPosition = position;
            setCurrentSong(mQueuePlayList.get(position));
            openAndPlay();

            Intent intentN = new Intent(INTENT_STATE);
            intentN.putExtra("state", "next");
            sendBroadcast(intentN);
        }
    }


    private int getNextPosition() {

        updateCurrentPosition();
        int position = mCurrentPosition;

        if (position + 1 >= mQueuePlayList.size()) {

            if (mRepeatMode == REPEAT_ALL)
                return 0;

            return -1;
        }
        return position + 1;
    }


    public int getNextRepeatMode() {
        switch (mRepeatMode) {

            case NO_REPEAT:
                return REPEAT_ALL;

            case REPEAT_ALL:
                return NO_REPEAT;

            default:
                break;
        }
        return NO_REPEAT;
    }


    public void shuffleOnOff(boolean value) {

        setShuffle(value);

        if (value) {
            shuffle();
        } else {
            mQueuePlayList.clear();
            mQueuePlayList.addAll(mOriginalSongList);
        }

        //on met à jour la position
        updateCurrentPosition();
        notifyChange(ORDER_CHANGED);
        Log.i(TAG_LOG, "setShuffleEnabled");

    }


    private void shuffle() {
        boolean b = mQueuePlayList.remove(mCurrentSong);
        Collections.shuffle(mQueuePlayList);

        if (b)
            mQueuePlayList.add(0, mCurrentSong);

        setPosition(0, false);
    }


    public void setPosition(int position, boolean play) {

        if (position >= mQueuePlayList.size())
            return;

        mCurrentPosition = position;

        try {
            Song song = mQueuePlayList.get(position);
            if (!song.equals(mCurrentSong)) {
                setCurrentSong(song);

                if (play)
                    openAndPlay();
                else
                    open();

            } else if (play) {
                play();
            }
        } catch (Exception ignore) {} // prenvent crash on multi (speed) press title for playing
    }


    private void updateCurrentPosition() {
        int pos = mQueuePlayList.indexOf(mCurrentSong);

        if (pos != -1)
            mCurrentPosition = pos;

    }


    private void openAndPlay() {

        mPlayImmediately = true;

        open();
    }


    private void open() {

        Log.i(TAG_LOG, "open()");

        if (mediaPlayer2.isPlaying())
            mediaPlayer2.stop();

        currentPlayer = 1;
        firstPlay = true;

        Bundle extras = new Bundle();
        extras.putInt(EXTRA_POSITION, getPositionWithinPlayList());
        sendBroadcast(POSITION_CHANGED, extras);

        mediaPlayer1.reset();

        try {
            Uri songUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mCurrentSong.getId());

            mediaPlayer1.setDataSource(getApplicationContext(), songUri);
            mediaPlayer1.prepareAsync();

        } catch (IllegalArgumentException
                | SecurityException
                | IllegalStateException
                | NullPointerException
                | IOException e) {
            Log.w("open() ee", "ee", e);
        }
    }


    private final OnPreparedListener mOnPreparedListener1 = new OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            Log.d(TAG_LOG, "mOnPreparedListener1");

            // évite de charger la notification au démarrage de l'application
            if (!start)
                start = true;
            else
                notifyChange(META_CHANGED);


            if (mPlayImmediately) {
                play();
                mPlayImmediately = false;
            }

            if (!firstPlay) {
                mediaPlayer2.setNextMediaPlayer(mediaPlayer1);

            } else {
                prepareNext(mediaPlayer2);
                firstPlay = false;
            }
        }
    };


    private final OnPreparedListener mOnPreparedListener2 = new OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            Log.d(TAG_LOG, "mOnPreparedListener2");

            try {
                mediaPlayer1.setNextMediaPlayer(mediaPlayer2);
            } catch (IllegalArgumentException e) {
                Log.w(TAG_LOG, "mOnPreparedListener2" + e);

            }

        }
    };


    private final OnCompletionListener mOnCompletionListener1 = new OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            Log.d(TAG_LOG, "mOnCompletionListener1");

            if (mCurrentPosition + 1 == mQueuePlayList.size() && mRepeatMode == NO_REPEAT) { // si fin de liste de lecture
                mIsPlaying = false;
                mIsPaused = true;
                notifyChange(PLAYSTATE_CHANGED);
            } else if (mQueuePlayList.size() >= 1) {

                currentPlayer = 2;

                int position = getNextForGapless();
                mCurrentPosition = position;
                setCurrentSong(mQueuePlayList.get(position));
                notifyChange(META_CHANGED);
                prepareNext(mediaPlayer1);
            }
        }
    };


    private final OnCompletionListener mOnCompletionListener2 = new OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            Log.d(TAG_LOG, "mOnCompletionListener2");

            if (mCurrentPosition + 1 == mQueuePlayList.size() && mRepeatMode == NO_REPEAT) {
                mIsPlaying = false;
                mIsPaused = true;
                notifyChange(PLAYSTATE_CHANGED);

            } else if (mQueuePlayList.size() >= 1) {

                currentPlayer = 1;

                int position = getNextForGapless();
                mCurrentPosition = position;
                setCurrentSong(mQueuePlayList.get(position));
                notifyChange(META_CHANGED);
                prepareNext(mediaPlayer2);
            }
        }
    };


    private final OnErrorListener mOnErrorListener1 = (mediaPlayer, what, extra) -> {
        Log.w(TAG_LOG, "onError 1: " + String.valueOf(what) + " " + String.valueOf(extra));
        return false;
    };


    private final OnErrorListener mOnErrorListener2 = (mediaPlayer, what, extra) -> {
        Log.w(TAG_LOG, "onError 2: " + String.valueOf(what) + " " + String.valueOf(extra));
        return false;
    };


    private int getNextForGapless() {

        int position = mCurrentPosition;

        if (position + 1 >= mQueuePlayList.size()) {

            if (mRepeatMode == REPEAT_ALL)
                return 0;

            return -1;
        }
        return position + 1;
    }

    private void prepareNext(MediaPlayer mplayer) {
        Log.d(TAG_LOG, "prepareNext");

        int position = getNextForGapless();

        if (position >= 0 && position < mQueuePlayList.size()) {

            Song nSong = mQueuePlayList.get(position);
            Uri nextSong = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, nSong.getId());

            try {
                mplayer.reset();
                mplayer.setDataSource(getApplicationContext(), nextSong);
                mplayer.prepareAsync();
            } catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
                Log.w("open() ee", "ee", e);
            }
        }
    }


    private final BroadcastReceiver mHeadsetStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), Intent.ACTION_HEADSET_PLUG) && isPlaying()) {
                boolean plugged = intent.getIntExtra("state", 0) == 1;

                if (!plugged)
                    pause();
            }
        }
    };


    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_OFFHOOK:
                case TelephonyManager.CALL_STATE_RINGING:
                    pause();
                    break;
                default:
                    break;
            }
        }
    };


    @SuppressLint("HandlerLeak")
    private final Handler mDelayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (isPlaying() || mBound)
                return;

            stopSelf(mStartId);
        }
    };


    private final OnAudioFocusChangeListener mAudioFocusChangeListener = new OnAudioFocusChangeListener() {

        public void onAudioFocusChange(int focusChange) {

            switch (focusChange) {

                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (isPlaying()) {
                        pause();
                        mPausedByFocusLoss = true;
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (!isPlaying() && mPausedByFocusLoss) {
                        resume();
                        mPausedByFocusLoss = false;
                    }

                        mediaPlayer1.setVolume(1.0f, 1.0f);
                        mediaPlayer2.setVolume(1.0f, 1.0f);

                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (isPlaying()) {

                        mediaPlayer1.setVolume(0.1f, 0.1f);
                        mediaPlayer2.setVolume(0.1f, 0.1f);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
                    pause();
                    mPausedByFocusLoss = false;
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        mBound = true;
        return mBinder;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        mBound = false;
        if (isPlaying())
            return true;

        if (mQueuePlayList.size() > 0) {
            Message msg = mDelayedStopHandler.obtainMessage();
            mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
            return true;
        }

        stopSelf(mStartId);
        return true;
    }


    public static void seekTo(int msec) {

        if (currentPlayer == 1)
            mediaPlayer1.seekTo(msec);
        else
            mediaPlayer2.seekTo(msec);
    }


    private void setMediaPlayer1() {
        mediaPlayer1 = new MediaPlayer();
    }

    private void setMediaPlayer2() {
        mediaPlayer2 = new MediaPlayer();
    }

    private void setCurrentSong(Song value) {
        mCurrentSong = value;
    }

    public static boolean isPlaying() {
        return mIsPlaying;
    }

    public static boolean isPaused() {
        return mIsPaused;
    }

    public static boolean hasPlaylist() {
        return mHasPlaylist;
    }

    private static void setShuffle(boolean value) {
        mShuffle = value;
    }

    public static boolean isShuffleEnabled() {
        return mShuffle;
    }

    public static void setVolume(float vol) {

        mediaPlayer1.setVolume(vol, vol);
        mediaPlayer2.setVolume(vol, vol);
    }

    public void setRepeatMode(int mode) {
        setmRepeatMode(mode);
        notifyChange(REPEAT_MODE_CHANGED);
    }

    private static void setmRepeatMode(int value) {
        mRepeatMode = value;
    }

    public static int getRepeatMode() {
        return mRepeatMode;
    }


    public static String getSongTitle() {

        if (mCurrentSong != null)
            return mCurrentSong.getTitle();

        return null;
    }

    public static String getArtistName() {

        if (mCurrentSong != null)
            return mCurrentSong.getArtist();

        return null;
    }

    public static String getAlbumName() {

        if (mCurrentSong != null)
            return mCurrentSong.getAlbum();

        return null;
    }

    public static long getAlbumId() {

        if (mCurrentSong != null)
            return mCurrentSong.getAlbumId();

        return -1;
    }

    public static long getSongID() {

        if (mCurrentSong != null)
            return mCurrentSong.getId();

        return -1;
    }

    public static String getSongPath() {

        if (mCurrentSong != null)
            return mCurrentSong.getPath();

        return null;
    }

    public static int getTrackDuration() {

        if (currentPlayer == 1)
            return mediaPlayer1.getDuration();
        else
            return mediaPlayer2.getDuration();

    }

    public static int getPlayerPosition() {

        if (currentPlayer == 1)
            return mediaPlayer1.getCurrentPosition();
        else
            return mediaPlayer2.getCurrentPosition();

    }

    public static int getPositionWithinPlayList() {
        return mQueuePlayList.indexOf(mCurrentSong);
    }

    public static MediaSessionCompat getMediaSession() {
        return mMediaSession;
    }

    public class PlaybackBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    public static List<Song> getQueuePlayList() {
        return mQueuePlayList;
    }

}