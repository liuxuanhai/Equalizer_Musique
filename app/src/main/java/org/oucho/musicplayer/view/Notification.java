package org.oucho.musicplayer.view;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;

import org.oucho.musicplayer.MainActivity;
import org.oucho.musicplayer.services.PlayerService;
import org.oucho.musicplayer.R;
import org.oucho.musicplayer.utils.BitmapHelper;
import org.oucho.musicplayer.widget.MusiqueWidget;

import java.io.IOException;

import static org.oucho.musicplayer.MusiqueKeys.ARTWORK_URI;

public class Notification {

    public static final int NOTIFY_ID = 32;

    private static boolean sIsServiceForeground = false;

    private static boolean timer = false;

    public static void setState(boolean onOff){
        timer = onOff;
    }


    public static void updateNotification(Context context, @NonNull final PlayerService playerbackService) {


        if (!PlayerService.hasPlaylist()) {
            removeNotification(playerbackService);
            return; // no need to go further since there is nothing to display
        }

        PendingIntent togglePlayIntent = PendingIntent.getService(playerbackService, 0,
                new Intent(playerbackService, PlayerService.class).setAction(PlayerService.ACTION_TOGGLE), 0);

        PendingIntent nextIntent = PendingIntent.getService(playerbackService, 0,
                new Intent(playerbackService, PlayerService.class).setAction(PlayerService.ACTION_NEXT), 0);

        PendingIntent previousIntent = PendingIntent.getService(playerbackService, 0,
                new Intent(playerbackService, PlayerService.class).setAction(PlayerService.ACTION_PREVIOUS), 0);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(playerbackService, "CHANNEL_ID");


        builder.setContentTitle(PlayerService.getSongTitle())
                .setContentText(PlayerService.getArtistName());

        int toggleResId = PlayerService.isPlaying() ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp;

        builder.addAction(R.drawable.ic_fast_rewind_white_24dp, "", previousIntent)
                .addAction(toggleResId, "", togglePlayIntent)
                .addAction(R.drawable.ic_fast_forward_white_24dp, "", nextIntent)
                .setVisibility(android.app.Notification.VISIBILITY_PUBLIC);


        Intent intent = new Intent(playerbackService, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendInt = PendingIntent.getActivity(playerbackService, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendInt);

        if (!timer) {
            builder.setSmallIcon(R.drawable.ic_audiotrack_white_24dp);
        } else {
            builder.setSmallIcon(R.drawable.ic_timer2_white_24dp);
        }

        builder.setShowWhen(false);
        builder.setColor(ContextCompat.getColor(playerbackService, R.color.grey_900));

        Resources res = playerbackService.getResources();

        @SuppressLint("PrivateResource")
        int artSize = (int) res.getDimension(R.dimen.notification_art_size);

        Bitmap b = null;

        Uri uri = ContentUris.withAppendedId(ARTWORK_URI, PlayerService.getAlbumId());

        try {
            ContentResolver contentResolver = context.getContentResolver();
            b = BitmapHelper.decode(contentResolver.openInputStream(uri), artSize, artSize);

        } catch (IOException ignored) {}


        if (b != null) {
            setBitmapAndBuild(b, playerbackService, builder);

        } else {

            // TODO image inexistante
            //setBitmapAndBuild(bitmap, playerbackService, builder);

        }

        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, MusiqueWidget.class));

        Intent widget = new Intent(context, MusiqueWidget.class);
        widget.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        widget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(widget);
    }

    private static void setBitmapAndBuild(Bitmap bitmap, @NonNull PlayerService playerbackService, NotificationCompat.Builder builder) {

        Bitmap image = bitmap;

        if (image == null) {
            BitmapDrawable d = ((BitmapDrawable) ContextCompat.getDrawable(playerbackService, R.drawable.ic_audiotrack_white_24dp));
            image = d.getBitmap();
        }
        builder.setLargeIcon(image);

        builder.setStyle(new MediaStyle()
                .setMediaSession(PlayerService.getMediaSession().getSessionToken())
                .setShowActionsInCompactView(0, 1, 2));

        android.app.Notification notification = builder.build();

        boolean startForeground = PlayerService.isPlaying();
        if (startForeground) {
            playerbackService.startForeground(NOTIFY_ID, notification);
        } else {
            if (sIsServiceForeground) {
                playerbackService.stopForeground(false);
            }
            NotificationManager notificationManager = (NotificationManager) playerbackService.getSystemService(Context.NOTIFICATION_SERVICE);
            assert notificationManager != null;
            notificationManager.notify(NOTIFY_ID, notification);
        }

        sIsServiceForeground = startForeground;
    }

    private static void removeNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.cancel(NOTIFY_ID);
    }
}