package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

public class PowerUp {

    public static final int TYPE_DOUBLE_BULLET = 1;
    public static final int TYPE_KUNAI = 2;
    public static final int TYPE_SHIELD = 3;

    public int x, y;
    public int width, height;
    public boolean active = false;
    public int type;

    private static Bitmap doubleBulletBitmap;
    private static Bitmap kunaiBitmap;
    private static Bitmap shieldBitmap;

    private Bitmap powerUpBitmap;
    private final Rect collisionRect = new Rect();
    private final int fallSpeed = 250;

    private long spawnTime;
    private Paint effectPaint;

    public PowerUp(Resources res, int type) {
        this.type = type;

        if (type == TYPE_DOUBLE_BULLET && doubleBulletBitmap == null) {
            doubleBulletBitmap = createPowerUpBitmap(res, R.drawable.double_bullet, type);
        } else if (type == TYPE_KUNAI && kunaiBitmap == null) {
            kunaiBitmap = createPowerUpBitmap(res, R.drawable.kunai, type);
        } else if (type == TYPE_SHIELD && shieldBitmap == null) {
            shieldBitmap = createPowerUpBitmap(res, R.drawable.shield, type);
        }

        switch (type) {
            case TYPE_DOUBLE_BULLET:
                powerUpBitmap = doubleBulletBitmap;
                break;
            case TYPE_KUNAI:
                powerUpBitmap = kunaiBitmap;
                break;
            case TYPE_SHIELD:
                powerUpBitmap = shieldBitmap;
                break;
            default:
                powerUpBitmap = doubleBulletBitmap;
        }

        if (powerUpBitmap != null) {
            width = powerUpBitmap.getWidth();
            height = powerUpBitmap.getHeight();
        }

        if (type == TYPE_KUNAI || type == TYPE_DOUBLE_BULLET) {
            effectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            effectPaint.setStyle(Paint.Style.STROKE);
            effectPaint.setStrokeWidth(6f);
            effectPaint.setColor(type == TYPE_KUNAI ? 0xFF00FF00 : 0xFFFFD700);
        }

        x = -width;
        y = -height;
    }

    private Bitmap createPowerUpBitmap(Resources res, int resId, int type) {
        Bitmap original = BitmapCache.get(res, resId, 1);

        // Different sizes for different types
        int width;
        if (type == TYPE_DOUBLE_BULLET) {
            // 70% of the standard size
            width = (int) ((original.getWidth() / 10) * 0.7f);
        } else {
            width = original.getWidth() / 5;
        }

        int height = original.getHeight() * width / original.getWidth();

        Bitmap scaled = Bitmap.createScaledBitmap(original, width, height, true);
        Bitmap result = createCircularBitmap(scaled, type);

        if (scaled != result && !scaled.isRecycled()) {
            scaled.recycle();
        }

        return result;
    }

    private Bitmap createCircularBitmap(Bitmap source, int type) {
        int size = Math.max(source.getWidth(), source.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        int bgColor = 0, borderColor;
        switch (type) {
            case TYPE_SHIELD:
                bgColor = 0x8000FFFF;
                borderColor = 0xFF00FFFF;
                break;
            case TYPE_KUNAI:
                borderColor = 0xFFFF0000;
                break;
            case TYPE_DOUBLE_BULLET:
                borderColor = 0xFFFFD700;
                break;
            default:
                bgColor = 0x80FFFFFF;
                borderColor = 0xFFFFD700;
        }

        if (type != TYPE_KUNAI && type != TYPE_DOUBLE_BULLET) {
            bgPaint.setColor(bgColor);
            canvas.drawCircle(size / 2f, size / 2f, size / 2f - 5, bgPaint);
        }

        borderPaint.setColor(borderColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 5, borderPaint);

        Rect rect = new Rect(0, 0, size, size);
        RectF rectF = new RectF(rect);

        canvas.drawOval(rectF, imagePaint);
        imagePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        int left = (size - source.getWidth()) / 2;
        int top = (size - source.getHeight()) / 2;
        canvas.drawBitmap(source, left, top, imagePaint);

        return output;
    }

    public void spawnAt(int startX, int startY) {
        active = true;
        x = startX;
        y = startY;
        spawnTime = System.currentTimeMillis();
    }

    public void update(float deltaTime, int screenY) {
        if (!active) return;
        y += (int)(fallSpeed * deltaTime);
        if (y > screenY) {
            clear();
        }
    }

    public void clear() {
        active = false;
        x = -width;
        y = -height;
    }

    public Rect getCollisionShape() {
        collisionRect.set(x, y, x + width, y + height);
        return collisionRect;
    }

    public Bitmap getBitmap() {
        return powerUpBitmap;
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!active || powerUpBitmap == null) return;

        canvas.drawBitmap(powerUpBitmap, x, y, paint);

        if ((type == TYPE_KUNAI || type == TYPE_DOUBLE_BULLET) && effectPaint != null) {
            long elapsed = System.currentTimeMillis() - spawnTime;
            float pulse = (float) (Math.sin(elapsed / 100.0) * 0.15f + 1f);
            float radius = (width / 2f) * pulse;

            float centerX = x + width / 2f;
            float centerY = y + height / 2f;

            float progress = (elapsed % 1000) / 1000f;
            int alpha = (int) (255 * (1 - progress * 0.3f));
            effectPaint.setAlpha(Math.max(100, alpha));

            canvas.drawCircle(centerX, centerY, radius, effectPaint);
        }
    }

    public static void clearCache() {
        if (doubleBulletBitmap != null && !doubleBulletBitmap.isRecycled()) {
            doubleBulletBitmap.recycle();
            doubleBulletBitmap = null;
        }
        if (kunaiBitmap != null && !kunaiBitmap.isRecycled()) {
            kunaiBitmap.recycle();
            kunaiBitmap = null;
        }
        if (shieldBitmap != null && !shieldBitmap.isRecycled()) {
            shieldBitmap.recycle();
            shieldBitmap = null;
        }
    }
}