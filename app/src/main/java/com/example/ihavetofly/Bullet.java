package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public class Bullet {

    public int x, y;
    public int width, height;
    public int speed = 25;

    public Bitmap bullet;

    public Bullet(Resources res, int startX, int startY, int flightWidth){
        bullet = BitmapFactory.decodeResource(res,R.drawable.bullet);
        // Scale bullet theo flight width (ví dụ 1/4)
        width = flightWidth / 4;
        height = bullet.getHeight() * width / bullet.getWidth();
        bullet = Bitmap.createScaledBitmap(bullet,width,height,true);
        x = startX - width/2;
        y = startY;
    }

    public Rect getCollisionShape(){
        return new Rect(x,y,x+width,y+height);
    }
}
