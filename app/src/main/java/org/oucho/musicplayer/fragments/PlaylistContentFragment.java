package org.oucho.musicplayer.fragments;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Playlist;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.fragments.loaders.PlaylistLoader;
import org.oucho.musicplayer.utils.PlaylistsUtils;
import org.oucho.musicplayer.tools.CustomSwipe;
import org.oucho.musicplayer.tools.CustomSwipeAdapter;
import org.oucho.musicplayer.view.DragRecyclerView;
import org.oucho.musicplayer.tools.LockableViewPager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.oucho.musicplayer.MusiqueKeys.ARTWORK_URI;
import static org.oucho.musicplayer.MusiqueKeys.INTENT_QUEUEVIEW;
import static org.oucho.musicplayer.MusiqueKeys.INTENT_SET_MENU;


public class PlaylistContentFragment extends BaseFragment {

    private static final String PARAM_PLAYLIST_ID = "playlist_id";
    private static final String PARAM_PLAYLIST_NAME = "playlist_name";

    private MainActivity mActivity;

    private ArrayList<Song> mSongList = new ArrayList<>();
    private DragRecyclerView mRecyclerView;

    private Playlist mPlaylist;

    private SongListAdapter mAdapter;

    private int artSize;


    private final LoaderManager.LoaderCallbacks<List<Song>> mLoaderCallbacks = new LoaderCallbacks<List<Song>>() {


        @Override
        public Loader<List<Song>> onCreateLoader(int id, Bundle args) {

            return new PlaylistLoader(getActivity(), mPlaylist.getId());
        }

        @Override
        public void onLoadFinished(Loader<List<Song>> loader, List<Song> data) {
            mSongList = new ArrayList<>(data);
            mAdapter.notifyDataSetChanged();

            Intent menu = new Intent();
            menu.setAction(INTENT_SET_MENU);
            menu.putExtra("boolean", false);
            getContext().sendBroadcast(menu);

        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {
            // This constructor is intentionally empty, pourquoi ? parce que !
        }
    };

    public static PlaylistContentFragment newInstance(Playlist playlist) {
        PlaylistContentFragment fragment = new PlaylistContentFragment();

        Bundle args = new Bundle();

        args.putLong(PARAM_PLAYLIST_ID, playlist.getId());
        args.putString(PARAM_PLAYLIST_NAME, playlist.getName());

        fragment.setArguments(args);
        return fragment;
    }


    private void selectSong(int position) {

        if (mActivity != null) {
            mActivity.onSongSelected(mSongList, position);
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mActivity = (MainActivity) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        setHasOptionsMenu(true);
        if (args != null) {

            long id = args.getLong(PARAM_PLAYLIST_ID);
            String name = args.getString(PARAM_PLAYLIST_NAME);
            mPlaylist = new Playlist(id, name);

        }

        artSize = getContext().getResources().getDimensionPixelSize(R.dimen.fragment_playlist_item_art_size);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playlist_content, container, false);

        mRecyclerView = rootView.findViewById(R.id.list_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new SongListAdapter();


        ItemTouchHelper.Callback callback = new CustomSwipe(mAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mRecyclerView);


        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setOnItemMovedListener((oldPosition, newPosition) -> mAdapter.moveItem(oldPosition, newPosition));

        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();
        MainActivity.setViewID(R.id.fragment_playlist);

        MainActivity.setPlaylistFragmentState(true);

        MainActivity.setPlaylistName(mPlaylist.getName());

        LockableViewPager.setSwipeLocked(true);


        final int couleurTitre = ContextCompat.getColor(getContext(), R.color.grey_400);
        String titre = getContext().getString(R.string.playlists);

        if (!MainActivity.getAlbumFragmentState()) {

            if (android.os.Build.VERSION.SDK_INT >= 24) {
                getActivity().setTitle(Html.fromHtml("<font>"
                        + titre
                        + " </font> <small> <font color='" + couleurTitre + "'>"
                        + mPlaylist.getName()
                        + "</small></font>", Html.FROM_HTML_MODE_LEGACY));

            } else {

                //noinspection deprecation
                getActivity().setTitle(Html.fromHtml("<font>"
                        + titre
                        + " </font> <small> <font color='" + couleurTitre + "'>"
                        + mPlaylist.getName()
                        + "</small></font>"));
            }
        }

        // Active la touche back
        if(getView() == null){
            return;
        }

        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener((v, keyCode, event) -> {

            if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK){

                LockableViewPager.setSwipeLocked(false);

                if (MainActivity.getQueueLayout()) {

                    Intent intent = new Intent();
                    intent.setAction(INTENT_QUEUEVIEW);
                    getContext().sendBroadcast(intent);

                } else {

                    MainActivity.setPlaylistFragmentState(false);

                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.setCustomAnimations(R.anim.slide_out_bottom, R.anim.slide_out_bottom);
                    ft.remove(getFragmentManager().findFragmentById(R.id.fragment_playlist_list));
                    ft.commit();

                    getActivity().setTitle(getContext().getString(R.string.playlists));

                }

                return true;
            }
            return false;
        });
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        load();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }


    @Override
    public void load() {
        getLoaderManager().restartLoader(0, null, mLoaderCallbacks);
    }

    private class SongViewHolder extends RecyclerView.ViewHolder implements OnClickListener, OnTouchListener {

        final View itemView;
        final TextView vTitle;
        final TextView vArtist;
        final ImageButton vReorderButton;


        @SuppressLint("ClickableViewAccessibility")
        SongViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            vTitle = itemView.findViewById(R.id.title);
            vArtist = itemView.findViewById(R.id.artist);

            vReorderButton = itemView.findViewById(R.id.reorder_button);

            itemView.findViewById(R.id.song_info).setOnClickListener(this);
            vReorderButton.setOnTouchListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();

            switch (v.getId()) {
                case R.id.song_info:
                    selectSong(position);
                    break;
                default:
                    break;
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mRecyclerView.startDrag(itemView);
            return false;
        }

    }


    class SongListAdapter extends RecyclerView.Adapter<SongViewHolder> implements CustomSwipeAdapter {

        @Override
        public SongViewHolder onCreateViewHolder(ViewGroup parent, int type) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_playlist_content_item, parent, false);

            return new SongViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(SongViewHolder viewHolder, int position) {

            Song song = mSongList.get(position);
            viewHolder.vTitle.setText(song.getTitle());
            viewHolder.vArtist.setText(song.getArtist());

            Uri uri = ContentUris.withAppendedId(ARTWORK_URI, song.getAlbumId());
            Picasso.get()
                    .load(uri)
                    .resize(artSize, artSize)
                    .centerCrop()
                    .into(viewHolder.vReorderButton);
        }

        @Override
        public int getItemCount() {
            return mSongList.size();
        }

        void moveItem(int oldPosition, int newPosition) {
            if (oldPosition < 0 || oldPosition >= mSongList.size()
                    || newPosition < 0 || newPosition >= mSongList.size()) {
                return;
            }
            Collections.swap(mSongList, oldPosition, newPosition);

            PlaylistsUtils.moveItem(getActivity().getContentResolver(), mPlaylist.getId(), oldPosition, newPosition);

            notifyItemMoved(oldPosition, newPosition);
        }

        @Override
        public void onItemSwiped(int position) {

            Song s = mSongList.remove(position);

            PlaylistsUtils.removeFromPlaylist(getActivity().getContentResolver(), mPlaylist.getId(), s.getId());
            notifyItemRemoved(position);
        }
    }


}
