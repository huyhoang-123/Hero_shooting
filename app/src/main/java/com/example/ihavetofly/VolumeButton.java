package com.example.ihavetofly;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

public class VolumeButton {

    private Bitmap musicOn, musicOff, sfxOn, sfxOff;
    private Bitmap currentMusic, currentSfx;

    private Rect musicRect, sfxRect;

    private boolean musicMuted=false, sfxMuted=false;

    public VolumeButton(Context ctx, int screenX, int screenY){
        int btnSize = screenY/12;

        musicRect = new Rect(0, screenY/2-btnSize/2, btnSize, screenY/2+btnSize/2);
        sfxRect = new Rect(screenX-btnSize, screenY/2-btnSize/2, screenX, screenY/2+btnSize/2);

        musicOn = getBitmap(ctx,R.drawable.baseline_volume_up_24,btnSize,btnSize);
        musicOff = getBitmap(ctx,R.drawable.mute,btnSize,btnSize);
        sfxOn = getBitmap(ctx,R.drawable.speaker_high_volume,btnSize,btnSize);
        sfxOff = getBitmap(ctx,R.drawable.volume_speaker,btnSize,btnSize);

        currentMusic = musicOn;
        currentSfx = sfxOn;
    }

    private Bitmap getBitmap(Context ctx,int id,int w,int h){
        Drawable d = ContextCompat.getDrawable(ctx,id);
        if(d==null) return null;
        Bitmap bmp = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0,0,w,h);
        d.draw(c);
        return bmp;
    }

    public void draw(Canvas canvas){
        if(canvas==null) return;
        if(currentMusic!=null) canvas.drawBitmap(currentMusic,null,musicRect,null);
        if(currentSfx!=null) canvas.drawBitmap(currentSfx,null,sfxRect,null);
    }

    public boolean handleTouch(float x,float y){
        boolean changed=false;
        if(musicRect.contains((int)x,(int)y)){
            musicMuted=!musicMuted;
            currentMusic = musicMuted?musicOff:musicOn;
            changed=true;
        }
        if(sfxRect.contains((int)x,(int)y)){
            sfxMuted=!sfxMuted;
            currentSfx = sfxMuted?sfxOff:sfxOn;
            changed=true;
        }
        return changed;
    }

    public boolean isMusicMuted(){return musicMuted;}
    public boolean isSfxMuted(){return sfxMuted;}
}
