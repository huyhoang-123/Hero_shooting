package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Background {
    public int x, y, speed;
    public Bitmap background;

    public Background(int screenX, int screenY, Resources res, int drawableId) {
        Bitmap temp = BitmapFactory.decodeResource(res, drawableId);

        // Scale theo chiều cao để full màn, giữ nguyên tỉ lệ
        float ratio = (float) screenY / temp.getHeight();
        int newWidth = (int) (temp.getWidth() * ratio);

        background = Bitmap.createScaledBitmap(temp, newWidth, screenY, false);

        x = 0;
        y = 0;
        speed = 10; // tốc độ cuộn ngang
    }
}
