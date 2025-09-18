package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Background {

    public int x, y;
    public Bitmap background;

    public Background(int screenX, int screenY, Resources res, int drawableId) {
        // Decode và resize full màn hình
        Bitmap temp = BitmapFactory.decodeResource(res, drawableId);
        background = Bitmap.createScaledBitmap(temp, screenX, screenY, false);

        x = 0;
        y = 0;
    }
}
