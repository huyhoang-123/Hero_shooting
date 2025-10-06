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

import java.util.List;

public class GameWinScreen {

    private int screenX, screenY;
    private Rect replayButtonRect, nextLevelButtonRect;
    private Paint textPaint, buttonTextPaint, tablePaint, tableTextPaint;
    private Bitmap winImage;
    private Resources resources;

    public GameWinScreen(Resources res, int screenX, int screenY) {
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
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(6, 2, 2, Color.BLACK);

        buttonTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonTextPaint.setColor(Color.WHITE);
        buttonTextPaint.setTextAlign(Paint.Align.CENTER);

        tablePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tablePaint.setColor(Color.argb(200, 40, 40, 60));
        tablePaint.setShadowLayer(15, 0, 5, Color.argb(150, 0, 0, 0));

        tableTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tableTextPaint.setColor(Color.WHITE);
        tableTextPaint.setTextAlign(Paint.Align.CENTER);
        tableTextPaint.setShadowLayer(3, 1, 1, Color.BLACK);
    }

    private void initButtons() {
        int btnW = (int) (screenX * 0.35f);
        int btnH = (int) (screenY * 0.10f);
        int centerX = screenX / 2;

        replayButtonRect = new Rect(centerX - btnW - 20, (int) (screenY * 0.75f),
                centerX - 20, (int) (screenY * 0.75f) + btnH);

        nextLevelButtonRect = new Rect(centerX + 20, (int) (screenY * 0.75f),
                centerX + 20 + btnW, (int) (screenY * 0.75f) + btnH);
    }

    private void loadImages() {
        Bitmap tmp = BitmapFactory.decodeResource(resources, R.drawable.congratulations);
        int imgWidth = screenX / 2;
        int imgHeight = tmp.getHeight() * imgWidth / tmp.getWidth();
        winImage = Bitmap.createScaledBitmap(tmp, imgWidth, imgHeight, true);
        if (tmp != winImage) tmp.recycle();
    }

    public void draw(Canvas canvas, List<Integer> highScores, Paint paint) {
        canvas.drawBitmap(winImage, screenX / 2f - winImage.getWidth() / 2f, screenY * 0.15f, paint);

        drawHighScoreTable(canvas, highScores);
        drawButtons(canvas);
    }

    private void drawHighScoreTable(Canvas canvas, List<Integer> highScores) {
        int tableWidth = (int) (screenX * 0.7f);
        int rowHeight = (int) (screenY * 0.055f);
        int tableHeight = (highScores.size() + 1) * rowHeight + 60;

        float tableLeft = (screenX - tableWidth) / 2f;
        float tableTop = screenY * 0.15f + winImage.getHeight() + 60;

        RectF tableRect = new RectF(tableLeft, tableTop, tableLeft + tableWidth, tableTop + tableHeight);
        canvas.drawRoundRect(tableRect, 25, 25, tablePaint);

        tableTextPaint.setTextSize(screenY * 0.038f);
        float headerY = tableTop + 50;
        canvas.drawText("TOP 6 HIGH SCORES", tableRect.centerX(), headerY, tableTextPaint);

        tableTextPaint.setTextSize(screenY * 0.035f);
        float startY = headerY + rowHeight;

        for (int i = 0; i < highScores.size(); i++) {
            float rowY = startY + i * rowHeight;
            String rankText = (i + 1) + ".  " + highScores.get(i);
            canvas.drawText(rankText, tableRect.centerX(), rowY, tableTextPaint);
        }
    }

    private void drawButtons(Canvas canvas) {
        buttonTextPaint.setTextSize(screenY * 0.04f);
        buttonTextPaint.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics fm = buttonTextPaint.getFontMetrics();
        float textHeight = fm.bottom - fm.top;
        float textOffset = textHeight / 2 - fm.bottom;

        LinearGradient replayGradient = new LinearGradient(
                0, replayButtonRect.top, 0, replayButtonRect.bottom,
                Color.rgb(255, 180, 0), Color.rgb(255, 100, 0), Shader.TileMode.CLAMP
        );
        Paint replayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        replayPaint.setShader(replayGradient);
        canvas.drawRoundRect(new RectF(replayButtonRect), 30, 30, replayPaint);
        canvas.drawText("REPLAY",
                replayButtonRect.centerX(),
                replayButtonRect.centerY() + textOffset,
                buttonTextPaint
        );

        LinearGradient nextGradient = new LinearGradient(
                0, nextLevelButtonRect.top, 0, nextLevelButtonRect.bottom,
                Color.rgb(60, 180, 255), Color.rgb(0, 100, 200), Shader.TileMode.CLAMP
        );
        Paint nextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nextPaint.setShader(nextGradient);
        canvas.drawRoundRect(new RectF(nextLevelButtonRect), 30, 30, nextPaint);
        canvas.drawText("NEXT LEVEL",
                nextLevelButtonRect.centerX(),
                nextLevelButtonRect.centerY() + textOffset,
                buttonTextPaint
        );
    }

    public boolean handleTouch(float x, float y, Runnable onReplay, Runnable onNextLevel, Runnable onExit) {
        if (replayButtonRect.contains((int) x, (int) y)) {
            if (onReplay != null) onReplay.run();
            return true;
        } else if (nextLevelButtonRect.contains((int) x, (int) y)) {
            if (onNextLevel != null) onNextLevel.run();
            return true;
        }
        return false;
    }

    public void cleanup() {
        if (winImage != null && !winImage.isRecycled()) {
            winImage.recycle();
            winImage = null;
        }
    }
}