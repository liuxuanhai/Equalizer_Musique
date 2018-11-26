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

package org.oucho.musicplayer.view.chart.model;

import android.support.annotation.NonNull;

import static org.oucho.musicplayer.view.chart.util.Preconditions.checkNotNull;


public abstract class ChartEntry implements Comparable<ChartEntry> {


    private static final int DEFAULT_COLOR = -16777216;

    boolean isVisible;

    private final String mLabel;

    private final int mColor;

    private float mX;
    private float mY;
    private float mValue;
    private final float mShadowDx;
    private final float mShadowDy;
    private final float mShadowRadius;

    private final int[] mShadowColor;


    ChartEntry(String label, float value) {

        mLabel = label;
        mValue = value;

        mColor = DEFAULT_COLOR;

        mShadowRadius = 0;
        mShadowDx = 0;
        mShadowDy = 0;
        mShadowColor = new int[4];
    }


    public boolean isVisible() {
        return isVisible;
    }


    public String getLabel() {
        return mLabel;
    }

    public float getValue() {
        return mValue;
    }

    public void setValue(float value) {
        mValue = value;
    }

    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }

    public int getColor() {
        return mColor;
    }

    public float getShadowRadius() {
        return mShadowRadius;
    }

    public float getShadowDx() {
        return mShadowDx;
    }

    public float getShadowDy() {
        return mShadowDy;
    }

    public int[] getShadowColor() {
        return mShadowColor;
    }

    public void setCoordinates(float x, float y) {
        mX = x;
        mY = y;
    }

    public String toString() {
        return "Label=" + mLabel + " \n" + "Value=" + mValue + "\n" + "X = " + mX + "\n" + "Y = " + mY;
    }

    public int compareTo(@NonNull ChartEntry other) {
        checkNotNull(other);
        return Float.compare(this.getValue(), other.getValue());
    }
}
