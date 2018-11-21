package org.oucho.musicplayer.search.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.dialog.PlaylistPickerDialog;
import org.oucho.musicplayer.fragments.AlbumFragment;
import org.oucho.musicplayer.fragments.BaseFragment;
import org.oucho.musicplayer.fragments.adapters.BaseAdapter;
import org.oucho.musicplayer.fragments.loaders.AlbumLoader;
import org.oucho.musicplayer.fragments.loaders.SongLoader;
import org.oucho.musicplayer.fragments.loaders.SortOrder;
import org.oucho.musicplayer.search.SearchActivity;
import org.oucho.musicplayer.search.adapter.AlbumViewAdapter;
import org.oucho.musicplayer.utils.PlaylistsUtils;
import org.oucho.musicplayer.view.fastscroll.FastScrollRecyclerView;

import java.util.List;

import static org.oucho.musicplayer.MusiqueKeys.FILTER;
import static org.oucho.musicplayer.search.SearchActivity.setLoaderFilter;

public class SearchAlbumFragment extends BaseFragment {


    private AlbumViewAdapter mAdapterAlbum;
    private TextView noAlbumResult;
    private SearchActivity mSearchActivity;

    private boolean receiver;

    private long albumID;


    private Context mContext;

    public static SearchAlbumFragment newInstance() {

        return new SearchAlbumFragment();
    }


    private final LoaderManager.LoaderCallbacks<List<Album>> mAlbumLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<List<Album>>() {

        @Override
        public Loader<List<Album>> onCreateLoader(int id, Bundle args) {

            AlbumLoader loader = new AlbumLoader(getContext(), null);

            loader.setSortOrder(SortOrder.AlbumSortOrder.ALBUM_A_Z);


            if (!String.valueOf(args).equals("Bundle[{filter=}]"))
                setLoaderFilter(args, loader);

            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Album>> loader, List<Album> data) {
            mAdapterAlbum.setData(data);

            if (SearchActivity.getNewText().equals("")) {
                //SearchActivity.setAlbumTab(0);

                Intent setTab = new Intent();
                setTab.setAction("search.setAlbumTab");
                setTab.putExtra("setAlbumTab", 0);
                getContext().sendBroadcast(setTab);

            } else {
                //SearchActivity.setAlbumTab(data.size());

                Intent setTab = new Intent();
                setTab.setAction("search.setAlbumTab");
                setTab.putExtra("setAlbumTab", data.size());
                getContext().sendBroadcast(setTab);
            }

            if (data.size() == 0) {
                noAlbumResult.setVisibility(View.VISIBLE);
            } else {
                noAlbumResult.setVisibility(View.GONE);
            }
        }

        @Override
        public void onLoaderReset(Loader<List<Album>> loader) {
            // This constructor is intentionally empty, pourquoi ? parce que !
        }
    };



    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getContext();
        mSearchActivity = (SearchActivity) mContext;

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_search_fragment, container, false);

        FastScrollRecyclerView mRecyclerView = rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapterAlbum = new AlbumViewAdapter();
        mAdapterAlbum.setOnItemClickListener(mOnItemClickListener);

        mRecyclerView.setAdapter(mAdapterAlbum);

        noAlbumResult = rootView.findViewById(R.id.no_result);

        return rootView;

    }


    @Override
    public void load() {}


    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {
            Album album = mAdapterAlbum.getItem(position);

            switch (view.getId()) {
                case R.id.item_view:

                    Intent intent = new Intent();
                    intent.setAction("search.setTitle");
                    intent.putExtra("title", album.getAlbumName());
                    getActivity().sendBroadcast(intent);

                    Intent intent1 = new Intent();
                    intent1.setAction("search.setTabLayout");
                    intent1.putExtra("TabLayout", false);
                    getActivity().sendBroadcast(intent1);

                    Fragment fragment = AlbumFragment.newInstance(album);
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom);
                    ft.addToBackStack("SearchAlbum");
                    ft.replace(R.id.container2, fragment);
                    ft.commit();

                    break;
                case R.id.menu_button:
                    showMenu(position, view);
                    break;
                default:
                    break;
            }
        }
    };


    /* ***********
     * Menu item
     * ***********/
    private void showMenu(final int position, View v) {

        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();

        final Album album = mAdapterAlbum.getItem(position);
        inflater.inflate(R.menu.search_album_item, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_play_album:
                    playAlbum(album);
                    return true;
                case R.id.action_add_to_playlist:
                    showPlaylistPicker(album);
                    return true;
                default:
                    break;
            }
            return false;
        });
        popup.show();
    }


    private void showPlaylistPicker(final Album album) {
        PlaylistPickerDialog picker = PlaylistPickerDialog.newInstance();
        picker.setListener(playlist -> PlaylistsUtils.addAlbumToPlaylist(mContext.getContentResolver(), playlist.getId(), album.getId()));
        picker.show(getChildFragmentManager(), "pick_playlist");

    }


    @Override
    public void onPause() {
        super.onPause();

        if (receiver) {
            mContext.unregisterReceiver(mFragmentListener);
            receiver = false;
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        if (!receiver) {
            IntentFilter filter = new IntentFilter();

            filter.addAction("search.newkey");

            mContext.registerReceiver(mFragmentListener, filter);
            receiver = true;
        }

        updateList(SearchActivity.getNewText());

    }


    private final BroadcastReceiver mFragmentListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();

            assert receiveIntent != null;
            if (receiveIntent.equals("search.newkey")) {

                String value = intent.getStringExtra("text");

                updateList(value);
            }
        }
    };

    private void updateList(String newText) {

        Bundle args = null;
        if (newText != null) {
            args = new Bundle();
            args.putString(FILTER, newText);
        }

        getLoaderManager().restartLoader(0, args, mAlbumLoaderCallbacks);

    }



    private void playAlbum(Album album) {

        albumID = album.getId();

        getLoaderManager().restartLoader(0, null, mSongLoaderCallbacks);

    }

    private final LoaderManager.LoaderCallbacks<List<Song>> mSongLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<Song>>() {

        @Override
        public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
            SongLoader loader = new SongLoader(getActivity());

            loader.setSelection(MediaStore.Audio.Media.ALBUM_ID + " = ?", new String[]{String.valueOf(albumID)});
            loader.setSortOrder(MediaStore.Audio.Media.TRACK);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Song>> loader, List<Song> songList) {

            mSearchActivity.onSongSelected(songList, 0);
        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {}
    };

}

