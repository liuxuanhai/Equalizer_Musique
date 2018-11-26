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

package org.oucho.musicplayer.db.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

public class Song implements Parcelable {

    @SuppressWarnings("unused")
    private static final String TAG = "Song";

    private final long id;
    private final String title;
    private final String artist;
    private final String album;
    private final int trackNumber;
    private final int discNumber;
    private final long albumId;
    private final String genre;
    private final int duration;
    private final int year;
    private final String path;


    public Song(long id, String title, String artist, String album, long albumId, int trackNumber, int discNumber, int duration, int year, String genre, String path) {
        super();
        this.id = id;
        this.title = title == null ? MediaStore.UNKNOWN_STRING : title;
        this.artist = artist == null ? MediaStore.UNKNOWN_STRING : artist;
        this.album = album == null ? MediaStore.UNKNOWN_STRING : album;
        this.albumId = albumId;
        this.trackNumber = trackNumber;
        this.discNumber = discNumber;
        this.duration = duration;
        this.year = year;
        this.genre = genre;
        this.path = path;
    }

    public long getId() {
        return id;
    }

    public String getAlbum() {
        return album;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public long getAlbumId() {
        return albumId;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public int getDiscNumber() {
        return discNumber;
    }

    public String getGenre() {
        return genre;
    }

    public int getDuration() {
        return duration;
    }

    public int getYear() {
        return year;
    }

    public String getPath() {
        return path;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.title);
        dest.writeString(this.artist);
        dest.writeString(this.album);
        dest.writeInt(this.trackNumber);
        dest.writeInt(this.discNumber);
        dest.writeLong(this.albumId);
        dest.writeString(this.genre);
        dest.writeInt(this.duration);
        dest.writeInt(this.year);
        dest.writeString(this.path);
    }

    private Song(Parcel in) {
        this.id = in.readLong();
        this.title = in.readString();
        this.artist = in.readString();
        this.album = in.readString();
        this.trackNumber = in.readInt();
        this.discNumber = in.readInt();
        this.albumId = in.readLong();
        this.genre = in.readString();
        this.duration = in.readInt();
        this.year = in.readInt();
        this.path = in.readString();
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Song> CREATOR = new Parcelable.Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel source) {
            return new Song(source);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

}
