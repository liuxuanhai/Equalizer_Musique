package org.oucho.musicplayer.db.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

public class Album implements Parcelable {

    private final long id;
    private final String albumName;
    private final String artistName;
    private final int year;
    private final int trackCount;
    private final String cover;


    public Album(long id, String albumName, String artistName, int year, int trackCount, String cover) {
        super();
        this.id = id;
        this.albumName = albumName == null ? MediaStore.UNKNOWN_STRING : albumName;
        this.artistName = artistName == null ? MediaStore.UNKNOWN_STRING : artistName;
        this.year = year;
        this.trackCount = trackCount;
        this.cover = cover;
    }

    public long getId() {
        return id;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getArtistName() {
        return artistName;
    }

    public int getYear() {
        return year;
    }

    public int getTrackCount() {
        return trackCount;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.albumName);
        dest.writeString(this.artistName);
        dest.writeInt(this.year);
        dest.writeInt(this.trackCount);
        dest.writeString(this.cover);
    }

    private Album(Parcel in) {
        this.id = in.readLong();
        this.albumName = in.readString();
        this.artistName = in.readString();
        this.year = in.readInt();
        this.trackCount = in.readInt();
        this.cover = in.readString();
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Album> CREATOR = new Parcelable.Creator<Album>() {
        @Override
        public Album createFromParcel(Parcel source) {
            return new Album(source);
        }

        @Override
        public Album[] newArray(int size) {
            return new Album[size];
        }
    };
}
