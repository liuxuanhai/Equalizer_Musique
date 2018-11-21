package org.oucho.musicplayer.search.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.dialog.PlaylistPickerDialog;
import org.oucho.musicplayer.fragments.BaseFragment;
import org.oucho.musicplayer.fragments.adapters.BaseAdapter;
import org.oucho.musicplayer.fragments.loaders.SongLoader;
import org.oucho.musicplayer.search.SearchActivity;
import org.oucho.musicplayer.search.adapter.SongViewAdapter;
import org.oucho.musicplayer.utils.PlaylistsUtils;
import org.oucho.musicplayer.utils.PrefSort;
import org.oucho.musicplayer.view.fastscroll.FastScrollRecyclerView;

import java.util.ArrayList;
import java.util.List;

import static org.oucho.musicplayer.search.SearchActivity.setLoaderFilter;

public class SearchSongFragment extends BaseFragment implements MusiqueKeys {


    private boolean receiver;

    private Context mContext;
    private TextView noSongResult;
    private SongViewAdapter mSongAdapert;
    private SearchActivity mSearchActivity;


    public static SearchSongFragment newInstance() {
        return new SearchSongFragment();
    }


    private final LoaderManager.LoaderCallbacks<List<Song>> mSongLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<Song>>() {

        @Override
        public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
            SongLoader loader = new SongLoader(mContext);

            loader.setSortOrder(PrefSort.getInstance().getSongSortOrder());

            if (!String.valueOf(args).equals("Bundle[{filter=}]"))
                setLoaderFilter(args, loader);

            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Song>> loader, List<Song> data) {
            mSongAdapert.setData(data);

            if (data.size() == 0) {
                noSongResult.setVisibility(View.VISIBLE);
            } else {
                noSongResult.setVisibility(View.GONE);
            }
        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {}
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
        mSongAdapert = new SongViewAdapter();
        mSongAdapert.setOnItemClickListener(mOnItemClickListener);

        mRecyclerView.setAdapter(mSongAdapert);

        noSongResult = rootView.findViewById(R.id.no_result);
        return rootView;
    }


    private final BaseAdapter.OnItemClickListener mOnItemClickListener = (position, view) -> {
        switch (view.getId()) {
            case R.id.item_view:
                selectSong(position);
                break;
            case R.id.menu_button:
                showMenu(position, view);
                break;
            default:
                break;
        }
    };


    /* ***********
     * Menu item
     * ***********/
    private void showMenu(final int position, View v) {

        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();

        final Song song = mSongAdapert.getItem(position);
        inflater.inflate(R.menu.search_song_item, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_add_to_queue:
                    mSearchActivity.addToQueue(song);
                    return true;
                case R.id.action_add_to_playlist:
                    showPlaylistPicker(song);
                    return true;
                default: //do nothing
                    break;
            }
            return false;
        });
        popup.show();
    }

    private void selectSong(int position) {
        List<Song> songs = new ArrayList<>();

        if (mSearchActivity != null) {
            songs.add(mSongAdapert.getItem(position));

            mSearchActivity.onSongSelected(songs, 0);
        }
    }





    private void showPlaylistPicker(final Song song) {
        PlaylistPickerDialog picker = PlaylistPickerDialog.newInstance();
        picker.setListener(playlist -> PlaylistsUtils.addSongToPlaylist(getActivity().getContentResolver(), playlist.getId(), song.getId()));
        picker.show(getChildFragmentManager(), "pick_playlist");

    }


    @Override
    public void load() {
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

        getLoaderManager().restartLoader(0, args, mSongLoaderCallbacks);
    }
}

