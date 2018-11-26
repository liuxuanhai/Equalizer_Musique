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

package org.oucho.musicplayer.view;


import android.content.Context;
import android.graphics.PointF;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;


public class CustomLayoutManager extends LinearLayoutManager {
    private static final float MILLISECONDS_PER_INCH = 120f;
    private final Context mContext;

    public CustomLayoutManager(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, final int position) {

        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(mContext) {

            //This controls the direction in which smoothScroll looks
            //for your view
            @Override
            public PointF computeScrollVectorForPosition
            (int targetPosition) {
                return CustomLayoutManager.this.computeScrollVectorForPosition(targetPosition);
            }

            //This returns the milliseconds it takes to
            //scroll one pixel.
            @Override
            protected float calculateSpeedPerPixel (DisplayMetrics displayMetrics) {
                return MILLISECONDS_PER_INCH/displayMetrics.densityDpi;
            }
        };

        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }

    // Usage:

    //mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    //mLayoutManager = new CustomLayoutManager(getActivity());
    //mRecyclerView.setLayoutManager(mLayoutManager);
}

