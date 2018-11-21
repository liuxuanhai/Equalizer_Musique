package org.oucho.musicplayer.view.chart.model;

import android.support.annotation.NonNull;

import java.util.ArrayList;

import static org.oucho.musicplayer.view.chart.util.Preconditions.checkNotNull;
import static org.oucho.musicplayer.view.chart.util.Preconditions.checkPositionIndex;


public abstract class ChartSet {


    private final ArrayList<ChartEntry> mEntries;

    private final float mAlpha;

    ChartSet() {
        mEntries = new ArrayList<>();
        mAlpha = 1;
    }

    void addEntry(@NonNull ChartEntry e) {
        mEntries.add(checkNotNull(e));
    }


    public void updateValues(@NonNull float[] newValues) {

        checkNotNull(newValues);
        if (newValues.length != size()) throw new IllegalArgumentException("New set values given doesn't match previous " + "number of entries.");

        int nEntries = size();
        for (int i = 0; i < nEntries; i++)
            setValue(i, newValues[i]);
    }

    public int size() {
        return mEntries.size();
    }

    public ArrayList<ChartEntry> getEntries() {
        return mEntries;
    }

    public ChartEntry getEntry(int index) {
        return mEntries.get(checkPositionIndex(index, size()));
    }

    public float getValue(int index) {
        return mEntries.get(checkPositionIndex(index, size())).getValue();
    }

    public String getLabel(int index) {
        return mEntries.get(checkPositionIndex(index, size())).getLabel();
    }

    public float getAlpha() {
        return mAlpha;
    }

    private void setValue(int index, float value) {
        mEntries.get(checkPositionIndex(index, size())).setValue(value);
    }

    public String toString() {
        return mEntries.toString();
    }

}
