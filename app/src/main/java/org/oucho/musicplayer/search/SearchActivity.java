package org.oucho.musicplayer.search;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.services.PlayerService;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Artist;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.fragments.loaders.AlbumLoader;
import org.oucho.musicplayer.fragments.loaders.ArtistLoader;
import org.oucho.musicplayer.fragments.loaders.BaseLoader;
import org.oucho.musicplayer.fragments.loaders.SongLoader;
import org.oucho.musicplayer.search.fragment.SearchAlbumFragment;
import org.oucho.musicplayer.search.fragment.SearchArtistFragment;
import org.oucho.musicplayer.search.fragment.SearchSongFragment;
import org.oucho.musicplayer.tools.LockableViewPager;

import java.util.List;

import static org.oucho.musicplayer.MusiqueKeys.FILTER;

public class SearchActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {


    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ActionBar actionBar;

    private String mTabArtists = "";
    private static String mTabAlbums = "";
    private String mTabSongs = "";
    private static String textNew = "";

    private Context mContext;

    private boolean receiver = false;
    private boolean mServiceBound = false;

    private FragmentManager mFragmentManager;

    private PlayerService mPlayerService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mContext = getApplicationContext();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final int mUIFlag = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getWindow().getDecorView().setSystemUiVisibility(mUIFlag);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        ViewPager viewPager = findViewById(R.id.container);
        viewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);


        actionBar = getSupportActionBar();

        assert actionBar != null;
        SearchView searchView = new SearchView(actionBar.getThemedContext());

        searchView.setIconifiedByDefault(false);
        actionBar.setCustomView(searchView);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        searchView.setOnQueryTextListener(searchQueryListener);

        mFragmentManager = getSupportFragmentManager();
        mFragmentManager.addOnBackStackChangedListener(this);
    }


    private static void setTextNew(String value) {
        textNew = value;
    }

    public static String getNewText() {
        return textNew;
    }

    private void setAlbumTab(int value) {

        mTabAlbums = " (" + value + ')';
        if (mTabAlbums.equals(" (0)")) {
            mTabAlbums = "";
        }

        mSectionsPagerAdapter.notifyDataSetChanged();

    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            PlayerService.PlaybackBinder binder = (PlayerService.PlaybackBinder) service;
            mPlayerService = binder.getService();
            mServiceBound = true;
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


    private void showTab(final boolean value) {

        Handler mHandler = new Handler();

        if (!value) {
            mHandler.postDelayed(() -> actionBar.setDisplayShowCustomEnabled(false), 200);
        } else {
            mHandler.postDelayed(() -> actionBar.setDisplayShowCustomEnabled(true), 200);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();


        if (mServiceBound) {
            mPlayerService = null;

            unbindService(mServiceConnection);
            mServiceBound = false;
        }

        if (receiver) {
            mContext.unregisterReceiver(mSearchListener);
            receiver = false;
        }

        setTextNew("");

        MainActivity.setChercheActivity(false);

    }

    @Override
    protected void onResume() {
        super.onResume();

        MainActivity.setChercheActivity(true);

        if (!receiver) {
            IntentFilter filter = new IntentFilter();

            filter.addAction("search.setTitle");
            filter.addAction("search.setTabLayout");
            filter.addAction("search.setAlbumTab");


            mContext.registerReceiver(mSearchListener, filter);
            receiver = true;
        }

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
        }
    }




    private final BroadcastReceiver mSearchListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();

            assert receiveIntent != null;
            if (receiveIntent.equals("search.setTitle")) {

                String titre = intent.getStringExtra("title");
                setTitre(titre);
            }


            if (receiveIntent.equals("search.setTabLayout")) {

                boolean value = intent.getBooleanExtra("TabLayout", false);

                showTab(value);

                if (!value) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                    //Find the currently focused view, so we can grab the correct window token from it.
                    View view = getCurrentFocus();

                    //If no view currently has focus, create a new one, just so we can grab a window token from it
                    if (view == null) {
                        view = new View(mContext);
                    }
                    assert imm != null;
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }


            if (receiveIntent.equals("search.setAlbumTab")) {

                int tab = intent.getIntExtra("setAlbumTab", 0);
                setAlbumTab(tab);
            }
        }
    };


    private final SearchView.OnQueryTextListener searchQueryListener = new SearchView.OnQueryTextListener() {

        @Override
        public boolean onQueryTextSubmit(String query) {
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            String mSearchKeywords = newText.toLowerCase();

            setTextNew(mSearchKeywords);

            Intent i = new Intent();
            i.setAction("search.newkey");
            i.putExtra("text", mSearchKeywords);
            sendBroadcast(i);

            Bundle args = new Bundle();
            args.putString(FILTER, mSearchKeywords);

            getSupportLoaderManager().restartLoader(0, args, mAlbumLoaderCallbacks);
            getSupportLoaderManager().restartLoader(1, args, mArtistLoaderCallbacks);
            getSupportLoaderManager().restartLoader(2, args, mSongLoaderCallbacks);

            return true;
        }
    };


    @Override
    public void onBackPressed() {

        if (mFragmentManager.getBackStackEntryCount() > 0) {

            if (mFragmentManager.getBackStackEntryCount() == 1)
                showTab(true);

            super.onBackPressed();

        } else {

            MainActivity.setChercheActivity(false);
            LockableViewPager.setSwipeLocked(false);
            finish();
        }
    }

    @Override
    public void onBackStackChanged() {
    }


    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        @SuppressLint("UseSparseArrays")
        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    return SearchArtistFragment.newInstance();
                case 1:
                    return SearchAlbumFragment.newInstance();
                case 2:
                    return SearchSongFragment.newInstance();
                default:
                    break;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {

            String album = getResources().getString(R.string.albums) + mTabAlbums;
            String artist = getResources().getString(R.string.artists) + mTabArtists;
            String song = getResources().getString(R.string.title) + mTabSongs;

            switch (position) {
                case 0:
                    return artist;
                case 1:
                    return album;
                case 2:
                    return song;
            }
            return null;
        }
    }


    public static void setLoaderFilter(Bundle args, BaseLoader loader) {
        String filter;
        if (args != null) {
            filter = args.getString(FILTER);
        } else {
            filter = "";
        }
        loader.setFilter(filter);
    }


    /******************************************************************************
     * Loaders
     ******************************************************************************
     * Album
     **********/
    private final LoaderManager.LoaderCallbacks<List<Album>> mAlbumLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<Album>>() {

        @Override
        public Loader<List<Album>> onCreateLoader(int id, Bundle args) {
            AlbumLoader loader = new AlbumLoader(SearchActivity.this);
            setLoaderFilter(args, loader);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Album>> loader, List<Album> data) {
            mTabAlbums = " (" + data.size() + ')';
            if (mTabAlbums.equals(" (0)")) {
                mTabAlbums = "";
            }
            mSectionsPagerAdapter.notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<List<Album>> loader) {
            // This constructor is intentionally empty, pourquoi ? parce que !
        }
    };


    /**********
     * Artist
     **********/
    private final LoaderManager.LoaderCallbacks<List<Artist>> mArtistLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<Artist>>() {

        @Override
        public Loader<List<Artist>> onCreateLoader(int id, Bundle args) {
            ArtistLoader loader = new ArtistLoader(SearchActivity.this);
            setLoaderFilter(args, loader);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Artist>> loader, List<Artist> data) {
            mTabArtists = " (" + data.size() + ')';
            if (mTabArtists.equals(" (0)")) {
                mTabArtists = "";
            }
            mSectionsPagerAdapter.notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<List<Artist>> loader) {}
    };


    /**********
     * Song
     **********/
    private final LoaderManager.LoaderCallbacks<List<Song>> mSongLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<Song>>() {

        @Override
        public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
            SongLoader loader = new SongLoader(SearchActivity.this);
            setLoaderFilter(args, loader);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Song>> loader, List<Song> data) {

            mTabSongs = " (" + data.size() + ')';
            if (mTabSongs.equals(" (0)")) {
                mTabSongs = "";
            }
            mSectionsPagerAdapter.notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {}
    };


    /******************************************************************************
     * Titre
     ******************************************************************************/
    private void setTitre(String titre){


        final int couleurTitre = ContextCompat.getColor(mContext, R.color.colorAccent);

        if (android.os.Build.VERSION.SDK_INT >= 24) {
            setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>" + titre + "</font>", Html.FROM_HTML_MODE_LEGACY));
        } else {
            //noinspection deprecation
            setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>" + titre + "</font>"));
        }
    }

}
