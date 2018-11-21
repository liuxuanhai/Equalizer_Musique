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
