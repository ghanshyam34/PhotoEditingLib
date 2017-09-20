package com.myphotoeditinglibrarys;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.myphotoeditinglibrary.R;

import static android.R.attr.inAnimation;
import static android.R.attr.outAnimation;
import static android.R.attr.visibility;

/**
 * Created by Ghanshyam on 7/24/2017.
 */
public class MyTextview extends LinearLayout implements View.OnClickListener{

    ImageButton ibFontDecrease,ibFontIncrease,ibColorFormat,ibFontChange,ibEdit;
    public MyTextview(Context context) {
        super(context);
        initview(context);
    }

    public MyTextview(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initview(context);
    }

    public MyTextview(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initview(context);
    }

    public void initview(Context context){
        if (getChildCount() > 0) {
            removeAllViews();
        }
        LinearLayout.LayoutParams layoutParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT);
        layoutParams.weight = 1f;

        ibFontDecrease = new ImageButton(context);
        ibFontDecrease.setBackground(getResources().getDrawable(R.drawable.white_rectangle_ripple_effect));
        ibFontDecrease.setLayoutParams(layoutParams);
        ibFontDecrease.setImageResource(R.drawable.icon_decreases);


        ibFontIncrease = new ImageButton(context);
        ibFontIncrease.setLayoutParams(layoutParams);
        ibFontIncrease.setBackground(getResources().getDrawable(R.drawable.white_rectangle_ripple_effect));
        ibFontIncrease.setImageResource(R.drawable.icon_increase);

        ibColorFormat = new ImageButton(context);
        ibColorFormat.setLayoutParams(layoutParams);
        ibColorFormat.setBackground(getResources().getDrawable(R.drawable.white_rectangle_ripple_effect));
        ibColorFormat.setImageResource(R.drawable.icon_color);

        ibFontChange = new ImageButton(context);
        ibFontChange.setLayoutParams(layoutParams);
        ibFontChange.setBackground(getResources().getDrawable(R.drawable.white_rectangle_ripple_effect));
        ibFontChange.setImageResource(R.drawable.icon_font);

        ibEdit = new ImageButton(context);
        ibEdit.setLayoutParams(layoutParams);
        ibEdit.setBackground(getResources().getDrawable(R.drawable.white_rectangle_ripple_effect));
        ibEdit.setImageResource(R.drawable.icon_edit);

        addView(ibEdit);
        addView(ibColorFormat);
        addView(ibFontChange);
        addView(ibFontDecrease);
        addView(ibFontIncrease);


        ibFontDecrease.setOnClickListener(this);
        ibFontIncrease.setOnClickListener(this);
        ibColorFormat.setOnClickListener(this);
        ibFontChange.setOnClickListener(this);
        ibEdit.setOnClickListener(this);

//        if(isFirsttime) {
//            isFirsttime  = false;
//            Util.animation(this, 0, 0, geth, 0, View.VISIBLE, true, 0);
//            Util.animation(this, 0, 0, 0, h, View.GONE, true, 0);
//        }
    }

    TextUpdateListener textUpdateListener;
    public void setTextUpdateListener(TextUpdateListener listener){
        this.textUpdateListener = listener;
    }

    @Override
    public void onClick(View view) {

        if(textUpdateListener != null) {

            if (view == ibFontDecrease) {
                textUpdateListener.decreaseTextSize();
            } else if (view == ibFontIncrease) {
                textUpdateListener.increaseTextSize();
            } else if (view == ibColorFormat) {
                textUpdateListener.changeTextColor();
            } else if (view == ibFontChange) {
                textUpdateListener.changeTextFont();
            } else if (view == ibEdit) {
                textUpdateListener.editText();
            }
        }
    }

    public interface TextUpdateListener{
        public void increaseTextSize();
        public void decreaseTextSize();
        public void changeTextColor();
        public void changeTextFont();
        public void editText();
    }

    boolean isFirsttime = true;
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public void setVisibility(int visibility)
    {
        super.setVisibility(visibility);
//        if(visibility == VISIBLE){
//            Util.animation(this,0,0,this.getHeight(),0,View.VISIBLE,true,400);
//        }else{
//            Util.animation(this,0,0,0,this.getHeight(),View.GONE,true,400);
//        }

    }

//    @Override
//    protected void onVisibilityChanged(@NonNull View changedView,int visibility) {
//
//        if (visibility == VISIBLE)
//        {
////            Util.runFadeOutAnimationOn(changedView,getWidth(),getHeight());
//            Util.getAnimation(this,0,0,(int)(getY()+getHeight()),(int)(getY()),300,visibility);
//        }
//        else
//        {
////            Util.runFadeOutAnimationOnBottom(changedView,getWidth(),getHeight());
//            Util.getAnimation(this,0,0,(int)(getY()),(int)(getY()+getHeight()),300,visibility);
//        }
////        super.onVisibilityChanged(changedView, visibility);
//    }
}
