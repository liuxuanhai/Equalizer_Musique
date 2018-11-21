package org.oucho.musicplayer.view.chart.renderer;

import org.oucho.musicplayer.view.chart.model.ChartEntry;
import org.oucho.musicplayer.view.chart.model.ChartSet;
import org.oucho.musicplayer.view.chart.view.ChartView.Style;

import java.text.DecimalFormat;
import java.util.ArrayList;


public abstract class AxisRenderer {

    private static final float DEFAULT_STEPS_NUMBER = 3;

    ArrayList<String> labels;

    ArrayList<Float> labelsValues;

    ArrayList<Float> labelsPos;

    float labelsStaticPos;

    float minLabelValue;

    float screenStep;

    float axisPosition;

    float mandatoryBorderSpacing;

    boolean handleValues;

    float mInnerChartLeft;
    float mInnerChartTop;
    float mInnerChartRight;
    float mInnerChartBottom;

    Style style;

    private float maxLabelValue;

    private float step;


    AxisRenderer() {
        reset();
    }


    public void init(ArrayList<ChartSet> data, Style style) {

        if (handleValues) {
            if (minLabelValue == 0 && maxLabelValue == 0) {
                float[] borders;
                if (hasStep()) borders = findBorders(data, step); // no borders, step
                else borders = findBorders(data); // no borders, no step
                minLabelValue = borders[0];
                maxLabelValue = borders[1];
            }
            if (!hasStep()) setBorderValues(minLabelValue, maxLabelValue);
            labelsValues = calculateValues(minLabelValue, maxLabelValue, step);
            labels = convertToLabelsFormat(labelsValues, style.getLabelsFormat());
        } else {
            labels = extractLabels(data);
        }
        this.style = style;
    }


    void dispose() {
        axisPosition = defineAxisPosition();
        labelsStaticPos = defineStaticLabelsPosition(axisPosition, style.getAxisLabelsSpacing());
    }

    public void measure(int left, int top, int right, int bottom) {

        mInnerChartLeft = measureInnerChartLeft(left);
        mInnerChartTop = measureInnerChartTop(top);
        mInnerChartRight = measureInnerChartRight(right);
        mInnerChartBottom = measureInnerChartBottom(bottom);
    }

    protected abstract float defineAxisPosition();

    protected abstract float defineStaticLabelsPosition(float axisCoordinate, int distanceToAxis);

    protected abstract float measureInnerChartLeft(int left);

    protected abstract float measureInnerChartTop(int top);

    protected abstract float measureInnerChartRight(int right);

    protected abstract float measureInnerChartBottom(int bottom);


    private void reset() {

        mandatoryBorderSpacing = 0;
        step = -1;
        labelsStaticPos = 0;
        axisPosition = 0;
        minLabelValue = 0;
        maxLabelValue = 0;
        handleValues = false;
    }


    void defineMandatoryBorderSpacing(float innerStart, float innerEnd) {

        if (mandatoryBorderSpacing == 1)
            mandatoryBorderSpacing = (innerEnd - innerStart - style.getAxisBorderSpacing() * 2)
                    / labels.size() / 2;
    }


    void defineLabelsPosition(float innerStart, float innerEnd) {

        int nLabels = labels.size();
        screenStep = (innerEnd
                - innerStart
                - style.getAxisTopSpacing()
                - style.getAxisBorderSpacing() * 2
                - mandatoryBorderSpacing * 2) / (nLabels - 1);

        labelsPos = new ArrayList<>(nLabels);
        float currPos = innerStart + style.getAxisBorderSpacing() + mandatoryBorderSpacing;
        for (int i = 0; i < nLabels; i++) {
            labelsPos.add(currPos);
            currPos += screenStep;
        }
    }


    private ArrayList<String> convertToLabelsFormat(ArrayList<Float> values, DecimalFormat format) {

        int size = values.size();
        ArrayList<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            result.add(format.format(values.get(i)));
        return result;
    }


    private ArrayList<String> extractLabels(ArrayList<ChartSet> sets) {

        int size = sets.get(0).size();
        ArrayList<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            result.add(sets.get(0).getLabel(i));
        return result;
    }


    private float[] findBorders(ArrayList<ChartSet> sets) {

        float max = Integer.MIN_VALUE;
        float min = Integer.MAX_VALUE;

        for (ChartSet set : sets) {  // Find minimum and maximum value out of all chart entries
            for (ChartEntry e : set.getEntries()) {
                if (e.getValue() >= max) max = e.getValue();
                if (e.getValue() <= min) min = e.getValue();
            }
        }

        if (max < 0) max = 0;
        if (min > 0) min = 0;

        if (min == max) max += 1;  // All given set values are equal

        return new float[]{min, max};
    }


    private float[] findBorders(ArrayList<ChartSet> sets, float step) {

        float[] borders = findBorders(sets);
        while ((borders[1] - borders[0]) % step != 0) borders[1] += 1; // Assure border fit step

        return borders;
    }


    private ArrayList<Float> calculateValues(float min, float max, float step) {

        ArrayList<Float> result = new ArrayList<>();
        float pos = min;
        while (pos <= max) {
            result.add(pos);
            pos += step;
        }

        // Set max Y axis label in case isn't already there
        if (result.get(result.size() - 1) < max) result.add(pos);

        return result;
    }


    public float getInnerChartLeft() {
        return mInnerChartLeft;
    }


    public float getInnerChartTop() {
        return mInnerChartTop;
    }

    public float getInnerChartRight() {
        return mInnerChartRight;
    }


    public float getInnerChartBottom() {
        return mInnerChartBottom;
    }


    public float[] getInnerChartBounds() {
        return new float[]{mInnerChartLeft, mInnerChartTop, mInnerChartRight, mInnerChartBottom};
    }


    private boolean hasStep() {
        return (step != -1);
    }

    public void setHandleValues(boolean bool) {
        handleValues = bool;
    }


    public void setInnerChartBounds(float left, float top, float right, float bottom) {

        mInnerChartLeft = left;
        mInnerChartTop = top;
        mInnerChartRight = right;
        mInnerChartBottom = bottom;
    }


    private void setBorderValues(float min, float max, float step) {

        if (min >= max) throw new IllegalArgumentException(
                "Minimum border value must be greater than maximum values");

        this.step = step;
        maxLabelValue = max;
        minLabelValue = min;
    }


    public void setBorderValues(float min, float max) {

        if (!hasStep()) step = (max - min) / DEFAULT_STEPS_NUMBER;
        setBorderValues(min, max, step);
    }


    public enum LabelPosition {
        NONE,
        OUTSIDE,
        INSIDE
    }

}