package com.myphotoeditinglibrarys;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.myphotoeditinglibrary.R;

import static android.R.attr.visibility;

/**
 * Created by Ghanshyam on 8/11/2017.
 */
public class Util {

    public static int getWidth(TextView textView, String text) {

        Rect bounds = new Rect();
        Paint textPaint = textView.getPaint();
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        int height = bounds.height();
        int width = bounds.width();

        return width;

    }

    public static int getHeight(TextView textView, String text) {

        Rect bounds = new Rect();
        Paint textPaint = textView.getPaint();
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        int height = bounds.height();
        int width = bounds.width();

        return height;

    }

    /*public static void animateView(Context ctx, final View viewLayout,
                                   int direction) {

        try {

            viewLayout.setVisibility(View.VISIBLE);

            Animation animLayout = AnimationUtils.loadAnimation(ctx, direction);
            animLayout.setDuration(1000);
            animLayout.setFillAfter(true);

            animLayout.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {

                    viewLayout.clearAnimation();

                }
            });

            viewLayout.startAnimation(animLayout);

            // viewLayout.setVisibility(View.VISIBLE);

        } catch (Exception e) {

            e.printStackTrace();

        }

    }*/

    public void animation(final View view, float x1, float x2, float y1, float y2, final int visible1,final int visible2){
        TranslateAnimation anim = new TranslateAnimation(x1, x2,y1, y2);
        anim.setDuration(500);
        anim.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation arg0)
            {


            }

            @Override
            public void onAnimationRepeat(Animation arg0)
            {


            }

            @Override
            public void onAnimationEnd(Animation arg0)
            {
                // show whatever you want to show after animation is finished.
                view.setVisibility(visible1);
            }
        });
        view.startAnimation(anim);
    }

    public static void getAnimation(final View viewLayout, int x1, int x2,
                             int y1, int y2, int durationInMilisecond,final int visibility) {

//		viewLayout.setRotationY(360);
//		new RotateAnimation(fromDegrees,toDegrees, pivotX, pivotY)
        Animation animation = new TranslateAnimation(x1, x2, y1, y2);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                viewLayout.clearAnimation();
//                viewLayout.setVisibility(visibility);
            }
        });

        animation.setDuration(durationInMilisecond);
        // animation.setFillAfter(true);
        viewLayout.startAnimation(animation);
