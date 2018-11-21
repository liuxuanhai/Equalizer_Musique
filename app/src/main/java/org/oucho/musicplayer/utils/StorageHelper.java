package org.oucho.musicplayer.utils;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import org.oucho.musicplayer.MusiqueApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;


public class StorageHelper {


    private static final String TAG = "StorageHelper";

    private static final String PRIMARY_VOLUME_NAME = "primary";


    public static boolean isWritable(@NonNull final File file) {
        boolean isExisting = file.exists();

        try {
            FileOutputStream output = new FileOutputStream(file, true);
            try {
                output.close();
            }
            catch (IOException ignore) {}
        }
        catch (java.io.FileNotFoundException e) {
            return false;
        }
        boolean result = file.canWrite();

        // Ensure that file is not created during this process.
        if (!isExisting) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }

        return result;
    }

    private static void scanFile(String[] paths) {
        MediaScannerConnection.scanFile(MusiqueApplication.getInstance(), paths, null, null);
    }

    private static File getTargetFile(File source, File targetDir) {
        File file = new File(targetDir, source.getName());
        if (!source.getParentFile().equals(targetDir) && !file.exists())
            deleteFile(file);

        return file;
    }

    public static boolean copyFile(@NonNull final File source, @NonNull final File targetDir, boolean scann) {
        InputStream inStream = null;
        OutputStream outStream = null;

        boolean success = false;

        File target = getTargetFile(source, targetDir);

        try {
            inStream = new FileInputStream(source);

            if (isWritable(target)) {

                FileChannel inChannel = new FileInputStream(source).getChannel();
                FileChannel outChannel = new FileOutputStream(target).getChannel();
                inChannel.transferTo(0, inChannel.size(), outChannel);
                success = true;

                try {
                    inChannel.close();
                } catch (Exception ignored) { }

                try {
                    outChannel.close();
                } catch (Exception ignored) { }

            } else {

                DocumentFile targetDocument = getDocumentFile(target);

                if (targetDocument != null)
                    outStream = MusiqueApplication.getInstance().getContentResolver().openOutputStream(targetDocument.getUri());

                if (outStream != null) {

                    byte[] buffer = new byte[4096]; // MAGIC_NUMBER
                    int bytesRead;
                    while ((bytesRead = inStream.read(buffer)) != -1)
                        outStream.write(buffer, 0, bytesRead);
                    success = true;
                }

            }

        } catch (Exception e) {
            Log.e(TAG, "Error when copying file from " + source.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
            return false;
        }

        finally {

            try {
                if (inStream != null)
                    inStream.close();
            } catch (Exception ignored) { }

            try {
                if (outStream != null)
                    outStream.close();
            } catch (Exception ignored) { }
        }

        if (success && scann)
            scanFile(new String[] { target.getPath() });
        return success;
    }


    public static void deleteFile(@NonNull final File file) {

        boolean success = false;

        if (file.delete())
            success =  true;

        if (!success) {
            DocumentFile document = getDocumentFile(file);
            if (document != null)
                document.delete();
        }
    }


    private static DocumentFile getDocumentFile(File file) {
        Uri treeUri;

        String baseFolder;

        String fullPath;
        try {
            fullPath = file.getCanonicalPath();
        } catch (IOException e) {
            return null;
        }

        treeUri = PreferenceUtil.getTreeUris();

        if (treeUri == null) {
            return null;
        }

        baseFolder = getFullPathFromTreeUri(MusiqueApplication.getInstance(), treeUri);

        if (baseFolder == null) {
            return null;
        }

        String relativePath = fullPath.substring(baseFolder.length() + 1);

        DocumentFile document = DocumentFile.fromTreeUri(MusiqueApplication.getInstance(), treeUri);

        String[] parts = relativePath.split("\\/");
        for (int i = 0; i < parts.length; i++) {
            DocumentFile nextDocument = document.findFile(parts[i]);

            if (nextDocument == null) {
                if (i < parts.length - 1) {
                    return null;
                } else {
                    nextDocument = document.createFile("image", parts[i]);
                }
            }
            document = nextDocument;
        }

        return document;
    }


    @Nullable
    private static String getFullPathFromTreeUri(Context context, Uri treeUri) {
        if (treeUri == null) {
            return null;
        }
        String volumePath = getVolumePath(context, getVolumeIdFromTreeUri(treeUri));
        if (volumePath == null) {
            return File.separator;
        }
        if (volumePath.endsWith(File.separator)) {
            volumePath = volumePath.substring(0, volumePath.length() - 1);
        }

        String documentPath = getDocumentPathFromTreeUri(treeUri);
        if (documentPath.endsWith(File.separator)) {
            documentPath = documentPath.substring(0, documentPath.length() - 1);
        }

        if (documentPath.length() > 0) {
            if (documentPath.startsWith(File.separator)) {
                return volumePath + documentPath;
            }
            else {
                return volumePath + File.separator + documentPath;
            }
        }
        else {
            return volumePath;
        }
    }

    private static String getVolumePath(Context context, final String volumeId) {

        try {
            StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);

            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");

            assert mStorageManager != null;
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getUuid = storageVolumeClazz.getMethod("getUuid");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
            Object result = getVolumeList.invoke(mStorageManager);

            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String uuid = (String) getUuid.invoke(storageVolumeElement);
                Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);

                // primary volume?
                if (primary && PRIMARY_VOLUME_NAME.equals(volumeId)) {
                    return (String) getPath.invoke(storageVolumeElement);
                }

                // other volumes?
                if (uuid != null) {
                    if (uuid.equals(volumeId)) {
                        return (String) getPath.invoke(storageVolumeElement);
                    }
                }
            }

            // not found.
            return null;
        }
        catch (Exception ex) {
            return null;
        }
    }


    private static String getVolumeIdFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");

        if (split.length > 0) {
            return split[0];
        }
        else {
            return null;
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getDocumentPathFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");
        if ((split.length >= 2) && (split[1] != null)) {
            return split[1];
        } else {
            return File.separator;
        }
    }


    private static boolean externalMemoryAvailable() {
        File[] storages = ContextCompat.getExternalFilesDirs(MusiqueApplication.getInstance(), null);
        return storages.length > 1 && storages[0] != null && storages[1] != null;

    }


    public static String getSdcardPath(Context context) {
        for(File file : context.getExternalFilesDirs("external")) {
            if (file != null && !file.equals(context.getExternalFilesDir("external"))) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");

                if (index < 0)
                    Log.w(TAG, "Unexpected external file dir: " + file.getAbsolutePath());
                else
                    return new File(file.getAbsolutePath().substring(0, index)).getPath();
            }
        }
        return null;
    }

    public static int getSourceFreeBytes(String path) {
        if (externalMemoryAvailable()) {
            StatFs stat = new StatFs(path);
            long bytesAvailable = stat.getAvailableBytes();
            return (int) bytesAvailable;
        } else {
            return -1;
        }
    }

    public static int getInternalFreeBytes() {
        StatFs stat = new StatFs(MusiqueApplication.getInstance().getCacheDir().getPath());
        long bytesAvailable = stat.getAvailableBytes();
        return (int) bytesAvailable;
    }



}
