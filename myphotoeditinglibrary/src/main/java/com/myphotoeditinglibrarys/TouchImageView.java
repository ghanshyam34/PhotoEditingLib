package com.myphotoeditinglibrarys;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.OverScroller;
import android.widget.Scroller;

import com.myphotoeditinglibrary.R;

import java.util.ArrayList;
import java.util.List;

public class TouchImageView extends ImageView {

    public interface Constants {
        float SELECTED_LAYER_ALPHA = 0.15F;
    }

    public interface FrameViewCallback {
        void onEntitySelected(@Nullable TouchImageView.BaseEntity entity);
        void onEntityDoubleTap(@NonNull TouchImageView.BaseEntity entity);
    }

    private final List<TouchImageView.BaseEntity> entities = new ArrayList<>();
    @Nullable
    private TouchImageView.BaseEntity selectedEntity;

    private Paint selectedLayerPaint;

    @Nullable
    private FrameViewCallback frameViewCallback;

    private ScaleGestureDetector scaleGestureDetector;
    private TouchImageView.RotateGestureDetector rotateGestureDetector;
    private TouchImageView.MoveGestureDetector moveGestureDetector;
    private GestureDetectorCompat gestureDetectorCompat;

    private static final String DEBUG = "DEBUG";

    //
    // SuperMin and SuperMax multipliers. Determine how much the image can be
    // zoomed below or above the zoom boundaries, before animating back to the
    // min/max zoom boundary.
    //
    private static final float SUPER_MIN_MULTIPLIER = .75f;
    private static final float SUPER_MAX_MULTIPLIER = 1.25f;

    //
    // Scale of image ranges from minScale to maxScale, where minScale == 1
    // when the image is stretched to fit view.
    //
    private float normalizedScale;

    //
    // Matrix applied to image. MSCALE_X and MSCALE_Y should always be equal.
    // MTRANS_X and MTRANS_Y are the other values used. prevMatrix is the matrix
    // saved prior to the screen rotating.
    //
    private Matrix matrix, prevMatrix;

    private static enum State { NONE, DRAG, ZOOM, FLING, ANIMATE_ZOOM };
    private State state;

    private float minScale;
    private float maxScale;
    private float superMinScale;
    private float superMaxScale;
    private float[] m;

    private Context context;
    private Fling fling;

    private ScaleType mScaleType;

    private boolean imageRenderedAtLeastOnce;
    private boolean onDrawReady;

    private ZoomVariables delayedZoomVariables;

    //
    // Size of view and previous view size (ie before rotation)
    //
    private int viewWidth, viewHeight, prevViewWidth, prevViewHeight;

    //
    // Size of image when it is stretched to fit view. Before and After rotation.
    //
    private float matchViewWidth, matchViewHeight, prevMatchViewWidth, prevMatchViewHeight;

    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;
    private GestureDetector.OnDoubleTapListener doubleTapListener = null;
    private OnTouchListener userTouchListener = null;
    private OnTouchImageViewListener touchImageViewListener = null;

    public TouchImageView(Context context) {
        super(context);
        sharedConstructing(context);
    }

    public TouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sharedConstructing(context);
    }

    public TouchImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        sharedConstructing(context);
    }

    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TouchImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        sharedConstructing(context);
    }

    private void sharedConstructing(Context context) {
        super.setClickable(true);
        setWillNotDraw(false);
        this.context = context;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(context, new GestureListener());
        matrix = new Matrix();
        prevMatrix = new Matrix();
        m = new float[9];
        normalizedScale = 1;
        if (mScaleType == null) {
            mScaleType = ScaleType.FIT_CENTER;
        }
        minScale = 1;
        maxScale = 3;
        superMinScale = SUPER_MIN_MULTIPLIER * minScale;
        superMaxScale = SUPER_MAX_MULTIPLIER * maxScale;
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);
        setState(State.NONE);
        onDrawReady = false;
//        super.setOnTouchListener(new PrivateOnTouchListener());
        init(getContext());
    }

    @Override
    public void setOnTouchListener(View.OnTouchListener l) {
        super.setOnTouchListener(l);
        userTouchListener = l;
    }

    public void setOnTouchImageViewListener(OnTouchImageViewListener l) {
        touchImageViewListener = l;
    }

    public void setOnDoubleTapListener(GestureDetector.OnDoubleTapListener l) {
        doubleTapListener = l;
    }

