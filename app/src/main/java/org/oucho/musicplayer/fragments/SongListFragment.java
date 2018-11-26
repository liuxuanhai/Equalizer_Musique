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

package org.oucho.musicplayer.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.dialog.ConvertDialog;
import org.oucho.musicplayer.dialog.PlaylistPickerDialog;
import org.oucho.musicplayer.dialog.SaveTagProgressDialog;
import org.oucho.musicplayer.dialog.TagSongEditorDialog;
import org.oucho.musicplayer.fragments.adapters.BaseAdapter;
import org.oucho.musicplayer.fragments.adapters.SongListAdapter;
import org.oucho.musicplayer.fragments.loaders.SongLoader;
import org.oucho.musicplayer.fragments.loaders.SortOrder;
import org.oucho.musicplayer.utils.PlaylistsUtils;
import org.oucho.musicplayer.utils.PrefSort;
import org.oucho.musicplayer.view.fastscroll.FastScrollRecyclerView;

import java.util.List;


public class SongListFragment extends BaseFragment implements MusiqueKeys {

    @SuppressWarnings("unused")
    private static final String TAG_LOG = "SongListFragment";

    private Context mContext;
    private MainActivity mActivity;
    private SongListAdapter mAdapter;
    private SharedPreferences préférences = null;
    private songReceiver songReceiver;
    private boolean isRegistered = false;
    private String titre;
    private String tri;

    private final LoaderManager.LoaderCallbacks<List<Song>> mLoaderCallbacks = new LoaderCallbacks<List<Song>>() {

        @Override
        public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
            SongLoader loader = new SongLoader(getActivity());

            loader.setSortOrder(PrefSort.getInstance().getSongSortOrder());
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Song>> loader, List<Song> songList) {
            populateAdapter(songList);
        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {
            //  Auto-generated method stub

        }
    };


    private void populateAdapter(List<Song> songList) {
        mAdapter.setData(songList);
    }

    private final BaseAdapter.OnItemClickListener mOnItemClickListener = (position, view) -> {
        switch (view.getId()) {
            case R.id.item_view:
                selectSong(position);
                break;
            case R.id.menu_button:
                showMenu(position, view);
                break;
            default: //do nothing
                break;
        }
    };

    public static SongListFragment newInstance() {

        return new SongListFragment();
    }



    /* *********************************************************************************************
     * Menu titre
     * ********************************************************************************************/

