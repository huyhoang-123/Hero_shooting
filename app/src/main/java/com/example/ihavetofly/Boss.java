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
    public int level = 1;

    private static Bitmap bossBitmap;
    private static Bitmap bossExplodeBitmap;
    private static Bitmap bossLv3Bitmap;
    private static Bitmap bossLv3ExplodeBitmap;
    private static final int MOVE_SPEED = 300;
    private static final int REQUIRED_HITS = 100;
    private static final long EXPLODE_ANIMATION_DURATION = 1000;

    private boolean movingRight = true;
    private final Rect collisionRect = new Rect();
    private int hitCount = 0;
    private final int screenX, screenY;

    public Boss(Resources res, int screenX, int screenY) {
        this.screenX = screenX;
        this.screenY = screenY;

        if (bossBitmap == null) {
            Bitmap bmp = BitmapCache.get(res, R.drawable.boss, 1);
            int w = screenX / 2;
            int h = bmp.getHeight() * w / bmp.getWidth();
            bossBitmap = Bitmap.createScaledBitmap(bmp, w, h, true);
        }

        if (bossExplodeBitmap == null) {
            Bitmap bmp = BitmapCache.get(res, R.drawable.boss_explode_nobg, 1);
            int w = screenX / 2;
            int h = bmp.getHeight() * w / bmp.getWidth();
            bossExplodeBitmap = Bitmap.createScaledBitmap(bmp, w, h, true);
        }

        if (bossLv3Bitmap == null) {
            Bitmap bmp = BitmapCache.get(res, R.drawable.boss_lv3, 1);
            int w = screenX / 2;
            int h = bmp.getHeight() * w / bmp.getWidth();
            bossLv3Bitmap = Bitmap.createScaledBitmap(bmp, w, h, true);
        }

        if (bossLv3ExplodeBitmap == null) {
            Bitmap bmp = BitmapCache.get(res, R.drawable.boss_lv3_explode, 1);
            int w = screenX / 2;
            int h = bmp.getHeight() * w / bmp.getWidth();
            bossLv3ExplodeBitmap = Bitmap.createScaledBitmap(bmp, w, h, true);
        }

        width = bossBitmap.getWidth();
        height = bossBitmap.getHeight();
        y = screenY / 6;
        x = -width;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void spawn() {
        active = true;
        isExploding = false;
        spawnTime = System.currentTimeMillis();
        x = 0;
        movingRight = true;
        hitCount = 0;
    }

    public void update(float deltaTime, int screenX) {
        if (!active) return;

        long currentTime = System.currentTimeMillis();

        if (isExploding) {
            if (currentTime - explodeTime >= EXPLODE_ANIMATION_DURATION) {
                clear();
            }
            return;
        }

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

    public void hit() {
        if (!isExploding) {
            hitCount++;
            if (hitCount >= REQUIRED_HITS) {
                startExplode();
            }
        }
    }

    public int getHitCount() {
        return hitCount;
    }

    public int getRemainingHits() {
        return Math.max(0, REQUIRED_HITS - hitCount);
    }

    public void startExplode() {
        isExploding = true;
        explodeTime = System.currentTimeMillis();
    }

    public void clear() {
        active = false;
        isExploding = false;
        x = -width;
        hitCount = 0;
    }

    public Rect getCollisionShape() {
        collisionRect.set(x, y, x + width, y + height);
        return collisionRect;
    }

    public Bitmap getBitmap() {
        if (isExploding) {
            return (level == 3) ? bossLv3ExplodeBitmap : bossExplodeBitmap;
        }
        return (level == 3) ? bossLv3Bitmap : bossBitmap;
    }

    public boolean shouldShootRocket(long currentTime, long lastRocketTime) {
        return !isExploding && (currentTime - lastRocketTime >= 2000);
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
        if (bossLv3Bitmap != null && !bossLv3Bitmap.isRecycled()) {
            bossLv3Bitmap.recycle();
            bossLv3Bitmap = null;
        }
        if (bossLv3ExplodeBitmap != null && !bossLv3ExplodeBitmap.isRecycled()) {
            bossLv3ExplodeBitmap.recycle();
            bossLv3ExplodeBitmap = null;
        }
    }
}