//    @Override
//    public void setImageResource(int resId) {
//        super.setImageResource(resId);
//        savePreviousImageValues();
//        fitImageToView();
//    }
//
//    @Override
//    public void setImageBitmap(Bitmap bm) {
//        super.setImageBitmap(bm);
//        savePreviousImageValues();
//        fitImageToView();
//    }
//
//    @Override
//    public void setImageDrawable(Drawable drawable) {
//        super.setImageDrawable(drawable);
//        savePreviousImageValues();
//        fitImageToView();
//    }
//
//    @Override
//    public void setImageURI(Uri uri) {
//        super.setImageURI(uri);
//        savePreviousImageValues();
//        fitImageToView();
//    }

    public void fitImage(){
        savePreviousImageValues();
        fitImageToView();
    }

    @Override
    public void setScaleType(ScaleType type) {
        if (type == ScaleType.FIT_START || type == ScaleType.FIT_END) {
            throw new UnsupportedOperationException("TouchImageView does not support FIT_START or FIT_END");
        }
        if (type == ScaleType.MATRIX) {
            super.setScaleType(ScaleType.MATRIX);

        } else {
            mScaleType = type;
            if (onDrawReady) {
                //
                // If the image is already rendered, scaleType has been called programmatically
                // and the TouchImageView should be updated with the new scaleType.
                //
                setZoom(this);
            }
        }
    }

    @Override
    public ScaleType getScaleType() {
        return mScaleType;
    }

    /**
     * Returns false if image is in initial, unzoomed state. False, otherwise.
     * @return true if image is zoomed
     */
    public boolean isZoomed() {
        return normalizedScale != 1;
    }

    /**
     * Return a Rect representing the zoomed image.
     * @return rect representing zoomed image
     */
    public RectF getZoomedRect() {
        if (mScaleType == ScaleType.FIT_XY) {
            throw new UnsupportedOperationException("getZoomedRect() not supported with FIT_XY");
        }
        PointF topLeft = transformCoordTouchToBitmap(0, 0, true);
        PointF bottomRight = transformCoordTouchToBitmap(viewWidth, viewHeight, true);

        float w = getDrawable().getIntrinsicWidth();
        float h = getDrawable().getIntrinsicHeight();
        return new RectF(topLeft.x / w, topLeft.y / h, bottomRight.x / w, bottomRight.y / h);
    }

    /**
     * Save the current matrix and view dimensions
     * in the prevMatrix and prevView variables.
     */
    private void savePreviousImageValues() {
        if (matrix != null && viewHeight != 0 && viewWidth != 0) {
            matrix.getValues(m);
            prevMatrix.setValues(m);
            prevMatchViewHeight = matchViewHeight;
            prevMatchViewWidth = matchViewWidth;
            prevViewHeight = viewHeight;
            prevViewWidth = viewWidth;
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putFloat("saveScale", normalizedScale);
        bundle.putFloat("matchViewHeight", matchViewHeight);
        bundle.putFloat("matchViewWidth", matchViewWidth);
        bundle.putInt("viewWidth", viewWidth);
        bundle.putInt("viewHeight", viewHeight);
        matrix.getValues(m);
        bundle.putFloatArray("matrix", m);
        bundle.putBoolean("imageRendered", imageRenderedAtLeastOnce);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            normalizedScale = bundle.getFloat("saveScale");
            m = bundle.getFloatArray("matrix");
            prevMatrix.setValues(m);
            prevMatchViewHeight = bundle.getFloat("matchViewHeight");
            prevMatchViewWidth = bundle.getFloat("matchViewWidth");
            prevViewHeight = bundle.getInt("viewHeight");
            prevViewWidth = bundle.getInt("viewWidth");
            imageRenderedAtLeastOnce = bundle.getBoolean("imageRendered");
            super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
            return;
        }

        super.onRestoreInstanceState(state);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.i("onDraw",""+canvas);
        onDrawReady = true;
        imageRenderedAtLeastOnce = true;
        if (delayedZoomVariables != null) {
            setZoom(delayedZoomVariables.scale, delayedZoomVariables.focusX, delayedZoomVariables.focusY, delayedZoomVariables.scaleType);
            delayedZoomVariables = null;
        }

        if(bitmap != null) {
            canvas.drawBitmap(bitmap, null, new RectF(0, 0, width, height), paint);
        }
        drawAllEntities(canvas,paint);
        Log.i("onDraw bitmap ===== ",""+bitmap+"     "+entities);
        super.onDraw(canvas);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        savePreviousImageValues();
    }

    /**
     * Get the max zoom multiplier.
     * @return max zoom multiplier.
     */
    public float getMaxZoom() {
        return maxScale;
    }

    /**
     * Set the max zoom multiplier. Default value: 3.
     * @param max max zoom multiplier.
     */
    public void setMaxZoom(float max) {
        maxScale = max;
        superMaxScale = SUPER_MAX_MULTIPLIER * maxScale;
    }

    /**
     * Get the min zoom multiplier.
     * @return min zoom multiplier.
     */
    public float getMinZoom() {
        return minScale;
    }

    /**
     * Get the current zoom. This is the zoom relative to the initial
     * scale, not the original resource.
     * @return current zoom multiplier.
     */
    public float getCurrentZoom() {
        return normalizedScale;
    }

    /**
     * Set the min zoom multiplier. Default value: 1.
     * @param min min zoom multiplier.
     */
    public void setMinZoom(float min) {
        minScale = min;
        superMinScale = SUPER_MIN_MULTIPLIER * minScale;
    }

    /**
     * Reset zoom and translation to initial state.
     */
    public void resetZoom() {
        normalizedScale = 1;
        fitImageToView();
    }

    /**
     * Set zoom to the specified scale. Image will be centered by default.
     * @param scale
     */
    public void setZoom(float scale) {
        setZoom(scale, 0.5f, 0.5f);
    }

    /**
     * Set zoom to the specified scale. Image will be centered around the point
     * (focusX, focusY). These floats range from 0 to 1 and denote the focus point
     * as a fraction from the left and top of the view. For example, the top left
     * corner of the image would be (0, 0). And the bottom right corner would be (1, 1).
     * @param scale
     * @param focusX
     * @param focusY
     */
    public void setZoom(float scale, float focusX, float focusY) {
        setZoom(scale, focusX, focusY, mScaleType);
    }

    /**
     * Set zoom to the specified scale. Image will be centered around the point
     * (focusX, focusY). These floats range from 0 to 1 and denote the focus point
     * as a fraction from the left and top of the view. For example, the top left
     * corner of the image would be (0, 0). And the bottom right corner would be (1, 1).
     * @param scale
     * @param focusX
     * @param focusY
     * @param scaleType
     */
    public void setZoom(float scale, float focusX, float focusY, ScaleType scaleType) {
        //
        // setZoom can be called before the image is on the screen, but at this point,
        // image and view sizes have not yet been calculated in onMeasure. Thus, we should
        // delay calling setZoom until the view has been measured.
        //
        if (!onDrawReady) {
            delayedZoomVariables = new ZoomVariables(scale, focusX, focusY, scaleType);
            return;
        }

        if (scaleType != mScaleType) {
            setScaleType(scaleType);
        }
        resetZoom();
        scaleImage(scale, viewWidth / 2, viewHeight / 2, true);
        matrix.getValues(m);
        m[Matrix.MTRANS_X] = -((focusX * getImageWidth()) - (viewWidth * 0.5f));
        m[Matrix.MTRANS_Y] = -((focusY * getImageHeight()) - (viewHeight * 0.5f));
        matrix.setValues(m);
        fixTrans();
        setImageMatrix(matrix);
    }

    public void setZoom(TouchImageView img) {
        PointF center = img.getScrollPosition();
        setZoom(img.getCurrentZoom(), center.x, center.y, img.getScaleType());
    }

    /**
     * Return the point at the center of the zoomed image. The PointF coordinates range
     * in value between 0 and 1 and the focus point is denoted as a fraction from the left
     * and top of the view. For example, the top left corner of the image would be (0, 0).
     * And the bottom right corner would be (1, 1).
     * @return PointF representing the scroll position of the zoomed image.
     */
    public PointF getScrollPosition() {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return null;
        }
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();

        PointF point = transformCoordTouchToBitmap(viewWidth / 2, viewHeight / 2, true);
        point.x /= drawableWidth;
        point.y /= drawableHeight;
        return point;
    }

    /**
     * Set the focus point of the zoomed image. The focus points are denoted as a fraction from the
     * left and top of the view. The focus points can range in value between 0 and 1.
     * @param focusX
     * @param focusY
     */
    public void setScrollPosition(float focusX, float focusY) {
        setZoom(normalizedScale, focusX, focusY);
    }

    /**
     * Performs boundary checking and fixes the image matrix if it
     * is out of bounds.
     */
    private void fixTrans() {
        matrix.getValues(m);
        float transX = m[Matrix.MTRANS_X];
        float transY = m[Matrix.MTRANS_Y];

        float fixTransX = getFixTrans(transX, viewWidth, getImageWidth());
        float fixTransY = getFixTrans(transY, viewHeight, getImageHeight());

        if (fixTransX != 0 || fixTransY != 0) {
            matrix.postTranslate(fixTransX, fixTransY);
        }
    }

    /**
     * When transitioning from zooming from focus to zoom from center (or vice versa)
     * the image can become unaligned within the view. This is apparent when zooming
     * quickly. When the content size is less than the view size, the content will often
     * be centered incorrectly within the view. fixScaleTrans first calls fixTrans() and
     * then makes sure the image is centered correctly within the view.
     */
    private void fixScaleTrans() {
        fixTrans();
        matrix.getValues(m);
        if (getImageWidth() < viewWidth) {
            m[Matrix.MTRANS_X] = (viewWidth - getImageWidth()) / 2;
        }

        if (getImageHeight() < viewHeight) {
            m[Matrix.MTRANS_Y] = (viewHeight - getImageHeight()) / 2;
        }
        matrix.setValues(m);
    }

    private float getFixTrans(float trans, float viewSize, float contentSize) {
        float minTrans, maxTrans;

        if (contentSize <= viewSize) {
            minTrans = 0;
            maxTrans = viewSize - contentSize;

        } else {
            minTrans = viewSize - contentSize;
            maxTrans = 0;
        }

        if (trans < minTrans)
            return -trans + minTrans;
        if (trans > maxTrans)
            return -trans + maxTrans;
        return 0;
    }

    private float getFixDragTrans(float delta, float viewSize, float contentSize) {
        if (contentSize <= viewSize) {
            return 0;
        }
        return delta;
    }

    private float getImageWidth() {
        return matchViewWidth * normalizedScale;
    }

    private float getImageHeight() {
        return matchViewHeight * normalizedScale;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable drawable = getDrawable();
        if (drawable == null || drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0) {
            setMeasuredDimension(0, 0);
            return;
        }

        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        viewWidth = setViewSize(widthMode, widthSize, drawableWidth);
        viewHeight = setViewSize(heightMode, heightSize, drawableHeight);

        //
        // Set view dimensions
        //
        setMeasuredDimension(viewWidth, viewHeight);

        //
        // Fit content within view
        //
        fitImageToView();
    }

    /**
     * If the normalizedScale is equal to 1, then the image is made to fit the screen. Otherwise,
     * it is made to fit the screen according to the dimensions of the previous image matrix. This
     * allows the image to maintain its zoom after rotation.
     */
    private void fitImageToView() {
        Drawable drawable = getDrawable();
        if (drawable == null || drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0) {
            return;
        }
        if (matrix == null || prevMatrix == null) {
            return;
        }

        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();

        //
        // Scale image for view
        //
        float scaleX = (float) viewWidth / drawableWidth;
        float scaleY = (float) viewHeight / drawableHeight;

        switch (mScaleType) {
            case CENTER:
                scaleX = scaleY = 1;
                break;

            case CENTER_CROP:
                scaleX = scaleY = Math.max(scaleX, scaleY);
                break;

            case CENTER_INSIDE:
                scaleX = scaleY = Math.min(1, Math.min(scaleX, scaleY));

            case FIT_CENTER:
                scaleX = scaleY = Math.min(scaleX, scaleY);
                break;

            case FIT_XY:
                break;

            default:
                //
                // FIT_START and FIT_END not supported
                //
                throw new UnsupportedOperationException("TouchImageView does not support FIT_START or FIT_END");

        }

        //
        // Center the image
        //
        float redundantXSpace = viewWidth - (scaleX * drawableWidth);
        float redundantYSpace = viewHeight - (scaleY * drawableHeight);
        matchViewWidth = viewWidth - redundantXSpace;
        matchViewHeight = viewHeight - redundantYSpace;
        if (!isZoomed() && !imageRenderedAtLeastOnce) {
            //
            // Stretch and center image to fit view
            //
            matrix.setScale(scaleX, scaleY);
            matrix.postTranslate(redundantXSpace / 2, redundantYSpace / 2);
            normalizedScale = 1;

        } else {
            //
            // These values should never be 0 or we will set viewWidth and viewHeight
            // to NaN in translateMatrixAfterRotate. To avoid this, call savePreviousImageValues
            // to set them equal to the current values.
            //
            if (prevMatchViewWidth == 0 || prevMatchViewHeight == 0) {
                savePreviousImageValues();
            }

            prevMatrix.getValues(m);

            //
            // Rescale Matrix after rotation
            //
            m[Matrix.MSCALE_X] = matchViewWidth / drawableWidth * normalizedScale;
            m[Matrix.MSCALE_Y] = matchViewHeight / drawableHeight * normalizedScale;

            //
            // TransX and TransY from previous matrix
            //
            float transX = m[Matrix.MTRANS_X];
            float transY = m[Matrix.MTRANS_Y];

            //
            // Width
            //
            float prevActualWidth = prevMatchViewWidth * normalizedScale;
            float actualWidth = getImageWidth();
            translateMatrixAfterRotate(Matrix.MTRANS_X, transX, prevActualWidth, actualWidth, prevViewWidth, viewWidth, drawableWidth);

            //
            // Height
            //
            float prevActualHeight = prevMatchViewHeight * normalizedScale;
            float actualHeight = getImageHeight();
            translateMatrixAfterRotate(Matrix.MTRANS_Y, transY, prevActualHeight, actualHeight, prevViewHeight, viewHeight, drawableHeight);

            //
            // Set the matrix to the adjusted scale and translate values.
            //
            matrix.setValues(m);
        }
        fixTrans();
        setImageMatrix(matrix);
    }

    /**
     * Set view dimensions based on layout params
     *
     * @param mode
     * @param size
     * @param drawableWidth
     * @return
     */
    private int setViewSize(int mode, int size, int drawableWidth) {
        int viewSize;
        switch (mode) {
            case MeasureSpec.EXACTLY:
                viewSize = size;
                break;

            case MeasureSpec.AT_MOST:
                viewSize = Math.min(drawableWidth, size);
                break;

            case MeasureSpec.UNSPECIFIED:
                viewSize = drawableWidth;
                break;

            default:
                viewSize = size;
                break;
        }
        return viewSize;
    }

    /**
     * After rotating, the matrix needs to be translated. This function finds the area of image
     * which was previously centered and adjusts translations so that is again the center, post-rotation.
     *
     * @param axis Matrix.MTRANS_X or Matrix.MTRANS_Y
     * @param trans the value of trans in that axis before the rotation
     * @param prevImageSize the width/height of the image before the rotation
     * @param imageSize width/height of the image after rotation
     * @param prevViewSize width/height of view before rotation
     * @param viewSize width/height of view after rotation
     * @param drawableSize width/height of drawable
     */
    private void translateMatrixAfterRotate(int axis, float trans, float prevImageSize, float imageSize, int prevViewSize, int viewSize, int drawableSize) {
        if (imageSize < viewSize) {
            //
            // The width/height of image is less than the view's width/height. Center it.
            //
            m[axis] = (viewSize - (drawableSize * m[Matrix.MSCALE_X])) * 0.5f;

        } else if (trans > 0) {
            //
            // The image is larger than the view, but was not before rotation. Center it.
            //
            m[axis] = -((imageSize - viewSize) * 0.5f);

        } else {
            //
            // Find the area of the image which was previously centered in the view. Determine its distance
            // from the left/top side of the view as a fraction of the entire image's width/height. Use that percentage
            // to calculate the trans in the new view width/height.
            //
            float percentage = (Math.abs(trans) + (0.5f * prevViewSize)) / prevImageSize;
            m[axis] = -((percentage * imageSize) - (viewSize * 0.5f));
        }
    }

    private void setState(State state) {
        this.state = state;
    }

    public boolean canScrollHorizontallyFroyo(int direction) {
        return canScrollHorizontally(direction);
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        matrix.getValues(m);
        float x = m[Matrix.MTRANS_X];

        if (getImageWidth() < viewWidth) {
            return false;

        } else if (x >= -1 && direction < 0) {
            return false;

        } else if (Math.abs(x) + viewWidth + 1 >= getImageWidth() && direction > 0) {
            return false;
        }

        return true;
    }

    /**
     * Gesture Listener detects a single click or long click and passes that on
     * to the view's listener.
     * @author Ortiz
     *
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e)
        {
            if(doubleTapListener != null) {
                return doubleTapListener.onSingleTapConfirmed(e);
            }
            return performClick();
        }

        @Override
        public void onLongPress(MotionEvent e)
        {
            performLongClick();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            if (fling != null) {
                //
                // If a previous fling is still active, it should be cancelled so that two flings
                // are not run simultaenously.
                //
                fling.cancelFling();
            }
            fling = new Fling((int) velocityX, (int) velocityY);
            compatPostOnAnimation(fling);
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            boolean consumed = false;
            if(doubleTapListener != null) {
                consumed = doubleTapListener.onDoubleTap(e);
            }
            if (state == State.NONE) {
                float targetZoom = (normalizedScale == minScale) ? maxScale : minScale;
                DoubleTapZoom doubleTap = new DoubleTapZoom(targetZoom, e.getX(), e.getY(), false);
                compatPostOnAnimation(doubleTap);
                consumed = true;
            }
            return consumed;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            if(doubleTapListener != null) {
                return doubleTapListener.onDoubleTapEvent(e);
            }
            return false;
        }
    }

    public interface OnTouchImageViewListener {
        public void onMove();
    }

    /**
     * Responsible for all touch events. Handles the heavy lifting of drag and also sends
     * touch events to Scale Detector and Gesture Detector.
     * @author Ortiz
     *
     */
    private class PrivateOnTouchListener implements OnTouchListener {

        //
        // Remember last point position for dragging
        //
        private PointF last = new PointF();

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if (scaleGestureDetector != null) {
                scaleGestureDetector.onTouchEvent(event);
                rotateGestureDetector.onTouchEvent(event);
                moveGestureDetector.onTouchEvent(event);
                gestureDetectorCompat.onTouchEvent(event);
            }

            mScaleDetector.onTouchEvent(event);
            mGestureDetector.onTouchEvent(event);
            PointF curr = new PointF(event.getX(), event.getY());

            if (state == State.NONE || state == State.DRAG || state == State.FLING) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        last.set(curr);
                        if (fling != null)
                            fling.cancelFling();
                        setState(State.DRAG);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (state == State.DRAG) {
                            float deltaX = curr.x - last.x;
                            float deltaY = curr.y - last.y;
                            float fixTransX = getFixDragTrans(deltaX, viewWidth, getImageWidth());
                            float fixTransY = getFixDragTrans(deltaY, viewHeight, getImageHeight());
                            matrix.postTranslate(fixTransX, fixTransY);
                            fixTrans();
                            last.set(curr.x, curr.y);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        setState(State.NONE);
                        break;
                }
            }

            setImageMatrix(matrix);

            //
            // User-defined OnTouchListener
            //
            if(userTouchListener != null) {
                userTouchListener.onTouch(v, event);
            }

            //
            // OnTouchImageViewListener is set: TouchImageView dragged by user.
            //
            if (touchImageViewListener != null) {
                touchImageViewListener.onMove();
            }

            //
            // indicate event was handled
            //
            return true;
        }
    }

    /**
     * ScaleListener detects user two finger scaling and scales image.
     * @author Ortiz
     *
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            setState(State.ZOOM);
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            if (selectedEntity != null) {
                float scaleFactorDiff = detector.getScaleFactor();
                selectedEntity.getBaseCreator().postScale(scaleFactorDiff - 1.0F);
                updateUI();
            }else {
                scaleImage(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY(), true);

                //
                // OnTouchImageViewListener is set: TouchImageView pinch zoomed by user.
                //
                if (touchImageViewListener != null) {
                    touchImageViewListener.onMove();
                }
            }
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
            setState(State.NONE);
            boolean animateToZoomBoundary = false;
            float targetZoom = normalizedScale;
            if (normalizedScale > maxScale) {
                targetZoom = maxScale;
                animateToZoomBoundary = true;

            } else if (normalizedScale < minScale) {
                targetZoom = minScale;
                animateToZoomBoundary = true;
            }

            if (animateToZoomBoundary) {
                DoubleTapZoom doubleTap = new DoubleTapZoom(targetZoom, viewWidth / 2, viewHeight / 2, true);
                compatPostOnAnimation(doubleTap);
            }
        }
    }

    private void scaleImage(double deltaScale, float focusX, float focusY, boolean stretchImageToSuper) {

        float lowerScale, upperScale;
        if (stretchImageToSuper) {
            lowerScale = superMinScale;
            upperScale = superMaxScale;

        } else {
            lowerScale = minScale;
            upperScale = maxScale;
        }

        float origScale = normalizedScale;
        normalizedScale *= deltaScale;
        if (normalizedScale > upperScale) {
            normalizedScale = upperScale;
            deltaScale = upperScale / origScale;
        } else if (normalizedScale < lowerScale) {
            normalizedScale = lowerScale;
            deltaScale = lowerScale / origScale;
        }

        matrix.postScale((float) deltaScale, (float) deltaScale, focusX, focusY);
        fixScaleTrans();
    }

    /**
     * DoubleTapZoom calls a series of runnables which apply
     * an animated zoom in/out graphic to the image.
     * @author Ortiz
     *
     */
    private class DoubleTapZoom implements Runnable {

        private long startTime;
        private static final float ZOOM_TIME = 500;
        private float startZoom, targetZoom;
        private float bitmapX, bitmapY;
        private boolean stretchImageToSuper;
        private AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
        private PointF startTouch;
        private PointF endTouch;

        DoubleTapZoom(float targetZoom, float focusX, float focusY, boolean stretchImageToSuper) {
            setState(State.ANIMATE_ZOOM);
            startTime = System.currentTimeMillis();
            this.startZoom = normalizedScale;
            this.targetZoom = targetZoom;
            this.stretchImageToSuper = stretchImageToSuper;
            PointF bitmapPoint = transformCoordTouchToBitmap(focusX, focusY, false);
            this.bitmapX = bitmapPoint.x;
            this.bitmapY = bitmapPoint.y;

            //
            // Used for translating image during scaling
            //
            startTouch = transformCoordBitmapToTouch(bitmapX, bitmapY);
            endTouch = new PointF(viewWidth / 2, viewHeight / 2);
        }

        @Override
        public void run() {
            float t = interpolate();
            double deltaScale = calculateDeltaScale(t);
            scaleImage(deltaScale, bitmapX, bitmapY, stretchImageToSuper);
            translateImageToCenterTouchPosition(t);
            fixScaleTrans();
            setImageMatrix(matrix);

            //
            // OnTouchImageViewListener is set: double tap runnable updates listener
            // with every frame.
            //
            if (touchImageViewListener != null) {
                touchImageViewListener.onMove();
            }

            if (t < 1f) {
                //
                // We haven't finished zooming
                //
                compatPostOnAnimation(this);

            } else {
                //
                // Finished zooming
                //
                setState(State.NONE);
            }
        }

        /**
         * Interpolate between where the image should start and end in order to translate
         * the image so that the point that is touched is what ends up centered at the end
         * of the zoom.
         * @param t
         */
        private void translateImageToCenterTouchPosition(float t) {
            float targetX = startTouch.x + t * (endTouch.x - startTouch.x);
            float targetY = startTouch.y + t * (endTouch.y - startTouch.y);
            PointF curr = transformCoordBitmapToTouch(bitmapX, bitmapY);
            matrix.postTranslate(targetX - curr.x, targetY - curr.y);
        }

        /**
         * Use interpolator to get t
         * @return
         */
        private float interpolate() {
            long currTime = System.currentTimeMillis();
            float elapsed = (currTime - startTime) / ZOOM_TIME;
            elapsed = Math.min(1f, elapsed);
            return interpolator.getInterpolation(elapsed);
        }

        /**
         * Interpolate the current targeted zoom and get the delta
         * from the current zoom.
         * @param t
         * @return
         */
        private double calculateDeltaScale(float t) {
            double zoom = startZoom + t * (targetZoom - startZoom);
            return zoom / normalizedScale;
        }
    }

    /**
     * This function will transform the coordinates in the touch event to the coordinate
     * system of the drawable that the imageview contain
     * @param x x-coordinate of touch event
     * @param y y-coordinate of touch event
     * @param clipToBitmap Touch event may occur within view, but outside image content. True, to clip return value
     * 			to the bounds of the bitmap size.
     * @return Coordinates of the point touched, in the coordinate system of the original drawable.
     */
    private PointF transformCoordTouchToBitmap(float x, float y, boolean clipToBitmap) {
        matrix.getValues(m);
        float origW = getDrawable().getIntrinsicWidth();
        float origH = getDrawable().getIntrinsicHeight();
        float transX = m[Matrix.MTRANS_X];
        float transY = m[Matrix.MTRANS_Y];
        float finalX = ((x - transX) * origW) / getImageWidth();
        float finalY = ((y - transY) * origH) / getImageHeight();

        if (clipToBitmap) {
            finalX = Math.min(Math.max(finalX, 0), origW);
            finalY = Math.min(Math.max(finalY, 0), origH);
        }

        return new PointF(finalX , finalY);
    }

    /**
     * Inverse of transformCoordTouchToBitmap. This function will transform the coordinates in the
     * drawable's coordinate system to the view's coordinate system.
     * @param bx x-coordinate in original bitmap coordinate system
     * @param by y-coordinate in original bitmap coordinate system
     * @return Coordinates of the point in the view's coordinate system.
     */
    private PointF transformCoordBitmapToTouch(float bx, float by) {
        matrix.getValues(m);
        float origW = getDrawable().getIntrinsicWidth();
        float origH = getDrawable().getIntrinsicHeight();
        float px = bx / origW;
        float py = by / origH;
        float finalX = m[Matrix.MTRANS_X] + getImageWidth() * px;
        float finalY = m[Matrix.MTRANS_Y] + getImageHeight() * py;
        return new PointF(finalX , finalY);
    }

    /**
     * Fling launches sequential runnables which apply
     * the fling graphic to the image. The values for the translation
     * are interpolated by the Scroller.
     * @author Ortiz
     *
     */
    private class Fling implements Runnable {

        CompatScroller scroller;
        int currX, currY;

        Fling(int velocityX, int velocityY) {
            setState(State.FLING);
            scroller = new CompatScroller(context);
            matrix.getValues(m);

            int startX = (int) m[Matrix.MTRANS_X];
            int startY = (int) m[Matrix.MTRANS_Y];
            int minX, maxX, minY, maxY;

            if (getImageWidth() > viewWidth) {
                minX = viewWidth - (int) getImageWidth();
                maxX = 0;

            } else {
                minX = maxX = startX;
            }

            if (getImageHeight() > viewHeight) {
                minY = viewHeight - (int) getImageHeight();
                maxY = 0;

            } else {
                minY = maxY = startY;
            }

            scroller.fling(startX, startY, (int) velocityX, (int) velocityY, minX,
                    maxX, minY, maxY);
            currX = startX;
            currY = startY;
        }

        public void cancelFling() {
            if (scroller != null) {
                setState(State.NONE);
                scroller.forceFinished(true);
            }
        }

        @Override
        public void run() {

            //
            // OnTouchImageViewListener is set: TouchImageView listener has been flung by user.
            // Listener runnable updated with each frame of fling animation.
            //
            if (touchImageViewListener != null) {
                touchImageViewListener.onMove();
            }

            if (scroller.isFinished()) {
                scroller = null;
                return;
            }

            if (scroller.computeScrollOffset()) {
                int newX = scroller.getCurrX();
                int newY = scroller.getCurrY();
                int transX = newX - currX;
                int transY = newY - currY;
                currX = newX;
                currY = newY;
                matrix.postTranslate(transX, transY);
                fixTrans();
                setImageMatrix(matrix);
                compatPostOnAnimation(this);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private class CompatScroller {
        Scroller scroller;
        OverScroller overScroller;
        boolean isPreGingerbread;

        public CompatScroller(Context context) {
            if (VERSION.SDK_INT < VERSION_CODES.GINGERBREAD) {
                isPreGingerbread = true;
                scroller = new Scroller(context);

            } else {
                isPreGingerbread = false;
                overScroller = new OverScroller(context);
            }
        }

        public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
            if (isPreGingerbread) {
                scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
            } else {
                overScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
            }
        }

        public void forceFinished(boolean finished) {
            if (isPreGingerbread) {
                scroller.forceFinished(finished);
            } else {
                overScroller.forceFinished(finished);
            }
        }

        public boolean isFinished() {
            if (isPreGingerbread) {
                return scroller.isFinished();
            } else {
                return overScroller.isFinished();
            }
        }

        public boolean computeScrollOffset() {
            if (isPreGingerbread) {
                return scroller.computeScrollOffset();
            } else {
                overScroller.computeScrollOffset();
                return overScroller.computeScrollOffset();
            }
        }

        public int getCurrX() {
            if (isPreGingerbread) {
                return scroller.getCurrX();
            } else {
                return overScroller.getCurrX();
            }
        }

        public int getCurrY() {
            if (isPreGingerbread) {
                return scroller.getCurrY();
            } else {
                return overScroller.getCurrY();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void compatPostOnAnimation(Runnable runnable) {
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            postOnAnimation(runnable);

        } else {
            postDelayed(runnable, 1000/60);
        }
    }

    private class ZoomVariables {
        public float scale;
        public float focusX;
        public float focusY;
        public ScaleType scaleType;

        public ZoomVariables(float scale, float focusX, float focusY, ScaleType scaleType) {
            this.scale = scale;
            this.focusX = focusX;
            this.focusY = focusY;
            this.scaleType = scaleType;
        }
    }

    private void printMatrixInfo() {
        float[] n = new float[9];
        matrix.getValues(n);
        Log.d(DEBUG, "Scale: " + n[Matrix.MSCALE_X] + " TransX: " + n[Matrix.MTRANS_X] + " TransY: " + n[Matrix.MTRANS_Y]);
    }

    FontSetting fontSetting;
    private void init(@NonNull Context context) {

        this.fontSetting = new FontSetting(getResources());

        selectedLayerPaint = new Paint();
        selectedLayerPaint.setAlpha((int) (255 * Constants.SELECTED_LAYER_ALPHA));
        selectedLayerPaint.setAntiAlias(true);

        this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        this.rotateGestureDetector = new RotateGestureDetector(context, new RotateListener());
        this.moveGestureDetector = new MoveGestureDetector(context, new MoveListener());
        this.gestureDetectorCompat = new GestureDetectorCompat(context, new TapsListener());

//        setOnTouchListener(new PrivateOnTouchListener());
        super.setOnTouchListener(new PrivateOnTouchListener());

        updateUI();

        Log.i("tocuh init() ======= ",""+context);
    }

    public BaseEntity getSelectedEntity() {
        return selectedEntity;
    }

    public List<BaseEntity> getEntities() {
        return entities;
    }

    public void setframeViewCallback(@Nullable FrameViewCallback callback) {
        this.frameViewCallback = callback;
    }

    public void addEntity(@Nullable BaseEntity entity) {
        if (entity != null) {
            entities.add(entity);
            selectEntity(entity, false);
        }
    }

    public void addEntityAndPosition(@Nullable BaseEntity entity) {
        if (entity != null) {
            initEntityBorder(entity);
            initialTranslateAndScale(entity);
            entities.add(entity);
            selectEntity(entity, true);
        }
    }

    private void initEntityBorder(@NonNull BaseEntity entity ) {
        int strokeSize = getResources().getDimensionPixelSize(R.dimen.stroke_size);
        Paint borderPaint = new Paint();
        borderPaint.setStrokeWidth(strokeSize);
        borderPaint.setAntiAlias(true);
        borderPaint.setColor(ContextCompat.getColor(getContext(), R.color.stroke_color));

        entity.setBorderPaint(borderPaint);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        Log.i("dispatchDraw====",""+canvas+"    "+bitmap+"  "+width+"   "+height);
        super.dispatchDraw(canvas);
        if (selectedEntity != null) {
            selectedEntity.draw(canvas, selectedLayerPaint);
        }
    }

    int colorfilter = -1;
    Paint paint = new Paint();
    public void setColorFiltration(int colorfilter){
        this.colorfilter = colorfilter;
        float[] colorTransform = new float[]{
                Color.red(colorfilter)/255f, 0, 0, 0, 0,
                0, Color.green(colorfilter)/255f, 0, 0, 0,
                0, 0, Color.blue(colorfilter)/255f, 0, 0,
                0, 0, 0, 1, 0};
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0f);
        colorMatrix.set(colorTransform);
        ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorFilter);
        invalidate();
    }

    Bitmap bitmap;
    int height;
    int width;
    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
//        fitImage();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if(width == 0 || height == 0) {
            this.width = w;
            this.height = h;
            invalidate();
        }
        super.onSizeChanged(w, h, oldw, oldh);
    }


