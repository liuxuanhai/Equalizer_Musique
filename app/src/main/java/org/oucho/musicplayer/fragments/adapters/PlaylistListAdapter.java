package org.oucho.musicplayer.fragments.adapters;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Playlist;
import org.oucho.musicplayer.utils.PlaylistsUtils;

import java.util.Collections;
import java.util.List;


public class PlaylistListAdapter extends Adapter<PlaylistListAdapter.PlaylistViewHolder> {

    private Context mContext;

    private List<Playlist> mPlaylistList = Collections.emptyList();

    public void setData(List<Playlist> data) {
        mPlaylistList = data;
        notifyDataSetChanged();
    }

    public Playlist getItem(int position) {
        return mPlaylistList.get(position);
    }

    @Override
    public int getItemCountImpl() {
        return mPlaylistList.size();
    }

    @Override
    public int getItemViewTypeImpl() {
        return 0;
    }

    @Override
    public void onBindViewHolderImpl(PlaylistViewHolder viewHolder, int position) {
        Playlist playlist = getItem(position);
        viewHolder.vName.setText(playlist.getName());

    }

    @Override
    public PlaylistViewHolder onCreateViewHolderImpl(ViewGroup parent) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_playlist_item, parent, false);
        return new PlaylistViewHolder(itemView);
    }


    class PlaylistViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView vName;

        public PlaylistViewHolder(View itemView) {
            super(itemView);
            vName = itemView.findViewById(R.id.name);

            itemView.findViewById(R.id.delete_playlist).setOnClickListener(this);

            itemView.setOnClickListener(this);
            mContext = itemView.getContext();
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();

            int id = v.getId();

            if (id == R.id.delete_playlist) {

                deletePlaylist(position);

            } else {
                triggerOnItemClickListener(position, v);
            }
        }
    }

    private void deletePlaylist(int position) {

        final long playlist = mPlaylistList.get(position).getId();

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(mContext.getString(R.string.deletePlaylistConfirm));
        builder.setPositiveButton(R.string.delete, (dialog, which) -> PlaylistsUtils.deletePlaylist(mContext.getContentResolver(), playlist));
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
}
