package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public class Bird {

    public int x, y, width, height, speed;
    public boolean wasShot = false;

    private static Bitmap[] birdFrames;
    private static int referenceCount = 0;
    private int frameIndex = 0;

    private final Rect collisionRect = new Rect();

    public Bird(Resources res){
        if (birdFrames == null) {
            initBirdFrames(res);
        }
        referenceCount++;

        if (birdFrames != null && birdFrames.length > 0 && birdFrames[0] != null) {
            width = birdFrames[0].getWidth();
            height = birdFrames[0].getHeight();
        } else {
            width = 1;
            height = 1;
        }
    }

    private synchronized static void initBirdFrames(Resources res) {
        if (birdFrames != null) return;

        int[] BIRD_RES = {R.drawable.bird1, R.drawable.bird2, R.drawable.bird3};
        birdFrames = new Bitmap[BIRD_RES.length];

        for (int i = 0; i < BIRD_RES.length; i++) {
            Bitmap tmp = BitmapCache.get(res, BIRD_RES[i], 1);

            if (tmp == null) {
                tmp = BitmapFactory.decodeResource(res, BIRD_RES[i]);
            }

            if (tmp == null) {
                tmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            }

            int origW = Math.max(1, tmp.getWidth());
            int origH = Math.max(1, tmp.getHeight());

            int w = origW / 15;
            if (w <= 0) w = Math.max(1, origW / 3);
            int h = (int) ((long) origH * w / origW);
            if (h <= 0) h = 1;

            birdFrames[i] = Bitmap.createScaledBitmap(tmp, w, h, true);
        }
    }

    public void updateFrame() {
        if (birdFrames == null || birdFrames.length == 0) return;
        frameIndex = (frameIndex + 1) % birdFrames.length;
    }

    public Bitmap getBird() {
        if (birdFrames == null || birdFrames.length == 0) return null;
        return birdFrames[frameIndex];
    }

    public Rect getCollisionShape() {
        collisionRect.set(x, y, x + width, y + height);
        return collisionRect;
    }

    public static void clearCache() {
        referenceCount--;
        if (referenceCount <= 0) {
            referenceCount = 0;
            if (birdFrames != null) {
                for (Bitmap bmp : birdFrames) {
                    if (bmp != null && !bmp.isRecycled()) {
                        bmp.recycle();
                    }
                }
                birdFrames = null;
            }
        }
    }
}