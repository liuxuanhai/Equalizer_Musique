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
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.MusiqueApplication;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.dialog.ConvertDialog;
import org.oucho.musicplayer.dialog.PlaylistPickerDialog;
import org.oucho.musicplayer.dialog.SaveTagProgressDialog;
import org.oucho.musicplayer.dialog.TagSongEditorDialog;
import org.oucho.musicplayer.fragments.adapters.AlbumSongListAdapter;
import org.oucho.musicplayer.fragments.adapters.BaseAdapter;
import org.oucho.musicplayer.fragments.loaders.SongLoader;
import org.oucho.musicplayer.search.SearchActivity;
import org.oucho.musicplayer.services.PlayerService;
import org.oucho.musicplayer.tools.LockableViewPager;
import org.oucho.musicplayer.utils.PlaylistsUtils;
import org.oucho.musicplayer.view.CustomLayoutManager;
import org.oucho.musicplayer.view.fastscroll.FastScrollRecyclerView;

import java.io.File;
import java.util.List;
import java.util.Locale;


public class AlbumFragment extends BaseFragment implements MusiqueKeys {

    @SuppressWarnings("unused")
    private static final String TAG_LOG = "Album Fragment";

    private Album mAlbum;
    private AlbumSongListAdapter mAdapter;
    private MainActivity mActivity;

    private FastScrollRecyclerView mRecyclerView;
    private Receiver Receiver;
    private boolean isRegistered = false;

    private final Handler mHandler = new Handler();

    private String albumName = "";
    private String Artiste = "";
    private String Année = "";
    private String nb_Morceaux = "";

    private String tri = "a-z";

    private TextView durée;
    private TextView year;
    private List<Song> listeTitre;

    private Context mContext;

    private SearchActivity mSearchActivity = null;


    public static AlbumFragment newInstance(Album album) {
        AlbumFragment fragment = new AlbumFragment();

        Bundle args = new Bundle();
        args.putParcelable("album", album);
        fragment.setArguments(args);

        return fragment;
    }


