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

public class Artist implements Parcelable {

    private final long id;
    private final String name;
    private final int albumCount;
    private final int trackCount;

    public Artist(long id, String name, int albumCount, int trackCount) {
        super();
        this.id = id;
        this.name = name == null ? MediaStore.UNKNOWN_STRING : name;
        this.albumCount = albumCount;
        this.trackCount = trackCount;
    }

    public String getName() {
        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.name);
        dest.writeInt(this.albumCount);
        dest.writeInt(this.trackCount);
    }

    private Artist(Parcel in) {
        this.id = in.readLong();
        this.name = in.readString();
        this.albumCount = in.readInt();
        this.trackCount = in.readInt();
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Artist> CREATOR = new Parcelable.Creator<Artist>() {
        @Override
        public Artist createFromParcel(Parcel source) {
            return new Artist(source);
        }

        @Override
        public Artist[] newArray(int size) {
            return new Artist[size];
        }
    };

}
