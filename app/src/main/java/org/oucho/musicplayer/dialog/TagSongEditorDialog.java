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

package org.oucho.musicplayer.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.utils.BitmapHelper;

import java.io.File;
import java.util.Locale;


public class TagSongEditorDialog extends DialogFragment implements MusiqueKeys {

    @SuppressWarnings("unused")
    private static final String TAG = "TagSongEditorDialog";

    private static Song mSong;

    private EditText mTitleEditText;
    private EditText mArtistEditText;
    private EditText mAlbumEditText;
    private EditText mGenreEditText;
    private EditText mYearEditText;
    private EditText mTrackEditText;
    private EditText mDiscEditText;
    private EditText mCommentEditText;


    private String titre;
    private String artist;
    private String album;
    private String genre;
    private String year;
    private String track;
    private String discNo;
    private String comment;

    private Bitmap cover;

    private ImageView artWork;

    private String newArtwork = null;

    private String fragSource;


    public static TagSongEditorDialog newInstance(Song song, String fragSource) {
        TagSongEditorDialog fragment = new TagSongEditorDialog();

        Bundle args = new Bundle();

        args.putParcelable("song", song);
        args.putString("fragSource", fragSource);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();

        if (bundle != null) {
            mSong = bundle.getParcelable("song");

            fragSource = bundle.getString("fragSource");
        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.edit_tags);

        @SuppressLint("InflateParams")
        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_tag_song_editor, null);
        builder.setView(dialogView);

        mTitleEditText = dialogView.findViewById(R.id.title);
        mArtistEditText = dialogView.findViewById(R.id.artist);
        mAlbumEditText = dialogView.findViewById(R.id.album);
        mGenreEditText = dialogView.findViewById(R.id.genre);
        mYearEditText = dialogView.findViewById(R.id.year);
        mTrackEditText = dialogView.findViewById(R.id.track_number);
        mDiscEditText = dialogView.findViewById(R.id.disc_number);
        mCommentEditText = dialogView.findViewById(R.id.comment);


        artWork = dialogView.findViewById(R.id.artwork);
        artWork.setOnClickListener(mOnClickListener);

        getValues();

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {

            dismiss();

            Intent intent = new Intent();

            if (fragSource.equals("AlbumFragment"))
                intent.setAction(SONG_TAG);
            else
                intent.setAction(LIST_SONG_TAG);

            intent.putExtra("song", mSong);

            intent.putExtra("title", mTitleEditText.getText().toString());
            intent.putExtra("artistName", mArtistEditText.getText().toString());
            intent.putExtra("albumName", mAlbumEditText.getText().toString());
            intent.putExtra("genre", mGenreEditText.getText().toString());
            intent.putExtra("year", mYearEditText.getText().toString());
            intent.putExtra("track", mTrackEditText.getText().toString());
            intent.putExtra("disc", mDiscEditText.getText().toString());
            intent.putExtra("comment", mCommentEditText.getText().toString());

            intent.putExtra("cover", newArtwork);

            getContext().sendBroadcast(intent);

           if (cover != null)
                cover.recycle();

        }).setNegativeButton(android.R.string.cancel, (dialog, which) -> dismiss());


        return builder.create();
    }

    private void getValues() {

        if (Locale.getDefault().toString().equals("fr_FR"))
            TagOptionSingleton.getInstance().setLanguage("fra");

        File file = new File(mSong.getPath());

        AudioFile audioFile = null;

        try {
            audioFile = AudioFileIO.read(file);
        } catch (Exception e) {
            Log.e(TAG, "Error on open file: " + e);
        }

        assert audioFile != null;

        Tag tag = audioFile.getTag();

        titre = tag.getFirst(FieldKey.TITLE);
        artist = tag.getFirst(FieldKey.ARTIST);
        album = tag.getFirst(FieldKey.ALBUM);
        genre = tag.getFirst(FieldKey.GENRE);
        year = tag.getFirst(FieldKey.YEAR);
        track = tag.getFirst(FieldKey.TRACK);
        discNo = tag.getFirst(FieldKey.DISC_NO);
        comment = tag.getFirst(FieldKey.COMMENT);

        try {
            byte[] bCover = tag.getFirstArtwork().getBinaryData();

            if (bCover != null)
                cover = BitmapHelper.byteToBitmap(tag.getFirstArtwork().getBinaryData());
        } catch (NullPointerException e) {
            Log.i(TAG, "Cover not found");
        }

        setEntrys();
    }

    private void setEntrys() {
        mTitleEditText.setText(titre);
        mArtistEditText.setText(artist);
        mAlbumEditText.setText(album);
        mGenreEditText.setText(genre);
        mYearEditText.setText(year);
        mTrackEditText.setText(track);
        mDiscEditText.setText(String.valueOf(discNo));

        mCommentEditText.setText(comment);

        artWork.setImageBitmap(cover);
    }


    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.artwork:
                    FilePickerDialog.with(getFragmentManager())
                            .onImageSelected(path -> {

                                Log.d(TAG, "result = " + path);

                                File imgFile = new  File(path);

                                if(imgFile.exists()){

                                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

                                    artWork.setImageBitmap(myBitmap);

                                    newArtwork = path;
                                }

                            }).show();
                    break;

                default:
                    break;
            }
        }
    };

}
