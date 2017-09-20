package com.myphotoeditinglibrarys;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
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
import android.widget.ImageView;

import com.myphotoeditinglibrary.R;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.left;
import static android.R.attr.src;
import static android.R.attr.x;
import static android.support.v7.widget.AppCompatDrawableManager.get;

/**
 * Created by Ghanshyam on 7/24/2017.
 */
public class MyCustomImageview extends ImageView {

    static Context mContext;
    public interface Constants {
        float SELECTED_LAYER_ALPHA = 0.15F;
    }

    public interface FrameViewCallback {
        void onEntitySelected(@Nullable BaseEntity entity);
        void onEntityDoubleTap(@NonNull BaseEntity entity);
        void onEntitySingleTap(@NonNull BaseEntity entity);
        void onRemainigEntityList(List<BaseEntity> list);
    }

    private final List<BaseEntity> entities = new ArrayList<>();
    @Nullable
    private BaseEntity selectedEntity;

    private Paint selectedLayerPaint;

    @Nullable
    private FrameViewCallback frameViewCallback;

    private ScaleGestureDetector scaleGestureDetector;
    private RotateGestureDetector rotateGestureDetector;
    private MoveGestureDetector moveGestureDetector;
    private GestureDetectorCompat gestureDetectorCompat;


    public MyCustomImageview(Context context) {
        super(context);
        init(context);
    }

    public MyCustomImageview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MyCustomImageview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MyCustomImageview(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    FontSetting fontSetting;

    private void init(@NonNull Context context) {
        mContext = context;

        this.fontSetting = new FontSetting(getResources());

//        setWillNotDraw(false);

        selectedLayerPaint = new Paint();
        selectedLayerPaint.setAlpha((int) (255 * Constants.SELECTED_LAYER_ALPHA));
        selectedLayerPaint.setAntiAlias(true);

        this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        this.rotateGestureDetector = new RotateGestureDetector(context, new RotateListener());
        this.moveGestureDetector = new MoveGestureDetector(context, new MoveListener());
        this.gestureDetectorCompat = new GestureDetectorCompat(context, new TapsListener());

        setOnTouchListener(onTouchListener);

        updateUI();
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

    private void initEntityBorder(@NonNull BaseEntity entity) {
        int strokeSize = getResources().getDimensionPixelSize(R.dimen.stroke_size);
        Paint borderPaint = new Paint();
        borderPaint.setStrokeWidth(strokeSize);
        borderPaint.setAntiAlias(true);
        borderPaint.setColor(ContextCompat.getColor(getContext(), R.color.border_color));

        entity.setBorderPaint(borderPaint);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (selectedEntity != null) {
            selectedEntity.draw(canvas, selectedLayerPaint);
        }
    }

    public int colorfilter = -1;
    Paint paint = new Paint();

    public void setColorFiltration(int colorfilter,int opacity) {
        this.colorfilter = colorfilter;
        float[] colorTransform = new float[]{
                Color.red(colorfilter) / 255f, 0, 0, 0, 0,
                0, Color.green(colorfilter) / 255f, 0, 0, 0,
                0, 0, Color.blue(colorfilter) / 255f, 0, 0,
                0, 0, 0, 1, 0};
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0f);
        colorMatrix.set(colorTransform);
        ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorFilter);
        if(opacity > 0)
          paint.setAlpha(opacity);
        else
          paint.setAlpha(255);

        invalidate();
    }