//    @Override
//    protected void onDraw(Canvas canvas) {
//        if(bitmap != null) {
//            canvas.drawBitmap(bitmap, null, new RectF(0, 0, width, height), paint);
//        }
//        drawAllEntities(canvas,paint);
//        super.onDraw(canvas);
//    }


    private void drawAllEntities(Canvas canvas, Paint paint) {
        for (int i = 0; i < entities.size(); i++) {
            entities.get(i).draw(canvas, paint);
        }
    }

    public Bitmap getThumbnailImage() {
        selectEntity(null, false);

        Bitmap bmp = Bitmap.createBitmap(getLayoutParams().width,getLayoutParams().height, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(Color.WHITE);
        Canvas canvas = new Canvas(bmp);
        drawAllEntities(canvas,null);
        return bmp;
    }

    private void updateUI() {
        invalidate();
    }

    private void handleTranslate(PointF delta) {
        if (selectedEntity != null) {
            float newCenterX = selectedEntity.absoluteCenterX() + delta.x;
            float newCenterY = selectedEntity.absoluteCenterY() + delta.y;
            boolean needUpdateUI = false;
            if (newCenterX >= 0 && newCenterX <= getWidth()) {
                selectedEntity.getBaseCreator().postTranslate(delta.x / getWidth(), 0.0F);
                needUpdateUI = true;
            }
            if (newCenterY >= 0 && newCenterY <= getHeight()) {
                selectedEntity.getBaseCreator().postTranslate(0.0F, delta.y / getHeight());
                needUpdateUI = true;
            }
            if (needUpdateUI) {
                updateUI();
            }
        }
    }

    private void initialTranslateAndScale(@NonNull BaseEntity entity) {
        entity.moveToCanvasCenter();
        entity.getBaseCreator().setScale(entity.getBaseCreator().initialScale());
    }

    private void selectEntity(@Nullable BaseEntity entity, boolean updateCallback) {
        if (selectedEntity != null) {
            selectedEntity.setIsSelected(false);
        }
        if (entity != null) {
            entity.setIsSelected(true);
        }
        selectedEntity = entity;
        invalidate();
        if (updateCallback && frameViewCallback != null) {
            frameViewCallback.onEntitySelected(entity);
        }
    }

    public void unselectEntity() {
        if (selectedEntity != null) {
            selectEntity(null, true);
        }
    }

    @Nullable
    private BaseEntity findEntityAtPoint(float x, float y) {
        BaseEntity selected = null;
        PointF p = new PointF(x, y);
        for (int i = entities.size() - 1; i >= 0; i--) {
            if (entities.get(i).pointInLayerRect(p)) {
                selected = entities.get(i);
                break;
            }
        }
        return selected;
    }

    private void updateSelectionOnTap(MotionEvent e) {
        BaseEntity entity = findEntityAtPoint(e.getX(), e.getY());
        selectEntity(entity, true);
    }

    private void updateOnLongPress(MotionEvent e) {
        if (selectedEntity != null) {
            PointF p = new PointF(e.getX(), e.getY());
            if (selectedEntity.pointInLayerRect(p)) {
                bringLayerToFront(selectedEntity);
            }
        }
    }

    private void bringLayerToFront(@NonNull BaseEntity entity) {
        // removing and adding brings baseCreater to front
        if (entities.remove(entity)) {
            entities.add(entity);
            invalidate();
        }
    }

    private void moveEntityToBack(@Nullable BaseEntity entity) {
        if (entity == null) {
            return;
        }
        if (entities.remove(entity)) {
            entities.add(0, entity);
            invalidate();
        }
    }

    public void flipSelectedEntity() {
        if (selectedEntity == null) {
            return;
        }
        selectedEntity.getBaseCreator().flip();
        invalidate();
    }

    public void moveSelectedBack() {
        moveEntityToBack(selectedEntity);
    }

    public void deletedSelectedEntity() {
        if (selectedEntity == null) {
            return;
        }
        if (entities.remove(selectedEntity)) {
            selectedEntity.release();
            selectedEntity = null;
            invalidate();
        }
    }

    public void release() {
        for (BaseEntity entity : entities) {
            entity.release();
        }
    }

//    private final OnTouchListener onTouchListener = new OnTouchListener() {
//
//        @Override
//        public boolean onTouch(View v, MotionEvent event) {
//            if (scaleGestureDetector != null) {
//                scaleGestureDetector.onTouchEvent(event);
//                rotateGestureDetector.onTouchEvent(event);
//                moveGestureDetector.onTouchEvent(event);
//                gestureDetectorCompat.onTouchEvent(event);
//            }
//            return true;
//        }
//    };

    private class TapsListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (frameViewCallback != null && selectedEntity != null) {
                frameViewCallback.onEntityDoubleTap(selectedEntity);
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            updateOnLongPress(e);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            updateSelectionOnTap(e);
            return true;
        }
    }

