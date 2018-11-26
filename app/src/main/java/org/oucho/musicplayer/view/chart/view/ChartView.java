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

package org.oucho.musicplayer.view.chart.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Typeface;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.RelativeLayout;

import org.oucho.musicplayer.R;
import org.oucho.musicplayer.view.chart.model.ChartSet;
import org.oucho.musicplayer.view.chart.renderer.AxisRenderer;
import org.oucho.musicplayer.view.chart.renderer.XRenderer;
import org.oucho.musicplayer.view.chart.renderer.YRenderer;

import java.text.DecimalFormat;
import java.util.ArrayList;

import static org.oucho.musicplayer.view.chart.util.Preconditions.checkNotNull;


@SuppressWarnings("UnusedReturnValue")
public abstract class ChartView extends RelativeLayout {

    private static final String TAG = "chart.view.ChartView";

    private static final int DEFAULT_WIDTH = 200;

    private static final int DEFAULT_HEIGHT = 100;

    private final XRenderer xRndr;

    private final YRenderer yRndr;

    private final Style style;

    private ArrayList<ChartSet> data;

    private Orientation mOrientation;

    private ArrayList<Float> mThresholdStartValues;

    private ArrayList<Float> mThresholdEndValues;

    private ArrayList<ArrayList<Region>> mRegions;

    private final GestureDetector mGestureDetector;

    private OnClickListener mChartListener;

    private boolean mReadyToDraw;

