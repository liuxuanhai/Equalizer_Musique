package org.oucho.musicplayer.view.fastscroll;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.R;

import static org.oucho.musicplayer.MusiqueKeys.FICHIER_PREFS;

public class FastScroller extends LinearLayout {

    public interface SectionIndexer {

        String getSectionText(int position);
    }

    private static final int sBubbleAnimDuration = 100;
    private static final int sScrollbarHideDelay = 1000;
    private static final int sScrollbarAnimDuration = 300;

    @ColorInt private int mBubbleColor;
    @ColorInt private int mHandleColor;

    private int mHeight;
    private View mScrollbar;
    private TextView mBubbleView;
    private ImageView mHandleView;
    private Drawable mBubbleImage;
    private Drawable mHandleImage;
    private boolean mHideScrollbar;
    private RecyclerView mRecyclerView;
    private SectionIndexer mSectionIndexer;
    private ViewPropertyAnimator mScrollbarAnimator;
    private ViewPropertyAnimator mBubbleAnimator;

    private final Runnable mScrollbarHider = this::hideScrollbar;

    private final RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (!mHandleView.isSelected() && isEnabled()) {
                setViewPositions(getScrollProportion(recyclerView));
            }
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (isEnabled()) {
                switch (newState) {
                case RecyclerView.SCROLL_STATE_DRAGGING:
                    getHandler().removeCallbacks(mScrollbarHider);
                    cancelAnimation(mScrollbarAnimator);

                    if (!isViewVisible(mScrollbar)) {
                        showScrollbar();
                    }

                    break;

                case RecyclerView.SCROLL_STATE_IDLE:
                    if (mHideScrollbar && !mHandleView.isSelected()) {
                        getHandler().postDelayed(mScrollbarHider, sScrollbarHideDelay);
                    }

                    break;
                default:
                    break;
                }
            }
        }
    };

    public FastScroller(Context context) {
        super(context);
        layout(context, null);
        setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
    }

    public FastScroller(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        layout(context, attrs);
        setLayoutParams(generateLayoutParams(attrs));
    }

    @Override
    public void setLayoutParams(@NonNull ViewGroup.LayoutParams params) {
        params.width = LayoutParams.WRAP_CONTENT;
        super.setLayoutParams(params);
    }

    public void setLayoutParams(@NonNull ViewGroup viewGroup) {
        @IdRes int recyclerViewId = mRecyclerView.getId();

        if (recyclerViewId == NO_ID) {
            throw new IllegalArgumentException("RecyclerView must have a view ID");
        }

        if (viewGroup instanceof ConstraintLayout) {
            ConstraintSet constraintSet = new ConstraintSet();
            @IdRes int layoutId = getId();

            constraintSet.connect(layoutId, ConstraintSet.TOP, recyclerViewId, ConstraintSet.TOP);
            constraintSet.connect(layoutId, ConstraintSet.BOTTOM, recyclerViewId, ConstraintSet.BOTTOM);
            constraintSet.connect(layoutId, ConstraintSet.END, recyclerViewId, ConstraintSet.END);
            constraintSet.applyTo((ConstraintLayout) viewGroup);

        } else if (viewGroup instanceof CoordinatorLayout) {
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) getLayoutParams();

            layoutParams.setAnchorId(recyclerViewId);
            layoutParams.anchorGravity = GravityCompat.END;
            setLayoutParams(layoutParams);

        } else if (viewGroup instanceof FrameLayout) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();

            layoutParams.gravity = GravityCompat.END;
            setLayoutParams(layoutParams);

        } else if (viewGroup instanceof RelativeLayout) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) getLayoutParams();
            int endRule = RelativeLayout.ALIGN_END;

            layoutParams.addRule(RelativeLayout.ALIGN_TOP, recyclerViewId);
            layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, recyclerViewId);
            layoutParams.addRule(endRule, recyclerViewId);
            setLayoutParams(layoutParams);

        } else {
            throw new IllegalArgumentException("Parent ViewGroup must be a ConstraintLayout, CoordinatorLayout, FrameLayout, or RelativeLayout");
        }
    }

    public void setSectionIndexer(SectionIndexer sectionIndexer) {
        mSectionIndexer = sectionIndexer;
    }

    public void attachRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;

        if (mRecyclerView != null) {
            mRecyclerView.addOnScrollListener(mScrollListener);
        }
    }

    public void detachRecyclerView() {
        if (mRecyclerView != null) {
            mRecyclerView.removeOnScrollListener(mScrollListener);
            mRecyclerView = null;
        }
    }


    public void setHideScrollbar(boolean hideScrollbar) {
        mHideScrollbar = hideScrollbar;
        mScrollbar.setVisibility(hideScrollbar ? GONE : VISIBLE);
    }


    public void setHandleColor(@ColorInt int color) {
        mHandleColor = color;

        if (mHandleImage == null) {
            mHandleImage = DrawableCompat.wrap(ContextCompat.getDrawable(getContext(), R.drawable.fastscroll_handle));
            mHandleImage.mutate();
        }

        DrawableCompat.setTint(mHandleImage, mHandleColor);
        mHandleView.setImageDrawable(mHandleImage);
    }


    public void setBubbleColor(@ColorInt int color) {
        mBubbleColor = color;

        if (mBubbleImage == null) {
            mBubbleImage = DrawableCompat.wrap(ContextCompat.getDrawable(getContext(), R.drawable.fastscroll_bubble));
            mBubbleImage.mutate();
        }

        DrawableCompat.setTint(mBubbleImage, mBubbleColor);
        mBubbleView.setBackground(mBubbleImage);
    }


    public void setBubbleTextColor(@ColorInt int color) {
        mBubbleView.setTextColor(color);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setVisibility(enabled ? VISIBLE : GONE);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {


        SharedPreferences préférences = getContext().getSharedPreferences(FICHIER_PREFS, Context.MODE_PRIVATE);
        String getTriSong = préférences.getString("song_sort_order", "");
        String getTriAlbum = préférences.getString("album_sort_order", "");
        int currentView = MainActivity.getViewID();
        boolean songByID = currentView == R.id.fragment_song_layout;
        boolean albumByID = currentView == R.id.fragment_album_list_layout;

        boolean voirBubulle = true;

        if (albumByID && ("_id DESC".equals(getTriAlbum)))
            voirBubulle = false;

        if (songByID && ("_id DESC".equals(getTriSong)))
            voirBubulle = false;


        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if (event.getX() < mHandleView.getX() - ViewCompat.getPaddingStart(mHandleView)) {
                return false;
            }

            setHandleSelected(true);

            getHandler().removeCallbacks(mScrollbarHider);
            cancelAnimation(mScrollbarAnimator);
            cancelAnimation(mBubbleAnimator);

            if (!isViewVisible(mScrollbar)) {
                showScrollbar();
            }

            if (mSectionIndexer != null && !isViewVisible(mBubbleView) && voirBubulle ) {
                showBubble();
            }

            return true;
        case MotionEvent.ACTION_MOVE:
            final float y = event.getY();
            setViewPositions(y);
            setRecyclerViewPosition(y);
            return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            setHandleSelected(false);

            if (mHideScrollbar)
                getHandler().postDelayed(mScrollbarHider, sScrollbarHideDelay);

            if (isViewVisible(mBubbleView))
                hideBubble();

            return true;
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = h;
    }

    private void setRecyclerViewPosition(float y) {
        if (mRecyclerView != null && mRecyclerView.getAdapter() != null) {
            int itemCount = mRecyclerView.getAdapter().getItemCount();
            float proportion;

            if (mHandleView.getY() == 0) {
                proportion = 0f;
            } else if (mHandleView.getY() + mHandleView.getHeight() >= mHeight) {
                proportion = 1f;
            } else {
                proportion = y / (float) mHeight;
            }

            int targetPos = getValueInRange(itemCount - 1, (int) (proportion * (float) itemCount));
            mRecyclerView.getLayoutManager().scrollToPosition(targetPos);

            if (mSectionIndexer != null) {
                mBubbleView.setText(mSectionIndexer.getSectionText(targetPos));
            }
        }
    }

    private float getScrollProportion(RecyclerView recyclerView) {
        final int verticalScrollOffset = recyclerView.computeVerticalScrollOffset();
        final int verticalScrollRange = recyclerView.computeVerticalScrollRange();
        float proportion = (float) verticalScrollOffset / ((float) verticalScrollRange - mHeight);
        return mHeight * proportion;
    }

    private int getValueInRange(int max, int value) {
        int minimum = Math.max(0, value);
        return Math.min(minimum, max);
    }

    private void setViewPositions(float y) {
        int bubbleHeight = mBubbleView.getHeight();
        int handleHeight = mHandleView.getHeight();

        mBubbleView.setY(getValueInRange(mHeight - bubbleHeight - handleHeight / 2, (int) (y - bubbleHeight)));
        mHandleView.setY(getValueInRange(mHeight - handleHeight, (int) (y - (float) handleHeight / 2)));
    }

    private boolean isViewVisible(View view) {
        return view != null && view.getVisibility() == VISIBLE;
    }

    private void cancelAnimation(ViewPropertyAnimator animator) {
        if (animator != null) {
            animator.cancel();
        }
    }

    private void showBubble() {
        mBubbleView.setVisibility(VISIBLE);
        mBubbleAnimator = mBubbleView.animate().alpha(1f)
                .setDuration(sBubbleAnimDuration)
                .setListener(new AnimatorListenerAdapter() {
                });
    }

    private void hideBubble() {
        mBubbleAnimator = mBubbleView.animate().alpha(0f)
                .setDuration(sBubbleAnimDuration)
                .setListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mBubbleView.setVisibility(GONE);
                        mBubbleAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        mBubbleView.setVisibility(GONE);
                        mBubbleAnimator = null;
                    }
                });
    }

    private void showScrollbar() {
        if (mRecyclerView.computeVerticalScrollRange() - mHeight > 0) {
            float transX = getResources().getDimensionPixelSize(R.dimen.fastscroll_scrollbar_padding);

            mScrollbar.setTranslationX(transX);
            mScrollbar.setVisibility(VISIBLE);
            mScrollbarAnimator = mScrollbar.animate().translationX(0f).alpha(1f)
                    .setDuration(sScrollbarAnimDuration)
                    .setListener(new AnimatorListenerAdapter() {
                        // adapter required for new alpha value to stick
                    });
        }
    }

    private void hideScrollbar() {
        float transX = getResources().getDimensionPixelSize(R.dimen.fastscroll_scrollbar_padding);

        mScrollbarAnimator = mScrollbar.animate().translationX(transX).alpha(0f)
                .setDuration(sScrollbarAnimDuration)
                .setListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mScrollbar.setVisibility(GONE);
                        mScrollbarAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        mScrollbar.setVisibility(GONE);
                        mScrollbarAnimator = null;
                    }
                });
    }

    private void setHandleSelected(boolean selected) {
        mHandleView.setSelected(selected);
        DrawableCompat.setTint(mHandleImage, selected ? mBubbleColor : mHandleColor);
    }

    private void layout(Context context, AttributeSet attrs) {
        inflate(context, R.layout.fastscroller, this);

        setClipChildren(false);
        setOrientation(HORIZONTAL);

        mBubbleView = findViewById(R.id.fastscroll_bubble);
        mHandleView = findViewById(R.id.fastscroll_handle);
        mScrollbar = findViewById(R.id.fastscroll_scrollbar);

        @ColorInt int bubbleColor = Color.GRAY;
        @ColorInt int handleColor = Color.DKGRAY;
        @ColorInt int textColor = Color.WHITE;

        boolean hideScrollbar = true;

        if (attrs != null) {
            @SuppressLint("CustomViewStyleable")
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FastScrollRecyclerView, 0, 0);

            if (typedArray != null) {
                try {
                    bubbleColor = typedArray.getColor(R.styleable.FastScrollRecyclerView_bubbleColor, bubbleColor);
                    handleColor = typedArray.getColor(R.styleable.FastScrollRecyclerView_handleColor, handleColor);
                    textColor = typedArray.getColor(R.styleable.FastScrollRecyclerView_bubbleTextColor, textColor);
                    hideScrollbar = typedArray.getBoolean(R.styleable.FastScrollRecyclerView_hideScrollbar, true);
                } finally {
                    typedArray.recycle();
                }
            }
        }

        setHandleColor(handleColor);
        setBubbleColor(bubbleColor);
        setBubbleTextColor(textColor);
        setHideScrollbar(hideScrollbar);
    }
}
