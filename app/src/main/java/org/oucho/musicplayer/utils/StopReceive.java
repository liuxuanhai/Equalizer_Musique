package org.oucho.musicplayer.utils;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import org.oucho.musicplayer.view.Notification;

public class StopReceive  extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        String etat = intent.getAction();
        String halt = intent.getStringExtra("halt");

        if ("org.oucho.musicplayer.STOP".equals(etat) && "stop".equals(halt)) {

            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                NotificationManager notificationManager;
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                assert notificationManager != null;
                notificationManager.cancel(Notification.NOTIFY_ID);

            }, 500);
        }
    }
}