    private final OnPreDrawListener drawListener = new OnPreDrawListener() {
        @SuppressLint("NewApi")
        @Override
        public boolean onPreDraw() {

            ChartView.this.getViewTreeObserver().removeOnPreDrawListener(this);

            style.init();

            yRndr.init(data, style);
            xRndr.init(data, style);

            int mChartLeft = getPaddingLeft();
            int mChartTop = getPaddingTop() + style.fontMaxHeight / 2;
            int mChartRight = getMeasuredWidth() - getPaddingRight();
            int mChartBottom = getMeasuredHeight() - getPaddingBottom();

            yRndr.measure(mChartLeft, mChartTop, mChartRight, mChartBottom);
            xRndr.measure(mChartLeft, mChartTop, mChartRight, mChartBottom);

            // Negotiate chart inner boundaries.
            // Both renderers may require different space to draw axis stuff.
            final float[] bounds = negotiateInnerChartBounds(yRndr.getInnerChartBounds(), xRndr.getInnerChartBounds());
            yRndr.setInnerChartBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
            xRndr.setInnerChartBounds(bounds[0], bounds[1], bounds[2], bounds[3]);

            // Dispose the various axis elements in their positions
            yRndr.dispose();
            xRndr.dispose();

            // Parse threshold screen coordinates
            if (!mThresholdStartValues.isEmpty()) {
                for (int i = 0; i < mThresholdStartValues.size(); i++) {
                    mThresholdStartValues.set(i, yRndr.parsePos(0, mThresholdStartValues.get(i)));
                    mThresholdEndValues.set(i, yRndr.parsePos(0, mThresholdEndValues.get(i)));
                }
            }

            // Process data to define screen coordinates
            digestData();

            // In case Views extending ChartView need to pre process data before the onDraw
            //onPreDrawChart();

            // Define entries regions
            if (mRegions.isEmpty()) {
                int dataSize = data.size();
                int setSize;
                mRegions = new ArrayList<>(dataSize);
                ArrayList<Region> regionSet;

                for (int i = 0; i < dataSize; i++) {
                    setSize = data.get(0).size();
                    regionSet = new ArrayList<>(setSize);

                    for (int j = 0; j < setSize; j++)
                        regionSet.add(new Region());

                    mRegions.add(regionSet);
                }
            }
            defineRegions(mRegions, data);

            ChartView.this.setLayerType(LAYER_TYPE_SOFTWARE, null);

            return mReadyToDraw = true;
        }
    };


    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
        mGestureDetector = new GestureDetector(context, new GestureListener());
        xRndr = new XRenderer();
        yRndr = new YRenderer();
        style = new Style(context, attrs);
    }


    public ChartView(Context context) {
        super(context);

        init();
        mGestureDetector = new GestureDetector(context, new GestureListener());
        xRndr = new XRenderer();
        yRndr = new YRenderer();
        style = new Style(context);
    }

    private void init() {

        mReadyToDraw = false;
        mThresholdStartValues = new ArrayList<>();
        mThresholdEndValues = new ArrayList<>();
        data = new ArrayList<>();
        mRegions = new ArrayList<>();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        this.setWillNotDraw(false);
        style.init();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        style.clean();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int tmpWidth = widthMeasureSpec;
        int tmpHeight = heightMeasureSpec;

        if (widthMode == MeasureSpec.AT_MOST) tmpWidth = DEFAULT_WIDTH;

        if (heightMode == MeasureSpec.AT_MOST) tmpHeight = DEFAULT_HEIGHT;

        setMeasuredDimension(tmpWidth, tmpHeight);
    }


    /**
     * The method listens for chart clicks and checks whether it intercepts
     * a known Region. It will then use the registered Listener.onClick
     * to return the region's index.
     */
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {

        super.onTouchEvent(event);
        return mChartListener != null && mGestureDetector.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mReadyToDraw) {

            // Draw grid
            if (style.hasVerticalGrid()) drawVerticalGrid(canvas);
            if (style.hasHorizontalGrid()) drawHorizontalGrid(canvas);


            // Draw data
            if (!data.isEmpty())
                onDrawChart(canvas, data);

            // Draw Axis Y
            yRndr.draw(canvas);

            // Draw axis X
            xRndr.draw(canvas);

        }
    }


    private void digestData() {

        int nEntries = data.get(0).size();
        for (ChartSet set : data) {
            for (int i = 0; i < nEntries; i++) {
                set.getEntry(i).setCoordinates(xRndr.parsePos(i, set.getValue(i)), yRndr.parsePos(i, set.getValue(i)));
            }
        }
    }


    void defineRegions(ArrayList<ArrayList<Region>> regions, ArrayList<ChartSet> data) {}

    protected abstract void onDrawChart(Canvas canvas, ArrayList<ChartSet> data);

    public void addData(@NonNull ChartSet set) {

        checkNotNull(set);

        if (!data.isEmpty() && set.size() != data.get(0).size())
            throw new IllegalArgumentException("The number of entries between sets doesn't match.");

        data.add(set);
    }

    public void show() {
        this.getViewTreeObserver().addOnPreDrawListener(drawListener);
        postInvalidate();
    }

    public void notifyDataUpdate() {

        // Ignore update if chart is not even ready to draw or if it is still animating
        if (mReadyToDraw) {

            digestData();
            defineRegions(mRegions, data);
            invalidate();

        } else {
            Log.w(TAG, "Unexpected data update notification. " + "Chart is still not displayed or still displaying.");
        }

    }


    private float[] negotiateInnerChartBounds(float[] innersA, float[] innersB) {

        return new float[]{(innersA[0] > innersB[0]) ? innersA[0] : innersB[0],
                (innersA[1] > innersB[1]) ? innersA[1] : innersB[1],
                (innersA[2] < innersB[2]) ? innersA[2] : innersB[2],
                (innersA[3] < innersB[3]) ? innersA[3] : innersB[3]};
    }


    private void drawVerticalGrid(Canvas canvas) {

        final float offset = (getInnerChartRight() - getInnerChartLeft()) / style.gridColumns;
        float marker = getInnerChartLeft();

        if (style.hasYAxis)
            marker += offset;

        while (marker < getInnerChartRight()) {
            canvas.drawLine(marker, getInnerChartTop(), marker, getInnerChartBottom(), style.gridPaint);
            marker += offset;
        }

        canvas.drawLine(getInnerChartRight(), getInnerChartTop(), getInnerChartRight(), getInnerChartBottom(), style.gridPaint);
    }


    private void drawHorizontalGrid(Canvas canvas) {

        final float offset = (getInnerChartBottom() - getInnerChartTop()) / style.gridRows;
        float marker = getInnerChartTop();

        while (marker < getInnerChartBottom()) {
            canvas.drawLine(getInnerChartLeft(), marker, getInnerChartRight(), marker, style.gridPaint);
            marker += offset;
        }

        if (!style.hasXAxis)
            canvas.drawLine(getInnerChartLeft(), getInnerChartBottom(), getInnerChartRight(), getInnerChartBottom(), style.gridPaint);
    }


    void setOrientation() {

        mOrientation = checkNotNull(Orientation.VERTICAL);
        if (mOrientation == Orientation.VERTICAL) {
            yRndr.setHandleValues(true);
        } else {
            xRndr.setHandleValues(true);
        }
    }

    float getInnerChartBottom() {
        return yRndr.getInnerChartBottom();
    }

    float getInnerChartLeft() {
        return xRndr.getInnerChartLeft();
    }

    private float getInnerChartRight() {
        return xRndr.getInnerChartRight();
    }

    float getInnerChartTop() {
        return yRndr.getInnerChartTop();
    }

    public ChartView setYLabels() {
        style.yLabelsPositioning = checkNotNull(AxisRenderer.LabelPosition.NONE);
        return this;
    }

    public ChartView setXLabels() {
        style.xLabelsPositioning = checkNotNull(AxisRenderer.LabelPosition.NONE);
        return this;
    }

    public ChartView setXAxis(boolean bool) {
        style.hasXAxis = bool;
        return this;
    }

    public ChartView setYAxis(boolean bool) {
        style.hasYAxis = bool;
        return this;
    }

    public ChartView setAxisBorderValues(float minValue, float maxValue) {

        if (mOrientation == Orientation.VERTICAL)
            yRndr.setBorderValues(minValue, maxValue);
        else
            xRndr.setBorderValues(minValue, maxValue);

        return this;
    }


    @Override
    public void setOnClickListener(OnClickListener listener) {
        this.mChartListener = listener;
    }


    public ChartView setGrid(@IntRange(from = 0) int rows, @IntRange(from = 0) int columns, @NonNull Paint paint) {

        if (rows < 0 || columns < 0)
            throw new IllegalArgumentException("Number of rows/columns can't be smaller than 0.");

        style.gridRows = rows;
        style.gridColumns = columns;
        style.gridPaint = checkNotNull(paint);
        return this;
    }


    void applyShadow(Paint paint, float alpha, float dx, float dy, float radius, int[] color) {

        paint.setAlpha((int) (alpha * Style.FULL_ALPHA));
        paint.setShadowLayer(radius, dx, dy, Color.argb(((int) (alpha * Style.FULL_ALPHA) < color[0]) ? (int) (alpha * Style.FULL_ALPHA) : color[0], color[1], color[2], color[3]));
    }


    public enum Orientation {
        VERTICAL
    }


    public class Style {

        static final int FULL_ALPHA = 255;

        private static final int DEFAULT_COLOR = Color.BLACK;

        private static final int DEFAULT_GRID_OFF = 0;

        /**
         * Chart
         */
        private Paint chartPaint;

        /**
         * Axis
         */
        private boolean hasXAxis;

        private boolean hasYAxis;

        private final float axisThickness;

        private final int axisColor;

        /**
         * Distance between axis and label
         */
        private final int axisLabelsSpacing;

        /**
         * Spacing between axis labels and chart sides
         */
        private final int axisBorderSpacing;

        /**
         * Spacing between chart top and axis label
         */
        private final int axisTopSpacing;

        /**
         * Grid
         */
        private Paint gridPaint;

        /**
         * Font
         */
        private AxisRenderer.LabelPosition xLabelsPositioning;

        private AxisRenderer.LabelPosition yLabelsPositioning;

        private Paint labelsPaint;

        private final int labelsColor;

        private final float fontSize;

        private Typeface typeface;

        /**
         * Height of the text based on the font style defined.
         * Includes uppercase height and bottom padding of special
         * lowercase letter such as g, p, etc.
         */
        private int fontMaxHeight;

        private int gridRows;

        private int gridColumns;

        /**
         * Labels Metric to draw together with labels.
         */
        private final DecimalFormat labelsFormat;


        Style(Context context) {

            axisColor = DEFAULT_COLOR;
            axisThickness = context.getResources().getDimension(R.dimen.grid_thickness);
            hasXAxis = true;
            hasYAxis = true;

            xLabelsPositioning = AxisRenderer.LabelPosition.OUTSIDE;
            yLabelsPositioning = AxisRenderer.LabelPosition.OUTSIDE;
            labelsColor = DEFAULT_COLOR;
            fontSize = context.getResources().getDimension(R.dimen.font_size);

            axisLabelsSpacing = context.getResources().getDimensionPixelSize(R.dimen.axis_labels_spacing);
            axisBorderSpacing = context.getResources().getDimensionPixelSize(R.dimen.axis_border_spacing);
            axisTopSpacing = context.getResources().getDimensionPixelSize(R.dimen.axis_top_spacing);

            gridRows = DEFAULT_GRID_OFF;
            gridColumns = DEFAULT_GRID_OFF;

            labelsFormat = new DecimalFormat();
        }


        Style(Context context, AttributeSet attrs) {

            TypedArray arr = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ChartAttrs, 0, 0);

            hasXAxis = arr.getBoolean(R.styleable.ChartAttrs_chart_axis, true);
            hasYAxis = arr.getBoolean(R.styleable.ChartAttrs_chart_axis, true);
            axisColor = arr.getColor(R.styleable.ChartAttrs_chart_axisColor, DEFAULT_COLOR);
            axisThickness = arr.getDimension(R.styleable.ChartAttrs_chart_axisThickness,
                    context.getResources().getDimension(R.dimen.axis_thickness));

            switch (arr.getInt(R.styleable.ChartAttrs_chart_labels, 0)) {
                case 1:
                    xLabelsPositioning = AxisRenderer.LabelPosition.INSIDE;
                    yLabelsPositioning = AxisRenderer.LabelPosition.INSIDE;
                    break;
                case 2:
                    xLabelsPositioning = AxisRenderer.LabelPosition.NONE;
                    yLabelsPositioning = AxisRenderer.LabelPosition.NONE;
                    break;
                default:
                    xLabelsPositioning = AxisRenderer.LabelPosition.OUTSIDE;
                    yLabelsPositioning = AxisRenderer.LabelPosition.OUTSIDE;
                    break;
            }

            labelsColor = arr.getColor(R.styleable.ChartAttrs_chart_labelColor, DEFAULT_COLOR);

            fontSize = arr.getDimension(R.styleable.ChartAttrs_chart_fontSize,
                    context.getResources().getDimension(R.dimen.font_size));

            String typefaceName = arr.getString(R.styleable.ChartAttrs_chart_typeface);
            if (typefaceName != null) typeface = Typeface.createFromAsset(getResources()
                    .getAssets(), typefaceName);

            axisLabelsSpacing = arr.getDimensionPixelSize(R.styleable.ChartAttrs_chart_axisLabelsSpacing,
                    context.getResources().getDimensionPixelSize(R.dimen.axis_labels_spacing));
            axisBorderSpacing = arr.getDimensionPixelSize(R.styleable.ChartAttrs_chart_axisBorderSpacing,
                    context.getResources().getDimensionPixelSize(R.dimen.axis_border_spacing));
            axisTopSpacing = arr.getDimensionPixelSize(R.styleable.ChartAttrs_chart_axisTopSpacing,
                    context.getResources().getDimensionPixelSize(R.dimen.axis_top_spacing));

            gridRows = DEFAULT_GRID_OFF;
            gridColumns = DEFAULT_GRID_OFF;

            labelsFormat = new DecimalFormat();
        }

        private void init() {

            chartPaint = new Paint();
            chartPaint.setColor(axisColor);
            chartPaint.setStyle(Paint.Style.STROKE);
            chartPaint.setStrokeWidth(axisThickness);
            chartPaint.setAntiAlias(true);

            labelsPaint = new Paint();
            labelsPaint.setColor(labelsColor);
            labelsPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            labelsPaint.setAntiAlias(true);
            labelsPaint.setTextSize(fontSize);
            labelsPaint.setTypeface(typeface);

            fontMaxHeight = (int) (style.labelsPaint.descent() - style.labelsPaint.ascent());
        }

        private void clean() {

            chartPaint = null;
            labelsPaint = null;
        }


        /**
         * Get label's height.
         *
         * @param text Label to measure
         * @return Height of label
         */
        public int getLabelHeight(String text) {

            final Rect rect = new Rect();
            style.labelsPaint.getTextBounds(text, 0, text.length(), rect);
            return rect.height();
        }

        public Paint getChartPaint() {
            return chartPaint;
        }

        public float getAxisThickness() {
            return axisThickness;
        }

        /**
         * If axis x (not the labels) should be displayed.
         *
         * @return True if axis x is displayed
         */
        public boolean hasXAxis() {
            return hasXAxis;
        }

        /**
         * If axis y (not the labels) should be displayed.
         *
         * @return True if axis y is displayed
         */
        public boolean hasYAxis() {
            return hasYAxis;
        }

        public Paint getLabelsPaint() {
            return labelsPaint;
        }

        public int getFontMaxHeight() {
            return fontMaxHeight;
        }

        public AxisRenderer.LabelPosition getXLabelsPositioning() {
            return xLabelsPositioning;
        }

        public AxisRenderer.LabelPosition getYLabelsPositioning() {
            return yLabelsPositioning;
        }

        public int getAxisLabelsSpacing() {
            return axisLabelsSpacing;
        }

        public int getAxisBorderSpacing() {
            return axisBorderSpacing;
        }

        public int getAxisTopSpacing() {
            return axisTopSpacing;
        }

        public DecimalFormat getLabelsFormat() {
            return labelsFormat;
        }

        private boolean hasHorizontalGrid() {
            return gridRows > 0;
        }

        private boolean hasVerticalGrid() {
            return gridColumns > 0;
        }

    }


    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent ev) {

            if (mChartListener != null) mChartListener.onClick(ChartView.this);
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {

            return true;
        }

    }

}
