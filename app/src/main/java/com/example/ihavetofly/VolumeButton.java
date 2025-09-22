package com.example.ihavetofly;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class VolumeButton {
    private Bitmap volumeOnBitmap;
    private Bitmap volumeOffBitmap;
    private RectF buttonRect;
    private boolean isVisible = true;
    private float x, y;
    private int size;
    private Paint paint;
    private GameAudioManager audioManager;

    // Animation properties
    private float scale = 1.0f;
    private float targetScale = 1.0f;
    private boolean isPressed = false;

    public VolumeButton(Context context, int screenX, int screenY) {
        audioManager = GameAudioManager.getInstance(context);

        // Calculate button size and position
        size = (int)(screenY * 0.08f); // 8% of screen height
        x = screenX - size - (screenX * 0.05f); // 5% margin from right edge
        y = screenY * 0.45f; // Center vertically

        buttonRect = new RectF(x, y, x + size, y + size);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);

        loadBitmaps(context.getResources());
    }

    private void loadBitmaps(Resources res) {
        try {
            // Load volume on bitmap (speaker_high_volume.png)
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_4444;

            Bitmap tempOn = BitmapFactory.decodeResource(res, R.drawable.speaker_high_volume, options);
            if (tempOn != null) {
                volumeOnBitmap = Bitmap.createScaledBitmap(tempOn, size, size, true);
                if (tempOn != volumeOnBitmap) {
                    tempOn.recycle();
                }
            }

            // Load volume off bitmap (volume_speaker.png - mute icon)
            Bitmap tempOff = BitmapFactory.decodeResource(res, R.drawable.volume_speaker, options);
            if (tempOff != null) {
                volumeOffBitmap = Bitmap.createScaledBitmap(tempOff, size, size, true);
                if (tempOff != volumeOffBitmap) {
                    tempOff.recycle();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update() {
        // Smooth scale animation
        float scaleSpeed = 0.15f;
        if (Math.abs(scale - targetScale) > 0.01f) {
            scale += (targetScale - scale) * scaleSpeed;
        } else {
            scale = targetScale;
        }
    }

    public void draw(Canvas canvas) {
        if (!isVisible) return;

        canvas.save();

        // Apply scale animation - không cần cameraX vì nút cố định trên màn hình
        float centerX = x + size / 2f;
        float centerY = y + size / 2f;
        canvas.scale(scale, scale, centerX, centerY);

        // Choose appropriate bitmap dựa trên trạng thái tổng thể
        // Mute khi CẢ HAI đều tắt, volume khi có ít nhất 1 cái bật
        Bitmap currentBitmap = (audioManager.isMuted() && audioManager.isMusicMuted()) ? volumeOffBitmap : volumeOnBitmap;

        if (currentBitmap != null) {
            // Add subtle shadow effect
            paint.setAlpha(50);
            canvas.drawBitmap(currentBitmap, x + 3, y + 3, paint);

            // Draw main button - không trừ cameraX
            paint.setAlpha(255);
            canvas.drawBitmap(currentBitmap, x, y, paint);
        }

        canvas.restore();
    }

    public boolean handleTouch(float touchX, float touchY, int action) {
        if (!isVisible) return false;

        // Không cần điều chỉnh touch coordinates vì button cố định trên màn hình
        boolean isInButton = touchX >= x && touchX <= x + size &&
                touchY >= y && touchY <= y + size;

        android.util.Log.d("VolumeButton", "Touch event - action: " + action + ", isInButton: " + isInButton + ", touchX: " + touchX + ", touchY: " + touchY + ", buttonX: " + x + ", buttonY: " + y + ", size: " + size);

        switch (action) {
            case 0: // ACTION_DOWN
                if (isInButton) {
                    isPressed = true;
                    targetScale = 0.9f; // Shrink when pressed
                    android.util.Log.d("VolumeButton", "Button pressed");
                    return true;
                }
                break;

            case 1: // ACTION_UP
                if (isPressed) {
                    isPressed = false;
                    targetScale = 1.0f; // Return to normal size

                    if (isInButton) {
                        // Toggle tất cả âm thanh (cả music và sound effects) trong game
                        android.util.Log.d("VolumeButton", "Button clicked - toggling audio");
                        audioManager.toggleAll();

                        // Brief scale effect
                        targetScale = 1.2f;
                        new Thread(() -> {
                            try {
                                Thread.sleep(100);
                                targetScale = 1.0f;
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    }
                    return true;
                }
                break;

            case 2: // ACTION_MOVE
                if (isPressed && !isInButton) {
                    // Cancel press if moved outside button
                    isPressed = false;
                    targetScale = 1.0f;
                }
                break;
        }

        return false;
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void cleanup() {
        if (volumeOnBitmap != null && !volumeOnBitmap.isRecycled()) {
            volumeOnBitmap.recycle();
            volumeOnBitmap = null;
        }

        if (volumeOffBitmap != null && !volumeOffBitmap.isRecycled()) {
            volumeOffBitmap.recycle();
            volumeOffBitmap = null;
        }
    }

    // Getter for button bounds (useful for debugging)
    public RectF getButtonRect() {
        return buttonRect;
    }
}