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

package org.oucho.musicplayer.fragments.loaders;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.MediaStore;

import org.oucho.musicplayer.db.model.Song;

import java.util.ArrayList;
import java.util.List;


public class SongLoader extends BaseLoader<List<Song>> {

    @SuppressWarnings("unused")
    private static final String TAG_LOG = "SongLoader";

    private static final String[] sProjection = {MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA};

    public SongLoader(Context context) {
        super(context);

    }

    @Override
    public List<Song> loadInBackground() {
        List<Song> mSongList = new ArrayList<>();

        Cursor cursor = getSongCursor();

        if (cursor != null && cursor.moveToFirst()) {
            int idCol = cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID);
            if (idCol == -1) {
                idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            }

            int titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK);

            int trackDur = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

            int yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR);

            int pathCol  = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);


            do {

                long id = cursor.getLong(idCol);
                String title = cursor.getString(titleCol);

                String artist = cursor.getString(artistCol);

                String album = cursor.getString(albumCol);

                long albumId = cursor.getLong(albumIdCol);

                int track = cursor.getInt(trackCol);

                int duration = cursor.getInt(trackDur);

                int year = cursor.getInt(yearCol);

                String path = cursor.getString(pathCol);

                int disc = 0;
                String trackNb = String.valueOf(track);

                if (track >= 1000 && track < 10000) {

                    track = Integer.valueOf(trackNb.substring(1)); // remove first char
                    disc = Integer.valueOf(trackNb.substring(0,1)); // get char 1

                } else if (track >= 10000 && track < 100000) {

                    track = Integer.valueOf(trackNb.substring(2)); // remove 2 first chars
                    disc = Integer.valueOf(trackNb.substring(0,2)); // get char 1 & 2
                }

                // ne pas integrer recher genre, trop long à charger
                mSongList.add(new Song(id, title, artist, album, albumId, track, disc, duration, year, "unknow", path));
            } while (cursor.moveToNext());

        }

        if (cursor != null)
            cursor.close();

        return mSongList;
    }


    private Cursor getSongCursor() {

        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String selection = getSelectionString();
        String[] selectionArgs = getSelectionArgs();

        selection = DatabaseUtils.concatenateWhere(selection, MediaStore.Audio.Media.IS_MUSIC+" = 1");

        String fieldName = MediaStore.Audio.Media.TITLE;
        String filter = getFilter();
        return getCursor(musicUri, sProjection, selection, selectionArgs, fieldName, filter);
    }

}