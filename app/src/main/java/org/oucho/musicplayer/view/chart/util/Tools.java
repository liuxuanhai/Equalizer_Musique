package org.oucho.musicplayer.view.chart.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;



public final class Tools {

    private Tools() {
    }

    public static float fromDpToPx(float dp) {

        try {
            return dp * Resources.getSystem().getDisplayMetrics().density;
        } catch (Exception e) {
            return dp;
        }
    }

    public static Bitmap drawableToBitmap(@NonNull Drawable drawable) {

        if (drawable instanceof BitmapDrawable)
            return ((BitmapDrawable) drawable).getBitmap();

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

}