//    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
//        @Override
//        public boolean onScale(ScaleGestureDetector detector) {
//            if (selectedEntity != null) {
//                float scaleFactorDiff = detector.getScaleFactor();
//                selectedEntity.getBaseCreator().postScale(scaleFactorDiff - 1.0F);
//                updateUI();
//            }
//            return true;
//        }
//    }

    private class RotateListener extends RotateGestureDetector.SimpleOnRotateGestureListener {
        @Override
        public boolean onRotate(RotateGestureDetector detector) {
            if (selectedEntity != null) {
                selectedEntity.getBaseCreator().postRotate(-detector.getRotationDegreesDelta());
                updateUI();
            }
            return true;
        }
    }

    private class MoveListener extends MoveGestureDetector.SimpleOnMoveGestureListener {
        @Override
        public boolean onMove(MoveGestureDetector detector) {
            handleTranslate(detector.getFocusDelta());
            return true;
        }
    }

    public static class Font {

        private int color;
        private String typeface;
        private float size;

        public Font() {
        }

        public void increaseSize(float diff) {
            size = size + diff;
        }

        public void decreaseSize(float diff) {
            if (size - diff >= Limits.MIN_FONT_SIZE) {
                size = size - diff;
            }
        }

        public int getColor() {
            return color;
        }

        public void setColor(int color) {
            this.color = color;
        }

        public String getTypeface() {
            return typeface;
        }

        public void setTypeface(String typeface) {
            this.typeface = typeface;
        }

        public float getSize() {
            return size;
        }

        public void setSize(float size) {
            this.size = size;
        }

        private interface Limits {
            float MIN_FONT_SIZE = 0.01F;
        }
    }

    public static class BaseCreater {

        @FloatRange(from = 0.0F, to = 360.0F)
        private float rotationInDegrees;

        private float scale;
        private float x;
        private float y;
        private boolean isFlipped;

        public BaseCreater() {
            reset();
        }

        protected void reset() {
            this.rotationInDegrees = 0.0F;
            this.scale = 1.0F;
            this.isFlipped = false;
            this.x = 0.0F;
            this.y = 0.0F;
        }

        public void postScale(float scaleDiff) {
            float newVal = scale + scaleDiff;
            if (newVal >= getMinScale() && newVal <= getMaxScale()) {
                scale = newVal;
            }
        }

        protected float getMaxScale() {
            return Limits.MAX_SCALE;
        }

        protected float getMinScale() {
            return Limits.MIN_SCALE;
        }

        public void postRotate(float rotationInDegreesDiff) {
            this.rotationInDegrees += rotationInDegreesDiff;
            this.rotationInDegrees %= 360.0F;
        }

        public void postTranslate(float dx, float dy) {
            this.x += dx;
            this.y += dy;
        }

        public void flip() {
            this.isFlipped = !isFlipped;
        }

        public float initialScale() {
            return Limits.INITIAL_ENTITY_SCALE;
        }

        public float getRotationInDegrees() {
            return rotationInDegrees;
        }

        public void setRotationInDegrees(@FloatRange(from = 0.0, to = 360.0) float rotationInDegrees) {
            this.rotationInDegrees = rotationInDegrees;
        }

        public float getScale() {
            return scale;
        }

        public void setScale(float scale) {
            this.scale = scale;
        }

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }

        public boolean isFlipped() {
            return isFlipped;
        }

        public void setFlipped(boolean flipped) {
            isFlipped = flipped;
        }

        interface Limits {
            float MIN_SCALE = 0.06F;
            float MAX_SCALE = 4.0F;
            float INITIAL_ENTITY_SCALE = 0.4F;
        }
    }


    public static class TextCreater extends BaseCreater {

        private String text;
        private Font font;

        public TextCreater() {
        }

        @Override
        protected void reset() {
            super.reset();
            this.text = "";
            this.font = new Font();
        }

        @Override
        protected float getMaxScale() {
            return Limits.MAX_SCALE;
        }

        @Override
        protected float getMinScale() {
            return Limits.MIN_SCALE;
        }

        @Override
        public float initialScale() {
            return Limits.INITIAL_SCALE;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Font getFont() {
            return font;
        }

        public void setFont(Font font) {
            this.font = font;
        }

        public interface Limits {
            float MAX_SCALE = 1.0F;
            float MIN_SCALE = 0.2F;
            float MIN_BITMAP_HEIGHT = 0.13F;
            float FONT_SIZE_STEP = 0.008F;
            float INITIAL_FONT_SIZE = 0.075F;
            int INITIAL_FONT_COLOR = 0xff000000;
            float INITIAL_SCALE = 0.8F;
        }
    }

    public static class ImageEntity extends BaseEntity {

        @NonNull
        private final Bitmap bitmap;

        public ImageEntity(@NonNull BaseCreater layer,
                           @NonNull Bitmap bitmap,
                           @IntRange(from = 1) int canvasWidth,
                           @IntRange(from = 1) int canvasHeight) {
            super(layer, canvasWidth, canvasHeight);

            this.bitmap = bitmap;
            float width = bitmap.getWidth();
            float height = bitmap.getHeight();

            float widthAspect = 1.0F * canvasWidth / width;
            float heightAspect = 1.0F * canvasHeight / height;
            holyScale = Math.min(widthAspect, heightAspect);

            srcPoints[0] = 0; srcPoints[1] = 0;
            srcPoints[2] = width; srcPoints[3] = 0;
            srcPoints[4] = width; srcPoints[5] = height;
            srcPoints[6] = 0; srcPoints[7] = height;
            srcPoints[8] = 0; srcPoints[8] = 0;
        }

        @Override
        public void drawContent(@NonNull Canvas canvas, @Nullable Paint drawingPaint) {
            canvas.drawBitmap(bitmap, matrix, drawingPaint);
        }

        @Override
        public int getWidth() {
            return bitmap.getWidth();
        }

        @Override
        public int getHeight() {
            return bitmap.getHeight();
        }

        @Override
        public void release() {
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }


    public static class TextEntity extends BaseEntity {

        private final TextPaint textPaint;
        private final FontSetting fontSetting;
        @Nullable
        private Bitmap bitmap;

        public TextEntity(@NonNull TextCreater textLayer,
                          @IntRange(from = 1) int canvasWidth,
                          @IntRange(from = 1) int canvasHeight,
                          @NonNull FontSetting fontProvider) {
            super(textLayer, canvasWidth, canvasHeight);
            this.fontSetting = fontProvider;
            this.textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

            updateEntity(false);
        }

        private void updateEntity(boolean moveToPreviousCenter) {
            PointF oldCenter = absoluteCenter();
            Bitmap newBmp = createBitmap(getBaseCreator(), bitmap);
            if (bitmap != null && bitmap != newBmp && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
            this.bitmap = newBmp;

            float width = bitmap.getWidth();
            float height = bitmap.getHeight();

            @SuppressWarnings("UnnecessaryLocalVariable")
            float widthAspect = 1.0F * canvasWidth / width;
            this.holyScale = widthAspect;

            srcPoints[0] = 0;
            srcPoints[1] = 0;
            srcPoints[2] = width;
            srcPoints[3] = 0;
            srcPoints[4] = width;
            srcPoints[5] = height;
            srcPoints[6] = 0;
            srcPoints[7] = height;
            srcPoints[8] = 0;
            srcPoints[8] = 0;

            if (moveToPreviousCenter) {
                moveCenterTo(oldCenter);
            }
        }

        @NonNull
        private Bitmap createBitmap(@NonNull TextCreater textLayer, @Nullable Bitmap reuseBmp) {

            int boundsWidth = canvasWidth;

            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setTextSize(textLayer.getFont().getSize() * canvasWidth);
            textPaint.setColor(textLayer.getFont().getColor());
            textPaint.setTypeface(fontSetting.getTypeface(textLayer.getFont().getTypeface()));

            StaticLayout sl = new StaticLayout(
                    textLayer.getText(),
                    textPaint,
                    boundsWidth,
                    Layout.Alignment.ALIGN_CENTER,
                    1,
                    1,
                    true);

            int boundsHeight = sl.getHeight();

            int bmpHeight = (int) (canvasHeight * Math.max(TextCreater.Limits.MIN_BITMAP_HEIGHT,
                    1.0F * boundsHeight / canvasHeight));

            Bitmap bmp;
            if (reuseBmp != null && reuseBmp.getWidth() == boundsWidth
                    && reuseBmp.getHeight() == bmpHeight) {
                bmp = reuseBmp;
                bmp.eraseColor(Color.TRANSPARENT);
            } else {
                bmp = Bitmap.createBitmap(boundsWidth, bmpHeight, Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(bmp);
            canvas.save();

            if (boundsHeight < bmpHeight) {
                float textYCoordinate = (bmpHeight - boundsHeight) / 2;
                canvas.translate(0, textYCoordinate);
            }

            sl.draw(canvas);
            canvas.restore();

            return bmp;
        }

        @Override
        @NonNull
        public TextCreater getBaseCreator() {
            return (TextCreater) baseCreater;
        }

        @Override
        protected void drawContent(@NonNull Canvas canvas, @Nullable Paint drawingPaint) {
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, matrix, drawingPaint);
            }
        }

        @Override
        public int getWidth() {
            return bitmap != null ? bitmap.getWidth() : 0;
        }

        @Override
        public int getHeight() {
            return bitmap != null ? bitmap.getHeight() : 0;
        }

        public void updateEntity() {
            updateEntity(true);
        }

        @Override
        public void release() {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    public static abstract class BaseEntity {

        @NonNull
        protected final BaseCreater baseCreater;

        protected final Matrix matrix = new Matrix();

        private boolean isSelected;

        protected float holyScale;

        @IntRange(from = 0)
        protected int canvasWidth;

        @IntRange(from = 0)
        protected int canvasHeight;

        private final float[] destPoints = new float[10];

        protected final float[] srcPoints = new float[10];

        @NonNull
        private Paint borderPaint = new Paint();

        public BaseEntity(@NonNull BaseCreater baseCreater,
                          @IntRange(from = 1) int canvasWidth,
                          @IntRange(from = 1) int canvasHeight) {
            this.baseCreater = baseCreater;
            this.canvasWidth = canvasWidth;
            this.canvasHeight = canvasHeight;
        }

        private boolean isSelected() {
            return isSelected;
        }

        public void setIsSelected(boolean isSelected) {
            this.isSelected = isSelected;
        }

        protected void updateMatrix() {
            matrix.reset();

            float topLeftX = baseCreater.getX() * canvasWidth;
            float topLeftY = baseCreater.getY() * canvasHeight;

            float centerX = topLeftX + getWidth() * holyScale * 0.5F;
            float centerY = topLeftY + getHeight() * holyScale * 0.5F;

            float rotationInDegree = baseCreater.getRotationInDegrees();
            float scaleX = baseCreater.getScale();
            float scaleY = baseCreater.getScale();
            if (baseCreater.isFlipped()) {
                rotationInDegree *= -1.0F;
                scaleX *= -1.0F;
            }

            matrix.preScale(scaleX, scaleY, centerX, centerY);

            matrix.preRotate(rotationInDegree, centerX, centerY);

            matrix.preTranslate(topLeftX, topLeftY);

            matrix.preScale(holyScale, holyScale);
        }

        public float absoluteCenterX() {
            float topLeftX = baseCreater.getX() * canvasWidth;
            return topLeftX + getWidth() * holyScale * 0.5F;
        }

        public float absoluteCenterY() {
            float topLeftY = baseCreater.getY() * canvasHeight;

            return topLeftY + getHeight() * holyScale * 0.5F;
        }

        public PointF absoluteCenter() {
            float topLeftX = baseCreater.getX() * canvasWidth;
            float topLeftY = baseCreater.getY() * canvasHeight;

            float centerX = topLeftX + getWidth() * holyScale * 0.5F;
            float centerY = topLeftY + getHeight() * holyScale * 0.5F;

            return new PointF(centerX, centerY);
        }

        public void moveToCanvasCenter() {
            moveCenterTo(new PointF(canvasWidth * 0.5F, canvasHeight * 0.5F));
        }

        public void moveCenterTo(PointF moveToCenter) {
            PointF currentCenter = absoluteCenter();
            baseCreater.postTranslate(1.0F * (moveToCenter.x - currentCenter.x) / canvasWidth,
                    1.0F * (moveToCenter.y - currentCenter.y) / canvasHeight);
        }

        private final PointF pA = new PointF();
        private final PointF pB = new PointF();
        private final PointF pC = new PointF();
        private final PointF pD = new PointF();

        public boolean pointInLayerRect(PointF point) {

            updateMatrix();
            matrix.mapPoints(destPoints, srcPoints);

            pA.x = destPoints[0];
            pA.y = destPoints[1];
            pB.x = destPoints[2];
            pB.y = destPoints[3];
            pC.x = destPoints[4];
            pC.y = destPoints[5];
            pD.x = destPoints[6];
            pD.y = destPoints[7];

            return pointInTriangle(point, pA, pB, pC) || pointInTriangle(point, pA, pD, pC);
        }

        public final void draw(@NonNull Canvas canvas, @Nullable Paint drawingPaint) {

            updateMatrix();

            canvas.save();

            drawContent(canvas, drawingPaint);

            if (isSelected()) {
                int storedAlpha = borderPaint.getAlpha();
                if (drawingPaint != null) {
                    borderPaint.setAlpha(drawingPaint.getAlpha());
                }
                drawSelectedBg(canvas);
                borderPaint.setAlpha(storedAlpha);
            }

            canvas.restore();
        }

        private void drawSelectedBg(Canvas canvas) {
            matrix.mapPoints(destPoints, srcPoints);
            canvas.drawLines(destPoints, 0, 8, borderPaint);
            canvas.drawLines(destPoints, 2, 8, borderPaint);
        }

        @NonNull
        public BaseCreater getBaseCreator() {
            return baseCreater;
        }

        public void setBorderPaint(@NonNull Paint borderPaint) {
            this.borderPaint = borderPaint;
        }

        protected abstract void drawContent(@NonNull Canvas canvas, @Nullable Paint drawingPaint);

        public abstract int getWidth();

        public abstract int getHeight();

        public void release() {

        }

        @Override
        protected void finalize() throws Throwable {
            try {
                release();
            } finally {
                super.finalize();
            }
        }
    }

    public static boolean pointInTriangle(@NonNull PointF pt, @NonNull PointF v1,
                                          @NonNull PointF v2, @NonNull PointF v3) {
        boolean b1 = crossProduct(pt, v1, v2) < 0.0f;
        boolean b2 = crossProduct(pt, v2, v3) < 0.0f;
        boolean b3 = crossProduct(pt, v3, v1) < 0.0f;
        return (b1 == b2) && (b2 == b3);
    }
    private static float crossProduct(@NonNull PointF a, @NonNull PointF b, @NonNull PointF c) {
        return crossProduct(a.x, a.y, b.x, b.y, c.x, c.y);
    }

    private static float crossProduct(float ax, float ay, float bx, float by, float cx, float cy) {
        return (ax - cx) * (by - cy) - (bx - cx) * (ay - cy);
    }

    public static abstract class BaseGestureDetector {
        protected static final float PRESSURE_THRESHOLD = 0.67f;
        protected final Context mContext;
        protected boolean mGestureInProgress;
        protected MotionEvent mPrevEvent;
        protected MotionEvent mCurrEvent;
        protected float mCurrPressure;
        protected float mPrevPressure;
        protected long mTimeDelta;
        public BaseGestureDetector(Context context) {
            mContext = context;
        }

        public boolean onTouchEvent(MotionEvent event) {
            final int actionCode = event.getAction() & MotionEvent.ACTION_MASK;
            if (!mGestureInProgress) {
                handleStartProgressEvent(actionCode, event);
            } else {
                handleInProgressEvent(actionCode, event);
            }
            return true;
        }
        protected abstract void handleStartProgressEvent(int actionCode, MotionEvent event);

        protected abstract void handleInProgressEvent(int actionCode, MotionEvent event);


        protected void updateStateByEvent(MotionEvent curr) {
            final MotionEvent prev = mPrevEvent;

            if (mCurrEvent != null) {
                mCurrEvent.recycle();
                mCurrEvent = null;
            }
            mCurrEvent = MotionEvent.obtain(curr);

            mTimeDelta = curr.getEventTime() - prev.getEventTime();

            mCurrPressure = curr.getPressure(curr.getActionIndex());
            mPrevPressure = prev.getPressure(prev.getActionIndex());
        }

        protected void resetState() {
            if (mPrevEvent != null) {
                mPrevEvent.recycle();
                mPrevEvent = null;
            }
            if (mCurrEvent != null) {
                mCurrEvent.recycle();
                mCurrEvent = null;
            }
            mGestureInProgress = false;
        }

        public boolean isInProgress() {
            return mGestureInProgress;
        }


        public long getEventTime() {
            return mCurrEvent.getEventTime();
        }

    }

    public static class MoveGestureDetector extends BaseGestureDetector {

        private static final PointF FOCUS_DELTA_ZERO = new PointF();
        private final OnMoveGestureListener mListener;
        private PointF mCurrFocusInternal;
        private PointF mPrevFocusInternal;
        private PointF mFocusExternal = new PointF();
        private PointF mFocusDeltaExternal = new PointF();
        public MoveGestureDetector(Context context, OnMoveGestureListener listener) {
            super(context);
            mListener = listener;
        }

        @Override
        protected void handleStartProgressEvent(int actionCode, MotionEvent event) {
            switch (actionCode) {
                case MotionEvent.ACTION_DOWN:
                    resetState();
                    mPrevEvent = MotionEvent.obtain(event);
                    mTimeDelta = 0;

                    updateStateByEvent(event);
                    break;

                case MotionEvent.ACTION_MOVE:
                    mGestureInProgress = mListener.onMoveBegin(this);
                    break;
            }
        }

        @Override
        protected void handleInProgressEvent(int actionCode, MotionEvent event) {
            switch (actionCode) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mListener.onMoveEnd(this);
                    resetState();
                    break;

                case MotionEvent.ACTION_MOVE:
                    updateStateByEvent(event);

                    if (mCurrPressure / mPrevPressure > PRESSURE_THRESHOLD) {
                        final boolean updatePrevious = mListener.onMove(this);
                        if (updatePrevious) {
                            mPrevEvent.recycle();
                            mPrevEvent = MotionEvent.obtain(event);
                        }
                    }
                    break;
            }
        }

        protected void updateStateByEvent(MotionEvent curr) {
            super.updateStateByEvent(curr);

            final MotionEvent prev = mPrevEvent;

            mCurrFocusInternal = determineFocalPoint(curr);
            mPrevFocusInternal = determineFocalPoint(prev);

            boolean mSkipNextMoveEvent = prev.getPointerCount() != curr.getPointerCount();
            mFocusDeltaExternal = mSkipNextMoveEvent ? FOCUS_DELTA_ZERO : new PointF(mCurrFocusInternal.x - mPrevFocusInternal.x, mCurrFocusInternal.y - mPrevFocusInternal.y);

            mFocusExternal.x += mFocusDeltaExternal.x;
            mFocusExternal.y += mFocusDeltaExternal.y;
        }

        private PointF determineFocalPoint(MotionEvent e) {
            final int pCount = e.getPointerCount();
            float x = 0f;
            float y = 0f;

            for (int i = 0; i < pCount; i++) {
                x += e.getX(i);
                y += e.getY(i);
            }
            return new PointF(x / pCount, y / pCount);
        }

        public float getFocusX() {
            return mFocusExternal.x;
        }

        public float getFocusY() {
            return mFocusExternal.y;
        }

        public PointF getFocusDelta() {
            return mFocusDeltaExternal;
        }

        public interface OnMoveGestureListener {

            public boolean onMove(MoveGestureDetector detector);

            public boolean onMoveBegin(MoveGestureDetector detector);

            public void onMoveEnd(MoveGestureDetector detector);
        }

        public static class SimpleOnMoveGestureListener implements OnMoveGestureListener {
            public boolean onMove(MoveGestureDetector detector) {
                return false;
            }

            public boolean onMoveBegin(MoveGestureDetector detector) {
                return true;
            }

            public void onMoveEnd(MoveGestureDetector detector) {

            }
        }
    }

    public static class RotateGestureDetector extends TwoFingerGestureDetector {

        private final OnRotateGestureListener mListener;
        private boolean mSloppyGesture;


        public RotateGestureDetector(Context context, OnRotateGestureListener listener) {
            super(context);
            mListener = listener;
        }

        @Override
        protected void handleStartProgressEvent(int actionCode, MotionEvent event) {
            switch (actionCode) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    resetState();
                    mPrevEvent = MotionEvent.obtain(event);
                    mTimeDelta = 0;

                    updateStateByEvent(event);

                    mSloppyGesture = isSloppyGesture(event);
                    if (!mSloppyGesture) {
                        mGestureInProgress = mListener.onRotateBegin(this);
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (!mSloppyGesture) {
                        break;
                    }

                    mSloppyGesture = isSloppyGesture(event);
                    if (!mSloppyGesture) {
                        mGestureInProgress = mListener.onRotateBegin(this);
                    }

                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    if (!mSloppyGesture) {
                        break;
                    }

                    break;
            }
        }

        @Override
        protected void handleInProgressEvent(int actionCode, MotionEvent event) {
            switch (actionCode) {
                case MotionEvent.ACTION_POINTER_UP:
                    updateStateByEvent(event);

                    if (!mSloppyGesture) {
                        mListener.onRotateEnd(this);
                    }

                    resetState();
                    break;

                case MotionEvent.ACTION_CANCEL:
                    if (!mSloppyGesture) {
                        mListener.onRotateEnd(this);
                    }

                    resetState();
                    break;

                case MotionEvent.ACTION_MOVE:
                    updateStateByEvent(event);
                    if (mCurrPressure / mPrevPressure > PRESSURE_THRESHOLD) {
                        final boolean updatePrevious = mListener.onRotate(this);
                        if (updatePrevious) {
                            mPrevEvent.recycle();
                            mPrevEvent = MotionEvent.obtain(event);
                        }
                    }
                    break;
            }
        }

        @Override
        protected void resetState() {
            super.resetState();
            mSloppyGesture = false;
        }

        public float getRotationDegreesDelta() {
            double diffRadians = Math.atan2(mPrevFingerDiffY, mPrevFingerDiffX) - Math.atan2(mCurrFingerDiffY, mCurrFingerDiffX);
            return (float) (diffRadians * 180 / Math.PI);
        }

        public interface OnRotateGestureListener {
            public boolean onRotate(RotateGestureDetector detector);

            public boolean onRotateBegin(RotateGestureDetector detector);

            public void onRotateEnd(RotateGestureDetector detector);
        }

        public static class SimpleOnRotateGestureListener implements OnRotateGestureListener {
            public boolean onRotate(RotateGestureDetector detector) {
                return false;
            }

            public boolean onRotateBegin(RotateGestureDetector detector) {
                return true;
            }

            public void onRotateEnd(RotateGestureDetector detector) {

            }
        }
    }

    public static class ShoveGestureDetector extends TwoFingerGestureDetector {

        private final OnShoveGestureListener mListener;
        private float mPrevAverageY;
        private float mCurrAverageY;
        private boolean mSloppyGesture;

        public ShoveGestureDetector(Context context, OnShoveGestureListener listener) {
            super(context);
            mListener = listener;
        }

        @Override
        protected void handleStartProgressEvent(int actionCode, MotionEvent event) {
            switch (actionCode) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    resetState();
                    mPrevEvent = MotionEvent.obtain(event);
                    mTimeDelta = 0;

                    updateStateByEvent(event);

                    mSloppyGesture = isSloppyGesture(event);
                    if (!mSloppyGesture) {
                        mGestureInProgress = mListener.onShoveBegin(this);
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (!mSloppyGesture) {
                        break;
                    }

                    mSloppyGesture = isSloppyGesture(event);
                    if (!mSloppyGesture) {
                        mGestureInProgress = mListener.onShoveBegin(this);
                    }

                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    if (!mSloppyGesture) {
                        break;
                    }

                    break;
            }
        }

        @Override
        protected void handleInProgressEvent(int actionCode, MotionEvent event) {
            switch (actionCode) {
                case MotionEvent.ACTION_POINTER_UP:
                    updateStateByEvent(event);
                    if (!mSloppyGesture) {
                        mListener.onShoveEnd(this);
                    }
                    resetState();
                    break;

                case MotionEvent.ACTION_CANCEL:
                    if (!mSloppyGesture) {
                        mListener.onShoveEnd(this);
                    }
                    resetState();
                    break;

                case MotionEvent.ACTION_MOVE:
                    updateStateByEvent(event);
                    if (mCurrPressure / mPrevPressure > PRESSURE_THRESHOLD
                            && Math.abs(getShovePixelsDelta()) > 0.5f) {
                        final boolean updatePrevious = mListener.onShove(this);
                        if (updatePrevious) {
                            mPrevEvent.recycle();
                            mPrevEvent = MotionEvent.obtain(event);
                        }
                    }
                    break;
            }
        }

        @Override
        protected void updateStateByEvent(MotionEvent curr) {
            super.updateStateByEvent(curr);

            final MotionEvent prev = mPrevEvent;
            float py0 = prev.getY(0);
            float py1 = prev.getY(1);
            mPrevAverageY = (py0 + py1) / 2.0f;

            float cy0 = curr.getY(0);
            float cy1 = curr.getY(1);
            mCurrAverageY = (cy0 + cy1) / 2.0f;
        }

        @Override
        protected boolean isSloppyGesture(MotionEvent event) {
            boolean sloppy = super.isSloppyGesture(event);
            if (sloppy)
                return true;

            double angle = Math.abs(Math.atan2(mCurrFingerDiffY, mCurrFingerDiffX));
            return !((0.0f < angle && angle < 0.35f)
                    || 2.79f < angle && angle < Math.PI);
        }

        public float getShovePixelsDelta() {
            return mCurrAverageY - mPrevAverageY;
        }

        @Override
        protected void resetState() {
            super.resetState();
            mSloppyGesture = false;
            mPrevAverageY = 0.0f;
            mCurrAverageY = 0.0f;
        }

        public interface OnShoveGestureListener {
            public boolean onShove(ShoveGestureDetector detector);

            public boolean onShoveBegin(ShoveGestureDetector detector);

            public void onShoveEnd(ShoveGestureDetector detector);
        }

        public static class SimpleOnShoveGestureListener implements OnShoveGestureListener {
            public boolean onShove(ShoveGestureDetector detector) {
                return false;
            }

            public boolean onShoveBegin(ShoveGestureDetector detector) {
                return true;
            }

            public void onShoveEnd(ShoveGestureDetector detector) {
                // Do nothing, overridden implementation may be used
            }
        }
    }

    public static abstract class TwoFingerGestureDetector extends BaseGestureDetector {

        private final float mEdgeSlop;
        protected float mPrevFingerDiffX;
        protected float mPrevFingerDiffY;
        protected float mCurrFingerDiffX;
        protected float mCurrFingerDiffY;
        private float mRightSlopEdge;
        private float mBottomSlopEdge;
        private float mCurrLen;
        private float mPrevLen;

        public TwoFingerGestureDetector(Context context) {
            super(context);

            ViewConfiguration config = ViewConfiguration.get(context);
            mEdgeSlop = config.getScaledEdgeSlop();
        }

        @Override
        protected abstract void handleStartProgressEvent(int actionCode, MotionEvent event);

        @Override
        protected abstract void handleInProgressEvent(int actionCode, MotionEvent event);

        protected void updateStateByEvent(MotionEvent curr) {
            super.updateStateByEvent(curr);

            final MotionEvent prev = mPrevEvent;

            mCurrLen = -1;
            mPrevLen = -1;

            // Previous
            final float px0 = prev.getX(0);
            final float py0 = prev.getY(0);
            final float px1 = prev.getX(1);
            final float py1 = prev.getY(1);
            final float pvx = px1 - px0;
            final float pvy = py1 - py0;
            mPrevFingerDiffX = pvx;
            mPrevFingerDiffY = pvy;

            // Current
            final float cx0 = curr.getX(0);
            final float cy0 = curr.getY(0);
            final float cx1 = curr.getX(1);
            final float cy1 = curr.getY(1);
            final float cvx = cx1 - cx0;
            final float cvy = cy1 - cy0;
            mCurrFingerDiffX = cvx;
            mCurrFingerDiffY = cvy;
        }

        public float getCurrentSpan() {
            if (mCurrLen == -1) {
                final float cvx = mCurrFingerDiffX;
                final float cvy = mCurrFingerDiffY;
                mCurrLen = (float) Math.sqrt(cvx * cvx + cvy * cvy);
            }
            return mCurrLen;
        }

        public float getPreviousSpan() {
            if (mPrevLen == -1) {
                final float pvx = mPrevFingerDiffX;
                final float pvy = mPrevFingerDiffY;
                mPrevLen = (float) Math.sqrt(pvx * pvx + pvy * pvy);
            }
            return mPrevLen;
        }

        protected boolean isSloppyGesture(MotionEvent event) {
            // As orientation can change, query the metrics in touch down
            DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
            mRightSlopEdge = metrics.widthPixels - mEdgeSlop;
            mBottomSlopEdge = metrics.heightPixels - mEdgeSlop;

            final float edgeSlop = mEdgeSlop;
            final float rightSlop = mRightSlopEdge;
            final float bottomSlop = mBottomSlopEdge;

            final float x0 = event.getRawX();
            final float y0 = event.getRawY();
            final float x1 = getRawX(event, 1);
            final float y1 = getRawY(event, 1);

            boolean p0sloppy = x0 < edgeSlop || y0 < edgeSlop
                    || x0 > rightSlop || y0 > bottomSlop;
            boolean p1sloppy = x1 < edgeSlop || y1 < edgeSlop
                    || x1 > rightSlop || y1 > bottomSlop;

            if (p0sloppy && p1sloppy) {
                return true;
            } else if (p0sloppy) {
                return true;
            } else if (p1sloppy) {
                return true;
            }
            return false;
        }

        protected static float getRawX(MotionEvent event, int pointerIndex) {
            float offset = event.getX() - event.getRawX();
            if (pointerIndex < event.getPointerCount()) {
                return event.getX(pointerIndex) + offset;
            }
            return 0f;
        }


        protected static float getRawY(MotionEvent event, int pointerIndex) {
            float offset = event.getY() - event.getRawY();
            if (pointerIndex < event.getPointerCount()) {
                return event.getY(pointerIndex) + offset;
            }
            return 0f;
        }
    }

    public void undo(){
        if(entities != null && !entities.isEmpty()){
            if((entities.size()-1) >= 0 && entities.size() > 0){
                entities.remove(entities.size()-1);
                invalidate();
            }
        }
    }
}