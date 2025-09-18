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
    int x, y, birdCounter = 1;
    Bitmap bird1, bird2, bird3;

    public final int size;


    Bird(Resources res) {
        bird1 = BitmapFactory.decodeResource(res, R.drawable.bird1);
        bird2 = BitmapFactory.decodeResource(res, R.drawable.bird2);
        bird3 = BitmapFactory.decodeResource(res, R.drawable.bird3);

        // Giảm kích thước bird nhiều hơn
        int width = bird1.getWidth() / 20;
        int height = bird1.getHeight() / 20;

        width = Math.max(1, (int) (width * GameView.screenRatioX));
        height = Math.max(1, (int) (height * GameView.screenRatioY));

        // Đặt size = height để làm chim vuông (width = height)
        size = height;

        bird1 = Bitmap.createScaledBitmap(bird1, size, size, false);
        bird2 = Bitmap.createScaledBitmap(bird2, size, size, false);
        bird3 = Bitmap.createScaledBitmap(bird3, size, size, false);

        speed = (int)(7 * GameView.screenRatioY);
        y = -size;
        x = 0;
    }


    Bitmap getBird() {
        switch (birdCounter) {
            case 1: birdCounter++; return bird1;
            case 2: birdCounter++; return bird2;
            case 3: birdCounter = 1; return bird3;
            default: birdCounter = 1; return bird1;
        }
    }

    Rect getCollisionShape() {
        return new Rect(x, y, x + size, y + size);
    }
}
