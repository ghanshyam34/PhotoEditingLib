package com.myphotoeditinglibrarys;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.myphotoeditinglibrary.R;

/**
 * Created by Ghanshyam on 7/24/2017.
 */
public class MyFilterview extends RecyclerView{

//    public static final String FILTER_ARRAY_STRING[] = {"#F9F9F9",
//            "#E8E8E8",
//            "#D8D8D8",
//            "#CBCBCB",
//            "#B9B9B9",
//            "#A0A0A0",
//            "#868686",
//            "#676767",
//            "#555555",
//            "#3F3F3F",
//            "#262626",
//            "#131313"};

//    public int FILTER_ARRAY[];

    public static final int FILTER_ARRAY[] = {Color.BLUE,Color.RED,Color.GREEN,Color.BLACK,Color.CYAN,Color.MAGENTA,Color.YELLOW};

    public static class MyFilterItem{

    }

    public MyFilterview(Context context) {
        super(context);
        initview(context);
    }
    public MyFilterview(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initview(context);
    }

    public MyFilterview(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initview(context);
    }

    public void initview(Context context) {
        if (getChildCount() > 0) {
            removeAllViews();
        }

//        FILTER_ARRAY = new int[FILTER_ARRAY_STRING.length];
//        for(int i=0;i<FILTER_ARRAY_STRING.length;i++){
//            FILTER_ARRAY[i] = Color.parseColor(FILTER_ARRAY_STRING[i]);
//        }

        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        this.setAdapter(new Adapter(context));
        setBackgroundColor(getResources().getColor(android.R.color.white));
    }

    boolean isFirsttime = true;
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
       /* if(isFirsttime) {
            isFirsttime  = false;
            Util.animation(this, 0, 0, h, 0, View.VISIBLE, true, 0);
            Util.animation(this, 0, 0, 0, h, View.GONE, true, 0);
        }*/
    }

    Bitmap bitmap;
    public void updateImage(Context context,Bitmap bitmap){
        if (getChildCount() > 0) {
            removeAllViews();
        }
        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        this.bitmap = bitmap;
        this.setAdapter(new Adapter(context));
    }

    public interface SelectedListener{
        public void onSelectImage(int color);
    }

    SelectedListener selectedListener;
    public void setSelectedListener(SelectedListener listener){
        this.selectedListener = listener;
    }

    int alpha = 200;
    public void setAlpha(int alpha){
        this.alpha = alpha;
        this.setAdapter(new Adapter(getContext()));
    }

    class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        private final Context context;
        private final LayoutInflater layoutInflater;

        Adapter(@NonNull Context context) {
            this.context = context;
            this.layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ImageView imgview = new ImageView(context);
            ViewGroup.LayoutParams lp = new LayoutParams(140,140);
            imgview.setLayoutParams(lp);
            MarginLayoutParams lpt =(MarginLayoutParams)imgview.getLayoutParams();
            lpt.setMargins(10,10,10,10);
            imgview.setLayoutParams(lpt);
            return new ViewHolder(imgview);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if(bitmap != null) {
                holder.image.setImageBitmap(bitmap);
            }
            holder.image.setColorFilter(FILTER_ARRAY[position],PorterDuff.Mode.MULTIPLY);
            holder.image.setImageAlpha(200);
        }

        @Override
        public int getItemCount() {
            return FILTER_ARRAY.length;
        }

        private int getItem(int position) {
            return position;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView image;
            ViewHolder(View itemView) {
                super(itemView);
                image = (ImageView)itemView;
                image.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int pos = getAdapterPosition();
                        if (pos >= 0 && selectedListener != null) {
                            selectedListener.onSelectImage(FILTER_ARRAY[pos]);
                        }
                    }
                });
            }
        }
    }

    @Override
    public void setVisibility(int visibility)
    {
//        if(visibility == VISIBLE){
//            Util.animation(this,0,0,this.getHeight(),0,View.VISIBLE,true,400);
//        }else{
//            Util.animation(this,0,0,0,this.getHeight(),View.GONE,true,400);
//        }
        super.setVisibility(visibility);
    }
}
