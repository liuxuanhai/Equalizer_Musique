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
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Artist;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.dialog.ConvertDialog;
import org.oucho.musicplayer.dialog.PlaylistPickerDialog;
import org.oucho.musicplayer.dialog.SaveTagProgressDialog;
import org.oucho.musicplayer.dialog.TagAlbumEditorDialog;
import org.oucho.musicplayer.fragments.adapters.AlbumListAdapter;
import org.oucho.musicplayer.fragments.adapters.BaseAdapter;
import org.oucho.musicplayer.fragments.loaders.AlbumLoader;
import org.oucho.musicplayer.fragments.loaders.SongLoader;
import org.oucho.musicplayer.fragments.loaders.SortOrder;
import org.oucho.musicplayer.services.PlayerService;
import org.oucho.musicplayer.tools.LockableViewPager;
import org.oucho.musicplayer.utils.PlaylistsUtils;
import org.oucho.musicplayer.utils.PrefSort;
import org.oucho.musicplayer.view.CustomGridLayoutManager;
import org.oucho.musicplayer.view.fastscroll.FastScrollRecyclerView;

import java.util.List;

public class AlbumListFragment extends BaseFragment implements MusiqueKeys {

    @SuppressWarnings("unused")
    private static final String TAG_LOG = "AlbumListFragment";

    private Context mContext;
    private Menu menu;

    private AlbumListAdapter mAdapter;
    private ReloadView reloadReceiver;
    private SharedPreferences préférences = null;

    private String titre;
    private String tri;

    private boolean run = false;
    private boolean isRegistered = false;

    private FastScrollRecyclerView mRecyclerView;
    private final Handler mHandler = new Handler();

    private Artist mArtist;

    private List<Album> listeTitre;


    public static AlbumListFragment newInstance() {

        return new AlbumListFragment();
    }


    public static AlbumListFragment newInstance(Artist artist) {

        AlbumListFragment fragment = new AlbumListFragment();

        Bundle args = new Bundle();
        args.putParcelable("artist", artist);

        fragment.setArguments(args);
        return fragment;
    }


    private final LoaderManager.LoaderCallbacks<List<Album>> mLoaderCallbacks = new LoaderCallbacks<List<Album>>() {

        @Override
        public Loader<List<Album>> onCreateLoader(int id, Bundle args) {

            AlbumLoader loader;
            if(mArtist != null) {
                loader = new AlbumLoader(mContext, mArtist.getName());
                loader.setSortOrder(PrefSort.getInstance().getAlbumSortOrder());

            }  else {

                loader = new AlbumLoader(mContext);
                loader.setSortOrder(PrefSort.getInstance().getAlbumSortOrder());
            }

            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Album>> loader, List<Album> albumList) {
            mAdapter.setData(albumList);
            listeTitre = albumList;
        }

        @Override
        public void onLoaderReset(Loader<List<Album>> loader) {
            //  Auto-generated method stub
        }
    };




    /* *********************************************************************************************
     * Création de l'activité
     * ********************************************************************************************/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        setHasOptionsMenu(true);


        if (bundle != null) {
            mArtist = bundle.getParcelable("artist");
        }

        mContext = getContext();

        préférences = this.mContext.getSharedPreferences(FICHIER_PREFS, Context.MODE_PRIVATE);

        titre = mContext.getString(R.string.albums);

        setTri();

        reloadReceiver = new ReloadView();
        IntentFilter filter = new IntentFilter();
        filter.addAction("reload");
        filter.addAction(ALBUM_TAG);
        filter.addAction(REFRESH_TAG);
        filter.addAction(SET_TITLE);

        mContext.registerReceiver(reloadReceiver, filter);
        isRegistered = true;

