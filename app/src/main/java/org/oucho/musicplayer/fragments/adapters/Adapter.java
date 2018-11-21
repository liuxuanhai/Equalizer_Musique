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
