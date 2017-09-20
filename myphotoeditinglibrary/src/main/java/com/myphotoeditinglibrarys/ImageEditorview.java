package com.myphotoeditinglibrarys;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.myphotoeditinglibrary.R;
import java.io.ByteArrayOutputStream;
import java.util.List;
/**
 * Created by Ghanshyam on 7/25/2017.
 */
public class ImageEditorview extends RelativeLayout implements TxtEditDialogFragment.OnTextLayerCallback,View.OnClickListener {

    private static final int PIC_CROP = 111;

    public enum Options {
        FILTER, STICKER, TEXT,NONE;
    }
    private Options currenSelectedOptions = Options.NONE;
    private MyStickerview myStickerview;
    private MyTextview myTextview;
    private MyFilterview myfilterview;
    LinearLayout filter_layout;
    SeekBar seekbar_filter;
    LinearLayout bottomLayout;
    TextView filter,/*tagfriend,*/crop,sticker,text;

    private MyCustomImageview myCustomImageview;

    private TextView tv_done,tv_undo,tv_apply,tv_back;
    private RelativeLayout headrerview;
    LinearLayout bottomview;

    public ImageEditorview(Context context) {
        super(context);
        init();
    }

    public ImageEditorview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ImageEditorview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        if (getChildCount() > 0) {
            removeAllViews();
        }

        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_imageeditview, null, false);
        myCustomImageview = view.findViewById(R.id.myCustomImageview);
        myStickerview = view.findViewById(R.id.myStickerview);
        myTextview = view.findViewById(R.id.myTextview);
        myfilterview = view.findViewById(R.id.myfilterview);

        filter_layout = view.findViewById(R.id.filter_layout);
        seekbar_filter = view.findViewById(R.id.seekbar_filter);
        seekbar_filter.setVisibility(GONE);

        headrerview = view.findViewById(R.id.headrerview);
        bottomview = view.findViewById(R.id.bottomview);

        tv_back = view.findViewById(R.id.tv_back);
        tv_done = view.findViewById(R.id.tv_done);
        tv_undo = view.findViewById(R.id.tv_undo);
        tv_apply = view.findViewById(R.id.tv_apply);

        bottomLayout= (LinearLayout) view.findViewById(R.id.bottomLayout);
//        tagfriend = (TextView) view.findViewById(R.id.tagfriend);
        filter = (TextView)view.findViewById(R.id.filter);
        crop = (TextView)view.findViewById(R.id.crop);
        sticker = (TextView)view.findViewById(R.id.sticker);
        text = (TextView)view.findViewById(R.id.text);


        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(view, layoutParams);


        setApplyButtonVisibility(false);
        setDoneButtonVisibility(false);
    }

    Activity activity;

    public void addAttachActivity(Activity activit) {
        this.activity = activit;
        myCustomImageview.setframeViewCallback(frameViewCallback);

        myStickerview.setSelectedListener(new MyStickerview.SelectedListener() {
            @Override
            public void onSelectSticker(int stickerId) {
                addSticker(stickerId);
            }
        });

        myTextview.setTextUpdateListener(new MyTextview.TextUpdateListener() {
            @Override
            public void increaseTextSize() {
                increaseTextEntitySize();
            }

            @Override
            public void decreaseTextSize() {
                decreaseTextEntitySize();
            }

            @Override
            public void changeTextColor() {
                changeTextEntityColor();
            }

            @Override
            public void changeTextFont() {
                changeTextEntityFont();
            }

            @Override
            public void editText() {
                startTextEntityEditing();
            }
        });

        myfilterview.setSelectedListener(new MyFilterview.SelectedListener() {
            @Override
            public void onSelectImage(int color) {
//                headrerview.setVisibility(VISIBLE);
                setApplyButtonVisibility(true);
                setDoneButtonVisibility(false);
                int sat = 255 - seekbar_filter.getProgress();
                myCustomImageview.setColorFiltration(color,sat);
            }
        });

        tv_done.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                setApplyButtonVisibility(false);
                setDoneButtonVisibility(false);
                showView(Options.NONE);
                seekbar_filter.setProgress(0);
                if(onResultedBitmapListener != null){
                    onResultedBitmapListener.onResultedBitmap(myCustomImageview.getThumbnailImage());
                }
                hideImageEditor();
            }
        });

        tv_undo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                myCustomImageview.undo();
              /*  if(myCustomImageview.getEntities() == null || myCustomImageview.getEntities().isEmpty()) {
                    setApplyButtonVisibility(false);
                    setDoneButtonVisibility(false);
                    seekbar_filter.setProgress(0);
                    showView(Options.NONE);
                }*/
            }
        });

        tv_apply.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
              myCustomImageview.setThumbnailImage();
              setApplyButtonVisibility(false);
              seekbar_filter.setProgress(0);
              showView(Options.NONE);
