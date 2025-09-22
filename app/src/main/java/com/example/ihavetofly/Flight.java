package com.example.ihavetofly;

import static com.example.ihavetofly.GameView.screenRatioX;
import static com.example.ihavetofly.GameView.screenRatioY;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public class Flight {
    public int x, y, width, height;
    private GameView gameView;
    private Bitmap flightBitmap;
    private Bitmap deadBitmap;

    public boolean movingLeft = false;
    public boolean movingRight = false;
    public int toShoot = 0;
    public int speed;

    Flight(GameView gameView, Resources res, int screenX, int screenY) {
        this.gameView = gameView;

        Bitmap temp = BitmapFactory.decodeResource(res, R.drawable.space_ships);
        float scaleFactor = 0.35f;
        float ratio = (float) temp.getHeight() / temp.getWidth();

        height = (int)(temp.getHeight() * screenRatioY * scaleFactor);
        width = (int)(height / ratio);

        flightBitmap = Bitmap.createScaledBitmap(temp, width, height, false);
        deadBitmap = flightBitmap;

        x = screenX / 2 - width / 2;
        y = screenY - height - 50;

        speed = Math.max(2, (int)(10 * screenRatioX));
    }

    public void updatePosition(int bgWidth) {
        if (movingLeft) {
            x -= speed;
            if (x < 0) x = 0;
        }
        if (movingRight) {
            x += speed;
            if (x + width > bgWidth) x = bgWidth - width;
        }
    }

    public Bitmap getFlight() {
        if (toShoot != 0) {
            toShoot--;
            gameView.newBullet();
        }
        return flightBitmap;
    }

    public Rect getCollisionShape() {
        return new Rect(x, y, x + width, y + height);
    }

    public Bitmap getDead() {
        return deadBitmap;
    }
}
