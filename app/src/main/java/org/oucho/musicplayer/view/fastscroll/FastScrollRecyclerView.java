package org.oucho.musicplayer.view.fastscroll;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.oucho.musicplayer.R;
import org.oucho.musicplayer.view.fastscroll.FastScroller.SectionIndexer;

@SuppressWarnings("unused")
public class FastScrollRecyclerView extends RecyclerView {

    private FastScroller mFastScroller;

    public FastScrollRecyclerView(Context context) {
        super(context);
        layout(context, null);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public FastScrollRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FastScrollRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        layout(context, attrs);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);

        if (adapter instanceof SectionIndexer) {
            setSectionIndexer((SectionIndexer) adapter);
        } else if (adapter == null) {
            setSectionIndexer(null);
        }
    }


    private void setSectionIndexer(SectionIndexer sectionIndexer) {
        mFastScroller.setSectionIndexer(sectionIndexer);
    }

    public void setFastScrollEnabled(boolean enabled) {
        mFastScroller.setEnabled(enabled);
    }

    public void setHideScrollbar(boolean hideScrollbar) {
        mFastScroller.setHideScrollbar(hideScrollbar);
    }

    public void setHandleColor(@ColorInt int color) {
        mFastScroller.setHandleColor(color);
    }

    public void setBubbleColor(@ColorInt int color) {
        mFastScroller.setBubbleColor(color);
    }

    public void setBubbleTextColor(@ColorInt int color) {
        mFastScroller.setBubbleTextColor(color);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFastScroller.attachRecyclerView(this);

        ViewParent parent = getParent();

        if (parent instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) parent;
            viewGroup.addView(mFastScroller);
            mFastScroller.setLayoutParams(viewGroup);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mFastScroller.detachRecyclerView();
        super.onDetachedFromWindow();
    }

    private void layout(Context context, AttributeSet attrs) {
        mFastScroller = new FastScroller(context, attrs);
        mFastScroller.setId(R.id.fastscroller);
    }
}
