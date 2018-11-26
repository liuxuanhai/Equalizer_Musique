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

package org.oucho.musicplayer.view.chart.renderer;

import android.graphics.Canvas;
import android.graphics.Paint.Align;


public class XRenderer extends AxisRenderer {


    public XRenderer() {
        super();
    }


    /*
     * IMPORTANT: Method's order is crucial. Change it (or not) carefully.
     */
    @Override
    public void dispose() {

        super.dispose();

        defineMandatoryBorderSpacing(mInnerChartLeft, mInnerChartRight);
        defineLabelsPosition(mInnerChartLeft, mInnerChartRight);
    }

    @Override
    protected float defineAxisPosition() {

        float result = mInnerChartBottom;
        if (style.hasXAxis())
            result += style.getAxisThickness() / 2;
        return result;
    }

    @Override
    protected float defineStaticLabelsPosition(float axisCoordinate, int distanceToAxis) {

        float result = axisCoordinate;

        if (style.getXLabelsPositioning() == LabelPosition.INSIDE) { // Labels sit inside of chart
            result -= distanceToAxis;
            result -= style.getLabelsPaint().descent();
            if (style.hasXAxis())
                result -= style.getAxisThickness() / 2;

        } else if (style.getXLabelsPositioning() == LabelPosition.OUTSIDE) { // Labels sit outside of chart
            result += distanceToAxis;
            result += style.getFontMaxHeight() - style.getLabelsPaint().descent();
            if (style.hasXAxis())
                result += style.getAxisThickness() / 2;
        }
        return result;
    }

    public void draw(Canvas canvas) {

        // Draw axis
        if (style.hasXAxis())
            canvas.drawLine(mInnerChartLeft, axisPosition, mInnerChartRight, axisPosition, style.getChartPaint());

        // Draw labels
        if (style.getXLabelsPositioning() != LabelPosition.NONE) {
            style.getLabelsPaint().setTextAlign(Align.CENTER);

            int nLabels = labels.size();
            for (int i = 0; i < nLabels; i++) {
                canvas.drawText(labels.get(i), labelsPos.get(i), labelsStaticPos, style.getLabelsPaint());

            }
        }
    }

    public float parsePos(int index, double value) {

        if (handleValues)
            return (float) (mInnerChartLeft + (((value - minLabelValue) * screenStep) / (labelsValues.get(1) - minLabelValue)));
        else
            return labelsPos.get(index);
    }

    @Override
    protected float measureInnerChartLeft(int left) {

        return (style.getXLabelsPositioning() != LabelPosition.NONE) ? style.getLabelsPaint().measureText(labels.get(0)) / 2 : left;
    }

    @Override
    protected float measureInnerChartTop(int top) {
        return top;
    }

    @Override
    protected float measureInnerChartRight(int right) {

        // To manage horizontal width of the last axis label
        float lastLabelWidth = 0;
        // to fix possible crash on trying to access label by index -1.
        if (labels.size() > 0)
            lastLabelWidth = style.getLabelsPaint().measureText(labels.get(labels.size() - 1));

        float rightBorder = 0;
        if (style.getXLabelsPositioning() != LabelPosition.NONE && style.getAxisBorderSpacing() + mandatoryBorderSpacing < lastLabelWidth / 2)
            rightBorder = lastLabelWidth / 2 - (style.getAxisBorderSpacing() + mandatoryBorderSpacing);

        return right - rightBorder;
    }

    @Override
    protected float measureInnerChartBottom(int bottom) {

        float result = bottom;

        if (style.hasXAxis())
            result -= style.getAxisThickness();

        if (style.getXLabelsPositioning() == LabelPosition.OUTSIDE)
            result -= style.getFontMaxHeight() + style.getAxisLabelsSpacing();

        return result;
    }

}
