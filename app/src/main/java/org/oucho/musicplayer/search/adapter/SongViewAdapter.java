package org.oucho.musicplayer.search.adapter;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.fragments.adapters.BaseAdapter;
import org.oucho.musicplayer.view.fastscroll.FastScroller;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;

public class SongViewAdapter extends BaseAdapter<SongViewAdapter.SongViewHolder> implements FastScroller.SectionIndexer, MusiqueKeys {


    private List<Song> mSongList = Collections.emptyList();

    public SongViewAdapter() {
    }

    public void setData(List<Song> data) {
        mSongList = data;
        notifyDataSetChanged();
    }



    @Override
    public SongViewAdapter.SongViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.activity_search_song_item, parent, false);

        return new SongViewAdapter.SongViewHolder(itemView);
    }

    @Override
    public String getSectionText(int position) {

        Song song = mSongList.get(position);
        String toto = String.valueOf(song.getTitle());

        return stripAccents(String.valueOf(toto.toUpperCase().charAt(0)));
    }

    private static String stripAccents(String s) {
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return s;
    }

    @Override
    public void onBindViewHolder(SongViewAdapter.SongViewHolder holder, int position) {
        Song song = getItem(position);

        holder.vTitle.setText(song.getTitle());
        holder.vArtist.setText(song.getArtist());
    }

    public Song getItem(int position) {
        return mSongList.get(position);
    }

    @Override
    public int getItemCount() {
        return mSongList.size();
    }


    class SongViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView vTitle;
        final TextView vArtist;

        SongViewHolder(View itemView) {
            super(itemView);

            vTitle = itemView.findViewById(R.id.title);
            vArtist = itemView.findViewById(R.id.artist);
            itemView.findViewById(R.id.item_view).setOnClickListener(this);

            ImageButton menuButton = itemView.findViewById(R.id.menu_button);
            menuButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            triggerOnItemClickListener(position, v);
        }
    }
}
