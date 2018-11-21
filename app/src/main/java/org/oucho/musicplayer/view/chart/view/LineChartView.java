package org.oucho.musicplayer.view.chart.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.graphics.Shader;
import android.util.AttributeSet;

import org.oucho.musicplayer.R;
import org.oucho.musicplayer.view.chart.util.Tools;
import org.oucho.musicplayer.view.chart.model.ChartSet;
import org.oucho.musicplayer.view.chart.model.LineSet;
import org.oucho.musicplayer.view.chart.model.Point;

import java.util.ArrayList;


public class LineChartView extends ChartView {

    private static final float SMOOTH_FACTOR = 0.15f;

    private final Style mStyle;

    private final float mClickableRadius;


    public LineChartView(Context context, AttributeSet attrs) {

        super(context, attrs);

        setOrientation();
        mStyle = new Style();
        mClickableRadius = context.getResources().getDimension(R.dimen.dot_region_radius);
    }


    public LineChartView(Context context) {

        super(context);

        setOrientation();
        mStyle = new Style();
        mClickableRadius = context.getResources().getDimension(R.dimen.dot_region_radius);
    }

    private static int si(int setSize, int i) {

        if (i > setSize - 1) return setSize - 1;
        else if (i < 0) return 0;
        return i;
    }

    @Override
    public void onAttachedToWindow() {

        super.onAttachedToWindow();
        mStyle.init();
    }

    @Override
    public void onDetachedFromWindow() {

        super.onDetachedFromWindow();
        mStyle.clean();
    }

    @Override
    public void onDrawChart(Canvas canvas, ArrayList<ChartSet> data) {

        LineSet lineSet;
        Path linePath;

        for (ChartSet set : data) {

            lineSet = (LineSet) set;

            mStyle.mLinePaint.setColor(lineSet.getColor());
            mStyle.mLinePaint.setStrokeWidth(lineSet.getThickness());
            applyShadow(mStyle.mLinePaint, lineSet.getAlpha(), lineSet.getShadowDx(), lineSet.getShadowDy(), lineSet.getShadowRadius(), lineSet.getShadowColor());

            if (lineSet.isDashed())
                mStyle.mLinePaint.setPathEffect(new DashPathEffect(lineSet.getDashedIntervals(), lineSet.getDashedPhase()));
            else
                mStyle.mLinePaint.setPathEffect(null);

            if (!lineSet.isSmooth()) linePath = createLinePath(lineSet);
            else linePath = createSmoothLinePath(lineSet);

            //Draw background
            if (lineSet.hasFill() || lineSet.hasGradientFill())
                canvas.drawPath(createBackgroundPath(new Path(linePath), lineSet), mStyle.mFillPaint);

            //Draw line
            canvas.drawPath(linePath, mStyle.mLinePaint);

            //Draw points
            drawPoints(canvas, lineSet);

        }

    }

    @Override
    void defineRegions(ArrayList<ArrayList<Region>>
                               regions, ArrayList<ChartSet> data) {

        float x;
        float y;
        int dataSize = data.size();
        int setSize;
        for (int i = 0; i < dataSize; i++) {

            setSize = data.get(0).size();
            for (int j = 0; j < setSize; j++) {

                x = data.get(i).getEntry(j).getX();
                y = data.get(i).getEntry(j).getY();
                regions.get(i)
                        .get(j)
                        .set((int) (x - mClickableRadius), (int) (y - mClickableRadius),
                                (int) (x + mClickableRadius), (int) (y + mClickableRadius));
            }
        }
    }


    /**
     * Responsible for drawing points
     */
    private void drawPoints(Canvas canvas, LineSet set) {

        int begin = set.getBegin();
        int end = set.getEnd();
        Point dot;
        for (int i = begin; i < end; i++) {

            dot = (Point) set.getEntry(i);

            if (dot.isVisible()) {

                // Style dot
                mStyle.mDotsPaint.setColor(dot.getColor());
                mStyle.mDotsPaint.setAlpha((int) (set.getAlpha() * ChartView.Style.FULL_ALPHA));
                applyShadow(mStyle.mDotsPaint, set.getAlpha(), dot.getShadowDx(), dot
                        .getShadowDy(), dot.getShadowRadius(), dot.getShadowColor());

                // Draw dot
                canvas.drawCircle(dot.getX(), dot.getY(), dot.getRadius(), mStyle.mDotsPaint);

                //Draw dots stroke
                if (dot.hasStroke()) {

                    // Style stroke
                    mStyle.mDotsStrokePaint.setStrokeWidth(dot.getStrokeThickness());
                    mStyle.mDotsStrokePaint.setColor(dot.getStrokeColor());
                    mStyle.mDotsStrokePaint.setAlpha((int) (set.getAlpha() * ChartView.Style.FULL_ALPHA));
                    applyShadow(mStyle.mDotsStrokePaint, set.getAlpha(), dot.getShadowDx(), dot.getShadowDy(), dot.getShadowRadius(), dot.getShadowColor());

                    canvas.drawCircle(dot.getX(), dot.getY(), dot.getRadius(), mStyle.mDotsStrokePaint);
                }

                // Draw drawable
                if (dot.getDrawable() != null) {
                    Bitmap dotsBitmap = Tools.drawableToBitmap(dot.getDrawable());
                    canvas.drawBitmap(dotsBitmap, dot.getX() - dotsBitmap.getWidth() / 2, dot.getY() - dotsBitmap.getHeight() / 2, mStyle.mDotsPaint);
                }
            }
        }

    }


