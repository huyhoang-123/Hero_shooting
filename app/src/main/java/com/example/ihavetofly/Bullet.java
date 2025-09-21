package com.example.ihavetofly;

import static com.example.ihavetofly.GameView.screenRatioX;
import static com.example.ihavetofly.GameView.screenRatioY;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public class Bullet {
    int x, y, speed, width, height;
    Bitmap bullet;

    Bullet(Resources res, int screenX, int screenY) {
        // load bitmap gốc
        bullet = BitmapFactory.decodeResource(res, R.drawable.bullet);

        // đặt kích thước phù hợp với màn hình
        width = screenX / 15;  // giảm nhỏ hơn một chút
        height = screenY / 15;

        // scale theo tỉ lệ màn hình
        width = (int) (width * screenRatioX);
        height = (int) (height * screenRatioY);

        // resize bitmap
        bullet = Bitmap.createScaledBitmap(bullet, width, height, false);

        // speed cũng scale theo màn hình
        speed = (int) (50 * screenRatioY);
    }

    Rect getCollisionShape() {
        return new Rect(x, y, x + width, y + height);
    }
}
