package org.oucho.musicplayer;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.oucho.musicplayer.dialog.SaveTagProgressDialog;
import org.oucho.musicplayer.services.ConvertService;
import org.oucho.musicplayer.services.PlayerService;
import org.oucho.musicplayer.services.PlayerService.PlaybackBinder;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.dialog.AboutDialog;
import org.oucho.musicplayer.fragments.BaseFragment;
import org.oucho.musicplayer.fragments.LibraryFragment;
import org.oucho.musicplayer.fragments.PlayerFragment;
import org.oucho.musicplayer.fragments.adapters.QueueAdapter;
import org.oucho.musicplayer.tools.CustomSwipe;
import org.oucho.musicplayer.update.CheckUpdate;
import org.oucho.musicplayer.utils.NavigationUtils;
import org.oucho.musicplayer.view.Notification;
import org.oucho.musicplayer.utils.PreferenceUtil;
import org.oucho.musicplayer.utils.VolumeTimer;
import org.oucho.musicplayer.view.CustomLayoutManager;
import org.oucho.musicplayer.view.DragRecyclerView;
import org.oucho.musicplayer.view.ProgressBar;
import org.oucho.musicplayer.view.SeekArc;
import org.oucho.musicplayer.view.blurview.BlurView;
import org.oucho.musicplayer.view.blurview.RenderScriptBlur;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.oucho.musicplayer.R.id.drawer_layout;

