package org.oucho.musicplayer.search.adapter;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.fragments.adapters.BaseAdapter;
import org.oucho.musicplayer.view.fastscroll.FastScroller;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;

public class AlbumViewAdapter extends BaseAdapter<AlbumViewAdapter.AlbumViewHolder> implements FastScroller.SectionIndexer, MusiqueKeys {


    private List<Album> mAlbumList = Collections.emptyList();

    public AlbumViewAdapter() {

    }

    public void setData(List<Album> data) {
        mAlbumList = data;
        notifyDataSetChanged();
    }



    @Override
    public AlbumViewAdapter.AlbumViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.activity_search_song_item, parent, false);

        return new AlbumViewAdapter.AlbumViewHolder(itemView);
    }

    @Override
    public String getSectionText(int position) {

        Album album = mAlbumList.get(position);
        String toto = String.valueOf(album.getAlbumName())
                .replaceFirst("The ", "")
                .replaceFirst("A ", "");

        return stripAccents(String.valueOf(toto.toUpperCase().charAt(0)));
    }

    private static String stripAccents(String s) {
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return s;
    }

    @Override
    public void onBindViewHolder(AlbumViewAdapter.AlbumViewHolder holder, int position) {
        Album album = getItem(position);

        holder.vTitle.setText(album.getAlbumName());
        holder.vArtist.setText(album.getArtistName());
    }

    public Album getItem(int position) {
        return mAlbumList.get(position);
    }

    @Override
    public int getItemCount() {
        return mAlbumList.size();
    }


    class AlbumViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView vTitle;
        final TextView vArtist;

        AlbumViewHolder(View itemView) {
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
