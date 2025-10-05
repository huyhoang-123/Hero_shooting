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

    private Bitmap powerUpBitmap;
    private final Rect collisionRect = new Rect();
    private final int fallSpeed = 250;

    public PowerUp(Resources res, int type) {
        this.type = type;
        int resId;
        switch (type) {
            case TYPE_DOUBLE_BULLET:
                resId = R.drawable.double_bullet;
                break;
            case TYPE_KUNAI:
                resId = R.drawable.kunai;
                break;
            case TYPE_SHIELD:
                resId = R.drawable.shield;
                break;
            default:
                resId = R.drawable.double_bullet;
        }

        Bitmap original = BitmapCache.get(res, resId, 1);
        if (type == TYPE_DOUBLE_BULLET) {
            width = original.getWidth() / 8;
        } else {
            width = original.getWidth() / 5;
        }
        height = original.getHeight() * width / original.getWidth();

        Bitmap scaled = Bitmap.createScaledBitmap(original, width, height, true);
        powerUpBitmap = createCircularBitmap(scaled, type);

        x = -width;
        y = -height;
    }

    private Bitmap createCircularBitmap(Bitmap source, int type) {
        int size = Math.max(source.getWidth(), source.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Different colors for different power-ups
        int bgColor, borderColor, shadowColor;
        switch (type) {
            case TYPE_SHIELD:
                bgColor = 0x8000FFFF; // Cyan
                borderColor = 0xFF00FFFF;
                shadowColor = 0xAA00FFFF;
                break;
            case TYPE_KUNAI:
                bgColor = 0x80FF4444; // Red
                borderColor = 0xFFFF0000;
                shadowColor = 0xAAFF4444;
                break;
            default: // DOUBLE_BULLET
                bgColor = 0x80FFFFFF; // White
                borderColor = 0xFFFFD700; // Gold
                shadowColor = 0xAAFFFFFF;
        }

        // Draw blurred background circle
        paint.setColor(bgColor);
        paint.setShadowLayer(15f, 0, 0, shadowColor);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 5, paint);

        // Draw border
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(borderColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 5, paint);

        // Draw the actual image inside circle
        paint.reset();
        paint.setAntiAlias(true);
        Rect rect = new Rect(0, 0, size, size);
        RectF rectF = new RectF(rect);

        canvas.drawOval(rectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        int left = (size - source.getWidth()) / 2;
        int top = (size - source.getHeight()) / 2;
        canvas.drawBitmap(source, left, top, paint);

        return output;
    }

    public void spawnAt(int startX, int startY) {
        active = true;
        x = startX;
        y = startY;
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
}