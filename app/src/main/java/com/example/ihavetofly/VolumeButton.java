package com.example.ihavetofly;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

public class VolumeButton {

    private Bitmap volumeOn, volumeOff;
    private Bitmap currentVolume;
    private Rect volumeRect;
    private boolean volumeMuted = false;

    public VolumeButton(Context ctx, int screenX, int screenY) {
        int btnSize = screenY / 12;
        int margin = 20;

        // Position in TOP-RIGHT corner (below the HUD elements)
        int topMargin = (int) (screenY * 0.18f); // Below score/time/coin display
        volumeRect = new Rect(
                screenX - btnSize - margin,
                topMargin,
                screenX - margin,
                topMargin + btnSize
        );

        volumeOn = getBitmap(ctx, R.drawable.baseline_volume_up_24, btnSize, btnSize);
        volumeOff = getBitmap(ctx, R.drawable.baseline_volume_mute_24, btnSize, btnSize);

        currentVolume = volumeOn;
    }

    private Bitmap getBitmap(Context ctx, int id, int w, int h) {
        Drawable d = ContextCompat.getDrawable(ctx, id);
        if (d == null) return null;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, w, h);
        d.draw(c);
        return bmp;
    }

    public void draw(Canvas canvas) {
        if (canvas == null || currentVolume == null) return;
        canvas.drawBitmap(currentVolume, null, volumeRect, null);
    }

    public boolean handleTouch(float x, float y) {
        // Expanded touch area for better responsiveness
        int expandedMargin = 30;
        Rect expandedRect = new Rect(
                volumeRect.left - expandedMargin,
                volumeRect.top - expandedMargin,
                volumeRect.right + expandedMargin,
                volumeRect.bottom + expandedMargin
        );

        if (expandedRect.contains((int) x, (int) y)) {
            volumeMuted = !volumeMuted;
            currentVolume = volumeMuted ? volumeOff : volumeOn;
            return true;
        }
        return false;
    }

    public boolean isMusicMuted() {
        return volumeMuted;
    }

    public boolean isSfxMuted() {
        return volumeMuted;
    }
}