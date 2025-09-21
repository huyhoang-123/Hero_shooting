package com.example.ihavetofly;

import static com.example.ihavetofly.GameView.screenRatioX;
import static com.example.ihavetofly.GameView.screenRatioY;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public class Flight {
    public boolean isGoingUp = false;
    public int toShoot = 0;
    public int x, y, width, height;
    private GameView gameView;
    private Bitmap flightBitmap;
    private Bitmap deadBitmap;

    public boolean movingLeft = false;
    public boolean movingRight = false;

    public int speed;

    Flight(GameView gameView, int screenX, int screenY, Resources res) {
        this.gameView = gameView;

        Bitmap temp = BitmapFactory.decodeResource(res, R.drawable.space_ships);

        float scaleFactor = 0.35f;
        float ratio = (float) temp.getHeight() / temp.getWidth();

        height = (int)(temp.getHeight() * screenRatioY * scaleFactor);
        width = (int)(height / ratio);

        flightBitmap = Bitmap.createScaledBitmap(temp, width, height, false);
        deadBitmap = flightBitmap;

        // vị trí xuất phát
        x = screenX / 2 - width / 2;
        y = screenY - height - 50;

        speed = Math.max(2, (int)(10 * screenRatioX)); // giảm tốc độ di chuyển
    }

    public void updatePosition() {
        if (movingLeft) {
            x -= speed;
            // Không có giới hạn vì background sẽ di chuyển theo
        }

        if (movingRight) {
            x += speed;
            // Không có giới hạn vì background sẽ di chuyển theo
        }
    }

    public Bitmap getFlight() {
        if(toShoot != 0) {
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