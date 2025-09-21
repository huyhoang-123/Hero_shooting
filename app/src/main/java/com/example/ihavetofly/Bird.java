package com.example.ihavetofly;

import static com.example.ihavetofly.GameView.screenRatioX;
import static com.example.ihavetofly.GameView.screenRatioY;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public class Bird {

    public int speed;
    public boolean wasShot = true;
    public int x, y;
    private final Bitmap[] frames;
    private int frameIndex = 0;

    public final int size;

    Bird(Resources res) {
        // load và scale bitmap 1 lần
        Bitmap b1 = BitmapFactory.decodeResource(res, R.drawable.bird1);
        Bitmap b2 = BitmapFactory.decodeResource(res, R.drawable.bird2);
        Bitmap b3 = BitmapFactory.decodeResource(res, R.drawable.bird3);

        int width = b1.getWidth() / 12;
        int height = b1.getHeight() / 12;

        width = Math.max(1, (int)(width * screenRatioX));
        height = Math.max(1, (int)(height * screenRatioY));

        size = height;

        b1 = Bitmap.createScaledBitmap(b1, size, size, false);
        b2 = Bitmap.createScaledBitmap(b2, size, size, false);
        b3 = Bitmap.createScaledBitmap(b3, size, size, false);

        frames = new Bitmap[]{b1, b2, b3};

        speed = (int)(7 * screenRatioY);
        y = -size;
        x = 0;
    }

    public Bitmap getBird() {
        Bitmap b = frames[frameIndex];
        frameIndex = (frameIndex + 1) % frames.length;
        return b;
    }

    public Rect getCollisionShape() {
        return new Rect(x, y, x + size, y + size);
    }
}
