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

import android.provider.BaseColumns;
import android.provider.MediaStore;


public final class SortOrder {

    // DESC = ordre inverse

    public interface ArtistSortOrder {
        String ARTIST_A_Z = MediaStore.Audio.Artists.DEFAULT_SORT_ORDER;
    }


    public interface AlbumSortOrder {

        //titre
        String ALBUM_A_Z = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER;

        //Ne pas prendre en compte "The" lors du tri des albums par artiste
        String ALBUM_ARTIST = "REPLACE ('<BEGIN>' || " + MediaStore.Audio.Albums.ARTIST +", '<BEGIN>The ', '<BEGIN>')";

        String ALBUM_YEAR = MediaStore.Audio.Albums.FIRST_YEAR + " DESC";

        String ALBUM_AJOUT = BaseColumns._ID + " DESC";

    }


    public interface SongSortOrder {
        // titre
        String SONG_A_Z = MediaStore.Audio.Media.TITLE;

        String SONG_ARTIST = "REPLACE ('<BEGIN>' || " + MediaStore.Audio.Media.ARTIST +", '<BEGIN>The ', '<BEGIN>')";
        
        String SONG_ALBUM = MediaStore.Audio.Media.ALBUM;

        String SONG_YEAR = MediaStore.Audio.Media.YEAR + " DESC";

        String SONG_ADD = MediaStore.Audio.Media._ID + " DESC";

    }

}
