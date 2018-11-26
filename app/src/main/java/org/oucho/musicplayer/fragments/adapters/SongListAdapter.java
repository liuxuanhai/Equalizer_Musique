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

package org.oucho.musicplayer.fragments.adapters;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.view.fastscroll.FastScroller;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class SongListAdapter extends Adapter<SongListAdapter.SongViewHolder> implements FastScroller.SectionIndexer, MusiqueKeys {

    private final Context mContext;
    private final SharedPreferences préférences;
    private String getTri;
    private List<Song> mSongList = Collections.emptyList();

    private final int artSize;

    public SongListAdapter(Context context) {
        mContext = context;
        préférences = mContext.getSharedPreferences(FICHIER_PREFS, Context.MODE_PRIVATE);
        this.artSize = mContext.getResources().getDimensionPixelSize(R.dimen.fragment_song_list_item_art_size);
    }

    public void setData(List<Song> data) {
        mSongList = data;
        notifyDataSetChanged();
    }

    public List<Song> getSongList() {
        return mSongList;
    }

    @Override
    public SongViewHolder onCreateViewHolderImpl(ViewGroup parent) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.fragment_song_item, parent, false);

        return new SongViewHolder(itemView);
    }

    @Override
    public String getSectionText(int position) {
        Song song = mSongList.get(position);

        getTri = préférences.getString("song_sort_order", "");


        if ("REPLACE ('<BEGIN>' || artist, '<BEGIN>The ', '<BEGIN>')".equals(getTri)) {

            String toto = String.valueOf(song.getArtist())
                    .replaceFirst("The ", "");

            return stripAccents(String.valueOf(toto.toUpperCase().charAt(0)));

        } else if ("year DESC".equals(getTri)) {

            String toto = String.valueOf(song.getYear());

            return String.valueOf(toto);

        } else if ("album".equals(getTri)) {

            String toto = String.valueOf(song.getAlbum());

            return stripAccents(String.valueOf(toto.toUpperCase().charAt(0)));

        } else {

            String toto = String.valueOf(song.getTitle());

            return stripAccents(String.valueOf(toto.toUpperCase().charAt(0)));
        }
    }

    private static String stripAccents(String s) {
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return s;
    }

    @Override
    public void onBindViewHolderImpl(SongViewHolder holder, int position) {
        Song song = getItem(position);

        getTri = préférences.getString("song_sort_order", "");

        holder.vTitle.setText(song.getTitle());
        holder.vTitle.setTextColor(ContextCompat.getColor(mContext, R.color.grey_900));
        holder.vTitle.setTextSize(15);
        holder.vTitle.setTypeface(null, Typeface.NORMAL);

        holder.vArtist.setText(song.getArtist());
        holder.vArtist.setTextColor(ContextCompat.getColor(mContext, R.color.grey_600));
        holder.vArtist.setTextSize(15);
        holder.vArtist.setTypeface(null, Typeface.NORMAL);

        holder.vDuration.setText("(" + msToText(song.getDuration()) + ")");
        holder.vDuration.setTextColor(ContextCompat.getColor(mContext, R.color.grey_600));
        holder.vDuration.setVisibility(View.VISIBLE);

        holder.vYear.setVisibility(View.GONE);
        holder.vAlbum.setVisibility(View.GONE);


        if ("REPLACE ('<BEGIN>' || artist, '<BEGIN>The ', '<BEGIN>')".equals(getTri)) {

            holder.vArtist.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
            holder.vArtist.setTextSize(16);
            holder.vArtist.setTypeface(null, Typeface.BOLD);

        } else if ("year DESC".equals(getTri)) {

            holder.vDuration.setVisibility(View.GONE);

            holder.vYear.setText(String.valueOf(song.getYear()));
            holder.vYear.setVisibility(View.VISIBLE);

        } else if ("album".equals(getTri)){

            holder.vAlbum.setText(song.getAlbum());
            holder.vAlbum.setVisibility(View.VISIBLE);

            holder.vDuration.setVisibility(View.GONE);

        } else {

            holder.vTitle.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
            holder.vTitle.setTextSize(16);
            holder.vTitle.setTypeface(null, Typeface.BOLD);
        }

        Uri uri = ContentUris.withAppendedId(ARTWORK_URI, song.getAlbumId());
        Picasso.get()
                .load(uri)
                .resize(artSize, artSize)
                .centerCrop()
                .into(holder.vArtwork);
    }

    public Song getItem(int position) {
        return mSongList.get(position);
    }

    @Override
    public int getItemCountImpl() {
        return mSongList.size();
    }

    @Override
    public int getItemViewTypeImpl() {
        return 0;
    }


    class SongViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView vTitle;
        private final TextView vArtist;
        private final TextView vDuration;
        private final TextView vAlbum;
        private final TextView vYear;


        private final ImageView vArtwork;

        SongViewHolder(View itemView) {
            super(itemView);
            vTitle = itemView.findViewById(R.id.title);
            vArtist = itemView.findViewById(R.id.artist);
            vDuration = itemView.findViewById(R.id.duration);
            vAlbum = itemView.findViewById(R.id.album);
            vYear = itemView.findViewById(R.id.year);


            vArtwork = itemView.findViewById(R.id.artwork);
            itemView.setOnClickListener(this);

            ImageButton menuButton = itemView.findViewById(R.id.menu_button);
            menuButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            triggerOnItemClickListener(position, v);
        }
    }

    private String msToText(int msec) {
        return String.format(Locale.getDefault(), "%d:%02d", msec / 60000, (msec % 60000) / 1000);
    }
}
