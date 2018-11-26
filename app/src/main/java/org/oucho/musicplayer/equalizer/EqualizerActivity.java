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

package org.oucho.musicplayer.equalizer;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import org.oucho.musicplayer.view.chart.model.LineSet;
import org.oucho.musicplayer.view.chart.view.LineChartView;

import org.oucho.musicplayer.R;
import org.oucho.musicplayer.utils.NavigationUtils;


public class EqualizerActivity extends AppCompatActivity {

    private SwitchCompat mSwitchButton;
    private boolean mSwitchBound;

    private Spinner mSpinner;


    private LineSet dataset;
    private LineChartView chart;
    private float[] points;

    private LineSet datasetCenter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equalizer);

        Context context = getApplicationContext();

        int couleurTitre = ContextCompat.getColor(context, R.color.colorAccent);

        String titre = context.getString(R.string.equalizer);


        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            actionBar.setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>" + titre + "</font>", Html.FROM_HTML_MODE_LEGACY));
        } else {
            //noinspection deprecation
            actionBar.setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>" + titre + "</font>"));
        }

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);

        int screen_width = size.x;
        int screen_height = size.y;

        float ratio = (float) screen_height / (float) 1920;
        float ratio2 = (float) screen_width / (float) 1080;
        ratio = Math.min(ratio, ratio2);


        points = new float[AudioEffects.getNumberOfBands()];

        int colorGrille = ContextCompat.getColor(context, R.color.grey_400);
        int colorCenter = ContextCompat.getColor(context, R.color.grey_600);

        int colorCourbe = ContextCompat.getColor(context, R.color.colorAccent);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(colorGrille);
        paint.setStrokeWidth((float) (1.10 * ratio));

        datasetCenter = new LineSet();
        datasetCenter.setColor(colorCenter);
        datasetCenter.setSmooth(true);
        datasetCenter.setThickness(2);

        dataset = new LineSet();
        dataset.setColor(colorCourbe);
        dataset.setSmooth(true);
        dataset.setThickness(5);

        chart = findViewById(R.id.lineChart);


        chart.setXAxis(false);
        chart.setYAxis(false);

        chart.setYLabels();
        chart.setXLabels();
        chart.setGrid(8, 10, paint);

        chart.setAxisBorderValues(-300, 3300);

        mSwitchBound = false;

        init();

    }

    @Override
    public void onPause() {
        super.onPause();
        AudioEffects.savePrefs(this);
    }

    @Override
    public void onBackPressed() {
            NavigationUtils.showMainActivity(this);
    }

    private void bindSwitchToEqualizer() {
        if (!mSwitchBound && mSwitchButton != null) {

            mSwitchButton.setChecked(AudioEffects.areAudioEffectsEnabled());
            mSwitchButton.setOnCheckedChangeListener((buttonView, isChecked) -> AudioEffects.setAudioEffectsEnabled(isChecked));
            mSwitchBound = true;
        }
    }

    private void init() {


        bindSwitchToEqualizer();

        initBassBoost();

        initSeekBars();

        updateSeekBars();

        initPresets();

    }

    private void initPresets() {

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, AudioEffects.getEqualizerPresets(this));

        mSpinner = findViewById(R.id.presets_spinner);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner.setAdapter(adapter);

        mSpinner.setSelection(AudioEffects.getCurrentPreset());

        mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 1) {
                    AudioEffects.usePreset((short) (position - 1));
                }

                updateSeekBars();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //  Auto-generated method stub

            }
        });

    }

    private void initBassBoost() {
        SeekBar bassBoost = findViewById(R.id.bassboost_slider);
        assert bassBoost != null;
        bassBoost.setMax(AudioEffects.BASSBOOST_MAX_STRENGTH);
        bassBoost.setProgress(AudioEffects.getBassBoostStrength());
        bassBoost.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    AudioEffects.setBassBoostStrength((short) seekBar.getProgress());
                }
            }
        });
    }

    private void initSeekBars() {
            ViewGroup layout = findViewById(R.id.equalizer_layout);

            final short[] range = AudioEffects.getBandLevelRange();

            LayoutParams lp = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
            short bands = AudioEffects.getNumberOfBands();

            for (short band = 0; band < bands; band++) {

                View v = getLayoutInflater().inflate(R.layout.activity_equalizer_slider, layout, false);

                SeekBar seekBar = v.findViewById(R.id.seekBar);

                 assert range != null;
                seekBar.setMax((range[1]) - range[0]);

                seekBar.setTag(band);

                try {

                        @SuppressWarnings("ConstantConditions")
                        final short lowerEqualizerBandLevel = AudioEffects.getBandLevelRange()[0];

                        String centerFrew = (AudioEffects.getCenterFreq(band) / 1000) + "Hz";

                        points[band] = band - lowerEqualizerBandLevel;
                        dataset.addPoint(centerFrew, points[band]);
                        seekBar.setProgress(band - lowerEqualizerBandLevel);

                        datasetCenter.addPoint(centerFrew, points[band]);

                        chart.addData(dataset);
                        chart.addData(datasetCenter);

                        chart.show();


                        final TextView levelTextView = v.findViewById(R.id.level);

                        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {
                            }

                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                                if (fromUser) {
                                    short band = (Short) seekBar.getTag();
                                    short level = (short) (seekBar.getProgress() + range[0]);
                                    AudioEffects.setBandLevel(band, level);
                                    String niveau = (level > 0 ? "+" : "") + level / 100 + "dB";
                                    levelTextView.setText(niveau);
                                    mSpinner.setSelection(0);

                                    points[band] = level - lowerEqualizerBandLevel;
                                    dataset.updateValues(points);
                                    chart.notifyDataUpdate();
                                }
                            }
                        });


                } catch (Exception ignore) {}


                assert layout != null;
                layout.addView(v, band, lp);
            }
    }

    private void updateSeekBars() {
        ViewGroup layout = findViewById(R.id.equalizer_layout);

        final short[] range = AudioEffects.getBandLevelRange();

        short bands = AudioEffects.getNumberOfBands();

        for (short band = 0; band < bands; band++) {

            assert layout != null;
            View v = layout.getChildAt(band);

            final TextView freqTextView = v.findViewById(R.id.frequency);
            final TextView levelTextView = v.findViewById(R.id.level);
            final SeekBar seekBar = v.findViewById(R.id.seekBar);


            int freq = AudioEffects.getCenterFreq(band);
            if (freq < 1000 * 1000) {
                String frequence = freq / 1000 + "Hz";
                freqTextView.setText(frequence);
            } else {
                String frequence = freq / (1000 * 1000) + "kHz";
                freqTextView.setText(frequence);
            }


            short level = AudioEffects.getBandLevel(band);
            seekBar.setProgress(level - (range != null ? range[0] : 0));

            String niveau = (level > 0 ? "+" : "") + level / 100 + "dB";
            levelTextView.setText(niveau);

            try {
                @SuppressWarnings("ConstantConditions")
                short lowerEqualizerBandLevel = AudioEffects.getBandLevelRange()[0];

                points[band] = AudioEffects.getBandLevel(band) - lowerEqualizerBandLevel;
                dataset.updateValues(points);
                chart.notifyDataUpdate();
            } catch (Exception ignore) {}
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.equalizer, menu);
        MenuItem item = menu.findItem(R.id.action_switch);

        mSwitchButton = item.getActionView().findViewById(R.id.switch_button);

        bindSwitchToEqualizer();
        return true;
    }

}
