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

package org.oucho.musicplayer.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;


public class ToolbarDrawerToggle implements DrawerLayout.DrawerListener {
    private DrawerLayout mDrawerLayout;
    private DrawerArrowDrawable mArrowDrawable;
    private int[] mGravities;


    public ToolbarDrawerToggle(Context context, DrawerLayout drawerLayout, Toolbar toolbar, int[] gravities) {
        init(context, drawerLayout, toolbar, gravities);
    }

    @SuppressLint("RtlHardcoded")
    private void init(final Context context, DrawerLayout drawerLayout, Toolbar toolbar, int[] gravities) {
        mDrawerLayout = drawerLayout;
        mArrowDrawable = new DrawerArrowDrawable(context);


        if (gravities == null) {
            mGravities = new int[]{Gravity.LEFT, Gravity.TOP, Gravity.RIGHT, Gravity.BOTTOM};
        } else {
            mGravities = gravities;
        }
        toolbar.setNavigationIcon(mArrowDrawable);
        toolbar.setNavigationOnClickListener(v -> toggleDrawers());
    }

    private void toggleDrawers() {
        for (int gravity : mGravities) {
            toggleDrawer(gravity);
        }
    }

    private void toggleDrawer(int gravity) {
        if (mDrawerLayout.isDrawerOpen(gravity)) {
            mDrawerLayout.closeDrawer(gravity);
        } else {
            mDrawerLayout.openDrawer(gravity);
        }
    }


    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {
        mArrowDrawable.setProgress(slideOffset);
    }

    @Override
    public void onDrawerOpened(View drawerView) {
        mArrowDrawable.setProgress(1.0f);
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        mArrowDrawable.setProgress(0.0f);
    }

    @Override
    public void onDrawerStateChanged(int newState) {
        // This constructor is intentionally empty, pourquoi ? parce que !
    }
}
