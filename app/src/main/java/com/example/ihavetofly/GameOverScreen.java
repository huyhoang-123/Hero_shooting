package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

public class GameOverScreen {

    private int screenX, screenY;
    private Rect exitButtonRect, replayButtonRect, continueButtonRect;
    private Paint textPaint, buttonTextPaint;
    private Bitmap gameOverImage;
    private Resources resources;

    public GameOverScreen(Resources res, int screenX, int screenY) {
        this.resources = res;
        this.screenX = screenX;
        this.screenY = screenY;

        initPaints();
        initButtons();
        loadImages();
    }

    private void initPaints() {
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setShadowLayer(6, 2, 2, Color.BLACK);

        buttonTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonTextPaint.setColor(Color.WHITE);
        buttonTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void initButtons() {
        int btnW = (int) (screenX * 0.35f);
        int btnH = (int) (screenY * 0.10f);
        int centerX = screenX / 2;

        replayButtonRect = new Rect(centerX - btnW - 20, (int) (screenY * 0.65f),
                centerX - 20, (int) (screenY * 0.65f) + btnH);

        continueButtonRect = new Rect(centerX + 20, (int) (screenY * 0.65f),
                centerX + 20 + btnW, (int) (screenY * 0.65f) + btnH);

        exitButtonRect = new Rect(centerX - btnW / 2, (int) (screenY * 0.78f),
                centerX + btnW / 2, (int) (screenY * 0.78f) + btnH);
    }

    private void loadImages() {
        Bitmap tmp = BitmapFactory.decodeResource(resources, R.drawable.game_over);
        int imgWidth = screenX / 2;
        int imgHeight = tmp.getHeight() * imgWidth / tmp.getWidth();
        gameOverImage = Bitmap.createScaledBitmap(tmp, imgWidth, imgHeight, true);
        if (tmp != gameOverImage) tmp.recycle();
    }

    public void draw(Canvas canvas, int score, Bitmap flightBitmap, int flightX, int flightY, Paint paint) {
        canvas.drawBitmap(gameOverImage, screenX / 2f - gameOverImage.getWidth() / 2f,
                screenY * 0.25f - gameOverImage.getHeight() / 2f, paint);

        textPaint.setTextSize(screenY * 0.05f);
        canvas.drawText("GAME OVER SCORE: " + score, screenX / 2f - 200, screenY * 0.55f, textPaint);

        if (flightBitmap != null) {
            canvas.drawBitmap(flightBitmap, flightX, flightY, paint);
        }

        drawButtons(canvas);
    }

    private void drawButtons(Canvas canvas) {
        buttonTextPaint.setTextSize(screenY * 0.05f);

        LinearGradient replayGradient = new LinearGradient(
                0, replayButtonRect.top, 0, replayButtonRect.bottom,
                Color.rgb(255, 180, 0), Color.rgb(255, 100, 0), Shader.TileMode.CLAMP
        );
        Paint replayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        replayPaint.setShader(replayGradient);
        canvas.drawRoundRect(new RectF(replayButtonRect), 30, 30, replayPaint);
        canvas.drawText("REPLAY", replayButtonRect.centerX(), replayButtonRect.centerY() + 20, buttonTextPaint);

        LinearGradient contGradient = new LinearGradient(
                0, continueButtonRect.top, 0, continueButtonRect.bottom,
                Color.rgb(60, 180, 60), Color.rgb(0, 120, 0), Shader.TileMode.CLAMP
        );
        Paint contPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        contPaint.setShader(contGradient);
        canvas.drawRoundRect(new RectF(continueButtonRect), 30, 30, contPaint);
        canvas.drawText("CONTINUE", continueButtonRect.centerX(), continueButtonRect.centerY() + 20, buttonTextPaint);

        LinearGradient exitGradient = new LinearGradient(
                0, exitButtonRect.top, 0, exitButtonRect.bottom,
                Color.rgb(200, 60, 60), Color.rgb(120, 0, 0), Shader.TileMode.CLAMP
        );
        Paint exitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        exitPaint.setShader(exitGradient);
        canvas.drawRoundRect(new RectF(exitButtonRect), 30, 30, exitPaint);
        canvas.drawText("EXIT", exitButtonRect.centerX(), exitButtonRect.centerY() + 20, buttonTextPaint);
    }

    public boolean handleTouch(float x, float y, Runnable onReplay, Runnable onContinue, Runnable onExit) {
        if (replayButtonRect.contains((int) x, (int) y)) {
            if (onReplay != null) onReplay.run();
            return true;
        } else if (continueButtonRect.contains((int) x, (int) y)) {
            if (onContinue != null) onContinue.run();
            return true;
        } else if (exitButtonRect.contains((int) x, (int) y)) {
            if (onExit != null) onExit.run();
            return true;
        }
        return false;
    }

    public void cleanup() {
        if (gameOverImage != null && !gameOverImage.isRecycled()) {
            gameOverImage.recycle();
            gameOverImage = null;
        }
    }
}