package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public class Bird {

    public int x, y, width, height, speed;
    public boolean wasShot = false;

    private static Bitmap[] birdFrames;
    private int frameIndex = 0;

    private final Rect collisionRect = new Rect();

    public Bird(Resources res){
        if (birdFrames == null) {
            int[] BIRD_RES = {R.drawable.bird1, R.drawable.bird2, R.drawable.bird3};
            birdFrames = new Bitmap[BIRD_RES.length];

            for (int i = 0; i < BIRD_RES.length; i++) {
                Bitmap tmp = BitmapCache.get(res, BIRD_RES[i], 1);

                // Fallback: if cache returned null, try decodeResource directly
                if (tmp == null) {
                    tmp = BitmapFactory.decodeResource(res, BIRD_RES[i]);
                }

                // If still null (corrupt resource), create a tiny placeholder to avoid crashes
                if (tmp == null) {
                    tmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                }

                // Compute target width/height safely (never zero)
                int origW = Math.max(1, tmp.getWidth());
                int origH = Math.max(1, tmp.getHeight());

                int w = origW / 15;
                if (w <= 0) w = Math.max(1, origW / 3); // reasonable fallback if original is small
                int h = (int) ((long) origH * w / origW);
                if (h <= 0) h = 1;

                // Create scaled bitmap
                Bitmap scaled = Bitmap.createScaledBitmap(tmp, w, h, true);

                // Don't recycle tmp if it's from cache (BitmapCache manages it). If tmp != scaled and
                // tmp wasn't from cache, it's safe to recycle; but to keep logic simple and safe,
                // avoid recycling here to prevent double-recycle issues.
                birdFrames[i] = scaled;
            }
        }

        // Ensure we have at least one valid frame
        if (birdFrames != null && birdFrames.length > 0 && birdFrames[0] != null) {
            width = birdFrames[0].getWidth();
            height = birdFrames[0].getHeight();
        } else {
            // Safeguard defaults if something still went wrong
            width = 1;
            height = 1;
            if (birdFrames == null || birdFrames.length == 0) {
                birdFrames = new Bitmap[]{ Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) };
            } else if (birdFrames[0] == null) {
                birdFrames[0] = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }
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
