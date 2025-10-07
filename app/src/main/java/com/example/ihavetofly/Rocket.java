package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;

public class Rocket {

    public int x, y;
    public int width, height;
    public boolean active = false;

    private static Bitmap rocketBitmap;
    private final Rect collisionRect = new Rect();
    private static final int FALL_SPEED = 400;

    public Rocket(Resources res) {
        ensureBitmap(res);

        width = rocketBitmap.getWidth();
        height = rocketBitmap.getHeight();
        x = -width;
        y = -height;
    }

    public void spawn(int startX, int startY) {
        active = true;
        x = startX;
        y = startY;
    }

    public void update(float deltaTime, int screenY) {
        if (!active) return;
        y += (int) (FALL_SPEED * deltaTime);
        if (y > screenY) {
            clear();
        }
    }

    public void clear() {
        active = false;
        x = -width;
        y = -height;
    }

    public Rect getCollisionShape() {
        collisionRect.set(x, y, x + width, y + height);
        return collisionRect;
    }

    public Bitmap getBitmap() {
        return rocketBitmap;
    }

    public static void clearCache() {
        if (rocketBitmap != null && !rocketBitmap.isRecycled()) {
            rocketBitmap.recycle();
            rocketBitmap = null;
        }
    }

    public static synchronized void ensureBitmap(Resources res) {
        if (rocketBitmap == null || rocketBitmap.isRecycled()) {
            Bitmap bmp = BitmapCache.get(res, R.drawable.rocket, 1);
            if (bmp != null) {
                int w = bmp.getWidth() / 6;
                int h = bmp.getHeight() * w / bmp.getWidth();
                rocketBitmap = Bitmap.createScaledBitmap(bmp, w, h, true);
            }
        }
    }
}