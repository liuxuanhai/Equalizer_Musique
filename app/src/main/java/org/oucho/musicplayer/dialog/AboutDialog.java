package org.oucho.musicplayer.dialog;


import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.oucho.musicplayer.BuildConfig;
import org.oucho.musicplayer.R;

public class AboutDialog extends DialogFragment {

    private final ViewGroup nullParent = null;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder about = new AlertDialog.Builder(getActivity());

        View dialoglayout = getActivity().getLayoutInflater().inflate(R.layout.dialog_about, nullParent);
        String versionName = BuildConfig.VERSION_NAME;
        TextView versionView = dialoglayout.findViewById(R.id.version);
        versionView.setText(versionName);

        about.setView(dialoglayout);

        return about.create();
    }

}
