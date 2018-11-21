package org.oucho.musicplayer.view.chart.model;

import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;

import org.oucho.musicplayer.view.chart.util.Tools;

import static org.oucho.musicplayer.view.chart.util.Preconditions.checkNotNull;



@SuppressWarnings("UnusedReturnValue")
public class LineSet extends ChartSet {


    private static final int DEFAULT_COLOR = -16777216;

    private static final float LINE_THICKNESS = 4;


    private boolean mHasFill;
    private boolean mIsDashed;
    private boolean mIsSmooth;
    private boolean mHasGradientFill;

    private int mEnd;
    private int mBegin;
    private int mColor;
    private int mFillColor;

    private int[] mShadowColor;
    private int[] mGradientColors;

    private float mShadowDx;
    private float mShadowDy;
    private float mThickness;
    private float mShadowRadius;

    private float[] mDashedIntervals;
    private float[] mGradientPositions;


    public LineSet() {
        super();
        init();
    }


    private void init() {

        mThickness = Tools.fromDpToPx(LINE_THICKNESS);
        mColor = DEFAULT_COLOR;

        mIsDashed = false;
        mDashedIntervals = null;

        mIsSmooth = false;

        mHasFill = false;
        mFillColor = DEFAULT_COLOR;

        mHasGradientFill = false;
        mGradientColors = null;
        mGradientPositions = null;

        mBegin = 0;
        mEnd = 0;

        mShadowRadius = 0;
        mShadowDx = 0;
        mShadowDy = 0;
        mShadowColor = new int[4];
    }


    public void addPoint(String label, float value) {
        this.addPoint(new Point(label, value));
    }

    private void addPoint(@NonNull Point point) {
        this.addEntry(checkNotNull(point));
    }

    public boolean isDashed() {
        return mIsDashed;
    }

    public boolean isSmooth() {
        return mIsSmooth;
    }

    public LineSet setSmooth(boolean bool) {
        mIsSmooth = bool;
        return this;
    }

    public boolean hasFill() {
        return mHasFill;
    }

    public boolean hasGradientFill() {
        return mHasGradientFill;
    }

    public float getThickness() {
        return mThickness;
    }

    public LineSet setThickness(@FloatRange(from = 0.f) float thickness) {

        if (thickness < 0) throw new IllegalArgumentException("Line thickness can't be <= 0.");

        mThickness = thickness;
        return this;
    }

    public int getColor() {
        return mColor;
    }

    public LineSet setColor(@ColorInt int color) {
        mColor = color;
        return this;
    }

    public int getFillColor() {
        return mFillColor;
    }

    public int[] getGradientColors() {
        return mGradientColors;
    }

    public float[] getGradientPositions() {
        return mGradientPositions;
    }

    public int getBegin() {
        return mBegin;
    }

    public int getEnd() {
        if (mEnd == 0) return size();
        return mEnd;
    }

    public float[] getDashedIntervals() {
        return mDashedIntervals;
    }

    public int getDashedPhase() {
        return 0;
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

}