    private void showMenu(final int position, View v) {
        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        final Song song = mAdapter.getItem(position);
        inflater.inflate(R.menu.song_item, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_add_to_queue:
                    ((MainActivity) getActivity()).addToQueue(song);
                    return true;

                case R.id.action_edit_tags:
                    showID3TagEditor(song);
                    return true;
                case R.id.action_add_to_playlist:
                    showPlaylistPicker(song);
                    return true;
                case R.id.action_convert:
                    convert(song);
                    break;
                default: //do nothing
                    break;
            }
            return false;
        });

        popup.show();
    }

    private void showID3TagEditor(Song song) {
        TagSongEditorDialog dialog = TagSongEditorDialog.newInstance(song, "SongListFragment");
        dialog.show(getChildFragmentManager(), "edit_tags");
    }

    private void convert(Song song) {
        ConvertDialog dialog = ConvertDialog.newInstance(song);
        dialog.show(getChildFragmentManager(), "convert");
    }

    private void showPlaylistPicker(final Song song) {
        PlaylistPickerDialog picker = PlaylistPickerDialog.newInstance();
        picker.setListener(playlist -> PlaylistsUtils.addSongToPlaylist(getActivity().getContentResolver(), playlist.getId(), song.getId()));
        picker.show(getChildFragmentManager(), "pick_playlist");
    }

    private void selectSong(int position) {

        if (mActivity != null) {
            mActivity.onSongSelected(mAdapter.getSongList(), position);
        }
    }

    @Override
    public void load() {
        getLoaderManager().restartLoader(0, null, getLoaderCallbacks());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mActivity = (MainActivity) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }



    /* *********************************************************************************************
     * Création de l'activité
     * ********************************************************************************************/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mContext = getContext();

        préférences = this.getActivity().getSharedPreferences(FICHIER_PREFS, Context.MODE_PRIVATE);

        titre = mContext.getString(R.string.titles);

        setTri();

        songReceiver = new songReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(LIST_SONG_TAG);
        filter.addAction(REFRESH_TAG);


        mContext.registerReceiver(songReceiver, filter);
        isRegistered = true;

    }


    /* *********************************************************************************************
     * Création de la vue
     * ********************************************************************************************/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_song, container, false);

        FastScrollRecyclerView mRecyclerView = rootView.findViewById(R.id.recycler_view);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new SongListAdapter(getActivity());
        mAdapter.setOnItemClickListener(mOnItemClickListener);

        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, getLoaderCallbacks());

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;

        if (isRegistered) {
            mContext.unregisterReceiver(songReceiver);

            isRegistered = false;
        }
    }


    @Override
    public void onPause() {
        super.onPause();

        if (isRegistered) {
            mContext.unregisterReceiver(songReceiver);

            isRegistered = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(INTENT_STATE);
            filter.addAction(LIST_SONG_TAG);
            filter.addAction(REFRESH_TAG);
            filter.addAction(SET_TITLE);

            mContext.registerReceiver(songReceiver, filter);
            isRegistered = true;
        }

        // Active la touche back
        if(getView() == null){
            return;
        }

        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener((v, keyCode, event) -> {

            if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK){

                if (MainActivity.getQueueLayout()) {

                    Intent intent = new Intent();
                    intent.setAction(INTENT_QUEUEVIEW);
                    mContext.sendBroadcast(intent);

                } else {
                    Intent backToPrevious = new Intent();
                    backToPrevious.setAction(INTENT_BACK);
                    mContext.sendBroadcast(backToPrevious);
                }

                return true;
            }
            return false;
        });
    }




    /* *********************************************************************************************
     * Menu
     * ********************************************************************************************/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.song_sort_by, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        PrefSort prefSort = PrefSort.getInstance();
        switch (item.getItemId()) {
            case R.id.menu_sort_by_az:
                prefSort.setSongSortOrder(SortOrder.SongSortOrder.SONG_A_Z);
                load();
                tri = "a-z";
                setUserVisibleHint(true);
                break;

            case R.id.menu_sort_by_album:
                prefSort.setSongSortOrder(SortOrder.SongSortOrder.SONG_ALBUM);
                load();
                tri = mContext.getString(R.string.title_sort_album);
                setUserVisibleHint(true);
                break;

            case R.id.menu_sort_by_artist:
                prefSort.setSongSortOrder(SortOrder.SongSortOrder.SONG_ARTIST);
                load();
                tri = mContext.getString(R.string.title_sort_artist);
                setUserVisibleHint(true);
                break;

            case R.id.menu_sort_by_year:
                prefSort.setSongSortOrder(SortOrder.SongSortOrder.SONG_YEAR);
                load();
                tri = mContext.getString(R.string.title_sort_year);
                setUserVisibleHint(true);
                break;

            case R.id.menu_sort_by_ajout:
                prefSort.setSongSortOrder(SortOrder.SongSortOrder.SONG_ADD);
                load();
                tri = mContext.getString(R.string.title_sort_add);
                setUserVisibleHint(true);
                break;

            default: //do nothing
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private LoaderCallbacks<List<Song>> getLoaderCallbacks() {
        return mLoaderCallbacks;
    }




    /* *********************************************************************************************
     * Titre
     * ********************************************************************************************/

    private void setTri() {

        String getTri = préférences.getString("song_sort_order", "");

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
    }

    @Override
    public void setUserVisibleHint(boolean visible){
        super.setUserVisibleHint(visible);

        if (visible || isResumed()) {

            int couleurTitre = ContextCompat.getColor(mContext, R.color.grey_400);

            MainActivity.setViewID(R.id.fragment_song_layout);

            if (android.os.Build.VERSION.SDK_INT >= 24) {
                getActivity().setTitle(Html.fromHtml("<font>" + titre + " " + " " + " </font> <small> <font color='" + couleurTitre + "'>"
                                + tri + "</small></font>", Html.FROM_HTML_MODE_LEGACY));
            } else {
                //noinspection deprecation
                getActivity().setTitle(Html.fromHtml("<font>" + titre + " " + " " + " </font> <small> <font color='" + couleurTitre + "'>" + tri + "</small></font>"));
            }
        }
    }




    private class songReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();


            if (LIST_SONG_TAG.equals(receiveIntent)) {

                new SongListFragment.WriteTag(intent.getParcelableExtra("song"),
                        intent.getStringExtra("title"),
                        intent.getStringExtra("albumName"),
                        intent.getStringExtra("artistName"),
                        intent.getStringExtra("genre"),
                        intent.getStringExtra("year"),

                        intent.getStringExtra("track"),
                        intent.getStringExtra("disc"),
                        intent.getStringExtra("comment"),

                        intent.getStringExtra("cover")

                ).execute();

            }

            if (REFRESH_TAG.equals(receiveIntent)) {
                load();
            }
        }
    }


    @SuppressLint("StaticFieldLeak")
    class WriteTag extends AsyncTask<String, Integer, Boolean> {


        SaveTagProgressDialog newFragment;

        final Song song;

        final String title;
        final String albumName;
        final String artistName;
        final String genre;
        final String year;
        final String track;
        final String disc;
        final String comment;
        final String cover;


        WriteTag(Song song, String title, String albumName, String artistName, String genre, String year, String track, String disc, String comment, String cover) {
            this.song = song;

            this.title = title;
            this.artistName = artistName;
            this.albumName = albumName;
            this.genre = genre;
            this.year = year;

            this.track = track;
            this.disc = disc;

            this.comment = comment;

            this.cover = cover;
        }

        @Override
        protected Boolean doInBackground(String... arg0) {
            newFragment = new SaveTagProgressDialog();
            Bundle bundle = new Bundle();
            bundle.putString("type", "song");

            bundle.putParcelable("song", song);

            bundle.putString("title", title);
            bundle.putString("artistName", artistName);
            bundle.putString("albumName", albumName);
            bundle.putString("genre", genre);
            bundle.putString("year", year);
            bundle.putString("track", track);
            bundle.putString("disc", disc);
            bundle.putString("comment", comment);

            bundle.putString("cover", cover);

            newFragment.setArguments(bundle);
            newFragment.show(getFragmentManager(), "SaveTagSong");

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {

        }
    }

}