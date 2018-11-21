package org.oucho.musicplayer.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.QueueDbHelper;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.services.PlayerService;
import org.oucho.musicplayer.tools.LockableViewPager;
import org.oucho.musicplayer.utils.BitmapHelper;

import java.io.File;
import java.util.List;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class PlayerFragment extends BaseFragment
        implements MusiqueKeys,
        SharedPreferences.OnSharedPreferenceChangeListener {

    @SuppressWarnings("unused")
    private static final String TAG_LOG = "Player Fragment";

    private Context mContext;

    private View rootView;
    private SeekBar mSeekBar;
    private TextView nbTrack;
    private SharedPreferences préférences;
    private final PlayerService mPlayerService = MainActivity.getPlayerService();

    private final Handler mHandler = new Handler();

    private TextView bitrate;

    private int track = -1;
    private int total_track = -1;

    private boolean mServiceBound;

    private ImageView artworkView;

    public static PlayerFragment newInstance() {
        PlayerFragment fragment = new PlayerFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void load() {

    }

    /* *********************************************************************************************
 * Création du fragment
 * ********************************************************************************************/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getContext();

        préférences = mContext.getSharedPreferences(STATE_PREFS_NAME, MODE_PRIVATE);
        préférences.registerOnSharedPreferenceChangeListener(this);

        total_track = getSizeQueue();
        track = préférences.getInt("currentPosition", 0) + 1;

        Intent intent = new Intent();
        intent.setAction(INTENT_LAYOUTVIEW);
        intent.putExtra("vue", "playBarLayout");
        mContext.sendBroadcast(intent);

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        assert wm != null;
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        mHandler.postDelayed(() -> {

            String album = PlayerService.getAlbumName();
            getActivity().setTitle(album);

            Intent intent1 = new Intent();
            intent1.setAction(INTENT_TOOLBAR_SHADOW);
            intent1.putExtra("boolean", false);
            mContext.sendBroadcast(intent1);

        }, 300);

    }



    /* *********************************************************************************************
     * Création du visuel
     * ********************************************************************************************/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_player, container, false);

        SharedPreferences préférences = mContext.getSharedPreferences(STATE_PREFS_NAME, MODE_PRIVATE);
        track = préférences.getInt("currentPosition", 0) + 1;

        nbTrack = rootView.findViewById(R.id.nombre_titre);
        nbTrack.setText(track + "/" + total_track);

        bitrate = rootView.findViewById(R.id.bitrate);
        bitrate.setText(getBitrate());

        mSeekBar = rootView.findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);

        LinearLayout linearLayout = rootView.findViewById(R.id.root);
        linearLayout.setOnClickListener(mOnClickListener);

        artworkView = rootView.findViewById(R.id.artwork);

        updateAll();

        return rootView;
    }

    // empêche les clicks vers le layout du dessous
    private final View.OnClickListener mOnClickListener = view -> {

        if (mPlayerService == null) {
            return;
        }
        switch (view.getId()) {
            case R.id.root:
                //mPlayerService.toggle();
                break;


            default:
                break;
        }
    };


    private int getSizeQueue() {
        QueueDbHelper dbHelper = new QueueDbHelper(mContext);
        List<Song> playList = dbHelper.readAll();
        dbHelper.close();

        return playList.size();
    }

    private final Runnable mUpdateSeekBarRunnable = new Runnable() {

        @Override
        public void run() {

            updateSeekBar();

            mHandler.postDelayed(mUpdateSeekBarRunnable, 250);
        }
    };

    private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && mPlayerService != null && (PlayerService.isPlaying() || PlayerService.isPaused())) {
                PlayerService.seekTo(seekBar.getProgress());
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mHandler.removeCallbacks(mUpdateSeekBarRunnable);

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (mPlayerService != null && PlayerService.isPlaying()) {
                mHandler.post(mUpdateSeekBarRunnable);
            }

        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if ("currentPosition".equals(key)) {

            track = préférences.getInt("currentPosition", 0) + 1;
            nbTrack.setText(track + "/" + total_track);
        }
    }


    private final BroadcastReceiver mServiceListener = new BroadcastReceiver() {


        @Override
        public void onReceive(Context context, Intent intent) {

            if (mPlayerService == null) {
                return;
            }

            String action = intent.getAction();

            assert action != null;
            switch (action) {

                case PlayerService.PLAYSTATE_CHANGED:

                    if (PlayerService.isPlaying()) {
                        mHandler.post(mUpdateSeekBarRunnable);
                    } else {
                        mHandler.removeCallbacks(mUpdateSeekBarRunnable);
                    }
                    break;

                case PlayerService.META_CHANGED:
                    updateTrackInfo();
                    break;

                case PlayerService.QUEUE_CHANGED:
                case PlayerService.POSITION_CHANGED:
                case PlayerService.ITEM_ADDED:
                case PlayerService.ORDER_CHANGED:

                    break;
                default:
                    break;
            }
        }

    };


    @Override
    public void onPause() {
        super.onPause();

        mContext.unregisterReceiver(mServiceListener);

        if (mServiceBound) {
            mServiceBound = false;
        }
        mHandler.removeCallbacks(mUpdateSeekBarRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mServiceBound) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(PlayerService.META_CHANGED);
            filter.addAction(PlayerService.PLAYSTATE_CHANGED);
            filter.addAction(PlayerService.POSITION_CHANGED);
            filter.addAction(PlayerService.ITEM_ADDED);
            filter.addAction(PlayerService.ORDER_CHANGED);
            mContext.registerReceiver(mServiceListener, filter);
        } else {
            updateAll();
        }

        LockableViewPager.setSwipeLocked(true);

        // Active la touche back
        if (getView() == null) {
            return;
        }

        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener((v, keyCode, event) -> {

            if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {

                int viewID = MainActivity.getViewID();
                final int couleurTitre = ContextCompat.getColor(getContext(), R.color.grey_400);

                if( ! MainActivity.getPlaylistFragmentState() && ! MainActivity.getAlbumFragmentState())
                    LockableViewPager.setSwipeLocked(false);


                if (MainActivity.getQueueLayout()) {

                    Intent intent = new Intent();
                    intent.setAction(INTENT_QUEUEVIEW);
                    mContext.sendBroadcast(intent);

                    return true;


                } else if (getFragmentManager().findFragmentById(viewID) != null) {

                    Intent intent0 = new Intent();
                    intent0.setAction(INTENT_LAYOUTVIEW);
                    intent0.putExtra("vue", "layoutx");
                    mContext.sendBroadcast(intent0);

                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_out_bottom, R.anim.slide_out_bottom);
                    ft.remove(getFragmentManager().findFragmentById(viewID));
                    ft.commit();

                    Intent intent = new Intent();
                    intent.setAction("reload");
                    mContext.sendBroadcast(intent);

                    if (MainActivity.getAlbumFragmentState()) {

                        Intent titreAlbum = new Intent();
                        titreAlbum.setAction(SET_TITLE);
                        mContext.sendBroadcast(titreAlbum);

                    } else {
                        Intent shadow = new Intent();
                        shadow.setAction(INTENT_TOOLBAR_SHADOW);
                        shadow.putExtra("boolean", true);
                        mContext.sendBroadcast(shadow);
                    }

                    if (!MainActivity.getPlaylistFragmentState() && !MainActivity.getAlbumFragmentState()) {

                        Intent menu = new Intent();
                        menu.setAction(INTENT_SET_MENU);
                        menu.putExtra("boolean", true);
                        mContext.sendBroadcast(menu);

                        setHasOptionsMenu(true);
                    }

                    if (MainActivity.getViewID() == R.id.fragment_playlist_list || MainActivity.getViewID() == R.id.fragment_playlist) {
                        LockableViewPager.setSwipeLocked(false);
                        String titre = (mContext.getString(R.string.playlists));

                        if (MainActivity.getViewID() == R.id.fragment_playlist_list) {
                            setHasOptionsMenu(true);
                        } else {
                            String playlist = MainActivity.getPlaylistName();

                            if (android.os.Build.VERSION.SDK_INT >= 24) {
                                getActivity().setTitle(Html.fromHtml("<font>" + titre + " " + " " + " </font> <small> <font color='" + couleurTitre + "'>"
                                        + playlist + "</small></font>", Html.FROM_HTML_MODE_LEGACY));
                            } else {
                                //noinspection deprecation
                                getActivity().setTitle(Html.fromHtml("<font>" + titre + " " + " " + " </font> <small> <font color='" + couleurTitre + "'>" + playlist + "</small></font>"));
                            }

                        }
                    }


                    if (MainActivity.getViewID() == R.id.fragment_song_layout) {
                        LockableViewPager.setSwipeLocked(false);

                        SharedPreferences prefs = mContext.getSharedPreferences(FICHIER_PREFS, Context.MODE_PRIVATE);
                        String getTri = prefs.getString("song_sort_order", "");

                        String tri;
                        if ("year DESC".equals(getTri)) {
                            tri = mContext.getString(R.string.title_sort_year);
                        } else if ("REPLACE ('<BEGIN>' || artist, '<BEGIN>The ', '<BEGIN>')".equals(getTri)) {
                            tri = mContext.getString(R.string.title_sort_artist);
                        } else if ("album".equals(getTri)) {
                            tri = mContext.getString(R.string.title_sort_album);
                        } else if ("_id DESC".equals(getTri)) {
                            tri = mContext.getString(R.string.title_sort_add);
                        } else {
                            tri = "a-z";
                        }

                        MainActivity.setViewID(R.id.fragment_song_layout);

                        String titre = mContext.getString(R.string.titles);

                        if (android.os.Build.VERSION.SDK_INT >= 24) {
                            getActivity().setTitle(Html.fromHtml("<font>" + titre + " " + " " + " </font> <small> <font color='" + couleurTitre + "'>"
                                    + tri + "</small></font>", Html.FROM_HTML_MODE_LEGACY));
                        } else {
                            //noinspection deprecation
                            getActivity().setTitle(Html.fromHtml("<font>" + titre + " " + " " + " </font> <small> <font color='" + couleurTitre + "'>"
                                    + tri + "</small></font>"));
                        }
                        setHasOptionsMenu(true);
                    }

                    return true;
                }
                return false;
            }
            return false;
        });

    }


    private void updateAll() {
        if (mPlayerService != null) {

            updateTrackInfo();

            if (PlayerService.isPlaying()) {
                mHandler.post(mUpdateSeekBarRunnable);
            }
        }
    }


    private void updateTrackInfo() {

        if (mPlayerService != null) {

            String title = PlayerService.getSongTitle();
            final String artist = PlayerService.getArtistName();

            if (title != null)
                ((TextView) rootView.findViewById(R.id.song_title)).setText(title);


            if (artist != null)
                ((TextView) rootView.findViewById(R.id.song_artist)).setText(artist);

            try {

                String songPath = PlayerService.getSongPath();

                File file = null;
                if (songPath != null)
                    file = new File(songPath);

                if (file != null) {
                    AudioFile audioFile = AudioFileIO.read(file);
                    Tag tag = audioFile.getTag();

                    byte[] cover = tag.getFirstArtwork().getBinaryData();

                    Bitmap bitmap = BitmapHelper.byteToBitmap(cover);
                    artworkView.setImageBitmap(bitmap);
                }

            } catch (Exception ignore) {}


            final int duration = PlayerService.getTrackDuration();

            if (duration != -1) {
                ((TextView) rootView.findViewById(R.id.track_duration)).setText(msToText(duration));
                mSeekBar.setMax(duration);
                updateSeekBar();
            }

            bitrate.setText(getBitrate());
        }
    }


    private String msToText(int msec) {
        return String.format(Locale.getDefault(), "%d:%02d", msec / 60000, (msec % 60000) / 1000);
    }

    private void updateSeekBar() {
        if (mPlayerService != null) {
            int position = PlayerService.getPlayerPosition();
            mSeekBar.setProgress(position);

            //noinspection ConstantConditions
            ((TextView) rootView.findViewById(R.id.current_position)).setText(msToText(position));
        }
    }

    private String getBitrate() {

        AudioFile audioFile = null;

            try {
                String songPath = PlayerService.getSongPath();

                File file = null;
                if (songPath != null)
                    file = new File(songPath);

                if (file != null)
                    audioFile = AudioFileIO.read(file);

            } catch (Exception ignore) {}

        if (audioFile != null) {
            String bitRate = audioFile.getAudioHeader().getBitRate();
            String sampleRate = audioFile.getAudioHeader().getSampleRate();
            String mime = audioFile.getAudioHeader().getEncodingType();

            if (mime.contains("FLAC")) // remove bits depth info
                mime = "FLAC";

            int bit_depth = audioFile.getAudioHeader().getBitsPerSample();

            switch (sampleRate) {
                case "96000":
                    sampleRate = "96k";
                    break;
                case "88200":
                    sampleRate = "88.2k";
                    break;
                case "64000":
                    sampleRate = "64k";
                    break;
                case "48000":
                    sampleRate = "48k";
                    break;
                case "44100":
                    sampleRate = "44.1k";
                    break;
                case "32000":
                    sampleRate = "32k";
                    break;
                case "24000":
                    sampleRate = "24k";
                    break;
                case "22000":
                    sampleRate = "22k";
                    break;
            }

            return mime.toUpperCase() + " - " + bitRate.replace("~", "") + "kb/s" + " - " + bit_depth + "bits" + "/" + sampleRate + "Hz";
        }

        return null;
    }

}
