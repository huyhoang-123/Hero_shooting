package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;

public class Bullet {

    public int x, y;
    public int width, height;
    public int speed = 800;
    public boolean isKunai = false;

    public Bitmap bullet;

    private final Rect collisionRect = new Rect();

    public Bullet(Resources res, int startX, int startY, int flightWidth, boolean useKunai) {
        this.isKunai = useKunai;
        int resId = useKunai ? R.drawable.kunai : R.drawable.bullet;

        Bitmap bmp = BitmapCache.get(res, resId, 1);
        width = flightWidth / 4;
        height = bmp.getHeight() * width / bmp.getWidth();
        bullet = Bitmap.createScaledBitmap(bmp, width, height, true);
        x = startX - width / 2;
        y = startY;
    }

    public Rect getCollisionShape() {
        collisionRect.set(x, y, x + width, y + height);
        return collisionRect;
    }
}