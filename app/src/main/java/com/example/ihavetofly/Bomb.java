package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;

import java.util.Random;

public class Bomb {

    public int x, y;
    public int width, height;
    public boolean active = false;        // Bomb đang hoạt động
    public long spawnTime;                 // Thời gian spawn
    public boolean isRespawnBomb = false; // Bomb mới spawn có thể clear timeout

    private Bitmap bombBitmap;
    private Random random;

    // reusable collision rect
    private final Rect collisionRect = new Rect();

    public Bomb(Resources res, int screenX, int screenY){
        Bitmap bmp = BitmapCache.get(res, R.drawable.bomb_4, 1);

        width = bmp.getWidth() / 8; // scale nhỏ
        height = bmp.getHeight() * width / bmp.getWidth();
        bombBitmap = Bitmap.createScaledBitmap(bmp, width, height, true);

        random = new Random();
        x = -width;
        y = -height;
    }

    public void spawn(int screenX, boolean respawnBomb){
        active = true;
        y = -height;  // khởi tạo trên màn hình
        x = random.nextInt(Math.max(1, screenX - width));
        spawnTime = System.currentTimeMillis();
        isRespawnBomb = respawnBomb;
    }

    /**
     * deltaTime in seconds, speed in pixels/second
     */
    public void update(float deltaTime, int speed){
        if(!active) return;
        y += (int)(speed * deltaTime);
    }

    public void clear(){
        active = false;
        x = -width;
        y = -height;
        isRespawnBomb = false;
    }

    public Rect getCollisionShape(){
        collisionRect.set(x, y, x + width, y + height);
        return collisionRect;
    }

    public Bitmap getBitmap(){
        return bombBitmap;
    }
}
