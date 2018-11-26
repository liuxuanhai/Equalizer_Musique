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

package org.oucho.musicplayer.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.oucho.musicplayer.utils.PlaylistsUtils;
import org.oucho.musicplayer.R;


public class CreatePlaylistDialog extends DialogFragment {

    private OnPlaylistCreatedListener mListener;

    public static CreatePlaylistDialog newInstance() {

        return new CreatePlaylistDialog();
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        final View layout = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_playlist,
                new LinearLayout(getActivity()), false);



        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity())
                .setTitle(R.string.create_playlist)
                .setView(layout)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {
                            EditText editText = layout.findViewById(R.id.playlist_name);

                            PlaylistsUtils.createPlaylist(getActivity()
                                    .getContentResolver(), editText
                                    .getText().toString());

                            if (mListener != null) {
                                mListener.onPlaylistCreated();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> {
                            // This constructor is intentionally empty, pourquoi ? parce que !
                        });

        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);

        return builder.create();
    }


    public void setOnPlaylistCreatedListener(OnPlaylistCreatedListener listener) {
        mListener = listener;
    }


    public interface OnPlaylistCreatedListener {

        void onPlaylistCreated();
    }


}
