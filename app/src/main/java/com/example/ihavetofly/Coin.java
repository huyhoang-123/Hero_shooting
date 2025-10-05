package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;

public class Coin {

    public int x, y;
    public int width, height;
    public boolean active = false;

    private static Bitmap coinBitmap;
    private final Rect collisionRect = new Rect();
    private int fallSpeed = 300;

    public Coin(Resources res){
        if (coinBitmap == null) {
            Bitmap bmp = BitmapCache.get(res, R.drawable.coin1, 1);
            int w = bmp.getWidth() / 6;
            int h = bmp.getHeight() * w / bmp.getWidth();
            coinBitmap = Bitmap.createScaledBitmap(bmp, w, h, true);
        }
        width = coinBitmap.getWidth();
        height = coinBitmap.getHeight();
        x = -width;
        y = -height;
    }

    public void spawnAt(int startX, int startY){
        active = true;
        x = startX;
        y = startY;
    }

    public void update(float deltaTime, int screenY){
        if(!active) return;
        y += (int)(fallSpeed * deltaTime);
        if(y > screenY){
            clear();
        }
    }

    public void clear(){
        active = false;
        x = -width;
        y = -height;
    }

    public Rect getCollisionShape(){
        collisionRect.set(x, y, x + width, y + height);
        return collisionRect;
    }

    public Bitmap getBitmap(){ return coinBitmap; }

    public static void clearCache() {
        if (coinBitmap != null && !coinBitmap.isRecycled()) {
            coinBitmap.recycle();
            coinBitmap = null;
        }
    }
}