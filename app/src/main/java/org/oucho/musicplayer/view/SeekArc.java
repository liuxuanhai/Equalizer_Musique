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

package org.oucho.musicplayer.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.oucho.musicplayer.R;


public class SeekArc extends View {

	private static final String TAG_LOG = "SeeakArc";
	private static final int INVALID_PROGRESS_VALUE = -1;

    private Drawable mThumb;

	private int mMax = 100;
	private int mProgress = 0;
	private int mProgressWidth = 4;
	private int mArcWidth = 2;
	private int mStartAngle = 0;
	private int mSweepAngle = 360;
	private int mRotation = 0;

	private boolean mRoundedEdges = false;
	private boolean mTouchInside = true;
	private boolean mClockwise = true;
	private boolean mEnabled = true;

	private int mArcRadius = 0;
	private float mProgressSweep = 0;
	private final RectF mArcRect = new RectF();
	private Paint mArcPaint;
	private Paint mProgressPaint;
	private int mTranslateX;
	private int mTranslateY;
	private int mThumbXPos;
	private int mThumbYPos;
    private float mTouchIgnoreRadius;
	private OnSeekArcChangeListener mOnSeekArcChangeListener;

	public interface OnSeekArcChangeListener {

		void onProgressChanged(int progress);

		@SuppressWarnings("EmptyMethod")
		void onStartTrackingTouch();

		@SuppressWarnings("EmptyMethod")
		void onStopTrackingTouch();
	}

	public SeekArc(Context context) {
		super(context);
		init(context, null, 0);
	}

