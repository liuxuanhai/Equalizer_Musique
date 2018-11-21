package org.oucho.musicplayer.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.services.ConvertService;


public class ConvertDialog extends DialogFragment implements MusiqueKeys {

    @SuppressWarnings("unused")
    private static final String TAG = "ConvertDialog";

    private Song mSong;
    private Album mAlbum;

    private boolean isAlbum;

    private String format = "aac";
    private boolean HQ = false;

    private CheckBox mp3 ;
    private CheckBox aac;
    private CheckBox flac;
    private CheckBox hq;

    private TextView text;
    private TextView text1;

    private ImageView hq_logo;

    private SharedPreferences mPreferences;

    public static ConvertDialog newInstance(Album album) {
        ConvertDialog fragment = new ConvertDialog();

        Bundle args = new Bundle();
        args.putParcelable("album", album);
        args.putString("type", "album");

        fragment.setArguments(args);
        return fragment;
    }

    public static ConvertDialog newInstance(Song song) {
        ConvertDialog fragment = new ConvertDialog();

        Bundle args = new Bundle();

        args.putParcelable("song", song);
        args.putString("type", "song");

        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        Bundle bundle = getArguments();

        String type = bundle.getString("type");

        if (type != null && type.equals("album")) {
            isAlbum = true;
            mAlbum = bundle.getParcelable("album");

        } else if (type != null && type.equals("song")) {
            isAlbum = false;
            mSong = bundle.getParcelable("song");
        }



    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_convert, null, false);

        mp3 = view.findViewById(R.id.mp3);
        aac = view.findViewById(R.id.aac);
        flac = view.findViewById(R.id.flac);
        hq = view.findViewById(R.id.hq);

        text = view.findViewById(R.id.text);
        text1 = view.findViewById(R.id.text1);

        hq_logo = view.findViewById(R.id.hq_logo);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view).setTitle(getText(R.string.convert_format));

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {

            run();
            dismiss();

        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dismiss());

        initCheckBox();

        setText();

        return builder.create();
    }

    private void initCheckBox() {

        HQ = mPreferences.getBoolean("convert_hq", false);
        hq.setChecked(HQ);

        format = mPreferences.getString("convert_format", "aac");

        switch (format) {

            case "mp3":
                mp3.setChecked(true);
                hq_logo.setImageResource(R.drawable.ic_high_quality_grey_600_24dp);
                break;

            case "aac":
                aac.setChecked(true);
                hq_logo.setImageResource(R.drawable.ic_high_quality_grey_600_24dp);
                break;

            case "flac":
                flac.setChecked(true);
                hq_logo.setImageResource(R.drawable.ic_high_quality_grey_400_24dp);
                break;

            default:
                break;
        }


        aac.setOnClickListener(view -> {

            if (((CheckBox) view).isChecked()) {
                mp3.setChecked(false);
                flac.setChecked(false);
            } else {
                aac.setChecked(true);
            }


            hq_logo.setImageResource(R.drawable.ic_high_quality_grey_600_24dp);

            format = "aac";
            setText();
        });


        mp3.setOnClickListener(view -> {

            if (((CheckBox) view).isChecked()) {
                aac.setChecked(false);
                flac.setChecked(false);
            } else {
                mp3.setChecked(true);
            }

            hq_logo.setImageResource(R.drawable.ic_high_quality_grey_600_24dp);

            format = "mp3";
            setText();
        });


        flac.setOnClickListener(view -> {

            if (((CheckBox) view).isChecked()) {
                mp3.setChecked(false);
                aac.setChecked(false);
            } else {
                flac.setChecked(true);
            }

            hq_logo.setImageResource(R.drawable.ic_high_quality_grey_400_24dp);

            format = "flac";
            setText();
        });

        hq.setOnClickListener(view -> {

            if (((CheckBox) view).isChecked()) {
                HQ = true;
                hq.setChecked(true);
            } else {
                hq.setChecked(false);
                HQ = false;
            }

            setText();
        });

    }


    private void run() {

        mPreferences.edit().putString("convert_format", format).apply();
        mPreferences.edit().putBoolean("convert_hq", HQ).apply();

        Bundle bundle = new Bundle();

        if (isAlbum) {

            bundle.putString("type", "album");
            bundle.putParcelable("album", mAlbum);

        } else {

            bundle.putString("type", "song");
            bundle.putParcelable("song", mSong);
        }

        bundle.putString("format", format);
        bundle.putBoolean("hq", HQ);

        Intent intent = new Intent(getActivity(), ConvertService.class);
        intent.putExtras(bundle);

        getActivity().startService(intent);
    }

    private void setText() {

        switch (format) {
            case "aac":
                text.setText(getString(R.string.convert_aac));
                if (HQ) {
                    text1.setText(getString(R.string.convert_aac_hq));
                } else {
                    text1.setText(getString(R.string.convert_aac1));
                }
                break;

            case "mp3":
                text.setText(getString(R.string.convert_mp3));
                if (HQ) {
                    text1.setText(getString(R.string.convert_mp3_hq));
                } else {
                    text1.setText(getString(R.string.convert_mp31));
                }
                break;

            case "flac":
                text.setText(getString(R.string.convert_flac));
                text1.setText(getString(R.string.convert_flac1));
                break;

            default:
                break;
        }

    }

}
