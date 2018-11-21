package org.oucho.musicplayer.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.oucho.musicplayer.MusiqueApplication;
import org.oucho.musicplayer.MusiqueKeys;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.db.model.Album;
import org.oucho.musicplayer.db.model.Song;
import org.oucho.musicplayer.fragments.loaders.SongLoader;
import org.oucho.musicplayer.utils.PreferenceUtil;
import org.oucho.musicplayer.utils.StorageHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class SaveTagProgressDialog extends DialogFragment implements MusiqueKeys{

    private static final String TAG = "SaveTagProgressDialog";

    private TextView file_name;

    private static boolean isCancel;
    private boolean isAlbum;

    private ProgressBar progressBar;

    private Song mSong;
    private Album mAlbum;
    private List<Song> mSongList = new ArrayList<>();

    private AlertDialog controlDialog;
    private AsyncTask<String, Integer, Boolean> writeTask;

    private String title;
    private String artistName;
    private String albumName;
    private String genre;
    private String year;
    private String track;
    private String disc;
    private String comment;
    private String cover;

    private static boolean waitPermissionExternal;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        String type = bundle.getString("type");

        if (type != null && type.equals("album")) {

            isAlbum = true;

            mAlbum = bundle.getParcelable("album");
            albumName = bundle.getString("albumName");
            artistName = bundle.getString("artistName");
            genre = bundle.getString("genre");
            year = bundle.getString("year");
            cover = bundle.getString("cover");

            getLoaderManager().initLoader(0, null, mLoaderSongs);

        } else if (type != null && type.equals("song")) {

            isAlbum = false;

            mSong = bundle.getParcelable("song");
            title = bundle.getString("title");
            artistName = bundle.getString("artistName");
            albumName = bundle.getString("albumName");
            genre = bundle.getString("genre");
            year = bundle.getString("year");
            track = bundle.getString("track");
            disc = bundle.getString("disc");
            comment = bundle.getString("comment");
            cover = bundle.getString("cover");
        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_tag_progressbar, null, false);

        TextView titre = view.findViewById(R.id.titre);

        titre.setText(getText(R.string.tags_edition));

        file_name = view.findViewById(R.id.file_name);

        progressBar = view.findViewById(R.id.progress_bar);

        progressBar.setMax(100);
        progressBar.setProgress(0);

        builder.setView(view);
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> isCancel = true);

        controlDialog = builder.create();

        controlDialog.setCanceledOnTouchOutside(false);

        return controlDialog;
    }


    @Override
    public void onStart() {
        super.onStart();

        if (!isAlbum)
            writeTask = new WriteTags().execute();
    }


    @Override
    public void onStop() {

        if (writeTask.getStatus() == AsyncTask.Status.FINISHED) {

            Intent refresh = new Intent();
            refresh.setAction(REFRESH_TAG);
            MusiqueApplication.getInstance().sendBroadcast(refresh);
            controlDialog.cancel();
        } else {
            isCancel = true;
            new Handler().postDelayed(this::onStop, 500);
        }
        super.onStop();
    }


    @SuppressLint("StaticFieldLeak")
    class WriteTags extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(String... arg0) {
            boolean success = false;

            isCancel = false;

            long freespaceSource;

            if (isAlbum) {
                String size1 = String.valueOf(StorageHelper.getSourceFreeBytes(mSongList.get(0).getPath())).replace("-", "");
                freespaceSource = Long.parseLong(size1);
            } else {
                String size1 = String.valueOf(StorageHelper.getSourceFreeBytes(mSong.getPath())).replace("-", "");
                freespaceSource = Long.parseLong(size1);
            }

            String size0 = String.valueOf(StorageHelper.getInternalFreeBytes()).replace("-", "");
            long freespaceIntern = Long.parseLong(size0);

            if (freespaceIntern < 52428800 && freespaceSource < 52428800) {
                String msg = getContext().getString(R.string.stockage_space);
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show());

                onStop();

            } else {

                if (isAlbum)
                    success = writeTagsAlbum();
                else
                    success = writeTagsSong();
            }

            return success;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            onStop();
        }
    }



    private  boolean writeTagsSong() {

        boolean success = false;

        File song = new File(mSong.getPath());

        try {

            getActivity().runOnUiThread(() -> file_name.setText(mSong.getPath()));

            String filename = new File(mSong.getPath()).getName();
            String pathCache = MusiqueApplication.getInstance().getCacheDir().getPath() + "/";
            String pathSong = new File(mSong.getPath()).getParent();

            File file = new File(pathCache + filename);

            File test = new File(mSong.getPath() + ".test");
            waitPermissionExternal = false;
            if (!StorageHelper.isWritable(test) && PreferenceUtil.getTreeUris() == null) {

                Intent intent = new Intent();
                intent.setAction(STORAGE_ACCESS_FRAMEWORK);
                getContext().sendBroadcast(intent);

                while (!waitPermissionExternal) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }


            if (!isCancel) {

                if (file.exists())
                    StorageHelper.deleteFile(file);

                StorageHelper.copyFile(song, MusiqueApplication.getInstance().getCacheDir(), false);

                getActivity().runOnUiThread(() -> progressBar.setProgress(50));

                if (Locale.getDefault().toString().equals("fr_FR"))
                    TagOptionSingleton.getInstance().setLanguage("fra");

                AudioFile audioFile = AudioFileIO.read(file);

                Tag tag = audioFile.getTag();

                if (title.equals(""))
                    tag.deleteField(FieldKey.TITLE);
                else
                    tag.setField(FieldKey.TITLE, title);

                if (artistName.equals(""))
                    tag.deleteField(FieldKey.ARTIST);
                else
                    tag.setField(FieldKey.ARTIST, artistName);

                if (albumName.equals(""))
                    tag.deleteField(FieldKey.ALBUM);
                else
                    tag.setField(FieldKey.ALBUM, albumName);

                if (genre.equals(""))
                    tag.deleteField(FieldKey.GENRE);
                else
                    tag.setField(FieldKey.GENRE, genre);

                if (year.equals(""))
                    tag.deleteField(FieldKey.YEAR);
                else
                    tag.setField(FieldKey.YEAR, year);

                if (track.equals(""))
                    tag.deleteField(FieldKey.TRACK);
                else
                    tag.setField(FieldKey.TRACK, track);

                if (disc.equals("0") || disc.equals(""))
                    tag.deleteField(FieldKey.DISC_NO);
                else
                    tag.setField(FieldKey.DISC_NO, disc);

                if (comment.equals(""))
                    tag.deleteField(FieldKey.COMMENT);
                else
                    tag.setField(FieldKey.COMMENT, comment);

                if (cover != null) {
                    File art = new File(cover);
                    Artwork cover = ArtworkFactory.createArtworkFromFile(art);

                    if (audioFile.getExt().equals("mp3"))
                        tag.deleteArtworkField();

                    tag.setField(cover);
                }

                audioFile.commit();

                File target = new File(pathSong);

                if (StorageHelper.copyFile(file, target, true)) {
                    success = true;
                    StorageHelper.deleteFile(file);
                }

                getActivity().runOnUiThread(() -> progressBar.setProgress(100));
            }

        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return success;
    }



    private boolean writeTagsAlbum() {

        boolean success = false;

        float step = 100/mSongList.size();
        int totalCount = 0;

        for (int i = 0; i < mSongList.size(); i++) {

            if (isCancel)
                break;

            totalCount += 1;

            int j = i;

            try {
                File song = new File(mSongList.get(i).getPath());

                getActivity().runOnUiThread(() -> file_name.setText(mSongList.get(j).getPath()));

                String filename = new File(mSongList.get(i).getPath()).getName();
                String pathCache = MusiqueApplication.getInstance().getCacheDir().getPath() + "/";
                String pathSong = new File(mSongList.get(i).getPath()).getParent();

                File file = new File(pathCache + filename);

                File test = new File(mSongList.get(i).getPath() + ".test");
                waitPermissionExternal = false;
                if (!StorageHelper.isWritable(test) && PreferenceUtil.getTreeUris() == null) {

                    Intent intent = new Intent();
                    intent.setAction(STORAGE_ACCESS_FRAMEWORK);
                    getContext().sendBroadcast(intent);

                    while (!waitPermissionExternal) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }


                if (!isCancel) {

                    if (file.exists())
                        StorageHelper.deleteFile(file);

                    StorageHelper.copyFile(song, MusiqueApplication.getInstance().getCacheDir(), false);

                    float tc1 = (step * totalCount) - ((step / 3)*2);
                    getActivity().runOnUiThread(() -> progressBar.setProgress((int) tc1));

                    if (Locale.getDefault().toString().equals("fr_FR"))
                        TagOptionSingleton.getInstance().setLanguage("fra");

                    AudioFile audioFile = AudioFileIO.read(file);
                    Tag tag = audioFile.getTag();

                    if (artistName.equals(""))
                        tag.deleteField(FieldKey.ARTIST);
                    else
                        tag.setField(FieldKey.ARTIST, artistName);

                    if (albumName.equals(""))
                        tag.deleteField(FieldKey.ALBUM);
                    else
                        tag.setField(FieldKey.ALBUM, albumName);

                    if (genre.equals(""))
                        tag.deleteField(FieldKey.GENRE);
                    else
                        tag.setField(FieldKey.GENRE, genre);

                    if (year.equals(""))
                        tag.deleteField(FieldKey.YEAR);
                    else
                        tag.setField(FieldKey.YEAR, year);

                    if (cover != null) {
                        File art = new File(cover);
                        Artwork cover = ArtworkFactory.createArtworkFromFile(art);

                        if (audioFile.getExt().equals("mp3"))
                            tag.deleteArtworkField();

                        tag.setField(cover);
                    }

                    audioFile.commit();

                    float tc2 = (step * totalCount) - (step / 3);
                    getActivity().runOnUiThread(() -> progressBar.setProgress((int) tc2));

                    File target = new File(pathSong);

                    if (StorageHelper.copyFile(file, target, true)) {
                        success = true;
                        StorageHelper.deleteFile(file);
                    }

                    float tc3 = (step * totalCount);
                    getActivity().runOnUiThread(() -> progressBar.setProgress((int) tc3));

                }

            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }

        if (cover != null) {
            getContext().getContentResolver().delete(ContentUris.withAppendedId(ARTWORK_URI, mAlbum.getId()), null, null);
            Uri uri = ContentUris.withAppendedId(ARTWORK_URI, mAlbum.getId());
            Picasso.get().invalidate(uri);
        }

        return success;
    }


    private final LoaderManager.LoaderCallbacks<List<Song>> mLoaderSongs = new LoaderManager.LoaderCallbacks<List<Song>>() {

        @Override
        public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
            SongLoader loader = new SongLoader(getActivity());

            loader.setSelection(MediaStore.Audio.Media.ALBUM_ID + " = ?", new String[]{String.valueOf(mAlbum.getId())});
            loader.setSortOrder(MediaStore.Audio.Media.TRACK);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<Song>> loader, List<Song> songList) {
            mSongList = songList;
            writeTask = new WriteTags().execute();
        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {}
    };


    public static void setPermExt() {
        waitPermissionExternal = true;
    }

    public static void cancel() {
        isCancel = true;
    }

}
