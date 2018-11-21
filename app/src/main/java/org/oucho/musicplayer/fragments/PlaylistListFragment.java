package org.oucho.musicplayer.fragments;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.fragments.adapters.BaseAdapter;
import org.oucho.musicplayer.fragments.adapters.PlaylistListAdapter;
import org.oucho.musicplayer.dialog.CreatePlaylistDialog;
import org.oucho.musicplayer.db.model.Playlist;
import org.oucho.musicplayer.tools.LockableViewPager;
import org.oucho.musicplayer.view.fastscroll.FastScrollRecyclerView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.oucho.musicplayer.MusiqueKeys.INTENT_BACK;
import static org.oucho.musicplayer.MusiqueKeys.INTENT_QUEUEVIEW;

public class PlaylistListFragment extends BaseFragment {

    @SuppressWarnings("unused")
    private static final String TAG_LOG = "PlayListFragment";
    private Context mContext;

    private static final String[] sProjection = {
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME
    };

    private PlaylistListAdapter mAdapter;

    private final LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>() {

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            //  Auto-generated method stub
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            List<Playlist> list = new ArrayList<>();
            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndex(MediaStore.Audio.Genres._ID);
                int nameCol = cursor
                        .getColumnIndex(MediaStore.Audio.Genres.NAME);

                do {
                    long id = cursor.getLong(idCol);
                    String name = cursor.getString(nameCol);
                    list.add(new Playlist(id, name));
                } while (cursor.moveToNext());

                Collections.sort(list, (lhs, rhs) -> {
                    Collator c = Collator.getInstance(Locale.getDefault());
                    c.setStrength(Collator.PRIMARY);
                    return c.compare(lhs.getName(), rhs.getName());
                });
            }

            mAdapter.setData(list);

        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {

            Uri playlistsUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;

            return new CursorLoader(getActivity(), playlistsUri,
                    sProjection, null, null, null);
        }
    };

    public static PlaylistListFragment newInstance() {

        return new PlaylistListFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mContext = getContext();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, mLoaderCallbacks);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playlist_list, container, false);

        FastScrollRecyclerView mRecyclerView = rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new PlaylistListAdapter();
        mAdapter.setOnItemClickListener(mOnItemClickListener);

        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();

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

                } else if (MainActivity.getPlaylistFragmentState()) {

                    MainActivity.setPlaylistFragmentState(false);
                    LockableViewPager.setSwipeLocked(false);


                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_out_bottom, R.anim.slide_out_bottom);
                    ft.remove(getFragmentManager().findFragmentById(R.id.fragment_playlist_list));
                    ft.commit();

                    getActivity().setTitle(getContext().getString(R.string.playlists));

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


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.playlist, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_playlist:
                showCreatePlaylistDialog();
                break;
            default: //do nothing
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCreatePlaylistDialog() {
        CreatePlaylistDialog dialog = CreatePlaylistDialog.newInstance();
        dialog.setOnPlaylistCreatedListener(this::load);
        dialog.show(getChildFragmentManager(), "create_playlist");

    }

    @Override
    public void load() {
        getLoaderManager().restartLoader(0, null, mLoaderCallbacks);

    }

    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {
            Playlist playlist = mAdapter.getItem(position);

            PlaylistContentFragment fragment = PlaylistContentFragment.newInstance(playlist);

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_in_bottom);
            ft.replace(R.id.fragment_playlist_list, fragment);
            ft.commit();

        }
    };

    @Override
    public void setUserVisibleHint(boolean visible){
        super.setUserVisibleHint(visible);

        if (visible || isResumed()) {

            getActivity().setTitle(mContext.getString(R.string.playlists));

            MainActivity.setViewID(R.id.fragment_playlist_list);

        }
    }

}