public class MainActivity extends AppCompatActivity implements
        MusiqueKeys,
        OnNavigationItemSelectedListener {

    private static final String TAG = "Main Activity";

    private Context mContext;

    private View mQueueLayout;
    private List<Song> mQueue;
    private BlurView queueBlurView;
    private TextView timeAfficheur;
    private RelativeLayout layoutA;
    private RelativeLayout layoutB;
    private ProgressBar mProgressBar;
    private DrawerLayout mDrawerLayout;
    private ImageButton forwardButton;
    private QueueAdapter mQueueAdapter;
    private ImageButton previousButton;
    private ImageButton forwardButton0;
    private ImageButton previousButton0;
    private DragRecyclerView mQueueView;
    private CountDownTimer minuteurVolume;
    private NavigationView mNavigationView;

    private final VolumeTimer volume = new VolumeTimer();

    private Menu menu;
    private static ScheduledFuture mTask;
    private static PlayerService mPlayerService;

    private final Handler mHandler = new Handler();

    private boolean radioIsInstalled = false;

    private boolean mServiceBound = false;
    private boolean autoScrollQueue = false;

    private static String playlistName;

    private static int viewID;

    private static boolean running;
    private static boolean chercheActivity = false;

    private static boolean queueLayout = false;
    private static boolean playBarLayout = false;
    private static boolean albumFragmentState = false;
    private static boolean playlistFragmentState = false;

    private ImageView shuffleBar;
    private ImageView repeatBar;
    private ImageView repeatBar1;

    private RelativeLayout playbarShadow;

    /* *********************************************************************************************
     * Création de l'activité
     * ********************************************************************************************/

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();


        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final int mUIFlag = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getWindow().getDecorView().setSystemUiVisibility(mUIFlag);
        }

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            checkPermissions();
        }

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mQueueLayout = findViewById(R.id.queue_layout);
        mQueueView = findViewById(R.id.queue_view);
        mQueueView.setLayoutManager(new CustomLayoutManager(this));

        mQueueAdapter = new QueueAdapter(mContext, mQueueView);

        ItemTouchHelper.Callback callback = new CustomSwipe(mQueueAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mQueueView);

        mQueueView.setOnItemMovedListener((oldPosition, newPosition) -> {
            mQueueAdapter.moveItem(oldPosition, newPosition);
            mPlayerService.notifyChange(QUEUE_CHANGED);
        });

        mQueueAdapter.setOnItemClickListener((oldPosition, newPosition) -> {
            updateQueue();
            mQueueAdapter.notifyDataSetChanged();
            mPlayerService.notifyChange(QUEUE_CHANGED);
        });

        mQueueView.setAdapter(mQueueAdapter);

        findViewById(R.id.repeat0).setOnClickListener(mOnClickListener);
        findViewById(R.id.shuffle0).setOnClickListener(mOnClickListener);

        findViewById(R.id.track_info).setOnClickListener(mOnClickListener);

        findViewById(R.id.quick_prev).setOnClickListener(mOnClickListener);
        findViewById(R.id.quick_next).setOnClickListener(mOnClickListener);
        findViewById(R.id.quick_prev0).setOnClickListener(mOnClickListener);
        findViewById(R.id.quick_next0).setOnClickListener(mOnClickListener);

        findViewById(R.id.quick_prev).setOnLongClickListener(mOnLongClickListener);
        findViewById(R.id.quick_next).setOnLongClickListener(mOnLongClickListener);
        findViewById(R.id.quick_prev0).setOnLongClickListener(mOnLongClickListener);
        findViewById(R.id.quick_next0).setOnLongClickListener(mOnLongClickListener);

        findViewById(R.id.play_pause_toggle).setOnClickListener(mOnClickListener);
        findViewById(R.id.play_pause_toggle0).setOnClickListener(mOnClickListener);

        layoutA = findViewById(R.id.track_info);
        layoutB = findViewById(R.id.track_info0);

        previousButton = findViewById(R.id.quick_prev);
        forwardButton = findViewById(R.id.quick_next);
        previousButton0 = findViewById(R.id.quick_prev0);
        forwardButton0 = findViewById(R.id.quick_next0);

        timeAfficheur = findViewById(R.id.zZz);

        queueBlurView = findViewById(R.id.queueBlurView);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mProgressBar = findViewById(R.id.progress_bar);

        shuffleBar = findViewById(R.id.bar0_shuffle);
        repeatBar = findViewById(R.id.bar0_repeat);
        repeatBar1 = findViewById(R.id.bar0_repeat1);

        radioIsInstalled = checkApp();

        mNavigationView = findViewById(R.id.navigation_view);
        mNavigationView.inflateMenu(R.menu.navigation);
        mNavigationView.setNavigationItemSelectedListener(this);

        setNavigationMenu();

        playbarShadow = findViewById(R.id.playbar_shadow);

        if (savedInstanceState == null) {
            showLibrary();
        }

        CheckUpdate.onStart(this);
    }



    private void setNavigationMenu() {

        Menu navigatioMenu = mNavigationView.getMenu();

        if (radioIsInstalled) {
            navigatioMenu.setGroupVisible(R.id.add_radio, true);
            navigatioMenu.setGroupVisible(R.id.haut_default, false);
        } else {
            navigatioMenu.setGroupVisible(R.id.add_radio, false);
            navigatioMenu.setGroupVisible(R.id.haut_default, true);
        }
    }

    private boolean checkApp() {
        PackageManager packageManager = getPackageManager();

        try {
                packageManager.getPackageInfo(MusiqueKeys.APP_RADIO, 0);
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
    }



    private void setBlurView() {
        final float radius = 5f;

        final ViewGroup rootView = findViewById(R.id.drawer_layout);

        queueBlurView.setupWith(rootView)
                .blurAlgorithm(new RenderScriptBlur(mContext, true))
                .blurRadius(radius);
    }

    /* *********************************************************************************************
     * Navigation Drawer
     * ********************************************************************************************/

    public DrawerLayout getDrawerLayout() {
        return mDrawerLayout;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        mHandler.postDelayed(() -> mDrawerLayout.closeDrawers(), 300);

        switch (menuItem.getItemId()) {
            case R.id.action_equalizer:
            case R.id.action_equalizer0:
                NavigationUtils.showEqualizer(this);
                break;

            case R.id.action_timer:
            case R.id.action_timer0:
                if (! running) {
                    showTimePicker();
                } else {
                    showTimerInfo();
                }
                break;

            case R.id.action_radio:
                Intent radio = getPackageManager().getLaunchIntentForPackage(APP_RADIO);
                startActivity(radio);
                break;

            case R.id.nav_update:
                CheckUpdate.withInfo(this);
                break;

            case R.id.nav_about:
                showAboutDialog();
                break;

            case R.id.nav_exit:
                exit();
                break;

            default:
                break;
        }
        return true;
    }


    /**************
     * About dialog
     **************/

    private void showAboutDialog(){
        AboutDialog dialog = new AboutDialog();
        dialog.show(getSupportFragmentManager(), "about");
    }

    /* *********************************************************************************************
     * Click listener activity_main layout
     * ********************************************************************************************/

    private final OnClickListener mOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            if (mPlayerService == null) {
                return;
            }

            Vibrator vibes = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            switch (v.getId()) {
                case R.id.play_pause_toggle0:
                case R.id.play_pause_toggle:
                    mPlayerService.toggle();

                    Intent intentPl = new Intent(INTENT_STATE);
                    intentPl.putExtra("state", "play");
                    sendBroadcast(intentPl);
                    break;

                case R.id.quick_prev:
                case R.id.quick_prev0:
                    autoScrollQueue = true;
                    mPlayerService.playPrev();
                    Intent intentP = new Intent(INTENT_STATE);
                    intentP.putExtra("state", "prev");
                    sendBroadcast(intentP);

                    updateQueue();
                    mQueueAdapter.notifyDataSetChanged();
                    break;

                case R.id.quick_next:
                case R.id.quick_next0:
                    autoScrollQueue = true;

                    mPlayerService.playNext();

                    Intent intentN = new Intent(INTENT_STATE);
                    intentN.putExtra("state", "next");
                    sendBroadcast(intentN);

                    updateQueue();
                    mQueueAdapter.notifyDataSetChanged();
                    break;

                case R.id.shuffle0:
                    assert vibes != null;
                    vibes.vibrate(20);

                    boolean shuffle = PlayerService.isShuffleEnabled();
                    mPlayerService.shuffleOnOff(!shuffle);
                    updateShuffleButton();
                    updateQueue();
                    mPlayerService.notifyChange(QUEUE_CHANGED);
                    break;

                case R.id.repeat0:
                    assert vibes != null;
                    vibes.vibrate(20);

                    int mode = mPlayerService.getNextRepeatMode();
                    mPlayerService.setRepeatMode(mode);
                    updateRepeatButton();
                    updateQueue();
                    mPlayerService.notifyChange(QUEUE_CHANGED);
                    break;

                case R.id.track_info:
                    goPlayer();
                    break;

                default:
                    break;
            }
        }
    };


    private final View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {

            if (mPlayerService == null) {
                return false;
            }

            switch (v.getId()) {

                case R.id.quick_prev:
                case R.id.quick_prev0:
                    mHandler.postDelayed(fRewind, 300);
                    break;

                case R.id.quick_next:
                case R.id.quick_next0:
                    mHandler.postDelayed(fForward, 300);
                    break;

                default:
                    break;
            }

            return true;
        }
    };



    private void updateShuffleButton() {
        boolean shuffle = PlayerService.isShuffleEnabled();
        ImageView shuffleButton = findViewById(R.id.shuffle0);

        if (shuffle) {
            assert shuffleButton != null;
            shuffleButton.setImageResource(R.drawable.ic_shuffle_grey_600_24dp);
            shuffleBar.setVisibility(View.VISIBLE);

        } else {
            assert shuffleButton != null;
            shuffleButton.setImageResource(R.drawable.ic_shuffle_grey_400_24dp);
            shuffleBar.setVisibility(View.GONE);
        }
    }

    private void updateRepeatButton() {
        ImageView repeatButton = findViewById(R.id.repeat0);

        int mode = PlayerService.getRepeatMode();

        if (mode == PlayerService.NO_REPEAT) {
            assert repeatButton != null;
            repeatButton.setImageResource(R.drawable.ic_repeat_grey_400_24dp);
            repeatBar.setVisibility(View.GONE);
            repeatBar1.setVisibility(View.GONE);

        } else if (mode == PlayerService.REPEAT_ALL) {
            assert repeatButton != null;
            repeatButton.setImageResource(R.drawable.ic_repeat_grey_600_24dp);
            repeatBar.setVisibility(View.VISIBLE);
            repeatBar1.setVisibility(View.GONE);

        }
    }

    /* *********************************************************************************************
     * Menu
     * ********************************************************************************************/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity, menu);

        this.menu = menu;

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else {
                    showLibrary();
                }
                return true;

            case R.id.action_view_queue:
                autoScrollQueue = true;

                updateQueue();
                toggleQueue();

                return true;

            case R.id.action_search:
                NavigationUtils.showSearchActivity(this);
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }



    private void toggleQueue() {
        if (mQueueLayout.getVisibility() != View.VISIBLE) {
            mQueueLayout.setVisibility(View.VISIBLE);
            queueBlurView.setVisibility(View.VISIBLE);

            queueLayout = true;

            queueBlurView.setBlurAutoUpdate(true);

            setBlurView();

        } else {
            mQueueLayout.setVisibility(View.GONE);
            queueBlurView.setVisibility(View.GONE);

            queueLayout = false;

            queueBlurView.setBlurAutoUpdate(false);

        }
    }



    /* ************************
     * Affiche la bibliothèque
     * ************************/

    private void showLibrary() {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, LibraryFragment.newInstance())
                .commit();
    }


    /* *********************************************************************************************
     * Pause, resume etc.
     * ********************************************************************************************/

    @Override
    protected void onPause() {
        super.onPause();

        if (!PlayerService.isPlaying())
            killNotif();


        if (mServiceBound) {
            mPlayerService = null;

            unregisterReceiver(mServiceListener);

            unbindService(mServiceConnection);
            mServiceBound = false;
        }

        mHandler.removeCallbacks(mUpdateProgressBar);

    }

    @Override
    protected void onResume() {
        super.onResume();


        if (!mServiceBound) {
            Intent mServiceIntent = new Intent(this, PlayerService.class);
            bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            startService(mServiceIntent);

            IntentFilter filter = new IntentFilter();
            filter.addAction(PlayerService.META_CHANGED);
            filter.addAction(PlayerService.PLAYSTATE_CHANGED);
            filter.addAction(PlayerService.POSITION_CHANGED);
            filter.addAction(PlayerService.ITEM_ADDED);
            filter.addAction(PlayerService.ORDER_CHANGED);
            filter.addAction(INTENT_QUIT);
            filter.addAction(INTENT_QUEUEVIEW);
            filter.addAction(INTENT_LAYOUTVIEW);
            filter.addAction(INTENT_SET_MENU);
            filter.addAction(STORAGE_ACCESS_FRAMEWORK);


            registerReceiver(mServiceListener, filter);
        } else {
            updateAll();
        }

    }


    /* *********************************************************************************************
     * Prepare fragments.
     * ********************************************************************************************/


    @SuppressWarnings("RestrictedApi")
    private void refresh() {

        int fragmentNB = getSupportFragmentManager().getFragments().size();
        Fragment f;

        for (int pos = 0; pos < fragmentNB; pos++) {

            f = getSupportFragmentManager().getFragments().get(pos);

            if (f != null) {
                ((BaseFragment) f).load();
            }
        }

    }



    /***********************************************************************************************
     * Lecture
     **********************************************************************************************/

    private final Runnable mUpdateProgressBar = new Runnable() {

        @Override
        public void run() {

            mProgressBar.setProgress(PlayerService.getPlayerPosition());

            mHandler.postDelayed(mUpdateProgressBar, 250);
        }
    };

    private final Runnable fForward = new Runnable() {
        @Override
        public void run() {
            if (forwardButton.isPressed() || forwardButton0.isPressed()) {
                int position = PlayerService.getPlayerPosition();
                position += 7000;
                PlayerService.seekTo(position);
                mHandler.postDelayed(fForward, 250);
            }
        }
    };

    private final Runnable fRewind = new Runnable() {
        @Override
        public void run() {
            if (previousButton.isPressed() || previousButton0.isPressed()) {
                int position = PlayerService.getPlayerPosition();
                position -= 7000;
                PlayerService.seekTo(position);
                mHandler.postDelayed(fRewind, 250);
            }
        }
    };



    private final BroadcastReceiver mServiceListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();

            Log.i(TAG, "private final BroadcastReceiver mServiceListener = " + receiveIntent);


            if (mPlayerService == null) {
                return;
            }

            assert receiveIntent != null;
            if (receiveIntent.equals(INTENT_QUEUEVIEW)) {

                mQueueLayout.setVisibility(View.GONE);
                queueBlurView.setVisibility(View.GONE);

                queueLayout = false;
            }

            if (receiveIntent.equals(INTENT_LAYOUTVIEW)) {

                final float tailleBarre = getResources().getDimension(R.dimen.barre_lecture);

                if ("playBarLayout".equals(intent.getStringExtra("vue"))) {

                    TranslateAnimation animate = new TranslateAnimation(0, 0, tailleBarre, 0);
                    animate.setDuration(400);
                    animate.setFillAfter(true);
                    layoutB.startAnimation(animate);
                    layoutB.setVisibility(View.VISIBLE);

                    TranslateAnimation animate2 = new TranslateAnimation(0, 0, 0, -tailleBarre);
                    animate2.setDuration(400);
                    animate2.setFillAfter(true);
                    layoutA.startAnimation(animate2);
                    layoutA.setVisibility(View.GONE);

                    Animation fadeOut = new AlphaAnimation(1, 0);
                    fadeOut.setInterpolator(new AccelerateInterpolator());
                    fadeOut.setDuration(400);
                    mProgressBar.setAnimation(fadeOut);
                    mProgressBar.setVisibility(View.GONE);


                    playbarShadow.setElevation(0);
                    playBarLayout = true;

                } else {

                    // TranslateAnimation(float fromXDelta, float toXDelta, float fromYDelta, float toYDelta)
                    TranslateAnimation animate2 = new TranslateAnimation(0, 0, -tailleBarre, 0);
                    animate2.setDuration(400);
                    animate2.setFillAfter(true);
                    layoutA.startAnimation(animate2);
                    layoutA.setVisibility(View.VISIBLE);

                    TranslateAnimation animate = new TranslateAnimation(0, 0, 0, tailleBarre);
                    animate.setDuration(400);
                    animate.setFillAfter(true);
                    layoutB.startAnimation(animate);

                    // bug GONE, reste actif si pas de clearAnimation
                    mHandler.postDelayed(() -> {
                        layoutB.clearAnimation();
                        layoutB.setVisibility(View.GONE);
                    }, 400);


                    if (playBarLayout) {
                        Animation fadeIn = new AlphaAnimation(0, 1);
                        fadeIn.setInterpolator(new AccelerateInterpolator());
                        fadeIn.setDuration(400);
                        mProgressBar.setAnimation(fadeIn);
                        mProgressBar.setVisibility(View.VISIBLE);

                        float elevation = mContext.getResources().getDimension(R.dimen.playbar_elevation);

                        playbarShadow.setElevation(elevation);

                        playBarLayout = false;
                    } else {
                        mProgressBar.setVisibility(View.VISIBLE);

                    }
                }

                refresh();

                updateRepeatButton();
                updateShuffleButton();
                updateTrackInfo();

            }

            if (receiveIntent.equals(PlayerService.PLAYSTATE_CHANGED)) {
                if (PlayerService.isPlaying()) {
                    mHandler.post(mUpdateProgressBar);
                } else {
                    mHandler.removeCallbacks(mUpdateProgressBar);
                }
            }

            if (receiveIntent.equals(PlayerService.PLAYSTATE_CHANGED)) {
                setButtonDrawable();
                if (PlayerService.isPlaying()) {
                    mHandler.post(mUpdateProgressBar);
                } else {
                    mHandler.removeCallbacks(mUpdateProgressBar);
                }
            }

            if (receiveIntent.equals(PlayerService.POSITION_CHANGED)) {
                autoScrollQueue = true;
                updateQueue();
            }

            if (receiveIntent.equals(PlayerService.META_CHANGED)) {
                updateTrackInfo();
            }

            if (receiveIntent.equals(INTENT_QUIT) && "exit".equals(intent.getStringExtra("halt")))
                exit();

            if (receiveIntent.equals(INTENT_SET_MENU)) {
                boolean value = intent.getBooleanExtra("menu", false);

                setMenu(value);
            }

            if (receiveIntent.equals(STORAGE_ACCESS_FRAMEWORK))
                triggerStorageAccessFramework();

        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            PlayerService.PlaybackBinder binder = (PlaybackBinder) service;
            mPlayerService = binder.getService();
            mServiceBound = true;

            updateAll();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }
    };

    public void onSongSelected(List<Song> songList, int position) {
        if (mPlayerService == null) {
            return;
        }
        mPlayerService.setPlayList(songList, position);
    }

    public void addToQueue(Song song) {
        if (mPlayerService != null) {
            mPlayerService.addToQueue(song);
        }
    }


    private void updateAll() {
        if (mPlayerService != null) {

            updateRepeatButton();
            updateShuffleButton();
            updateQueue();
            updateTrackInfo();
            setButtonDrawable();

            if (PlayerService.isPlaying()) {
                mHandler.post(mUpdateProgressBar);
            }
        }
    }

    private void updateQueue() {

        if (mPlayerService == null) {
            return;
        }

        List<Song> queue = PlayerService.getQueuePlayList();

        if ( queue.size() != 0) {

            if (!queue.equals(mQueue)) {
                mQueue = queue;
                mQueueAdapter.setQueue(mQueue);
            }

            mQueueAdapter.notifyDataSetChanged();

            setQueueSelection(PlayerService.getPositionWithinPlayList());
        }

    }

    private void setQueueSelection(final int position) {

        mQueueAdapter.setSelection(position);

        if (autoScrollQueue) {
            mHandler.postDelayed(() -> {
                mQueueView.smoothScrollToPosition(position);
                autoScrollQueue = false;
            }, 100);
        }

    }

    /***********************************************************************************************
     * Barre de lecture
     **********************************************************************************************/

    private void setButtonDrawable() {
        if (mPlayerService != null) {
            ImageButton quickButton = findViewById(R.id.play_pause_toggle);
            ImageButton quickButton0 = findViewById(R.id.play_pause_toggle0);

            if (PlayerService.isPlaying()) {
                assert quickButton != null;
                quickButton.setImageResource(R.drawable.ic_pause_circle_filled_amber_a700_48dp);
                quickButton0.setImageResource(R.drawable.ic_pause_circle_filled_amber_a700_48dp);

            } else {
                assert quickButton != null;
                quickButton.setImageResource(R.drawable.ic_play_circle_filled_amber_a700_48dp);
                quickButton0.setImageResource(R.drawable.ic_play_circle_filled_amber_a700_48dp);
            }
        }
    }



    /******************************************************
     * Lancement du fragment player
     ******************************************************/

    // Todo coller son propre frame dans un layout pour simplifer ?
    private void goPlayer() {

        File file = new File(getApplicationInfo().dataDir, "/databases/Queue.db");

        if (file.exists()) {

            Fragment fragment = PlayerFragment.newInstance();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_in_bottom);

            if ( viewID == R.id.fragment_album_list_layout ){
                ft.replace(viewID, fragment);

                Log.i(TAG, "R.id.fragment_album_list_layout");

            } else if ( viewID == R.id.fragment_song_layout ){
                ft.replace(viewID, fragment);

                Log.i(TAG, "R.id.fragment_song_layout");

            } else if ( viewID == R.id.fragment_playlist_list ) {
                ft.replace(viewID, fragment);

                Log.i(TAG, "R.id.fragment_playlist_list");
            } else if ( viewID == R.id.fragment_playlist ) {
                ft.replace(viewID, fragment);
                Log.i(TAG, "R.id.fragment_playlist_content");
            }

            ft.commit();

            if (menu == null)
                return;

            mHandler.postDelayed(() -> menu.setGroupVisible(R.id.main_menu_group, false), 300);

        } else {
            Toast.makeText(mContext, "Vous devez d'abord sélectionner un titre", Toast.LENGTH_LONG).show();
        }

    }

    /******************************************************
     * Mise à jour des informations de la barre de lecture
     ******************************************************/

    private void updateTrackInfo() {

        String title = PlayerService.getSongTitle();
        String artist = PlayerService.getArtistName();


        if (title != null) {
            //noinspection ConstantConditions
            ((TextView) findViewById(R.id.song_title)).setText(title);
        }

        if (artist != null) {
            //noinspection ConstantConditions
            ((TextView) findViewById(R.id.song_artist)).setText(artist + ", "  + PlayerService.getAlbumName());
        }

        int duration = PlayerService.getTrackDuration();

        if (duration != -1) {
            mProgressBar.setMax(duration);
            mProgressBar.setProgress(PlayerService.getPlayerPosition());
        }

    }



    /***********************************************************************************************
     * Sleep Timer
     **********************************************************************************************/

    private void showTimePicker() {

        final String start = getString(R.string.start);
        final String cancel = getString(R.string.cancel);

        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.dialog_date_picker, null);

        final SeekArc mSeekArc;
        final TextView mSeekArcProgress;

        mSeekArc = view.findViewById(R.id.seekArc);
        mSeekArcProgress = view.findViewById(R.id.seekArcProgress);
        mSeekArc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {

            @Override
            public void onStopTrackingTouch() {
                // vide, obligatoire
            }

            @Override
            public void onStartTrackingTouch() {
                // vide, obligatoire
            }

            @Override
            public void onProgressChanged(int progress) {

                String minute;

                if (progress <= 1){
                    minute = "minute";
                } else {
                    minute = "minutes";
                }

                String temps = String.valueOf(progress) + " " + minute;

                mSeekArcProgress.setText(temps);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(start, (dialog, which) -> {

            int mins = mSeekArc.getProgress();
            startTimer(mins);
        });

        builder.setNegativeButton(cancel, (dialog, which) -> {
            // This constructor is intentionally empty, pourquoi ? parce que !
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showTimerInfo() {

        final String continuer = getString(R.string.continuer);
        final String cancelTimer = getString(R.string.cancel_timer);

        if (mTask.getDelay(TimeUnit.MILLISECONDS) < 0) {
            cancelTimer();
            return;
        }

        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.dialog_timer_info, null);
        final TextView timeLeft = view.findViewById(R.id.time_left);

        final String stopTimer = getString(R.string.stop_timer);

        final AlertDialog dialog = new AlertDialog.Builder(this).setPositiveButton(continuer, (dialog12, which) -> dialog12.dismiss()).setNegativeButton(cancelTimer, (dialog1, which) -> {
            cancelTimer();

            Toast.makeText(mContext, stopTimer, Toast.LENGTH_LONG).show();
        }).setView(view).create();

        new CountDownTimer(mTask.getDelay(TimeUnit.MILLISECONDS), 1000) {

            @Override
            public void onTick(long seconds) {

                long secondes = seconds;

                secondes = secondes / 1000;
                timeLeft.setText(String.format(getString(R.string.timer_info), ((secondes % 3600) / 60), ((secondes % 3600) % 60)));
            }

            @Override
            public void onFinish() {
                dialog.dismiss();
            }
        }.start();

        dialog.show();
    }

    private void startTimer(final int minutes) {

        final String impossible = getString(R.string.impossible);

        final String arret = getString(R.string.arret);
        final String minuteTxt;

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final int delay = (minutes * 60) * 1000;

        if (delay == 0) {
            Toast.makeText(this, impossible, Toast.LENGTH_LONG).show();
            return;
        }

        if (minutes == 1) {
            minuteTxt = getString(R.string.minute_singulier);
        } else {
            minuteTxt = getString(R.string.minute_pluriel);
        }

        mTask = scheduler.schedule(new StopTimer(), delay, TimeUnit.MILLISECONDS);

        Toast.makeText(this, arret + " " + minutes + " " + minuteTxt, Toast.LENGTH_LONG).show();

        running = true;
        timeAfficheur.setVisibility(View.VISIBLE);

        Notification.setState(true);
        Notification.updateNotification(mContext, mPlayerService);

        showTimeEcran();

        volume.baisser(mContext, mTask, delay);
    }

    class StopTimer implements Runnable {

        public void run() {

            if (running)
                mTask.cancel(true);

            Notification.setState(false);

            running = false;

        }
    }


    private  void cancelTimer() {

        if (running) {

            mTask.cancel(true);

            minuteurVolume.cancel();
            minuteurVolume = null;

            volume.getMinuteur().cancel();
            volume.setVolume(1.0f);
        }

        running = false;
        timeAfficheur.setVisibility(View.GONE);

        Notification.setState(false);

        Notification.updateNotification(mContext, mPlayerService);
    }

       /* ********************************
    * Afficher temps restant à l'écran
    * ********************************/

    private void showTimeEcran() {

        assert timeAfficheur != null;
        timeAfficheur.setVisibility(View.VISIBLE);

        minuteurVolume = new CountDownTimer(mTask.getDelay(TimeUnit.MILLISECONDS), 1000) {
            @Override
            public void onTick(long seconds) {

                long secondes = seconds;

                secondes = secondes / 1000;

                String textTemps = "zZz " + String.format(getString(R.string.timer_info), ((secondes % 3600) / 60), ((secondes % 3600) % 60));

                timeAfficheur.setText(textTemps);
            }

            @Override
            public void onFinish() {
                timeAfficheur.setVisibility(View.GONE);
            }

        }.start();
    }


   /* ********************************
    * Réduction progressive du volume
    * ********************************/

    private void exit() {

        if (running)
            cancelTimer();

        if (PlayerService.isPlaying())
            mPlayerService.toggle();

        PlayerService.setVolume(1.0f);

        killNotif();
        finish();
    }

    /***********************************************************************************************
     * Touche retour
     **********************************************************************************************/

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {

        DrawerLayout drawer = findViewById(drawer_layout);
        assert drawer != null;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }



    /***********************************************************************************************
     * Fermeture notification
     **********************************************************************************************/

    private void killNotif() {
        mHandler.postDelayed(() -> {
            NotificationManager notificationManager;
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert notificationManager != null;
            notificationManager.cancel(Notification.NOTIFY_ID);

        }, 500);
    }


    /* *********************************************************************************************
    * Gestion des permissions (Android >= 6.0)
    * *********************************************************************************************/

    private void checkPermissions() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {

                DialogUtils.showPermissionDialog(this, getString(R.string.permission_read_phone_state),
                        (dialog, which) -> ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSIONS_REQUEST_READ_PHONE_STATE));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        switch (requestCode) {

            case PERMISSIONS_REQUEST_READ_PHONE_STATE:

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(PlayerService.PREF_AUTO_PAUSE, true);
                    if (mPlayerService != null) {
                        mPlayerService.setAutoPauseEnabled();
                    }
                    editor.apply();
                }

                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                }
                break;

            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                refresh();
                break;

            default:
                break;
        }
    }


    private void triggerStorageAccessFramework() {

        DialogUtils.showPermissionDialog(this, getString(R.string.permission_write_sd_externe),
                (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    startActivityForResult(intent, REQUEST_CODE_STORAGE_ACCESS);
                });
    }



    @Override
    public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData) {
        if (requestCode == REQUEST_CODE_STORAGE_ACCESS) {
            Uri treeUri;
            if (resultCode == Activity.RESULT_OK) {

                treeUri = resultData.getData();

                PreferenceUtil.setSharedPreferenceUri(treeUri);

                // Persist access permissions.
                final int takeFlags = resultData.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                assert treeUri != null;
                this.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);

                ConvertService.setPermExt();

                SaveTagProgressDialog.setPermExt();

                refresh();
            } else {
                ConvertService.cancel();
                ConvertService.setPermExt();

                SaveTagProgressDialog.cancel();
                SaveTagProgressDialog.setPermExt();
            }
        }
    }

    private static class DialogUtils {

        private static void showPermissionDialog(Context context, String message, DialogInterface.OnClickListener listener) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.permission)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, listener)
                    .show();
        }
    }

    public static PlayerService getPlayerService() {
        return mPlayerService;
    }

    public static boolean getQueueLayout() {
        return queueLayout;
    }

    private void setMenu(Boolean value) {
        menu.setGroupVisible(R.id.main_menu_group, value);
    }

    public static boolean getPlaylistFragmentState() {
        return playlistFragmentState;
    }
    public static void setPlaylistFragmentState(Boolean value) {
        playlistFragmentState = value;
    }

    public static String getPlaylistName() {
        return playlistName;
    }
    public static void setPlaylistName(String value) {
        playlistName = value;
    }

    public static boolean getAlbumFragmentState() {
        return albumFragmentState;
    }
    public static void setAlbumFragmentState(Boolean value) {
        albumFragmentState = value;
    }

    public static int getViewID() {
        return viewID;
    }
    public static void setViewID(int id) {
        viewID = id;
    }

    public static boolean getChercheActivity() {
        return chercheActivity;
    }
    public static void setChercheActivity(Boolean value) {
        chercheActivity = value;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        NotificationManager notificationManager;
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.cancel(ConvertService.NOTIFID);
    }
}

