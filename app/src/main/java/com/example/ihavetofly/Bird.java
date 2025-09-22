package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public class Bird {

    public int x, y, width, height, speed;
    public boolean wasShot = false;

    private Bitmap[] birdFrames;  // 3 frame
    private int frameIndex = 0;

    public Bird(Resources res){
        int[] BIRD_RES = {R.drawable.bird1, R.drawable.bird2, R.drawable.bird3};
        birdFrames = new Bitmap[BIRD_RES.length];

        for(int i=0;i<BIRD_RES.length;i++){
            Bitmap tmp = BitmapFactory.decodeResource(res,BIRD_RES[i]);
            width = tmp.getWidth() / 15;   // scale nhỏ
            height = tmp.getHeight() * width / tmp.getWidth();
            birdFrames[i] = Bitmap.createScaledBitmap(tmp,width,height,true);
            tmp.recycle();
        }
    }

    public void updateFrame(){
        frameIndex = (frameIndex + 1) % birdFrames.length;
    }

    public Bitmap getBird(){
        return birdFrames[frameIndex];
    }

    public Rect getCollisionShape(){
        return new Rect(x,y,x+width,y+height);
    }
}
