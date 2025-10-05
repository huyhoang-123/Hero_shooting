package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Background {

    public Bitmap background;
    private int screenY;

    private float y1 = 0;
    private float y2 = 0;
    private float speed = 200; // pixels/second

    public Background(Resources res, int resId, int screenY){
        this.screenY = screenY;

        Bitmap tmp = BitmapFactory.decodeResource(res, resId);
        // Scale theo chiều cao màn hình (dọc)
        background = Bitmap.createScaledBitmap(tmp, tmp.getWidth(), screenY, true);
        if(tmp != background) tmp.recycle();

        y1 = 0;
        y2 = -screenY; // bản sao phía trên
    }

    public void update(float deltaTime){
        y1 += speed * deltaTime;
        y2 += speed * deltaTime;

        if(y1 >= screenY) y1 = y2 - screenY;
        if(y2 >= screenY) y2 = y1 - screenY;
    }

    public float getY1(){ return y1; }
    public float getY2(){ return y2; }
}