    private final LoaderManager.LoaderCallbacks<List<Song>> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<Song>>() {

        @Override
        public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
            SongLoader loader = new SongLoader(getActivity());

            loader.setSelection(MediaStore.Audio.Media.ALBUM_ID + " = ?", new String[]{String.valueOf(mAlbum.getId())});
            loader.setSortOrder(MediaStore.Audio.Media.TRACK);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Song>> loader, List<Song> songList) {
            mAdapter.setData(songList);

            listeTitre = songList;

            int duréeTotal = 0;

            for (int i = 0; i < listeTitre.size(); i++) {

                if (listeTitre.get(i).getId() == PlayerService.getSongID())
                    mRecyclerView.smoothScrollToPosition( i );

                duréeTotal =  duréeTotal + listeTitre.get(i).getDuration();
            }

            if (msToTextMinut(duréeTotal).equals("0") || msToTextMinut(duréeTotal).equals("1")) {

                String temps = msToTextMinut(duréeTotal) + " " + getString(R.string.minute_singulier);
                durée.setText(temps);

            } else {

                String temps = msToTextMinut(duréeTotal) + " " + getString(R.string.minute_pluriel);
                durée.setText(temps);
            }


            // bug android 6 ?, year = 0
            try {

                AudioFile audioFile = AudioFileIO.read(new File(songList.get(0).getPath()));

                if (audioFile.getExt().equals("flac")) {
                    Tag tag = audioFile.getTag();

                    if (mAlbum.getYear() == 0 && tag.getFirst(FieldKey.YEAR) != null) {

                        String an = tag.getFirst(FieldKey.YEAR);

                        year.setText(an);

                        ContentValues values = new ContentValues();

                        values.put(MediaStore.Audio.Media.YEAR, an);

                        getContext().getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values, MediaStore.Audio.Media.ALBUM_ID + "=" + mAlbum.getId(), null);

                        for (int i = 0; i < songList.size(); i++) {

                            getContext().getContentResolver().update(
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    values, MediaStore.Audio.Media._ID + "=" + songList.get(i).getId(),
                                    null
                            );
                        }
                    }
                }

            } catch (Exception ignore) {}
        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {
            //  Auto-generated method stub
        }
    };

    private String msToTextMinut(int msec) {
        return String.format(Locale.getDefault(), "%d", msec / 60000);
    }


    /* *********************************************************************************************
     * Création du fragment
     * ********************************************************************************************/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();

        mContext = getContext();

        Receiver = new Receiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_STATE);
        filter.addAction(SONG_TAG);
        filter.addAction(REFRESH_TAG);
        filter.addAction(SET_TITLE);

        mContext.registerReceiver(Receiver, filter);
        isRegistered = true;

        if (bundle != null) {

            mAlbum = bundle.getParcelable("album");

            assert mAlbum != null;
            albumName = mAlbum.getAlbumName();
            Artiste = mAlbum.getArtistName();
            Année = String.valueOf(mAlbum.getYear());

            String singulier = getString(R.string.title);
            String pluriel = getString(R.string.titles);

            if (mAlbum.getTrackCount() < 2) {
                nb_Morceaux = String.valueOf(mAlbum.getTrackCount()) + " " + singulier;
            } else {
                nb_Morceaux = String.valueOf(mAlbum.getTrackCount()) + " " + pluriel;
            }
        }

        setTri();

        if (MainActivity.getChercheActivity()) {

            mSearchActivity = (SearchActivity) mContext;

            if (android.os.Build.VERSION.SDK_INT >= 24) {
                getActivity().setTitle(Html.fromHtml("<font color=\"#FFA000\">" + albumName + " </font> <small> <font color=\"#CCCCCC\">", Html.FROM_HTML_MODE_LEGACY));
            } else {
                //noinspection deprecation
                getActivity().setTitle(Html.fromHtml("<font color=\"#FFA000\">" + albumName + " </font>"));
            }

        } else {

            mHandler.postDelayed(() -> {

                getActivity().setTitle(albumName);

                Intent intent = new Intent();
                intent.setAction(INTENT_TOOLBAR_SHADOW);
                intent.putExtra("boolean", false);
                mContext.sendBroadcast(intent);

            }, 300);
        }

    }


    private void setTri() {

        SharedPreferences préférences = this.getActivity().getSharedPreferences(FICHIER_PREFS, Context.MODE_PRIVATE);

        String getTri = préférences.getString("album_sort_order", "");

        if ("minyear DESC".equals(getTri)) {
            tri = mContext.getString(R.string.title_sort_year);
        } else if ("REPLACE ('<BEGIN>' || artist, '<BEGIN>The ', '<BEGIN>')".equals(getTri)) {
            tri = mContext.getString(R.string.title_sort_artist);
        } else {
            tri = "a-z";
        }
    }


    /* *********************************************************************************************
     * Création du visuel
     * ********************************************************************************************/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView;

        if (MainActivity.getChercheActivity()) {
            rootView = inflater.inflate(R.layout.fragment_album_search, container, false);
        } else {
            rootView = inflater.inflate(R.layout.fragment_album, container, false);
        }

        LinearLayout presentation = rootView.findViewById(R.id.presentation);
        presentation.setOnClickListener(null);

        ImageView artworkView = rootView.findViewById(R.id.album_artwork);

        int artSize = getResources().getDimensionPixelSize(R.dimen.fragment_album_art_size);

        Uri uri = ContentUris.withAppendedId(ARTWORK_URI, mAlbum.getId());
        Picasso.get()
                .load(uri)
                .resize(artSize, artSize)
                .centerCrop()
                .into(artworkView);

        TextView artist = rootView.findViewById(R.id.artist_name);
        artist.setText(Artiste);

        year = rootView.findViewById(R.id.year);
        year.setText(Année);

        TextView trackNb = rootView.findViewById(R.id.nb_track);
        trackNb.setText(nb_Morceaux);

        durée = rootView.findViewById(R.id.duration);

        mRecyclerView = rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new CustomLayoutManager(getActivity()));
        mAdapter = new AlbumSongListAdapter();
        mAdapter.setOnItemClickListener(mOnItemClickListener);

        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }


    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {

            mHandler.postDelayed(() -> mAdapter.notifyDataSetChanged(), 100);


            switch (view.getId()) {
                case R.id.item_view:
                    selectSong(position);
                    break;
                case R.id.buttonMenu:
                    showMenu(position, view);
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, mLoaderCallbacks);
    }

    @Override
    public void load() {
        getLoaderManager().restartLoader(0, null, mLoaderCallbacks);
    }

    private void selectSong(int position) {

        if (mSearchActivity != null) {
            mSearchActivity.onSongSelected(mAdapter.getSongList(), position);

        } else if (mActivity != null) {
            mActivity.onSongSelected(mAdapter.getSongList(), position);
        }
    }


    private void showMenu(final int position, View v) {

        PopupMenu popup = new PopupMenu(getActivity(), v);

        MenuInflater inflater = popup.getMenuInflater();

        final Song song = mAdapter.getItem(position);

        inflater.inflate(R.menu.song_item, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_add_to_queue:

                    if (MainActivity.getChercheActivity()) {
                        ((SearchActivity) getActivity()).addToQueue(song);
                    } else {
                        ((MainActivity) getActivity()).addToQueue(song);
                    }
                    break;

                case R.id.action_edit_tags:
                    showID3TagEditor(song);
                    break;
                case R.id.action_add_to_playlist:
                    showPlaylistPicker(song);
                    break;
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
        TagSongEditorDialog dialog = TagSongEditorDialog.newInstance(song, "AlbumFragment");
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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (! MainActivity.getChercheActivity()) {
            try {
                mActivity = (MainActivity) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString() + " must implement OnFragmentInteractionListener");
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (isRegistered) {
            mContext.unregisterReceiver(Receiver);

            isRegistered = false;
        }

        MainActivity.setAlbumFragmentState(false);
    }


    @Override
    public void onResume() {
        super.onResume();

        MainActivity.setAlbumFragmentState(true);

        LockableViewPager.setSwipeLocked(true);

        if (!isRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(INTENT_STATE);
            filter.addAction(SONG_TAG);
            filter.addAction(REFRESH_TAG);
            filter.addAction(SET_TITLE);

            mContext.registerReceiver(Receiver, filter);
            isRegistered = true;
        }


        if (!MainActivity.getChercheActivity()) {

            if (getView() == null) {
                return;
            }

            getView().setFocusableInTouchMode(true);
            getView().requestFocus();
            getView().setOnKeyListener((v, keyCode, event) -> {

                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {

                    LockableViewPager.setSwipeLocked(false);

                    if (MainActivity.getQueueLayout()) {

                        Intent intent = new Intent();
                        intent.setAction(INTENT_QUEUEVIEW);
                        mContext.sendBroadcast(intent);

                        return true;


                    } else if (getFragmentManager().findFragmentById(R.id.fragment_album_list_layout) != null) {

                        MainActivity.setAlbumFragmentState(false);

                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.setCustomAnimations(R.anim.slide_out_bottom, R.anim.slide_out_bottom);
                        ft.remove(getFragmentManager().findFragmentById(R.id.fragment_album_list_layout));
                        ft.commit();

                        Intent intent = new Intent();
                        intent.setAction("reload");
                        mContext.sendBroadcast(intent);

                        Intent shadow = new Intent();
                        shadow.setAction(INTENT_TOOLBAR_SHADOW);
                        shadow.putExtra("boolean", true);
                        mContext.sendBroadcast(shadow);

                        Intent titreListAlbum = new Intent();
                        titreListAlbum.setAction(SET_TITLE);
                        mContext.sendBroadcast(titreListAlbum);

                        return true;

                    }

                    MainActivity.setAlbumFragmentState(false);
                    Intent titreListAlbum = new Intent();
                    titreListAlbum.setAction(SET_TITLE);
                    mContext.sendBroadcast(titreListAlbum);

                    return false;
                }

                return false;
            });

        }

    }


    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();

            if (INTENT_STATE.equals(receiveIntent)) {

                if (intent.getStringExtra("state").equals("prev") || intent.getStringExtra("state").equals("next") || intent.getStringExtra("state").equals("play")) {

                    for (int i = 0; i < listeTitre.size(); i++) {
                        if (listeTitre.get(i).getId() == PlayerService.getSongID())
                            mRecyclerView.smoothScrollToPosition(i);
                    }

                    //   rustine lag next.prev    //
                    if (PlayerService.isPlaying()) {
                        mAdapter.notifyDataSetChanged();

                    } else {

                        mHandler.postDelayed(() -> mAdapter.notifyDataSetChanged(), 100);

                    }
                }
            }

            if (SONG_TAG.equals(receiveIntent)) {

                new WriteTag(intent.getParcelableExtra("song"),

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

                MainActivity.setAlbumFragmentState(false);
                LockableViewPager.setSwipeLocked(false);

                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.slide_out_bottom, R.anim.slide_out_bottom);
                ft.remove(getFragmentManager().findFragmentById(R.id.fragment_album_list_layout));
                ft.commit();

                Intent reload = new Intent();
                reload.setAction("reload");
                MusiqueApplication.getInstance().sendBroadcast(reload);

                Intent shadow = new Intent();
                shadow.setAction(INTENT_TOOLBAR_SHADOW);
                shadow.putExtra("boolean", true);
                MusiqueApplication.getInstance().sendBroadcast(shadow);

            }

            if (SET_TITLE.equals(receiveIntent)){
                if (tri.equals("a-z"))
                    getActivity().setTitle(albumName);

                if (tri.equals(getString(R.string.title_sort_artist)))
                    getActivity().setTitle(Artiste);

                if (tri.equals(getString(R.string.title_sort_year)))
                    getActivity().setTitle(Année);
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
