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
    private Random random;

    private final Rect collisionRect = new Rect();

    public Bomb(Resources res, int screenX, int screenY){
        if (bombBitmap == null) {
            Bitmap original = BitmapCache.get(res, R.drawable.bomb_4, 1);
            int originalWidth = original.getWidth();
            int originalHeight = original.getHeight();

            int w = originalWidth / 8;
            int h = originalHeight * w / originalWidth;
            bombBitmap = Bitmap.createScaledBitmap(original, w, h, true);
        }

        width = bombBitmap.getWidth();
        height = bombBitmap.getHeight();
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
        return bombBitmap;
    }

    public static void clearCache() {
        if (bombBitmap != null && !bombBitmap.isRecycled()) {
            bombBitmap.recycle();
            bombBitmap = null;
        }
    }
}