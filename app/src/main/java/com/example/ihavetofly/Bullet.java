package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;

public class Bullet {

    public int x, y;
    public int width, height;
    public int speed = 800;
    public boolean isKunai = false;

    private static Bitmap bulletBitmap;
    private static Bitmap kunaiBitmap;
    private static int referenceCount = 0;

    private final Rect collisionRect = new Rect();

    public Bullet(Resources res, int startX, int startY, int flightWidth, boolean useKunai) {
        this.isKunai = useKunai;

        if (bulletBitmap == null || kunaiBitmap == null) {
            initBulletBitmaps(res);
        }
        referenceCount++;

        Bitmap sourceBitmap = useKunai ? kunaiBitmap : bulletBitmap;
        if (sourceBitmap != null) {
            width = sourceBitmap.getWidth();
            height = sourceBitmap.getHeight();
        } else {
            width = 20;
            height = 20;
        }

        x = startX - width / 2;
        y = startY;
    }

    private static void initBulletBitmaps(Resources res) {
        if (bulletBitmap != null && kunaiBitmap != null) return;

        Bitmap bulletTmp = BitmapCache.get(res, R.drawable.bullet, 1);
        Bitmap kunaiTmp = BitmapCache.get(res, R.drawable.kunai, 1);

        // Scale the bitmaps to a reasonable size for bullets
        int bulletSize = 20; // Base bullet size

        if (bulletTmp != null) {
            int bulletW = bulletSize;
            int bulletH = bulletTmp.getHeight() * bulletW / bulletTmp.getWidth();
            bulletBitmap = Bitmap.createScaledBitmap(bulletTmp, bulletW, bulletH, true);
        }
        if (kunaiTmp != null) {
            int kunaiW = bulletSize;
            int kunaiH = kunaiTmp.getHeight() * kunaiW / kunaiTmp.getWidth();
            kunaiBitmap = Bitmap.createScaledBitmap(kunaiTmp, kunaiW, kunaiH, true);
        }
    }

    public Bitmap getBullet() {
        return isKunai ? kunaiBitmap : bulletBitmap;
    }

    public Rect getCollisionShape() {
        collisionRect.set(x, y, x + width, y + height);
        return collisionRect;
    }

    public static void clearCache() {
        referenceCount--;
        if (referenceCount <= 0) {
            referenceCount = 0;
            if (bulletBitmap != null && !bulletBitmap.isRecycled()) {
                bulletBitmap.recycle();
                bulletBitmap = null;
            }
            if (kunaiBitmap != null && !kunaiBitmap.isRecycled()) {
                kunaiBitmap.recycle();
                kunaiBitmap = null;
            }
        }
    }
}