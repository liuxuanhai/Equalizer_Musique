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
import android.net.Uri;
import android.provider.MediaStore;

import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.utils.Permissions;

import java.util.ArrayList;
import java.util.List;


public class PlaylistLoader extends BaseLoader<List<Song>> {
    private static final String[] sProjection = {
            MediaStore.Audio.Playlists.Members.AUDIO_ID,
            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA};

    private final long mPlaylistId;

    public PlaylistLoader(Context context, long playlistId) {
        super(context);
        mPlaylistId = playlistId;
    }

    @Override
    public List<Song> loadInBackground() {

        List<Song> playlist = new ArrayList<>();

        Cursor cursor = getPlaylistCursor();

        if (cursor != null && cursor.moveToFirst()) {
            int idCol = cursor
                    .getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID);
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
            int pathCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);


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

                playlist.add(new Song(id, title, artist, album, albumId, track, 0, duration, year, null, path));

            } while (cursor.moveToNext());

        }

        if (cursor != null)
            cursor.close();



        return playlist;
    }

    private Cursor getPlaylistCursor() {
        if (!Permissions.checkPermission(getContext())) {
            return null;
        }
        Uri musicUri = MediaStore.Audio.Playlists.Members.getContentUri( "external", mPlaylistId);

        return getContext().getContentResolver().query(musicUri,
                sProjection, getSelectionString(), getSelectionArgs(),
                MediaStore.Audio.Playlists.Members.PLAY_ORDER);
    }
}
