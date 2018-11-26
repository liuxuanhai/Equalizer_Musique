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

package org.oucho.musicplayer.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.RemoteViews;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.services.PlayerService;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.utils.BitmapHelper;

import static org.oucho.musicplayer.MusiqueKeys.ARTWORK_URI;
import static org.oucho.musicplayer.services.PlayerService.isPlaying;


public class MusiqueWidget extends AppWidgetProvider {

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        // prenvent memory clean
        Intent player = new Intent(context, WidgetService.class);
        context.startService(player);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.musique_widget);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);


        PendingIntent togglePlayIntent = PendingIntent.getService(context, 0,
                new Intent(context, PlayerService.class).setAction(PlayerService.ACTION_TOGGLE), 0);

        PendingIntent nextIntent = PendingIntent.getService(context, 0,
                new Intent(context, PlayerService.class).setAction(PlayerService.ACTION_NEXT), 0);

        PendingIntent previousIntent = PendingIntent.getService(context, 0,
                new Intent(context, PlayerService.class).setAction(PlayerService.ACTION_PREVIOUS), 0);

        if (isPlaying()) {
            views.setImageViewResource(R.id.play, R.drawable.ic_pause_circle_filled_amber_a700_36dp);
        } else {
            views.setImageViewResource(R.id.play, R.drawable.ic_play_circle_filled_amber_a700_36dp);
        }

        views.setOnClickPendingIntent(R.id.play, togglePlayIntent);
        views.setOnClickPendingIntent(R.id.next, nextIntent);
        views.setOnClickPendingIntent(R.id.prev, previousIntent);

        Uri uri = ContentUris.withAppendedId(ARTWORK_URI, PlayerService.getAlbumId());

        Bitmap b = null;

        try {
            ContentResolver contentResolver = context.getContentResolver();
            b = BitmapHelper.decode(contentResolver.openInputStream(uri), 55, 55);

        } catch (Exception ignored) {}

        views.setImageViewBitmap(R.id.cover, b);

        String title = PlayerService.getSongTitle();
        String album = PlayerService.getAlbumName();

        views.setTextViewText(R.id.titre, title);
        views.setTextViewText(R.id.album, album);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

}

