package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;

public class Boss {

    public int x, y;
    public int width, height;
    public boolean active = false;
    public boolean isExploding = false;
    public long spawnTime;
    public long explodeTime;

    private static Bitmap bossBitmap;
    private static Bitmap bossExplodeBitmap;
    private static final int MOVE_SPEED = 300;
    private static final long SURVIVE_DURATION = 10000; // 10 seconds
    private static final long EXPLODE_ANIMATION_DURATION = 1000; // 1 second

    private boolean movingRight = true;
    private final Rect collisionRect = new Rect();

    public Boss(Resources res, int screenX, int screenY) {
        if (bossBitmap == null) {
            Bitmap bmp = BitmapCache.get(res, R.drawable.boss, 1);
            int w = screenX / 2; // Boss size
            int h = bmp.getHeight() * w / bmp.getWidth();
            bossBitmap = Bitmap.createScaledBitmap(bmp, w, h, true);
        }

        if (bossExplodeBitmap == null) {
            Bitmap bmp = BitmapCache.get(res, R.drawable.boss_explode_nobg, 1);
            int w = screenX / 2;
            int h = bmp.getHeight() * w / bmp.getWidth();
            bossExplodeBitmap = Bitmap.createScaledBitmap(bmp, w, h, true);
        }

        width = bossBitmap.getWidth();
        height = bossBitmap.getHeight();
        y = screenY / 6; // 2/3 from top means 1/3 down
        x = -width;
    }

    public void spawn() {
        active = true;
        isExploding = false;
        spawnTime = System.currentTimeMillis();
        x = 0; // Start from left edge
        movingRight = true;
    }

    public void update(float deltaTime, int screenX) {
        if (!active) return;

        long currentTime = System.currentTimeMillis();
        long aliveTime = currentTime - spawnTime;

        // Check if boss should explode after 10 seconds
        if (!isExploding && aliveTime >= SURVIVE_DURATION) {
            startExplode();
            return;
        }

        // Handle explode animation
        if (isExploding) {
            if (currentTime - explodeTime >= EXPLODE_ANIMATION_DURATION) {
                clear();
            }
            return;
        }

        // Move boss left and right
        if (movingRight) {
            x += (int) (MOVE_SPEED * deltaTime);
            if (x + width >= screenX) {
                x = screenX - width;
                movingRight = false;
            }
        } else {
            x -= (int) (MOVE_SPEED * deltaTime);
            if (x <= 0) {
                x = 0;
                movingRight = true;
            }
        }
    }

    public void startExplode() {
        isExploding = true;
        explodeTime = System.currentTimeMillis();
    }

    public void clear() {
        active = false;
        isExploding = false;
        x = -width;
    }

    public Rect getCollisionShape() {
        collisionRect.set(x, y, x + width, y + height);
        return collisionRect;
    }

    public Bitmap getBitmap() {
        return isExploding ? bossExplodeBitmap : bossBitmap;
    }

    public boolean shouldShootRocket(long currentTime, long lastRocketTime) {
        return !isExploding && (currentTime - lastRocketTime >= 2000); // Shoot every 2 seconds
    }

    public static void clearCache() {
        if (bossBitmap != null && !bossBitmap.isRecycled()) {
            bossBitmap.recycle();
            bossBitmap = null;
        }
        if (bossExplodeBitmap != null && !bossExplodeBitmap.isRecycled()) {
            bossExplodeBitmap.recycle();
            bossExplodeBitmap = null;
        }
    }
}