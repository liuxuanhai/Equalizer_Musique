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

package org.oucho.musicplayer.fragments.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;


//@SuppressWarnings("WeakerAccess")
public abstract class BaseAdapter<V extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<V> {

    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    protected void triggerOnItemClickListener(int position, View view) {
        if(mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(position, view);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position, View view);
    }


    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        mOnItemLongClickListener = listener;
    }

    void triggerOnItemLongClickListener(int position, View view) {
        if(mOnItemLongClickListener != null) {
            mOnItemLongClickListener.onItemLongClick(position, view);
        }
    }

    @SuppressWarnings("unused")
    public interface OnItemLongClickListener {
        void onItemLongClick(int position, View view);
    }

}
