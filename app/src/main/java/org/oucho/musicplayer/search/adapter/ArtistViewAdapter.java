package org.oucho.musicplayer.search.adapter;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Artist;
import org.oucho.musicplayer.fragments.adapters.BaseAdapter;
import org.oucho.musicplayer.view.fastscroll.FastScroller;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;

public class ArtistViewAdapter extends BaseAdapter<ArtistViewAdapter.ArtistViewHolder> implements FastScroller.SectionIndexer, MusiqueKeys {


    private List<Artist> mArtistList = Collections.emptyList();

    public ArtistViewAdapter() {

    }

    public void setData(List<Artist> data) {
        mArtistList = data;
        notifyDataSetChanged();
    }

    @Override
    public ArtistViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.activity_search_list_artist_item, parent, false);

        return new ArtistViewAdapter.ArtistViewHolder(itemView);
    }

/*    @Override
    public ArtistViewAdapter.ArtistViewHolder onCreateViewHolderImpl(ViewGroup parent) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.activity_search_list_item, parent, false);

        return new ArtistViewAdapter.ArtistViewHolder(itemView);
    }*/

    @Override
    public String getSectionText(int position) {

        Artist artist = mArtistList.get(position);

        String toto = String.valueOf(artist.getName())
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
    public void onBindViewHolder(ArtistViewAdapter.ArtistViewHolder holder, int position) {
        Artist artist = getItem(position);
        holder.vArtist.setText(artist.getName());
    }

    public Artist getItem(int position) {
        return mArtistList.get(position);
    }

    @Override
    public int getItemCount() {
        return mArtistList.size();
    }



    class ArtistViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView vArtist;

        ArtistViewHolder(View itemView) {
            super(itemView);

            vArtist = itemView.findViewById(R.id.title);
            itemView.findViewById(R.id.item_view).setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            triggerOnItemClickListener(position, v);
        }
    }

}
