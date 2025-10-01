package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;

public class Bullet {

    public int x, y;
    public int width, height;
    public int speed = 800; // pixels per second (đã đổi thành tốc độ theo giây)

    public Bitmap bullet;

    // reusable collision rect
    private final Rect collisionRect = new Rect();

    public Bullet(Resources res, int startX, int startY, int flightWidth){
        Bitmap bmp = BitmapCache.get(res,R.drawable.bullet,1);
        // Scale bullet theo flight width (ví dụ 1/4)
        width = flightWidth / 4;
        height = bmp.getHeight() * width / bmp.getWidth();
        bullet = Bitmap.createScaledBitmap(bmp,width,height,true);
        x = startX - width/2;
        y = startY;
    }

    public Rect getCollisionShape(){
        collisionRect.set(x, y, x + width, y + height);
        return collisionRect;
    }
}
