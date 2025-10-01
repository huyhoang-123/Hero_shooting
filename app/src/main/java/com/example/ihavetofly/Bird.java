package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;

public class Bird {

    public int x, y, width, height, speed;
    public boolean wasShot = false;

    private static Bitmap[] birdFrames;  // dùng chung cho tất cả bird
    private int frameIndex = 0;

    private final Rect collisionRect = new Rect();

    public Bird(Resources res){
        if (birdFrames == null) {
            int[] BIRD_RES = {R.drawable.bird1, R.drawable.bird2, R.drawable.bird3};
            birdFrames = new Bitmap[BIRD_RES.length];

            for (int i = 0; i < BIRD_RES.length; i++) {
                Bitmap tmp = BitmapCache.get(res, BIRD_RES[i], 1);
                int w = tmp.getWidth() / 15;
                int h = tmp.getHeight() * w / tmp.getWidth();
                birdFrames[i] = Bitmap.createScaledBitmap(tmp, w, h, true);
            }
        }
        width = birdFrames[0].getWidth();
        height = birdFrames[0].getHeight();
    }

    public void updateFrame() {
        frameIndex = (frameIndex + 1) % birdFrames.length;
    }

    public Bitmap getBird() {
        return birdFrames[frameIndex];
    }

    public Rect getCollisionShape() {
        collisionRect.set(x, y, x + width, y + height);
        return collisionRect;
    }
}
