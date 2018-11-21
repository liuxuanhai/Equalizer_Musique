package org.oucho.musicplayer.fragments.adapters;


import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.services.PlayerService;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.tools.CustomSwipeAdapter;
import org.oucho.musicplayer.view.DragRecyclerView;

import java.util.Collections;
import java.util.List;

public class QueueAdapter extends BaseAdapter<QueueAdapter.QueueItemViewHolder> implements CustomSwipeAdapter {

    private final DragRecyclerView mQueueView;
    private final Context mContext;

    private List<Song> mQueue;
    private int mSelectedItemPosition = -1;

    public QueueAdapter(Context context, DragRecyclerView drag) {
        mContext = context;
        mQueueView = drag;

    }


    public void setQueue(List<Song> queue) {
        mQueue = queue;
        notifyDataSetChanged();
    }

    @Override
    public QueueItemViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.fragment_player_queue_item, parent, false);

        return new QueueItemViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(QueueItemViewHolder viewHolder, int position) {
        position = viewHolder.getAdapterPosition();

        Song song = mQueue.get(position);

        if (position == mSelectedItemPosition) {
            viewHolder.itemView.setSelected(true);

        } else {
            viewHolder.itemView.setSelected(false);
        }

        if (song.getId() == PlayerService.getSongID()) {
            viewHolder.vTitle.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));

        } else {
            viewHolder.vTitle.setTextColor(ContextCompat.getColor(mContext, R.color.grey_600));
        }

        viewHolder.vTitle.setText(song.getTitle());
        viewHolder.vArtist.setText(song.getArtist());
    }


    public void moveItem(int oldPosition, int newPosition) {
        if (oldPosition < 0 || oldPosition >= mQueue.size()
                || newPosition < 0 || newPosition >= mQueue.size()) {
            return;
        }

        Collections.swap(mQueue, oldPosition, newPosition);

        if (mSelectedItemPosition == oldPosition) {
            mSelectedItemPosition = newPosition;
        } else if (mSelectedItemPosition == newPosition) {
            mSelectedItemPosition = oldPosition;
        }
        notifyItemMoved(oldPosition, newPosition);
    }


    public void setSelection(int position) {
        int oldSelection = mSelectedItemPosition;
        mSelectedItemPosition = position;

        if (oldSelection >= 0 && oldSelection < mQueue.size()) {
            notifyItemChanged(oldSelection);
        }

        if (mSelectedItemPosition >= 0 && mSelectedItemPosition < mQueue.size()) {
            notifyItemChanged(mSelectedItemPosition);
        }
    }


    @Override
    public int getItemCount() {
        if (mQueue == null) {
            return 0;
        }
        return mQueue.size();
    }


    @Override
    public void onItemSwiped(int position) {
        mQueue.remove(position);
        notifyItemRemoved(position);
    }


    class QueueItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnTouchListener {

        final TextView vTitle;
        final TextView vArtist;
        final ImageButton vReorderButton;
        final View itemView;

        QueueItemViewHolder(View itemView) {
            super(itemView);
            vTitle = itemView.findViewById(R.id.title);
            vArtist = itemView.findViewById(R.id.artist);

            vReorderButton = itemView.findViewById(R.id.reorder_button);
            vReorderButton.setOnTouchListener(this);

            itemView.findViewById(R.id.song_info).setOnClickListener(this);

            itemView.setOnClickListener(this);

            this.itemView = itemView;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mQueueView.startDrag(itemView);
            return false;
        }

        @Override
        public void onClick(View v) {
            final int position = getAdapterPosition();

            triggerOnItemClickListener(position, v);

            if (MainActivity.getPlayerService() != null) {

                switch (v.getId()) {
                    case R.id.song_info:
                        MainActivity.getPlayerService().setPosition(position, true);
                        break;
                    default:
                        break;
                }
            }
        }
    }

}
