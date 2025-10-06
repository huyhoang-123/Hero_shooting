package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;

import java.util.Random;

public class Bomb {

    public int x, y;
    public int width, height;
    public boolean active = false;
    public long spawnTime;
    public boolean isRespawnBomb = false;

    private static Bitmap bombBitmap;
    private static final Object bitmapLock = new Object();
    private Random random;

    private final Rect collisionRect = new Rect();

    public Bomb(Resources res, int screenX, int screenY){
        synchronized (bitmapLock) {
            if (bombBitmap == null || bombBitmap.isRecycled()) {
                Bitmap original = BitmapCache.get(res, R.drawable.bomb_4, 1);
                if (original != null) {
                    int originalWidth = original.getWidth();
                    int originalHeight = original.getHeight();

                    int w = originalWidth / 8;
                    int h = originalHeight * w / originalWidth;
                    bombBitmap = Bitmap.createScaledBitmap(original, w, h, true);
                }
            }

            if (bombBitmap != null && !bombBitmap.isRecycled()) {
                width = bombBitmap.getWidth();
                height = bombBitmap.getHeight();
            } else {
                width = 50;
                height = 50;
            }
        }

        random = new Random();
        x = -width;
        y = -height;
    }

    public void spawn(int screenX, boolean respawnBomb){
        active = true;
        y = -height;
        x = random.nextInt(Math.max(1, screenX - width));
        spawnTime = System.currentTimeMillis();
        isRespawnBomb = respawnBomb;
    }

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
        synchronized (bitmapLock) {
            return bombBitmap;
        }
    }

    public static void clearCache() {
        synchronized (bitmapLock) {
            if (bombBitmap != null && !bombBitmap.isRecycled()) {
                bombBitmap.recycle();
                bombBitmap = null;
            }
        }
    }
}