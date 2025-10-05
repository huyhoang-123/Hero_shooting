package com.example.ihavetofly;

import android.graphics.Canvas;
import android.graphics.Paint;

public class ShieldEffect {

    private float centerX, centerY;
    private float radius;
    private Paint shieldPaint;
    private long startTime;
    private static final long DURATION = 5000; // 5 seconds

    public ShieldEffect(float centerX, float centerY, float radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.startTime = System.currentTimeMillis();

        shieldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shieldPaint.setStyle(Paint.Style.STROKE);
        shieldPaint.setStrokeWidth(6f);
        shieldPaint.setColor(0xFF00FFFF); // Cyan
        shieldPaint.setShadowLayer(20f, 0, 0, 0xFF00FFFF);
    }

    public void updatePosition(float centerX, float centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public void draw(Canvas canvas) {
        long elapsed = System.currentTimeMillis() - startTime;
        float progress = elapsed / (float) DURATION;

        // Pulsating effect
        float pulse = (float) (Math.sin(elapsed / 100.0) * 0.1f + 1f);
        float currentRadius = radius * pulse;

        // Fade out near end
        int alpha = (int) (255 * (1 - progress));
        if (alpha < 0) alpha = 0;

        shieldPaint.setAlpha(alpha);
        shieldPaint.setShadowLayer(20f * pulse, 0, 0, (alpha << 24) | 0x00FFFF);

        canvas.drawCircle(centerX, centerY, currentRadius, shieldPaint);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - startTime >= DURATION;
    }
}