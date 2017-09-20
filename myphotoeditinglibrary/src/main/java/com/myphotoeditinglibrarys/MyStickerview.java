package com.myphotoeditinglibrarys;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;
/**
 * Created by Ghanshyam on 7/24/2017.
 */
public class MyStickerview extends android.support.v7.widget.RecyclerView{

     private final int[] imgid = {
            R.drawable.abra,
            R.drawable.bellsprout,
            R.drawable.bracelet,
            R.drawable.bullbasaur,
            R.drawable.camera,
            R.drawable.candy,
            R.drawable.caterpie,
            R.drawable.charmander,
            R.drawable.mankey,
            R.drawable.map,
            R.drawable.mega_ball,
            R.drawable.meowth,
            R.drawable.pawprints,
            R.drawable.pidgey,
            R.drawable.pikachu,
            R.drawable.pikachu_1,
            R.drawable.pikachu_2,
            R.drawable.player,
            R.drawable.pointer,
            R.drawable.pokebag,
            R.drawable.pokeball,
            R.drawable.pokeballs,
            R.drawable.pokecoin,
            R.drawable.pokedex,
            R.drawable.potion,
            R.drawable.psyduck,
            R.drawable.rattata,
            R.drawable.revive,
            R.drawable.squirtle,
            R.drawable.star,
            R.drawable.star_1,
            R.drawable.superball,
            R.drawable.tornado,
            R.drawable.venonat,
            R.drawable.weedle,
            R.drawable.zubat
    };

    public MyStickerview(Context context) {
        super(context);
        initview(context);
    }

    public MyStickerview(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initview(context);
    }

    public MyStickerview(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initview(context);
    }

    public void initview(Context context) {
        if (getChildCount() > 0) {
            removeAllViews();
        }
        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        List<Integer> stickers = new ArrayList<>(imgid.length);
        for (Integer id : imgid) {
            stickers.add(id);
        }
        this.setAdapter(new Adapter(stickers,context));
        setBackgroundColor(getResources().getColor(android.R.color.white));
//        if(isFirsttime) {
//            isFirsttime  = false;
//            Util.animation(this, 0, 0, getHeight(), 0, View.VISIBLE, true, 0);
//            Util.animation(this, 0, 0, 0, getHeight(), View.GONE, true, 0);
//        }

    }

    public interface SelectedListener{
        public void onSelectSticker(int stickerId);
    }

    SelectedListener selectedListener;
    public void setSelectedListener(SelectedListener listener){
        this.selectedListener = listener;
    }

    class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        private final List<Integer> idList;
        private final Context context;
        private final LayoutInflater layoutInflater;

        Adapter(@NonNull List<Integer> idslist, @NonNull Context context) {
            this.idList = idslist;
            this.context = context;
            this.layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            ImageView imgview = new ImageView(context);
            ViewGroup.LayoutParams lp = new LayoutParams(80,80);
            imgview.setLayoutParams(lp);
            ViewGroup.MarginLayoutParams lpt =(MarginLayoutParams)imgview.getLayoutParams();
            lpt.setMargins(10,10,10,10);
            imgview.setLayoutParams(lpt);
            return new ViewHolder(imgview);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.image.setImageDrawable(ContextCompat.getDrawable(context, getItem(position)));
        }

        @Override
        public int getItemCount() {
            return idList.size();
        }

        private int getItem(int position) {
            return idList.get(position);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView image;
            ViewHolder(View itemView) {
                super(itemView);
                image = (ImageView)itemView;
                image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int pos = getAdapterPosition();
                        if (pos >= 0 && selectedListener != null) {
                            selectedListener.onSelectSticker(getItem(pos));
                        }
                    }
                });
            }
        }
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

}
