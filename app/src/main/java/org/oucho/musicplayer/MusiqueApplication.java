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

package org.oucho.musicplayer;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;

import org.oucho.musicplayer.bugDroid.IMMLeaks;
import org.oucho.musicplayer.equalizer.AudioEffects;
import org.oucho.musicplayer.utils.PrefSort;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class MusiqueApplication extends Application {

    private static MusiqueApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        setInstance(this);

        IMMLeaks.fixFocusedViewLeak(this);

        PrefSort.init(this);
        AudioEffects.init(this);

        ActivityManager actManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        assert actManager != null;
        actManager.getMemoryInfo(memInfo);
        long totalMemory = memInfo.totalMem / 1024 / 1024;

        Picasso picasso;

        int pool = (Runtime.getRuntime().availableProcessors() * 2) +1 ;

        if (totalMemory < 1500) {
            picasso = new Picasso.Builder(this)
                    .defaultBitmapConfig(Bitmap.Config.RGB_565)
                    .executor(new ThreadPoolExecutor(pool, pool, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()))
                    .build();
        } else if (totalMemory < 2500) {
            picasso = new Picasso.Builder(this)
                    .memoryCache(new LruCache(262144000))
                    .executor(new ThreadPoolExecutor(pool, pool, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()))
                    .build();
        } else {
            picasso = new Picasso.Builder(this)
                    .memoryCache(new LruCache(536870912))
                    .executor(new ThreadPoolExecutor(pool, pool, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()))
                    .build();
        }

        Picasso.setSingletonInstance(picasso);

        File file = new File(this.getDir("bin", 0), "ffmpeg");

        if (!file.exists())
            new CopyAssets(file).execute();
    }


    private static class CopyAssets extends AsyncTask<String, Integer, Boolean> {

        final File destination;

        CopyAssets(File destination) {
            this.destination = destination;
        }

        @Override
        protected Boolean doInBackground(String... arg0) {

            String arch = System.getProperty("os.arch");
            String filename;

            if ("i686".equals(arch))
                if (android.os.Build.MODEL.equals("ASUS_Z008D") || android.os.Build.MODEL.equals("ASUS_Z00AD"))
                    filename = "ffmpeg_x86_64";
                else
                    filename = "ffmpeg_x86";
            else
                filename = "ffmpeg_arm";

            try {
                InputStream is = MusiqueApplication.getInstance().getAssets().open(filename);
                OutputStream os = new FileOutputStream(destination);

                byte[] buffer = new byte[1024];
                while (is.read(buffer) > 0) {
                    os.write(buffer);
                }

                os.flush();
                os.close();
                is.close();

                final String abspath = destination.getAbsolutePath();
                Runtime.getRuntime().exec("chmod " + 755 + " " + abspath).waitFor();

            } catch (IOException | InterruptedException e) {
                Log.d("MusiqueApplication", "Error copy ffmpeg binary: " + e);
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {

        }
    }

    public static synchronized MusiqueApplication getInstance() {
        return sInstance;
    }

    private static void setInstance(MusiqueApplication value) {
        sInstance = value;
    }

 }
