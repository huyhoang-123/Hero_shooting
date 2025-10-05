package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;

public class Flight {

    public int x, y;
    public int width, height;
    public boolean movingLeft = false, movingRight = false, movingUp = false, movingDown = false;

    private Bitmap flightBitmap;
    private final float speed = 600;
    private int screenX, screenY;

    private int coinScore = 0;

    // Power-up states
    private boolean hasDoubleBullet = false;
    private boolean hasKunai = false;
    private boolean hasShield = false;
    private long doubleBulletEndTime = 0;
    private long kunaiEndTime = 0;
    private long shieldEndTime = 0;

    public Flight(int screenX, int screenY, Resources res) {
        this.screenX = screenX;
        this.screenY = screenY;

        Bitmap tmp = BitmapCache.get(res, R.drawable.space_ships, 1);
        width = screenX / 8;
        height = tmp.getHeight() * width / tmp.getWidth();
        flightBitmap = Bitmap.createScaledBitmap(tmp, width, height, true);

        x = screenX / 2 - width / 2;
        y = screenY - height - 50;
    }

    public void updatePosition(float deltaTime) {
        float dx = 0, dy = 0;
        if (movingLeft) dx -= speed * deltaTime;
        if (movingRight) dx += speed * deltaTime;
        if (movingUp) dy -= speed * deltaTime;
        if (movingDown) dy += speed * deltaTime;

        x += (int)dx;
        y += (int)dy;

        if (x < 0) x = 0;
        if (x + width > screenX) x = screenX - width;
        if (y < 0) y = 0;
        if (y + height > screenY) y = screenY - height;

        // Update power-up states
        long currentTime = System.currentTimeMillis();
        if (hasDoubleBullet && currentTime > doubleBulletEndTime) {
            hasDoubleBullet = false;
        }
        if (hasKunai && currentTime > kunaiEndTime) {
            hasKunai = false;
        }
        if (hasShield && currentTime > shieldEndTime) {
            hasShield = false;
        }
    }

    public Bitmap getFlight() {
        return flightBitmap;
    }

    public void collectCoin() {
        coinScore++;
    }

    public int getCoinScore() {
        return coinScore;
    }

    public void resetCoinScore() {
        coinScore = 0;
    }

    // Power-up methods
    public void activateDoubleBullet() {
        hasDoubleBullet = true;
        doubleBulletEndTime = System.currentTimeMillis() + 5000;
    }

    public void activateKunai() {
        hasKunai = true;
        kunaiEndTime = System.currentTimeMillis() + 5000;
    }

    public void activateShield() {
        hasShield = true;
        shieldEndTime = System.currentTimeMillis() + 5000;
    }

    public boolean hasDoubleBullet() {
        return hasDoubleBullet;
    }

    public boolean hasKunai() {
        return hasKunai;
    }

    public boolean hasShield() {
        return hasShield;
    }

    public void resetPowerUps() {
        hasDoubleBullet = false;
        hasKunai = false;
        hasShield = false;
        doubleBulletEndTime = 0;
        kunaiEndTime = 0;
        shieldEndTime = 0;
    }
}