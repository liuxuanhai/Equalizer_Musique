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

import android.graphics.drawable.Drawable;

import org.oucho.musicplayer.view.chart.util.Tools;


public class Point extends ChartEntry {

    private static final int DEFAULT_COLOR = -16777216;

    private final int mStrokeColor;

    private final boolean mHasStroke;

    private final float mRadius;
    private final float mStrokeThickness;
    private static final float DOTS_RADIUS = 3;
    private static final float DOTS_THICKNESS = 4;

    private final Drawable mDrawable;

    Point(String label, float value) {

        super(label, value);

        isVisible = false;

        mRadius = Tools.fromDpToPx(DOTS_THICKNESS);

        mHasStroke = false;
        mStrokeThickness = Tools.fromDpToPx(DOTS_RADIUS);
        mStrokeColor = DEFAULT_COLOR;

        mDrawable = null;
    }


    public boolean hasStroke() {
        return mHasStroke;
    }

    public float getStrokeThickness() {
        return mStrokeThickness;
    }

    public float getRadius() {
        return mRadius;
    }

    public int getStrokeColor() {
        return mStrokeColor;
    }

    public Drawable getDrawable() {
        return mDrawable;
    }

}
