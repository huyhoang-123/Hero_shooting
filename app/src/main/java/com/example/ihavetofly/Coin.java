package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;

public class Coin {

    public int x, y;
    public int width, height;
    public boolean active = false;

    private Bitmap coinBitmap;
    private final Rect collisionRect = new Rect();
    private int fallSpeed = 300; // tốc độ rơi px/s

    public Coin(Resources res){
        Bitmap bmp = BitmapCache.get(res, R.drawable.coin1, 1);
        width = bmp.getWidth() / 6;  // coin nhỏ hơn
        height = bmp.getHeight() * width / bmp.getWidth();
        coinBitmap = Bitmap.createScaledBitmap(bmp, width, height, true);
        x = -width;
        y = -height;
    }

    // coin rơi xuống tại vị trí bird bị bắn
    public void spawnAt(int startX, int startY){
        active = true;
        x = startX;
        y = startY;
    }

    public void update(float deltaTime, int screenY){
        if(!active) return;
        y += (int)(fallSpeed * deltaTime); // ← Fixed: use fallSpeed, not the parameter
        if(y > screenY){ // nếu rơi khỏi màn hình thì ẩn đi
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
}