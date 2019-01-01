package org.oucho.musicplayer.utils;

import org.oucho.musicplayer.db.model.Album;

import java.util.Comparator;

public class SortArtistByYear {




    public static class compareYear implements Comparator<Album> {

        public int compare(Album left, Album right) {
            int compare = 0;
            final int leftYear = left.getYear();
            final int rightYear = right.getYear();

            if (left.getArtistName().equalsIgnoreCase(right.getArtistName())) {
                if (leftYear < rightYear) {
                    compare = -1;
                } else if (leftYear > rightYear) {
                    compare = 1;
                }
            }

            return compare;
        }
    }
}