    public

//    private final List<BaseEntity> currenEntity = new ArrayList<>();
            Bitmap bitmap;
    int height;
    int width;

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (width == 0 || height == 0) {
            this.width = w;
            this.height = h;
            invalidate();
        }
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, null, new RectF(0, 0, width, height), paint);
        }
        drawAllEntities(canvas, null);
        super.onDraw(canvas);
    }

    private void drawAllEntities(Canvas canvas, Paint paint) {
        for (int i = 0; i < entities.size(); i++) {
            entities.get(i).draw(canvas, paint);
        }
    }

    public void setThumbnailImage() {

        try {
            selectEntity(null, false);
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmp.eraseColor(Color.WHITE);
            Canvas canvas = new Canvas(bmp);
            canvas.drawBitmap(bitmap, null, new RectF(0, 0, width, height), paint);
            drawAllEntities(canvas, null);
            this.bitmap = bmp;
//            if(currenEntity != null && !currenEntity.isEmpty()){
//                currenEntity.clear();
//            }
//            for(int i=0;i<entities.size();i++){
//                currenEntity.add(entities.get(i));
//            }
            resetview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetview() {
        if (entities != null && !entities.isEmpty()) {
            entities.clear();
            selectEntity(null, false);
        }
        setColorFiltration(Color.WHITE,0);
    }

    public Bitmap getThumbnailImage() {
        return bitmap;
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


    @Nullable
    private BaseEntity findCrossEntityAtPoint(float x, float y) {
        BaseEntity selected = null;
        PointF p = new PointF(x, y);
        for (int i = entities.size() - 1; i >= 0; i--) {
            if (entities.get(i).touchTheEntityPosition(p)){
                selected = entities.get(i);
                break;
            }
        }
        return selected;
    }

    private void updateSelectionOnTap(MotionEvent e) {
        BaseEntity crossEntity = findCrossEntityAtPoint(e.getX(), e.getY());
        if(crossEntity != null && crossEntity.isSelected()){
            undoSelf(crossEntity);
        }else{
            BaseEntity entity = findEntityAtPoint(e.getX(), e.getY());
            selectEntity(entity, true);
        }
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

    private final OnTouchListener onTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (scaleGestureDetector != null) {
                scaleGestureDetector.onTouchEvent(event);
                rotateGestureDetector.onTouchEvent(event);
                moveGestureDetector.onTouchEvent(event);
                gestureDetectorCompat.onTouchEvent(event);
            }
            return true;
        }
    };

    boolean isSelectedItem;
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
            if (frameViewCallback != null && selectedEntity != null) {
                frameViewCallback.onEntitySingleTap(selectedEntity);
            }
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (selectedEntity != null) {
                float scaleFactorDiff = detector.getScaleFactor();
                selectedEntity.getBaseCreator().postScale(scaleFactorDiff - 1.0F);
                updateUI();
            }
            return true;
        }
    }

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

        private String text, hint;
        private Font font;

        public TextCreater() {
        }

        @Override
        protected void reset() {
            super.reset();
            this.text = "";
            this.hint = "";
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

        public String getHint() {
            return hint;
        }


        public void setText(String text) {
            this.text = text;
        }

        public void setHint(String hint) {
            this.hint = hint;
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
            int INITIAL_FONT_COLOR2 = 0xff000000;
            float INITIAL_SCALE = 0.8F;
        }
    }

    public static class ImageEntity extends BaseEntity {

        Canvas currentCanvas;
        Rect rect = new Rect();
        @NonNull
        private final Bitmap bitmap;
//        private final Bitmap crossBitmap;

        public ImageEntity(@NonNull BaseCreater layer,
                           @NonNull Bitmap bitmap,
                           /*Bitmap cross,*/
                           @IntRange(from = 1) int canvasWidth,
                           @IntRange(from = 1) int canvasHeight) {
            super(layer, canvasWidth, canvasHeight);

            this.bitmap = bitmap;
//            this.crossBitmap = cross;
            float width = bitmap.getWidth();
            float height = bitmap.getHeight();

            float widthAspect = 1.0F * canvasWidth / width;
            float heightAspect = 1.0F * canvasHeight / height;
            holyScale = Math.min(widthAspect, heightAspect);

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
        }

        @Override
        public Canvas getCurrentCanvas(){
            return currentCanvas;
        }

        @Override
        public void drawContent(@NonNull Canvas canvas, @Nullable Paint drawingPaint) {
            this.currentCanvas = canvas;
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

        @Override
        public int getScaledBitmapWidth(Canvas canvas) {
            return bitmap.getScaledWidth(canvas);
        }

        @Override
        public int getScaledBitmapHeight(Canvas canvas) {
            return bitmap.getScaledHeight(canvas);
        }
    }

    public static class TextEntity extends BaseEntity {

        private final TextPaint textPaint;
        private final FontSetting fontSetting;


        @Nullable
        private Bitmap bitmap;
        Canvas currentCanvas;

        public TextEntity(@NonNull TextCreater textLayer,
                          @IntRange(from = 1) int canvasWidth,
                          @IntRange(from = 1) int canvasHeight,
                          @NonNull FontSetting fontP) {
            super(textLayer, canvasWidth, canvasHeight);
            this.fontSetting = fontP;
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

//            Rect textBounds = new Rect();
//            Paint paint = new Paint();
//            paint.setTextSize(textLayer.getFont().getSize() * canvasWidth);
//            paint.setTypeface(fontSetting.getTypeface(textLayer.getFont().getTypeface()));
//            paint.setColor(Color.YELLOW);
//            paint.getTextBounds(textLayer.getText(),0,textLayer.getText().length(), textBounds);
//            canvas.drawLine(((absoluteCenterX()*canvasWidth)+textBounds.width()),absoluteCenterY()+textBounds.height(),((absoluteCenterX()*canvasWidth))+textBounds.width(),absoluteCenterY()+textBounds.height()+25, paint);

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
        public Canvas getCurrentCanvas(){
            return currentCanvas;
        }

        @Override
        protected void drawContent(@NonNull Canvas canvas, @Nullable Paint drawingPaint) {
            this.currentCanvas = canvas;
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

        @Override
        public int getScaledBitmapWidth(Canvas canvas) {
            return bitmap.getScaledWidth(canvas);
        }

        @Override
        public int getScaledBitmapHeight(Canvas canvas) {
            return bitmap.getScaledHeight(canvas);
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

    private static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static abstract class BaseEntity {

        Matrix tempCrossMatrix = new Matrix();
//        Matrix tempRotateMatrix = new Matrix();
//        Matrix tempScaleMatrix = new Matrix();

        Bitmap crossBitmap;
//        Bitmap rotateBitmap;
//        Bitmap scaleBitmap;

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
            crossBitmap = BitmapFactory.decodeResource(mContext.getResources(),R.drawable.cross);
//            rotateBitmap = BitmapFactory.decodeResource(mContext.getResources(),R.drawable.cross);
//            scaleBitmap = BitmapFactory.decodeResource(mContext.getResources(),R.drawable.cross);

            crossBitmap = getCustomBitmap(crossBitmap);
//            rotateBitmap = getCustomBitmap(rotateBitmap);
//            scaleBitmap = getCustomBitmap(scaleBitmap);
        }

        public Bitmap getCustomBitmap(Bitmap crossBitmap){
            int w  = crossBitmap.getWidth() - 20;
            int h  = crossBitmap.getHeight() - 20;
            crossBitmap = Bitmap.createBitmap(crossBitmap,0,0,w,h);
            Bitmap bmOverlay = Bitmap.createBitmap(crossBitmap.getWidth(),
                    crossBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmOverlay);
            Paint mPaint = new Paint();
            mPaint.setColor(Color.WHITE);
            c.drawCircle(crossBitmap.getWidth()/2,crossBitmap.getHeight()/2,crossBitmap.getWidth()/2, mPaint);
//            c.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
            c.drawBitmap(crossBitmap,-10,-10, null);
            return bmOverlay;
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

            tempCrossMatrix.set(matrix);
//            tempRotateMatrix.set(matrix);
//            tempScaleMatrix.set(matrix);

//            tempMatrix.preTranslate(bitmap.getScaledWidth(canvas)-32,-32/*-dpToPx(32)*/);
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
                drawSelectedBg(canvas,drawingPaint);
                borderPaint.setAlpha(storedAlpha);
            }

            canvas.restore();
        }

        private void drawSelectedBg(Canvas canvas,@Nullable Paint drawingPaint) {
            matrix.mapPoints(destPoints, srcPoints);
            canvas.drawLines(destPoints, 0, 8, borderPaint);
            canvas.drawLines(destPoints, 2, 8, borderPaint);

            float[] points = {
                    0f, 0f,
                    getScaledBitmapWidth(getCurrentCanvas()), 0f,
                    getScaledBitmapWidth(getCurrentCanvas()),getScaledBitmapHeight(getCurrentCanvas()),
                    0f,getScaledBitmapHeight(getCurrentCanvas())
            };
            matrix.mapPoints(points);
            float entityCrossX = points[2];
            float entityCrossY = points[3];
//
//            if(Math.abs(entityPreX-entityCrossX) < crossBitmap.getScaledWidth(canvas) || Math.abs(entityPreY-entityCrossY) < crossBitmap.getScaledHeight(canvas)){
//                tempCrossMatrix.preTranslate(getScaledBitmapWidth(canvas)-(crossBitmap.getScaledWidth(canvas)/2),-(crossBitmap.getScaledHeight(canvas)/2)/*-dpToPx(32)*/);
//                canvas.drawBitmap(crossBitmap,tempCrossMatrix,null);
//            }else{
                canvas.drawBitmap(crossBitmap,entityCrossX-crossBitmap.getWidth()/2,entityCrossY-crossBitmap.getHeight()/2,null);
//            }
        }

        @NonNull
        public BaseCreater getBaseCreator() {
            return baseCreater;
        }

        public abstract Canvas getCurrentCanvas();

        public void setBorderPaint(@NonNull Paint borderPaint) {
            this.borderPaint = borderPaint;
        }

        protected abstract void drawContent(@NonNull Canvas canvas, @Nullable Paint drawingPaint);

        public abstract int getWidth();

        public abstract int getScaledBitmapWidth(Canvas canvas);

        public abstract int getScaledBitmapHeight(Canvas canvas);

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

        public boolean touchTheEntityPosition(PointF point){

            updateMatrix();

            float[] points = {
                    0f, 0f,
                    getScaledBitmapWidth(getCurrentCanvas()), 0f,
                    getScaledBitmapWidth(getCurrentCanvas()),getScaledBitmapHeight(getCurrentCanvas()),
                    0f,getScaledBitmapHeight(getCurrentCanvas())
            };

            matrix.mapPoints(points);
            float touchX = point.x;
            float touchY = point.y;
            float entityCrossX = points[2];
            float entityCrossY = points[3];

//            float entityRotateX = points[4];
//            float entityRotateY = points[5];
//
//            float entityScaleX = points[6];
//            float entityScaleY = points[7];

            if(Math.abs(entityCrossX - touchX) <= (crossBitmap.getScaledWidth(getCurrentCanvas())/2) && Math.abs(entityCrossY - touchY) <= (crossBitmap.getScaledHeight(getCurrentCanvas())/2)){
                Log.i("touch found CROSS == ","touch found CROSS");
                return true;
            }/*else if(Math.abs(entityRotateX - touchX) <= (rotateBitmap.getScaledWidth(getCurrentCanvas())/2) && Math.abs(entityRotateY - touchY) <= (rotateBitmap.getScaledHeight(getCurrentCanvas())/2)){
                Log.i("touch found ROTATE== ","touch found ROTATE");
                return true;
            }else if(Math.abs(entityScaleX - touchX) <= (scaleBitmap.getScaledWidth(getCurrentCanvas())/2) && Math.abs(entityScaleY - touchY) <= (scaleBitmap.getScaledHeight(getCurrentCanvas())/2)){
                Log.i("touch found SCALE== ","touch found SCALE");
                return true;
            }*/
            return false;
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

       public double ComputeAngle(float x, float y)
        {
            final double RADS_TO_DEGREES = 360 / (java.lang.Math.PI*2);
            double result = java.lang.Math.atan2(y,x) * RADS_TO_DEGREES;

            if (result < 0)
            {
                result = 360 + result;
            }

            return result;
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

    public void undo() {
        if (entities != null && !entities.isEmpty()) {
            if ((entities.size() - 1) >= 0 && entities.size() > 0) {
                entities.remove(entities.size() - 1);
                selectEntity(null, false);
            }
        }
        frameViewCallback.onRemainigEntityList(entities);
    }

    public void undoSelf(BaseEntity entity) {
        if(entities != null && !entities.isEmpty() ){
            entities.remove(entity);
            selectEntity(null, false);
        }
        frameViewCallback.onRemainigEntityList(entities);
    }

//    @Override
//    protected void dispatchDraw(Canvas canvas) {
//        if (isPressed()) {
//            canvas.saveLayer(null, selectedLayerPaint, Canvas.ALL_SAVE_FLAG);
//            super.dispatchDraw(canvas);
//            if (selectedEntity != null) {
//               selectedEntity.draw(canvas, selectedLayerPaint);
//            }
//            canvas.restore();
//        } else {
//            super.dispatchDraw(canvas);
//            if (selectedEntity != null) {
//                selectedEntity.draw(canvas, selectedLayerPaint);
//            }
//        }
//    }

}
