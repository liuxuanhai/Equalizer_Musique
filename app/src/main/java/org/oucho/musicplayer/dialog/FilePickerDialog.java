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
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.oucho.musicplayer.R;
import org.oucho.musicplayer.utils.StorageHelper;
import org.oucho.musicplayer.view.CustomLayoutManager;
import org.oucho.musicplayer.view.fastscroll.FastScrollRecyclerView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

public class FilePickerDialog extends BottomSheetDialogFragment {

    private static final String TAG = "FilePickerDialog";

    private ArrayList<File> folders;
    private BottomSheetAlbumsAdapter adapter;
    private TextView currentFolderPath;
    private OnImageSelected onImageSelected;
    private FragmentManager fragmentManager;
    private Spinner spinner;
    private View rootView;
    private String sdCardPath = null;
    private boolean canGoBack = false;

    private final int INTERNAL_STORAGE = 0;

    private int artSize;


    public static FilePickerDialog with(FragmentManager manager) {
        FilePickerDialog fragment = new FilePickerDialog();
        fragment.fragmentManager = manager;
        return fragment;
    }


    public FilePickerDialog onImageSelected(OnImageSelected callback) {
        onImageSelected = callback;
        return this;
    }

    public void show() {
        show(fragmentManager, getTag());
    }


    @Override
    public void setupDialog(Dialog dialog, int style) {

        rootView = View.inflate(getContext(), R.layout.dialog_file_picker, null);

        final FastScrollRecyclerView mRecyclerView = rootView.findViewById(R.id.folders);
        spinner = rootView.findViewById(R.id.storage_spinner);

        sdCardPath = StorageHelper.getSdcardPath(getContext());

        currentFolderPath = rootView.findViewById(R.id.current_path_txt);

        mRecyclerView.setLayoutManager(new CustomLayoutManager(getContext()));
        adapter = new BottomSheetAlbumsAdapter();
        mRecyclerView.setAdapter(adapter);

        initSpinner();
        displayContentFolder(Environment.getExternalStorageDirectory());

        dialog.setContentView(rootView);

        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) ((View) rootView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = layoutParams.getBehavior();

        if (behavior instanceof BottomSheetBehavior)
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);

        adapter.notifyDataSetChanged();

        artSize = getContext().getResources().getDimensionPixelSize(R.dimen.dialog_file_picker_art_size);
    }


    private void initSpinner() {

        spinner.setAdapter(new VolumeSpinnerAdapter(rootView.getContext()));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                switch(pos){
                    case INTERNAL_STORAGE:
                        displayContentFolder(Environment.getExternalStorageDirectory());
                        break;
                    default:

                        String sdCard = StorageHelper.getSdcardPath(getContext());

                        if (sdCard != null)
                            displayContentFolder(new File(sdCard ) );
                        else
                            displayContentFolder(Environment.getExternalStorageDirectory());
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private boolean canGoBack() {
        return canGoBack;
    }

    public interface OnImageSelected {
        void imageSelected(String path);
    }

    private void displayContentFolder(File dir) {
        canGoBack = false;
        if(dir.canRead()) {
            folders = new ArrayList<>();
            File parent = dir.getParentFile();

            try {
                if (parent.canRead()) {
                    canGoBack = true;
                    if (!parent.getName().equals("storage"))
                        folders.add(0, parent);
                }
            } catch (NullPointerException e) {
                Log.w(TAG, "error" + e);
            }

            FileFilter directoryFilter = file -> (file.isDirectory() && !file.isHidden())
                    || file.getName().toLowerCase().endsWith(".png")
                    || file.getName().toLowerCase().endsWith(".jpg");
            File[] files = dir.listFiles(directoryFilter);

            if (files != null && files.length > 0) {
                folders.addAll(new ArrayList<>(Arrays.asList(files)));
            }

            sort();

            String path = dir.getAbsolutePath().replace("/storage/emulated/0", "");
            if (path.equals(""))
                path = "/";

            currentFolderPath.setText(path);
            adapter.notifyDataSetChanged();
        }
    }

    private void sort() {
        Collections.sort(folders, (file1, file2) -> {

            boolean isDirectory1 = file1.isDirectory();
            boolean isDirectory2 = file2.isDirectory();

            if (isDirectory1 && !isDirectory2)
                return -1;

            if (!isDirectory1 && isDirectory2)
                return 1;

            return file1.getName().toLowerCase(Locale.getDefault()).compareTo(file2.getName().toLowerCase(Locale.getDefault()));
        });
    }

    private final BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN)
                dismiss();
        }
        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
    };


    private class VolumeSpinnerAdapter extends ArrayAdapter<String> {

        VolumeSpinnerAdapter(Context context) {
            super(context, R.layout.dialog_file_picker_spinner, R.id.spinner_volume_name);
            insert(getString(R.string.internal_storage), INTERNAL_STORAGE);
            if(sdCardPath != null)
                add(getString(R.string.extrnal_storage));
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            Drawable icon;

            switch (position){
                case INTERNAL_STORAGE:
                    icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_storage_amber_a700_24dp);
                    break;
                default:
                    icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_sd_storage_amber_a700_24dp);
                    break;
            }

            ((ImageView)view.findViewById(R.id.volume_image)).setImageDrawable(icon);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            Drawable icon;

            switch (position){
                case INTERNAL_STORAGE:
                    icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_storage_amber_a700_24dp);
                break;
                default:
                    icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_sd_storage_amber_a700_24dp);
                    break;
            }
            ((ImageView) view.findViewById(R.id.volume_image)).setImageDrawable(icon);
            return view;
        }
    }

    private class BottomSheetAlbumsAdapter extends RecyclerView.Adapter<BottomSheetAlbumsAdapter.ViewHolder> {

        BottomSheetAlbumsAdapter() { }

        public BottomSheetAlbumsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.dialog_file_picker_item, parent, false);
            v.setOnClickListener(viewHolderClickListener);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final BottomSheetAlbumsAdapter.ViewHolder holder, final int position) {

            File f = folders.get(position);

            if (f.isFile()) {

                Picasso.get()
                        .load(f)
                        .resize(artSize, artSize)
                        .centerCrop()
                        .into(holder.imgFolder);

                holder.imgFolder.setTag(f.getPath());

            } else {
                holder.imgFolder.setImageResource(R.drawable.ic_folder_grey_500_24dp);
            }

            holder.folderName.setText(f.getName());
            holder.folderName.setTag(f.getPath());

            if(canGoBack() && position == 0) // go to parent folder
                holder.folderName.setText("..");

        }

        public int getItemCount() {
            return folders.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            final TextView folderName;
            final ImageView imgFolder;

            ViewHolder(View itemView) {
                super(itemView);

                folderName = itemView.findViewById(R.id.name_folder);
                imgFolder = itemView.findViewById(R.id.folder_icon_bottom_sheet_item);
            }
        }

        private final View.OnClickListener viewHolderClickListener = view -> {

            String path = view.findViewById(R.id.name_folder).getTag().toString();

            File file = new File(path);

            if (!file.isFile()) {
                displayContentFolder(new File(path));
            } else {
                dismiss();
                onImageSelected.imageSelected(view.findViewById(R.id.folder_icon_bottom_sheet_item).getTag().toString());
            }
        };

    }

}

