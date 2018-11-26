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
import android.view.ViewGroup;


@SuppressWarnings("SameReturnValue")
public abstract class Adapter<V extends RecyclerView.ViewHolder> extends BaseAdapter<V> {

    @Override
    public V onCreateViewHolder(ViewGroup parent, int viewType) {

        return onCreateViewHolderImpl(parent);
    }

    @Override
    public void onBindViewHolder(V holder, int position) {

            onBindViewHolderImpl(holder, position);
    }

    @Override
    public int getItemCount() {
        return getItemCountImpl();
    }

    @Override
    public int getItemViewType(int position) {

        return getItemViewTypeImpl();
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    protected void triggerOnItemClickListener(int position, View view) {
        super.triggerOnItemClickListener(position, view);
    }

    protected abstract V onCreateViewHolderImpl(ViewGroup parent);

    protected abstract void onBindViewHolderImpl(V holder, int position);

    protected abstract int getItemCountImpl();

    protected abstract int getItemViewTypeImpl();

}