	public SeekArc(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, R.attr.seekArcStyle);
	}

	public SeekArc(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}

	private void init(Context context, AttributeSet attrs, int defStyle) {

		Log.i(TAG_LOG, "Initialising SeekArc");
		float density = context.getResources().getDisplayMetrics().density;

		int arcColor = ContextCompat.getColor(context, R.color.grey_300);
		int progressColor0 = ContextCompat.getColor(context, R.color.colorAccent);
		int thumbHalfheight;
		int thumbHalfWidth;
		mThumb = ContextCompat.getDrawable(context, R.drawable.seek_arc_control_selector);

		mProgressWidth = (int) (mProgressWidth * density);

		
		if (attrs != null) {

			final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekArc, defStyle, 0);

			Drawable thumb = a.getDrawable(R.styleable.SeekArc_thumb);
			if (thumb != null) {
				mThumb = thumb;
			}

			
			thumbHalfheight = mThumb.getIntrinsicHeight() / 2;
			thumbHalfWidth = mThumb.getIntrinsicWidth() / 2;
			mThumb.setBounds(-thumbHalfWidth, -thumbHalfheight, thumbHalfWidth, thumbHalfheight);

			mMax = a.getInteger(R.styleable.SeekArc_max, mMax);
			mProgress = a.getInteger(R.styleable.SeekArc_progress, mProgress);
			mProgressWidth = (int) a.getDimension(R.styleable.SeekArc_progressWidth, mProgressWidth);
			mArcWidth = (int) a.getDimension(R.styleable.SeekArc_arcWidth, mArcWidth);
			mStartAngle = a.getInt(R.styleable.SeekArc_startAngle, mStartAngle);
			mSweepAngle = a.getInt(R.styleable.SeekArc_sweepAngle, mSweepAngle);
			mRotation = a.getInt(R.styleable.SeekArc_rotation, mRotation);
			mRoundedEdges = a.getBoolean(R.styleable.SeekArc_roundEdges, mRoundedEdges);
			mTouchInside = a.getBoolean(R.styleable.SeekArc_touchInside, mTouchInside);
			mClockwise = a.getBoolean(R.styleable.SeekArc_clockwise, mClockwise);
			mEnabled = a.getBoolean(R.styleable.SeekArc_enabled, mEnabled);

			arcColor = a.getColor(R.styleable.SeekArc_arcColor, arcColor);
			progressColor0 = a.getColor(R.styleable.SeekArc_progressColor0, progressColor0);

			a.recycle();
		}

		mProgress = (mProgress > mMax) ? mMax : mProgress;
		mProgress = (mProgress < 0) ? 0 : mProgress;

		mSweepAngle = (mSweepAngle > 360) ? 360 : mSweepAngle;
		mSweepAngle = (mSweepAngle < 0) ? 0 : mSweepAngle;

		mProgressSweep = (float) mProgress / mMax * mSweepAngle;

		mStartAngle = (mStartAngle > 360) ? 0 : mStartAngle;
		mStartAngle = (mStartAngle < 0) ? 0 : mStartAngle;

		mArcPaint = new Paint();
		mArcPaint.setColor(arcColor);
		mArcPaint.setAntiAlias(true);
		mArcPaint.setStyle(Paint.Style.STROKE);
		mArcPaint.setStrokeWidth(mArcWidth);

		mProgressPaint = new Paint();
		mProgressPaint.setColor(progressColor0);
		mProgressPaint.setAntiAlias(true);
		mProgressPaint.setStyle(Paint.Style.STROKE);
		mProgressPaint.setStrokeWidth(mProgressWidth);

		if (mRoundedEdges) {
			mArcPaint.setStrokeCap(Paint.Cap.ROUND);
			mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {		
		if(!mClockwise) {
			canvas.scale(-1, 1, mArcRect.centerX(), mArcRect.centerY() );
		}

        int mAngleOffset = -90;
        final int arcStart = mStartAngle + mAngleOffset + mRotation;
		final int arcSweep = mSweepAngle;
		canvas.drawArc(mArcRect, arcStart, arcSweep, false, mArcPaint);
		canvas.drawArc(mArcRect, arcStart, mProgressSweep, false, mProgressPaint);

		if(mEnabled) {

			canvas.translate(mTranslateX - mThumbXPos, mTranslateY - mThumbYPos);
			mThumb.draw(canvas);
		}
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		final int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
		final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
		final int min = Math.min(width, height);
		float top;
		float left;
		int arcDiameter;

		mTranslateX = (int) (width * 0.5f);
		mTranslateY = (int) (height * 0.5f);
		
		arcDiameter = min - getPaddingLeft();
		mArcRadius = arcDiameter / 2;
		top = height / 2 - (arcDiameter / 2);
		left = width / 2 - (arcDiameter / 2);
		mArcRect.set(left, top, left + arcDiameter, top + arcDiameter);
	
		int arcStart = (int)mProgressSweep + mStartAngle  + mRotation + 90;
		mThumbXPos = (int) (mArcRadius * Math.cos(Math.toRadians(arcStart)));
		mThumbYPos = (int) (mArcRadius * Math.sin(Math.toRadians(arcStart)));
		
		setTouchInSide(mTouchInside);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mEnabled) {
			this.getParent().requestDisallowInterceptTouchEvent(true);

			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					onStartTrackingTouch();
					updateOnTouch(event);
					break;

				case MotionEvent.ACTION_MOVE:
					updateOnTouch(event);
					break;

				case MotionEvent.ACTION_UP:
					onStopTrackingTouch();
					setPressed(false);
					this.getParent().requestDisallowInterceptTouchEvent(false);
					break;

				case MotionEvent.ACTION_CANCEL:
					onStopTrackingTouch();
					setPressed(false);
					this.getParent().requestDisallowInterceptTouchEvent(false);
					break;

				default:
					break;
			}
			return true;
		}
		return false;
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		if (mThumb != null && mThumb.isStateful()) {
			int[] state = getDrawableState();
			mThumb.setState(state);
		}
		invalidate();
	}

	private void onStartTrackingTouch() {
		if (mOnSeekArcChangeListener != null) {
			mOnSeekArcChangeListener.onStartTrackingTouch();
		}
	}

	private void onStopTrackingTouch() {
		if (mOnSeekArcChangeListener != null) {
			mOnSeekArcChangeListener.onStopTrackingTouch();
		}
	}

	private void updateOnTouch(MotionEvent event) {
		boolean ignoreTouch = ignoreTouch(event.getX(), event.getY());
		if (ignoreTouch) {
			return;
		}
		setPressed(true);
        double mTouchAngle = getTouchDegrees(event.getX(), event.getY());
		int progress = getProgressForAngle(mTouchAngle);
		onProgressRefresh(progress);
	}

	private boolean ignoreTouch(float xPos, float yPos) {
		boolean ignore = false;
		float x = xPos - mTranslateX;
		float y = yPos - mTranslateY;

		float touchRadius = (float) Math.sqrt(((x * x) + (y * y)));
		if (touchRadius < mTouchIgnoreRadius) {
			ignore = true;
		}
		return ignore;
	}

	private double getTouchDegrees(float xPos, float yPos) {
		float x = xPos - mTranslateX;
		float y = yPos - mTranslateY;
		//invert the x-coord if we are rotating anti-clockwise
		x= (mClockwise) ? x:-x;
		// convert to arc Angle
		double angle = Math.toDegrees(Math.atan2(y, x) + (Math.PI / 2)
				- Math.toRadians(mRotation));
		if (angle < 0) {
			angle = 360 + angle;
		}
		angle -= mStartAngle;
		return angle;
	}

	private int getProgressForAngle(double angle) {
		int touchProgress = (int) Math.round(valuePerDegree() * angle);

		touchProgress = (touchProgress < 0) ? INVALID_PROGRESS_VALUE : touchProgress;
		touchProgress = (touchProgress > mMax) ? INVALID_PROGRESS_VALUE : touchProgress;
		return touchProgress;
	}

	private float valuePerDegree() {
		return (float) mMax / mSweepAngle;
	}

	private void onProgressRefresh(int progress) {
		updateProgress(progress);
	}

	private void updateThumbPosition() {
		int thumbAngle = (int) (mStartAngle + mProgressSweep + mRotation + 90);
		mThumbXPos = (int) (mArcRadius * Math.cos(Math.toRadians(thumbAngle)));
		mThumbYPos = (int) (mArcRadius * Math.sin(Math.toRadians(thumbAngle)));
	}
	
	private void updateProgress(int progress) {

		if (progress == INVALID_PROGRESS_VALUE) {
			return;
		}

		progress = (progress > mMax) ? mMax : progress;
		progress = (progress < 0) ? 0 : progress;
		mProgress = progress;

		if (mOnSeekArcChangeListener != null) {
			mOnSeekArcChangeListener.onProgressChanged(progress);
		}

		mProgressSweep = (float) progress / mMax * mSweepAngle;

		updateThumbPosition();

		invalidate();
	}

	public void setOnSeekArcChangeListener(OnSeekArcChangeListener l) {
		mOnSeekArcChangeListener = l;
	}

    public int getProgress() {
		return mProgress;
	}

    private void setTouchInSide(boolean isEnabled) {
		int thumbHalfheight = mThumb.getIntrinsicHeight() / 2;
		int thumbHalfWidth = mThumb.getIntrinsicWidth() / 2;
		mTouchInside = isEnabled;
		if (mTouchInside) {
			mTouchIgnoreRadius = (float) mArcRadius / 4;
		} else {

			mTouchIgnoreRadius = mArcRadius - Math.min(thumbHalfWidth, thumbHalfheight);
		}
	}

    public boolean isEnabled() {
		return mEnabled;
	}

	public void setEnabled(boolean enabled) {
		this.mEnabled = enabled;
	}

}