//              headrerview.setVisibility(VISIBLE);
              setDoneButtonVisibility(true);
            }
        });

        tv_back.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
//                onResultedBitmapListener.onCancel();
                hideImageEditor();
            }
        });

        seekbar_filter.setMax(150);
        seekbar_filter.setProgress(0);
        seekbar_filter.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

//                myCustomImageview.setColorFiltration(myCustomImageview.colorfilter,(float)(progress/255f));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(seekBar.getProgress() > 0 ) {
                    setApplyButtonVisibility(true);
                    setDoneButtonVisibility(false);
                    int sat = 255 - seekBar.getProgress();
                    myCustomImageview.setColorFiltration(myCustomImageview.colorfilter, sat);
                }
            }
        });

//        tagfriend.setOnClickListener(this);
        filter.setOnClickListener(this);
        crop.setOnClickListener(this);
        sticker.setOnClickListener(this);
        text.setOnClickListener(this);
    }

    public interface OnResultedBitmapListener{
        public void onResultedBitmap(Bitmap bitmap);
    }
    OnResultedBitmapListener onResultedBitmapListener;
    public void setOnResultedBitmapListener(OnResultedBitmapListener listener){
        this.onResultedBitmapListener = listener;
    }

    private final MyCustomImageview.FrameViewCallback frameViewCallback = new MyCustomImageview.FrameViewCallback() {
        @Override
        public void onEntitySelected(@Nullable MyCustomImageview.BaseEntity entity) {
            if (entity != null && entity instanceof MyCustomImageview.TextEntity) {
                myTextview.setVisibility(View.VISIBLE);
                myStickerview.setVisibility(View.GONE);
            }else if (entity != null && entity instanceof MyCustomImageview.ImageEntity) {
                myStickerview.setVisibility(View.VISIBLE);
                myTextview.setVisibility(View.GONE);
            }else if(entity == null && currenSelectedOptions == Options.TEXT){
                showOnlyView(Options.NONE);
            }else if(currenSelectedOptions == Options.TEXT){
                showOnlyView(currenSelectedOptions);
            }
        }

        @Override
        public void onEntitySingleTap(@NonNull MyCustomImageview.BaseEntity entity) {
            startTextEntityEditing();
        }

        @Override
        public void onEntityDoubleTap(@NonNull MyCustomImageview.BaseEntity entity) {
            startTextEntityEditing();
        }

        @Override
        public void onRemainigEntityList(List<MyCustomImageview.BaseEntity> list) {
            if(list == null || list.isEmpty()){
                setApplyButtonVisibility(false);
                setDoneButtonVisibility(false);
                seekbar_filter.setProgress(0);
                showView(Options.NONE);
            }
        }
    };

    private void addSticker(final int id) {
//        headrerview.setVisibility(VISIBLE);
        setApplyButtonVisibility(true);
        setDoneButtonVisibility(false);
        myCustomImageview.post(new Runnable() {
            @Override
            public void run() {
                MyCustomImageview.BaseCreater baseCreater = new MyCustomImageview.BaseCreater();
                Bitmap pica = BitmapFactory.decodeResource(getResources(), id);
//                Bitmap cross = BitmapFactory.decodeResource(getResources(), R.drawable.cross);
                MyCustomImageview.ImageEntity entity = new MyCustomImageview.ImageEntity(baseCreater, pica/*,cross*/, myCustomImageview.getWidth(), myCustomImageview.getHeight());
                myCustomImageview.addEntityAndPosition(entity);
            }
        });
    }

    public void setApplyButtonVisibility(boolean isvisible){
        if(isvisible)
          tv_apply.setVisibility(VISIBLE);
        else
          tv_apply.setVisibility(GONE);

        setUndoButtonVisibility(isvisible);
    }

    public void setDoneButtonVisibility(boolean isvisible){
        if(isvisible)
            tv_done.setVisibility(VISIBLE);
        else
            tv_done.setVisibility(GONE);
    }

    public void setUndoButtonVisibility(boolean isvisible){
        if(isvisible)
            tv_undo.setVisibility(VISIBLE);
        else
            tv_undo.setVisibility(GONE);
    }

    public void showImageEditor(){
        setVisibility(VISIBLE);
        setApplyButtonVisibility(false);
        setDoneButtonVisibility(false);
    }

    public void hideImageEditor(){
        setVisibility(GONE);
    }

    public void showView(Options options) {
        myCustomImageview.resetview();
        Bitmap currentBitmap = myCustomImageview.getThumbnailImage();
        myTextview.setVisibility(View.GONE);
        myfilterview.setVisibility(View.GONE);
        myStickerview.setVisibility(View.GONE);
        filter_layout.setVisibility(View.GONE);

//        headrerview.setVisibility(GONE);
        dismissDialog();
        switch (options) {
            case FILTER:
                if (currentBitmap != null) {
                    filter_layout.setVisibility(View.VISIBLE);
                    myfilterview.setVisibility(View.VISIBLE);
                    myfilterview.updateImage(activity,currentBitmap);

//                    setApplyButtonVisibility(true);
//                    setDoneButtonVisibility(false);
                }
                break;
            case STICKER:
                if (currentBitmap != null) {
                    myStickerview.setVisibility(View.VISIBLE);

//                    setApplyButtonVisibility(true);
//                    setDoneButtonVisibility(false);
                }
                break;
            case TEXT:
                if (currentBitmap != null) {
                    myTextview.setVisibility(View.VISIBLE);

                    addTextSticker(activity);
//                    setApplyButtonVisibility(true);
//                    setDoneButtonVisibility(false);
                }
                break;

            case NONE:
//                headrerview.setVisibility(GONE);
//                myCustomImageview.invalidate();
                break;
        }

        currenSelectedOptions  = options;
    }

    public void showOnlyView(Options options) {
        myTextview.setVisibility(View.GONE);
        myfilterview.setVisibility(View.GONE);
        myStickerview.setVisibility(View.GONE);
        filter_layout.setVisibility(View.GONE);
        switch (options) {
            case FILTER:
                filter_layout.setVisibility(View.VISIBLE);
                myfilterview.setVisibility(View.VISIBLE);
                break;
            case STICKER:
                myStickerview.setVisibility(View.VISIBLE);
                break;
            case TEXT:
                myTextview.setVisibility(View.VISIBLE);
                break;
            case NONE:
                break;
        }
    }

    public void setPhotoBitmap(final Bitmap bmp) {
        try {
            myCustomImageview.setBitmap(bmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*public void setPhotoBitmap(final Bitmap bmp) {

        try {
//
            File cDir = activity.getApplication().getExternalFilesDir(null);
            File saveFilePath = new File(cDir.getPath() + "/" + "my_image.jpg");
            FileOutputStream fileOutputStream = new FileOutputStream(saveFilePath);
            boolean isCompres = bmp.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            Log.i("filepath", saveFilePath.getAbsolutePath());
//            filePath = saveFilePath.getAbsolutePath();
//            myCustomImageview.setImageBitmap(bmp);
//            myCustomImageview.setImageDrawable(new BitmapDrawable(bmp));
//            myCustomImageview.setBackground(new BitmapDrawable(bmp));
            setCurrentBitmap(bmp);
            myCustomImageview.setBitmap(bmp);

            saveFilePath.deleteOnExit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    private void increaseTextEntitySize() {
        MyCustomImageview.TextEntity textEntity = currentTextEntity();
        if (textEntity != null) {
            textEntity.getBaseCreator().getFont().increaseSize(MyCustomImageview.TextCreater.Limits.FONT_SIZE_STEP);
            textEntity.updateEntity();
            myCustomImageview.invalidate();
        }
    }

    private void decreaseTextEntitySize() {
        MyCustomImageview.TextEntity textEntity = currentTextEntity();
        if (textEntity != null) {
            textEntity.getBaseCreator().getFont().decreaseSize(MyCustomImageview.TextCreater.Limits.FONT_SIZE_STEP);
            textEntity.updateEntity();
            myCustomImageview.invalidate();
        }
    }

    private void changeTextEntityColor() {
        MyCustomImageview.TextEntity textEntity = currentTextEntity();
        if (textEntity == null) {
            return;
        }
        int initialColor = textEntity.getBaseCreator().getFont().getColor();
        new ColorPickerDialog(getContext(), new ColorPickerDialog.OnColorChangedListener() {
            @Override
            public void colorChanged(int color) {
                MyCustomImageview.TextEntity textEntity = currentTextEntity();
                if (textEntity != null) {
                    textEntity.getBaseCreator().getFont().setColor(color);
                    textEntity.updateEntity();
                    myCustomImageview.invalidate();
                }
            }
        }, initialColor).show();

//        MyCustomImageview.TextEntity textEntity = currentTextEntity();
//        if (textEntity == null) {
//            return;
//        }
//
//        int initialColor = textEntity.getLayer().getFont().getColor();
//
//        ColorPickerDialogBuilder
//                .with(MainActivity.this)
//                .setTitle(R.string.select_color)
//                .initialColor(initialColor)
//                .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
//                .density(8) // magic number
//                .setPositiveButton(R.string.ok, new ColorPickerClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
//                        MyCustomImageview.TextEntity textEntity = currentTextEntity();
//                        if (textEntity != null) {
//                            textEntity.getLayer().getFont().setColor(selectedColor);
//                            textEntity.updateEntity();
//                            myCustomImageview.invalidate();
//                        }
//                    }
//                })
//                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                    }
//                })
//                .build()
//                .show();
    }

    private void changeTextEntityFont() {
        final List<String> fonts = myCustomImageview.fontSetting.getFontNames();
        FontsAdapter fontsAdapter = new FontsAdapter(getContext(), fonts, myCustomImageview.fontSetting);
        new AlertDialog.Builder(getContext())
                .setTitle("Select font")
                .setAdapter(fontsAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        MyCustomImageview.TextEntity textEntity = currentTextEntity();
                        if (textEntity != null) {
                            textEntity.getBaseCreator().getFont().setTypeface(fonts.get(which));
                            textEntity.updateEntity();
                            myCustomImageview.invalidate();
                        }
                    }
                })
                .show();
    }

    TxtEditDialogFragment fragment;

    public void startTextEntityEditing() {
//        headrerview.setVisibility(VISIBLE);
        setApplyButtonVisibility(true);
        setDoneButtonVisibility(false);
        MyCustomImageview.TextEntity textEntity = currentTextEntity();
        if (textEntity != null) {
            String text = "";
            if(textEntity.getBaseCreator().getText().equalsIgnoreCase("Text here")){
                fragment = TxtEditDialogFragment.getInstance("");
            }else{
                fragment = TxtEditDialogFragment.getInstance(textEntity.getBaseCreator().getText());
            }

            fragment.setOnTextLayerCallback(this);
            fragment.show(activity.getFragmentManager(), TxtEditDialogFragment.class.getName());
        }
    }

    public void dismissDialog() {
        Fragment prev = activity.getFragmentManager().findFragmentByTag(TxtEditDialogFragment.class.getName());
        if (prev != null) {
            DialogFragment df = (DialogFragment) prev;
            df.dismiss();
        }
       MyCustomImageview.TextEntity textEntity =  currentTextEntity();
        if(textEntity != null)
          textEntity.setIsSelected(false);
    }

    @Nullable
    private MyCustomImageview.TextEntity currentTextEntity() {
        if (myCustomImageview != null && myCustomImageview.getSelectedEntity() instanceof MyCustomImageview.TextEntity) {
            return ((MyCustomImageview.TextEntity) myCustomImageview.getSelectedEntity());
        } else {
            return null;
        }
    }

    private MyCustomImageview.TextCreater createTextCreator() {
        MyCustomImageview.TextCreater textCreator = new MyCustomImageview.TextCreater();
        MyCustomImageview.Font font = new MyCustomImageview.Font();

//        font.setColor(MyCustomImageview.TextCreater.Limits.INITIAL_FONT_COLOR);
        font.setColor(Color.LTGRAY);
        font.setSize(MyCustomImageview.TextCreater.Limits.INITIAL_FONT_SIZE);
        font.setTypeface(myCustomImageview.fontSetting.getDefaultFontName());

        textCreator.setFont(font);

//        if (BuildConfig.DEBUG) {
            textCreator.setText("Text here");
//        }

        return textCreator;
    }

    public void addTextSticker(Activity activity) {
        MyCustomImageview.TextCreater textCreator = createTextCreator();
        MyCustomImageview.TextEntity textEntity = new MyCustomImageview.TextEntity(textCreator, myCustomImageview.getWidth(),
                myCustomImageview.getHeight(), myCustomImageview.fontSetting);
        myCustomImageview.addEntityAndPosition(textEntity);

        PointF center = textEntity.absoluteCenter();
        center.y = center.y * 0.5F;
        textEntity.moveCenterTo(center);

        myCustomImageview.invalidate();

//        startTextEntityEditing();
    }

    @Override
    public void textChanged(@NonNull String text) {
        MyCustomImageview.TextEntity textEntity = currentTextEntity();
        if (textEntity != null) {
            MyCustomImageview.TextCreater textCreator = textEntity.getBaseCreator();
            if (!text.equals(textCreator.getText())) {
                if(isStringNullOrBlank(text)){
                    textCreator.setText("Text here");
                }else {
                   textCreator.setText(text);
                }
                textEntity.updateEntity();
                myCustomImageview.invalidate();
            }
        }
    }

    public static boolean isStringNullOrBlank(String str) {
        try{

            if (str == null) {
                return true;
            } else if (str.equals("null") || str.equals("") || (str != null && str.isEmpty()) || (str != null && str.length() <= 0) || str.equalsIgnoreCase("null")) {
                return true;
            }

        }catch(Exception e){

            e.printStackTrace();
        }
        return false;
    }

  /* public Bitmap tintImage(Bitmap bitmap, int color) {
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        Paint paint = new Paint();
        ColorFilter filter = new ColorMatrixColorFilter(cm);
        filter = new PorterDuffColorFilter(color, PorterDuff.Mode.OVERLAY);
        paint.setColorFilter(filter);

        Bitmap screenshot = Bitmap.createBitmap(myCustomImageview.getMeasuredWidth(), myCustomImageview.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(screenshot);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        myCustomImageview.layout(0, 0, myCustomImageview.getLayoutParams().width, myCustomImageview.getLayoutParams().height);
        myCustomImageview.draw(canvas);
        return bitmap;
    }*/

    public Uri getImageUri(Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(activity.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public void cropImage(){

       Intent cropIntent = new Intent("com.android.camera.action.CROP");
       cropIntent.setDataAndType(getImageUri(myCustomImageview.getThumbnailImage()), "image/*");
       cropIntent.putExtra("crop", "true");
       cropIntent.putExtra("aspectX", 1);
       cropIntent.putExtra("aspectY", 1);
       cropIntent.putExtra("outputX", 256);
       cropIntent.putExtra("outputY", 256);
       cropIntent.putExtra("return-data", true);
       activity.startActivityForResult(cropIntent,PIC_CROP);
   }

   public void onActivityResult(int requestCode, int resultCode, Intent data){
       if(resultCode == Activity.RESULT_OK && requestCode == PIC_CROP){
           Bundle extras = data.getExtras();
           Bitmap thePic = (Bitmap) extras.get("data");
           myCustomImageview.setBitmap(thePic);
           setApplyButtonVisibility(false);
           setDoneButtonVisibility(true);
       }
   }

    @Override
    public void onClick(View view) {
        /*if(view.getId() == tagfriend){

        }else */if(view.getId() == R.id.filter){
            showView(ImageEditorview.Options.FILTER);
        }else if(view.getId() == R.id.crop){
            cropImage();
        }else if(view.getId() == R.id.sticker){
            showView(ImageEditorview.Options.STICKER);
        }else if(view.getId() == R.id.text){
            showView(ImageEditorview.Options.TEXT);
        }
    }
}