    private Path createLinePath(LineSet set) {

        Path res = new Path();

        int begin = set.getBegin();
        int end = set.getEnd();
        for (int i = begin; i < end; i++) {
            if (i == begin)
                res.moveTo(set.getEntry(i).getX(), set.getEntry(i).getY());
            else
                res.lineTo(set.getEntry(i).getX(), set.getEntry(i).getY());
        }

        return res;
    }


    private Path createSmoothLinePath(LineSet set) {

        float thisPointX;
        float thisPointY;
        float nextPointX;
        float nextPointY;
        float startDiffX;
        float startDiffY;
        float endDiffX;
        float endDiffY;
        float firstControlX;
        float firstControlY;
        float secondControlX;
        float secondControlY;

        Path res = new Path();
        res.moveTo(set.getEntry(set.getBegin()).getX(), set.getEntry(set.getBegin()).getY());

        int begin = set.getBegin();
        int end = set.getEnd();
        for (int i = begin; i < end - 1; i++) {

            thisPointX = set.getEntry(i).getX();
            thisPointY = set.getEntry(i).getY();

            nextPointX = set.getEntry(i + 1).getX();
            nextPointY = set.getEntry(i + 1).getY();

            startDiffX = (nextPointX - set.getEntry(si(set.size(), i - 1)).getX());
            startDiffY = (nextPointY - set.getEntry(si(set.size(), i - 1)).getY());

            endDiffX = (set.getEntry(si(set.size(), i + 2)).getX() - thisPointX);
            endDiffY = (set.getEntry(si(set.size(), i + 2)).getY() - thisPointY);

            firstControlX = thisPointX + (SMOOTH_FACTOR * startDiffX);
            firstControlY = thisPointY + (SMOOTH_FACTOR * startDiffY);

            secondControlX = nextPointX - (SMOOTH_FACTOR * endDiffX);
            secondControlY = nextPointY - (SMOOTH_FACTOR * endDiffY);

            res.cubicTo(firstControlX, firstControlY, secondControlX, secondControlY, nextPointX, nextPointY);
        }

        return res;

    }


    private Path createBackgroundPath(Path path, LineSet set) {

        mStyle.mFillPaint.setAlpha((int) (set.getAlpha() * ChartView.Style.FULL_ALPHA));

        if (set.hasFill())
            mStyle.mFillPaint.setColor(set.getFillColor());

        if (set.hasGradientFill())
            mStyle.mFillPaint.setShader(new LinearGradient(super.getInnerChartLeft(), super.getInnerChartTop(),
                        super.getInnerChartLeft(), super.getInnerChartBottom(), set.getGradientColors(),
                    set.getGradientPositions(), Shader.TileMode.MIRROR));

        path.lineTo(set.getEntry(set.getEnd() - 1).getX(), super.getInnerChartBottom());
        path.lineTo(set.getEntry(set.getBegin()).getX(), super.getInnerChartBottom());
        path.close();

        return path;
    }


    class Style {

        /**
         * Paint variables
         */
        private Paint mDotsPaint;

        private Paint mDotsStrokePaint;

        private Paint mLinePaint;

        private Paint mFillPaint;


        Style() {}

        private void init() {

            mDotsPaint = new Paint();
            mDotsPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mDotsPaint.setAntiAlias(true);

            mDotsStrokePaint = new Paint();
            mDotsStrokePaint.setStyle(Paint.Style.STROKE);
            mDotsStrokePaint.setAntiAlias(true);

            mLinePaint = new Paint();
            mLinePaint.setStyle(Paint.Style.STROKE);
            mLinePaint.setAntiAlias(true);

            mFillPaint = new Paint();
            mFillPaint.setStyle(Paint.Style.FILL);
        }

        private void clean() {

            mLinePaint = null;
            mFillPaint = null;
            mDotsPaint = null;
        }

    }

}
