package org.oucho.musicplayer.view.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;


class BlockingBlurController implements BlurController {

    private static final String TAG_LOG = "BlockingBlurController";


    //Bitmap size should be divisible by 16 to meet stride requirement
    private static final int ROUNDING_VALUE = 16;

    private static final float scaleFactor = DEFAULT_SCALE_FACTOR;
    private float blurRadius = DEFAULT_BLUR_RADIUS;
    private float roundingWidthScaleFactor = 1f;
    private float roundingHeightScaleFactor = 1f;

    private BlurAlgorithm blurAlgorithm;
    private Canvas internalCanvas;

    /**
     * Draw view hierarchy here.
     * Blur it.
     * Draw it on BlurView's canvas.
     */
    private Bitmap internalBitmap;

    private final View blurView;
    private final ViewGroup rootView;
    private final Rect relativeViewBounds = new Rect();

    private final ViewTreeObserver.OnPreDrawListener drawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {

            if (!isMeDrawingNow) {
                updateBlur();
            }
            return true;
        }
    };

    //Used to distinct parent draw() calls from Controller's draw() calls
    private boolean isMeDrawingNow;

    //must be set from message queue
    private final Runnable onDrawEndTask = new Runnable() {
        @Override
        public void run() {

            isMeDrawingNow = false;
        }
    };

    private boolean shouldTryToOffsetCoords = true;

    /**
     * @param blurView View which will draw it's blurred underlying content
     * @param rootView Root View where blurView's underlying content starts drawing.
     *                 Can be Activity's root content layout (android.R.id.content)
     *                 or some of your custom root layouts.
     */
    public BlockingBlurController(@NonNull View blurView, @NonNull ViewGroup rootView) {
        Log.w(TAG_LOG, "BlockingBlurController()");

        this.rootView = rootView;
        this.blurView = blurView;
        this.blurAlgorithm = new RenderScriptBlur(blurView.getContext(), true);

        int measuredWidth = blurView.getMeasuredWidth();
        int measuredHeight = blurView.getMeasuredHeight();

        if (isZeroSized(measuredWidth, measuredHeight)) {
            deferBitmapCreation();
            return;
        }

        init(measuredWidth, measuredHeight);
    }

    private int downScaleSize(float value) {

        return (int) Math.ceil(value / scaleFactor);
    }

    /**
     * Rounds a value to the nearest divisible by {@link #ROUNDING_VALUE} to meet stride requirement
     */
    private int roundSize(int value) {

        if (value % ROUNDING_VALUE == 0) {
            return value;
        }
        return value - (value % ROUNDING_VALUE) + ROUNDING_VALUE;
    }

    private void init(int measuredWidth, int measuredHeight) {

        Log.w(TAG_LOG, "init()");

        if (isZeroSized(measuredWidth, measuredHeight)) {
            blurView.setWillNotDraw(true);
            setBlurAutoUpdate(false);
            return;
        }
        blurView.setWillNotDraw(false);
        allocateBitmap(measuredWidth, measuredHeight);
        internalCanvas = new Canvas(internalBitmap);
        setBlurAutoUpdate(true);
    }

    private boolean isZeroSized(int measuredWidth, int measuredHeight) {

        return downScaleSize(measuredHeight) == 0 || downScaleSize(measuredWidth) == 0;
    }

    private void updateBlur() {

        isMeDrawingNow = true;
        blurView.invalidate();
    }

    /**
     * Deferring initialization until view is laid out
     */
    private void deferBitmapCreation() {
        Log.w(TAG_LOG, "deferBitmapCreation");

        blurView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                blurView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                int measuredWidth = blurView.getMeasuredWidth();
                int measuredHeight = blurView.getMeasuredHeight();

                init(measuredWidth, measuredHeight);
            }

        });
    }

    private void allocateBitmap(int measuredWidth, int measuredHeight) {

        //downscale overlay (blurred) bitmap
        int nonRoundedScaledWidth = downScaleSize(measuredWidth);
        int nonRoundedScaledHeight = downScaleSize(measuredHeight);

        int scaledWidth = roundSize(nonRoundedScaledWidth);
        int scaledHeight = roundSize(nonRoundedScaledHeight);

        roundingHeightScaleFactor = (float) nonRoundedScaledHeight / scaledHeight;
        roundingWidthScaleFactor = (float) nonRoundedScaledWidth / scaledWidth;

        internalBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, blurAlgorithm.getSupportedBitmapConfig());
    }

    //draw starting from blurView's position
    private void setupInternalCanvasMatrix() {

        //Log.w(TAG_LOG, "setupInternalCanvasMatrix()");

        blurView.getDrawingRect(relativeViewBounds);

        if (shouldTryToOffsetCoords) {
            try {
                rootView.offsetDescendantRectToMyCoords(blurView, relativeViewBounds);
            } catch (IllegalArgumentException e) {
                // BlurView is not a child of the rootView (i.e. it's in Dialog)  Fallback to regular coordinates system
                        shouldTryToOffsetCoords = false;
            }
        }

        float scaleFactorX = scaleFactor * roundingWidthScaleFactor;
        float scaleFactorY = scaleFactor * roundingHeightScaleFactor;

        float scaledLeftPosition = -relativeViewBounds.left / scaleFactorX;
        float scaledTopPosition = -relativeViewBounds.top / scaleFactorY;

        float scaledTranslationX = blurView.getTranslationX() / scaleFactorX;
        float scaledTranslationY = blurView.getTranslationY() / scaleFactorY;

        internalCanvas.translate(scaledLeftPosition - scaledTranslationX, scaledTopPosition - scaledTranslationY);
        internalCanvas.scale(1f / scaleFactorX, 1f / scaleFactorY);
    }

    /**
     * Draws whole view hierarchy on internal canvas
     */
    private void drawUnderlyingViews() {
        rootView.draw(internalCanvas);
    }

    @Override
    public void drawBlurredContent(Canvas canvas) {

        isMeDrawingNow = true;

        internalCanvas.save();
        setupInternalCanvasMatrix();
        drawUnderlyingViews();
        internalCanvas.restore();

        blurAndSave();
        draw(canvas);
    }

    private void draw(Canvas canvas) {
        canvas.save();
        canvas.scale(scaleFactor * roundingWidthScaleFactor, scaleFactor * roundingHeightScaleFactor);
        canvas.drawBitmap(internalBitmap, 0, 0, null);
        canvas.restore();
    }

    @Override
    public void onDrawEnd() {
        blurView.post(onDrawEndTask);
    }

    private void blurAndSave() {
        internalBitmap = blurAlgorithm.blur(internalBitmap, blurRadius);
    }

    @Override
    public void updateBlurViewSize() {
        int measuredWidth = blurView.getMeasuredWidth();
        int measuredHeight = blurView.getMeasuredHeight();

        init(measuredWidth, measuredHeight);
    }

    @Override
    public void destroy() {
        Log.w(TAG_LOG, "destroy()");

        setBlurAutoUpdate(false);
        blurAlgorithm.destroy();
        if (internalBitmap != null) {
            internalBitmap.recycle();
        }
    }

    @Override
    public void setBlurRadius(float radius) {
        this.blurRadius = radius;
    }

    @Override
    public void setBlurAlgorithm(BlurAlgorithm algorithm) {
        this.blurAlgorithm = algorithm;
    }

    @Override
    public void setBlurAutoUpdate(boolean enabled) {

        Log.w(TAG_LOG, "setBlurAutoUpdate()");

        blurView.getViewTreeObserver().removeOnPreDrawListener(drawListener);
        if (enabled) {
            blurView.getViewTreeObserver().addOnPreDrawListener(drawListener);
        }
    }
}
