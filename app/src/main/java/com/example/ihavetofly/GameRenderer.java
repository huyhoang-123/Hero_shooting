package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class GameRenderer {

    private final Paint paint;
    private final Paint textPaint;
    private final Paint bossHpPaint;
    private final Paint bossHpBgPaint;
    private final Rect bossHpRect;
    private final Rect bossHpBgRect;

    private final int screenY;

    public GameRenderer(Resources res, int screenX, int screenY) {
        this.screenY = screenY;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(screenY * 0.04f);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setShadowLayer(4, 2, 2, Color.BLACK);

        bossHpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bossHpPaint.setColor(Color.RED);

        bossHpBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bossHpBgPaint.setColor(Color.DKGRAY);

        bossHpRect = new Rect();
        bossHpBgRect = new Rect();
    }

    public void drawGameplay(Canvas canvas, Background bg, Flight flight, ShieldEffect shield,
                             EntityManager entityManager, int score, long gameTime) {

        // Flight character and shield are now drawn by GameView
        drawEntities(canvas, entityManager);
        // UI drawing is handled by GameView's drawGameplayHUD method
        drawBossUI(canvas, entityManager.getBossManager().getBoss());
    }

    private void drawEntities(Canvas canvas, EntityManager entityManager) {
        for (Bullet bullet : entityManager.getBullets()) {
            Bitmap bulletBmp = bullet.getBullet();
            if (bulletBmp != null) {
                canvas.drawBitmap(bulletBmp, bullet.x, bullet.y, paint);
            }
        }

        for (Bird bird : entityManager.getBirds()) {
            if (!bird.wasShot) {
                Bitmap birdBmp = bird.getBird();
                if (birdBmp != null) {
                    canvas.drawBitmap(birdBmp, bird.x, bird.y, paint);
                }
            }
        }

        Bomb bomb = entityManager.getBomb();
        if (bomb.active) {
            canvas.drawBitmap(bomb.getBitmap(), bomb.x, bomb.y, paint);
        }

        for (Coin coin : entityManager.getCoins()) {
            if (coin.active) {
                canvas.drawBitmap(coin.getBitmap(), coin.x, coin.y, paint);
            }
        }

        for (PowerUp powerUp : entityManager.getPowerUps()) {
            if (powerUp.active) {
                powerUp.draw(canvas, paint);
            }
        }

        Boss boss = entityManager.getBossManager().getBoss();
        if (boss.active) {
            canvas.drawBitmap(boss.getBitmap(), boss.x, boss.y, paint);
        }

        for (Rocket rocket : entityManager.getBossManager().getRockets()) {
            if (rocket.active) {
                canvas.drawBitmap(rocket.getBitmap(), rocket.x, rocket.y, paint);
            }
        }
    }


    private void drawBossUI(Canvas canvas, Boss boss) {
        if (!boss.active || boss.isExploding) return;

        int barWidth = canvas.getWidth() - 100;
        int barHeight = 30;
        int barX = 50;
        int barY = canvas.getHeight() - 100;

        bossHpBgRect.set(barX, barY, barX + barWidth, barY + barHeight);
        canvas.drawRect(bossHpBgRect, bossHpBgPaint);

        float hpPercent = boss.getRemainingHits() / 100f;
        int hpWidth = (int) (barWidth * hpPercent);

        bossHpRect.set(barX, barY, barX + hpWidth, barY + barHeight);
        canvas.drawRect(bossHpRect, bossHpPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("BOSS: " + boss.getRemainingHits() + " / 100",
                barX + barWidth / 2f, barY - 10, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    public Paint getPaint() {
        return paint;
    }

    public void cleanup() {
        // No resources to clean up
    }
}