//        viewLayout.setVisibility(View.VISIBLE);
    }

    public static Animation inFromRightAnimation() {

        Animation inFromRight = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, +1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        inFromRight.setDuration(500);
        inFromRight.setInterpolator(new AccelerateInterpolator());
        return inFromRight;
    }

    public static Animation outToLeftAnimation() {
        Animation outtoLeft = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, -1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        outtoLeft.setDuration(500);
        outtoLeft.setInterpolator(new AccelerateInterpolator());
        return outtoLeft;
    }

    public static Animation inFromLeftAnimation() {
        Animation inFromLeft = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, -1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        inFromLeft.setDuration(500);
        inFromLeft.setInterpolator(new AccelerateInterpolator());
        return inFromLeft;
    }

    public static Animation outToRightAnimation() {
        Animation outtoRight = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, +1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        outtoRight.setDuration(500);
        outtoRight.setInterpolator(new AccelerateInterpolator());
        return outtoRight;
    }

    public void addAnimation(ListView lv) {

        AnimationSet set = new AnimationSet(true);
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(300);
        set.addAnimation(animation);

        LayoutAnimationController controller = new LayoutAnimationController(
                set, 0.5f);

        lv.setLayoutAnimation(controller);

    }

    public static void animation(View v) {

        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(1000);
        set.addAnimation(animation);

        animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(100);
        set.addAnimation(animation);

        // LayoutAnimationController controller = new
        // LayoutAnimationController(set, 0.5f);
        v.setAnimation(animation);
    }

    public static void runFadeOutAnimationOn(final View target, int width,
                                             int height,final int visibility) {

//		if(target.getTranslationY() > 0){
//			target.setTranslationY(target.getY());
//		}


        target.setTranslationY(height);


        final ViewPropertyAnimator vpa = target.animate();
        vpa.setInterpolator(new DecelerateInterpolator());

//				.translationY(0f)
        vpa.translationY(0f).withLayer();
        vpa.setDuration(300l);
        vpa.setStartDelay(20);
        vpa.start();


//				// StartAction
//				myView.animate().translationX(100).withStartAction(new Runnable(){
//				  public void run(){
//				    viewer.setTranslationX(100-myView.getWidth());
//				    // do something
//				  }
//				});
//
//				// EndAction
//				myView.animate().alpha(0).withStartAction(new Runnable(){
//				  public void run(){
//				    // Remove the view from the layout called parent
//				    parent.removeView(myView);
//				  }
//				});

        // CustomAnimation.
        vpa.setListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {

//						target.setVisibility(View.VISIBLE);
//						 super.onAnimationStart(animation);

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

//						System.out.println("onAnimationRepeat animation ");
                animation.cancel();

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                target.setTranslationY(0);
//                target.setVisibility(visibility);
//						target.animate().alpha(0).withStartAction(new Runnable(){
//							  public void run(){
//							    // Remove the view from the layout called parent
//								  vpa.removeView(target);
//							  }
//							});

//						target.clearAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

//						System.out.println("onAnimationCancel animation ");
//						animation.cancel();
            }
        });

    }

    public static void runFadeOutAnimationOnBottom(final View target, int width,
                                             int height,final int visibility) {

        target.setTranslationY(0f);


        final ViewPropertyAnimator vpa = target.animate();
        vpa.setInterpolator(new DecelerateInterpolator());

//				.translationY(0f)
        vpa.translationY(0f).withLayer();
        vpa.setDuration(300l);
        vpa.setStartDelay(20);
        vpa.start();


//				// StartAction
//				myView.animate().translationX(100).withStartAction(new Runnable(){
//				  public void run(){
//				    viewer.setTranslationX(100-myView.getWidth());
//				    // do something
//				  }
//				});
//
//				// EndAction
//				myView.animate().alpha(0).withStartAction(new Runnable(){
//				  public void run(){
//				    // Remove the view from the layout called parent
//				    parent.removeView(myView);
//				  }
//				});

        // CustomAnimation.
        vpa.setListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {

//						target.setVisibility(View.VISIBLE);
//						 super.onAnimationStart(animation);

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

//						System.out.println("onAnimationRepeat animation ");
                animation.cancel();

            }

            @Override
            public void onAnimationEnd(Animator animation) {

                target.setTranslationY(0);
//                target.setVisibility(visibility);
//						target.animate().alpha(0).withStartAction(new Runnable(){
//							  public void run(){
//							    // Remove the view from the layout called parent
//								  vpa.removeView(target);
//							  }
//							});

//						target.clearAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

//						System.out.println("onAnimationCancel animation ");
//						animation.cancel();
            }
        });

    }


    public static void runPositionAnimationOn(final View target,int width,
                                              int height,int currentPos) {

//		if(target.getTranslationY() > 0){
//			target.setTranslationY(0);
//		}

        if(height < 0){


        }

        target.setTranslationY(height);
        final ViewPropertyAnimator vpa = target.animate();
        vpa.setInterpolator(new DecelerateInterpolator(1.0f));

//				.translationY(0f)
        vpa.translationY(0f);
        vpa.setDuration(300l);
        vpa.setStartDelay(20);

        // CustomAnimation.
        vpa.setListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {

//						target.setVisibility(View.VISIBLE);

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

//						System.out.println("onAnimationRepeat animation ");
                animation.cancel();

            }

            @Override
            public void onAnimationEnd(Animator animation) {

                target.setTranslationY(0);
//						target.clearAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

//						System.out.println("onAnimationCancel animation ");
//						animation.cancel();
            }
        });
    }

   /* public static void animation(final View view, float x1, float x2, float y1, float y2, final int visible1,final boolean isAnimate,long duration){
        final TranslateAnimation anim = new TranslateAnimation(x1, x2,y1, y2);
        anim.setDuration(duration);
        anim.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation arg0){

            }

            @Override
            public void onAnimationRepeat(Animation arg0){}

            @Override
            public void onAnimationEnd(Animation arg0){
                if(isAnimate) {
                    view.setVisibility(visible1);
                }
                view.clearAnimation();
            }
        });
        view.startAnimation(anim);
    }*/
}