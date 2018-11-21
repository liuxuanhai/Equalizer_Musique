package org.oucho.musicplayer.view.blurview;

import android.graphics.Canvas;

interface BlurController {
    float DEFAULT_SCALE_FACTOR = 8f;
    float DEFAULT_BLUR_RADIUS = 16f;

    void destroy();

    void updateBlurViewSize();
    void onDrawEnd();
    void setBlurRadius(float radius);

    void drawBlurredContent(Canvas canvas);

    void setBlurAutoUpdate(boolean enabled);

    void setBlurAlgorithm(BlurAlgorithm algorithm);
}