        load();
    }



    /* *********************************************************************************************
     * Création de la vue
     * ********************************************************************************************/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        int layout;

        if (mArtist != null) {
            layout = R.layout.fragment_search_liste_album;
        } else {
            layout = R.layout.fragment_liste_album;
        }

        View rootView = inflater.inflate(layout, container, false);

        mRecyclerView = rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new CustomGridLayoutManager(mContext, 2));

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        assert wm != null;
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int marginArtwork = getResources().getDimensionPixelSize(R.dimen.fragment_album_list_item_margin);

        int artworkSize = (size.x/2) - (marginArtwork * 2);

        mAdapter = new AlbumListAdapter(mContext, artworkSize);
        mAdapter.setOnItemClickListener(mOnItemClickListener);
        mAdapter.setOnItemLongClickListener(mOnItemLongClickListener);

        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }



    @Override
    public void load() {
            getLoaderManager().restartLoader(0, null, mLoaderCallbacks);
    }



    /* *********************************************************************************************
     * Menu des albums
     * ********************************************************************************************/

    private void showMenu(final int position, View v) {

        PopupMenu popup = new PopupMenu(mContext, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.song_item, popup.getMenu());
        final Album album = mAdapter.getItem(position);
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {

                case R.id.action_add_to_queue:
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("album", album);
                    getLoaderManager().restartLoader(1, bundle, mLoaderSong);
                    break;

                case R.id.action_add_to_playlist:
                    showPlaylistPicker(album);
                    break;

                case R.id.action_edit_tags:
                    showEditorDialog(album);
                    break;

                case R.id.action_convert:
                    convert(album);
                    break;

                default: //do nothing
                    break;

            }
            return false;
        });
        popup.show();
    }

    private final LoaderManager.LoaderCallbacks<List<Song>> mLoaderSong = new LoaderManager.LoaderCallbacks<List<Song>>() {

        @Override
        public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
            Album album = args.getParcelable("album");
            SongLoader loader = new SongLoader(getActivity());
            assert album != null;
            loader.setSelection(MediaStore.Audio.Media.ALBUM_ID + " = ?", new String[]{String.valueOf(album.getId())});
            loader.setSortOrder(MediaStore.Audio.Media.TRACK);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Song>> loader, List<Song> songList) {
            for (int i = 0; i < songList.size(); i++)
                ((MainActivity) getActivity()).addToQueue(songList.get(i));

        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {}
    };

    private void showEditorDialog(Album album) {
        TagAlbumEditorDialog dialog = TagAlbumEditorDialog.newInstance(album);
        dialog.show(getChildFragmentManager(), "edit_album_tags");
    }

    private void convert(Album album) {
        ConvertDialog dialog = ConvertDialog.newInstance(album);
        dialog.show(getChildFragmentManager(), "convert");
    }

    private void showPlaylistPicker(final Album album) {
        PlaylistPickerDialog picker = PlaylistPickerDialog.newInstance();
        picker.setListener(playlist -> PlaylistsUtils.addAlbumToPlaylist(mContext.getContentResolver(), playlist.getId(), album.getId()));
        picker.show(getChildFragmentManager(), "pick_playlist");
    }

    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {
            Album album = mAdapter.getItem(position);

            switch (view.getId()) {
                case R.id.album_artwork:
                case R.id.album_info:


                    Fragment fragment = AlbumFragment.newInstance(album);
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom);
                    ft.replace(R.id.fragment_album_list_layout, fragment);
                    ft.addToBackStack("AlbumFragment");
                    ft.commit();

                    mHandler.postDelayed(() -> showOverflowMenu(false), 300);


                    break;
                case R.id.menu_button:
                    showMenu(position, view);
                    break;
                default:
                    break;
            }
        }
    };


    private final BaseAdapter.OnItemLongClickListener mOnItemLongClickListener = new BaseAdapter.OnItemLongClickListener() {
        @Override
        public void onItemLongClick(int position, View view) {

            for (int i = 0; i < listeTitre.size(); i++) {
                if (listeTitre.get(i).getId() == PlayerService.getAlbumId())
                    mRecyclerView.smoothScrollToPosition( i );
            }
        }
    };

    /* *********************************************************************************************
     * Menu
     * ********************************************************************************************/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        this.menu = menu;

        inflater.inflate(R.menu.albumlist_sort_by, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        PrefSort prefSort = PrefSort.getInstance();
        switch (item.getItemId()) {

            case R.id.menu_sort_by_az:
                prefSort.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_A_Z);
                load();
                tri = "a-z";
                setUserVisibleHint(true);
                break;
            case R.id.menu_sort_by_artist:
                prefSort.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_ARTIST);
                load();
                tri = mContext.getString(R.string.title_sort_artist);
                setUserVisibleHint(true);
                break;
            case R.id.menu_sort_by_year:
                prefSort.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_YEAR);
                load();
                tri = mContext.getString(R.string.title_sort_year);
                setUserVisibleHint(true);
                break;
            case R.id.menu_sort_by_ajout:
                prefSort.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_AJOUT);
                load();
                tri = mContext.getString(R.string.title_sort_add);
                setUserVisibleHint(true);
                break;
            default: //do nothing
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showOverflowMenu(boolean showMenu){
        if(menu == null)
            return;

        menu.setGroupVisible(R.id.main_menu_group, showMenu);
    }


    @Override
    public void onPause() {
        super.onPause();

        if (isRegistered) {
            mContext.unregisterReceiver(reloadReceiver);
            isRegistered = false;
        }

        AlbumLoader.resetArtist();

    }


    @Override
    public void onResume() {
        super.onResume();

        if (!isRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("reload");
            filter.addAction(ALBUM_TAG);
            filter.addAction(REFRESH_TAG);
            filter.addAction(SET_TITLE);

            mContext.registerReceiver(reloadReceiver, filter);
            isRegistered = true;
        }


        if (!MainActivity.getChercheActivity()) {
            // Active la touche back
            if (getView() == null) {
                return;
            }

            getView().setFocusableInTouchMode(true);
            getView().requestFocus();
            getView().setOnKeyListener((v, keyCode, event) -> {

                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {

                    if (MainActivity.getQueueLayout()) {

                        Intent intent = new Intent();
                        intent.setAction(INTENT_QUEUEVIEW);
                        mContext.sendBroadcast(intent);

                        return true;

                    } else if (MainActivity.getAlbumFragmentState()) {

                        MainActivity.setAlbumFragmentState(false);
                        LockableViewPager.setSwipeLocked(false);
                        showOverflowMenu(true);

                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.setCustomAnimations(R.anim.slide_out_bottom, R.anim.slide_out_bottom);
                        ft.remove(getFragmentManager().findFragmentById(R.id.fragment_album_list_layout));
                        ft.commit();

                        Intent shadow = new Intent();
                        shadow.setAction(INTENT_TOOLBAR_SHADOW);
                        shadow.putExtra("boolean", true);
                        mContext.sendBroadcast(shadow);

                        setTitre();

                        return true;
                    }

                    return false;

                }
                return false;
            });
        }
    }

    /* *********************************************************************************************
     * Titre
     * ********************************************************************************************/

    private void setTri() {

        String getTri = préférences.getString("album_sort_order", "");

        if ("minyear DESC".equals(getTri)) {
            tri = mContext.getString(R.string.title_sort_year);
        } else if ("REPLACE ('<BEGIN>' || artist, '<BEGIN>The ', '<BEGIN>')".equals(getTri)) {
            tri = mContext.getString(R.string.title_sort_artist);
        } else if ("_id DESC".equals(getTri)) {
            tri = mContext.getString(R.string.title_sort_add);
        } else {
            tri = "a-z";
        }
    }


    private class ReloadView extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {

            String receiveIntent = intent.getAction();

            if ("reload".equals(receiveIntent)) {

                if (MainActivity.getViewID() != R.id.fragment_song_layout)
                    setUserVisibleHint(true);


                if (!MainActivity.getAlbumFragmentState())
                    showOverflowMenu(true);

                mAdapter.notifyDataSetChanged();

            } else if (SET_TITLE.equals(receiveIntent)) {

                setTitre();

            } else if (ALBUM_TAG.equals(receiveIntent)) {

                new WriteTag(intent.getParcelableExtra("album"),
                        intent.getStringExtra("albumName"),
                        intent.getStringExtra("artistName"),
                        intent.getStringExtra("genre"),
                        intent.getStringExtra("year"),
                        intent.getStringExtra("cover")
                ).execute();

            } else if (REFRESH_TAG.equals(receiveIntent)) {
                load();
            }
        }
    }


    @SuppressLint("StaticFieldLeak")
    class WriteTag extends AsyncTask<String, Integer, Boolean> {

        SaveTagProgressDialog newFragment;

        final String albumName;
        final String artistName;
        final String genre;
        final String year;
        final String cover;

        final Album album;

        WriteTag(Album album, String albumName, String artistName, String genre, String year, String cover) {
            this.album = album;
            this.albumName = albumName;
            this.artistName = artistName;
            this.genre = genre;
            this.year = year;
            this.cover = cover;
        }

        @Override
        protected Boolean doInBackground(String... arg0) {
            newFragment = new SaveTagProgressDialog();
            Bundle bundle = new Bundle();
            bundle.putString("type", "album");
            bundle.putString("albumName", albumName);
            bundle.putString("artistName", artistName);
            bundle.putString("genre", genre);
            bundle.putString("year", year);
            bundle.putString("cover", cover);

            bundle.putParcelable("album", album);

            newFragment.setArguments(bundle);
            newFragment.show(getFragmentManager(), "SaveTag");

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {

        }
    }

    private void setTitre() {
        final int couleurTitre = ContextCompat.getColor(mContext, R.color.grey_400);

        if (!MainActivity.getAlbumFragmentState()) {

            if (android.os.Build.VERSION.SDK_INT >= 24) {
                getActivity().setTitle(Html.fromHtml("<font>"
                        + titre
                        + " </font> <small> <font color='" + couleurTitre + "'>"
                        + tri
                        + "</small></font>", Html.FROM_HTML_MODE_LEGACY));

            } else {

                //noinspection deprecation
                getActivity().setTitle(Html.fromHtml("<font>"
                        + titre
                        + " </font> <small> <font color='" + couleurTitre + "'>"
                        + tri
                        + "</small></font>"));
            }
        }

    }

    @Override
    public void setUserVisibleHint(boolean visible){
        super.setUserVisibleHint(visible);

        if (visible || isResumed()) {

            if (MainActivity.getChercheActivity()) {
                Intent intent0 = new Intent();
                intent0.setAction("search.setTitle");
                intent0.putExtra("title", titre);
                getActivity().sendBroadcast(intent0);
            }

            // délai affichage lors du premier chargement nom appli --> tri actuel
            if (run) {

                MainActivity.setViewID(R.id.fragment_album_list_layout);
                setTitre();

            } else {

                MainActivity.setViewID(R.id.fragment_album_list_layout);
                run = true;
                mHandler.postDelayed(this::setTitre, 1000);
            }
        }
    }
}
