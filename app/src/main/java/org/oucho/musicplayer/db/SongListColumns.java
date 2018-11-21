package org.oucho.musicplayer.db;

import android.provider.BaseColumns;


interface SongListColumns extends BaseColumns {

    String COLUMN_NAME_ALBUM = "album";
    String COLUMN_NAME_TITLE = "title";
    String COLUMN_NAME_GENRE = "genre";
    String COLUMN_NAME_ARTIST = "artist";
    String COLUMN_NAME_SONG_ID = "song_id";
    String COLUMN_NAME_ALBUM_ID = "album_id";
    String COLUMN_NAME_TRACK_NUMBER = "number";
    String COLUMN_NAME_TRACK_DURATION = "duration";
    String COLUMN_NAME_DATA = "data";


}
