package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;

public class BossBullet {

    public int x, y;
    public int width, height;
    public boolean active = false;

    private static Bitmap[] bulletFrames;
    private static int referenceCount = 0;
    private int frameIndex = 0;
    private long lastFrameTime = 0;
    private static final long FRAME_DURATION = 80; // 80ms per frame for smooth animation

    private final Rect collisionRect = new Rect();
    private static final int FALL_SPEED = 500;

    public BossBullet(Resources res) {
        if (bulletFrames == null) {
            initBulletFrames(res);
        }
        referenceCount++;

        if (bulletFrames != null && bulletFrames.length > 0 && bulletFrames[0] != null) {
            width = bulletFrames[0].getWidth();
            height = bulletFrames[0].getHeight();
        } else {
            width = 40;
            height = 40;
        }

        x = -width;
        y = -height;
    }

    private static synchronized void initBulletFrames(Resources res) {
        if (bulletFrames != null) return;

        Bitmap spriteSheet = BitmapCache.get(res, R.drawable.boss_bullet, 1);
        if (spriteSheet == null) return;

        int frameCount = 4; // 4 frames in the sprite
        int sheetWidth = spriteSheet.getWidth();
        int sheetHeight = spriteSheet.getHeight();

        // Each frame width (sprite is horizontal)
        int frameWidth = sheetWidth / frameCount;
        int frameHeight = sheetHeight;

        // Desired bullet size BEFORE rotation
        int targetWidth = 300;  // You can adjust this
        int targetHeight = (frameHeight * targetWidth) / frameWidth;

        bulletFrames = new Bitmap[frameCount];

        // Extract frames in REVERSE order: frame 4, 3, 2, 1 (right to left)
        for (int i = 0; i < frameCount; i++) {
            // Calculate position from RIGHT to LEFT
            int frameX = (frameCount - 1 - i) * frameWidth;

            // Create bitmap for this frame
            Bitmap frame = Bitmap.createBitmap(spriteSheet, frameX, 0, frameWidth, frameHeight);

            // Scale to target size
            Bitmap scaledFrame = Bitmap.createScaledBitmap(frame, targetWidth, targetHeight, true);

            // Rotate 270 degrees clockwise to make it vertical (pointing down)
            Matrix matrix = new Matrix();
            matrix.postRotate(270);
            bulletFrames[i] = Bitmap.createBitmap(scaledFrame, 0, 0,
                    scaledFrame.getWidth(), scaledFrame.getHeight(), matrix, true);

            // Recycle temporary bitmaps
            if (frame != scaledFrame) {
                frame.recycle();
            }
            if (scaledFrame != bulletFrames[i]) {
                scaledFrame.recycle();
            }
        }
    }

    public void spawn(int startX, int startY) {
        active = true;
        x = startX;
        y = startY;
        frameIndex = 0;
        lastFrameTime = System.currentTimeMillis();
    }

    public void update(float deltaTime, int screenY) {
        if (!active) return;

        // If frames were cleared while this bullet is still alive, deactivate safely
        if (bulletFrames == null || bulletFrames.length == 0) {
            clear();
            return;
        }

        y += (int) (FALL_SPEED * deltaTime);

        // Update animation frame for fire effect
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime >= FRAME_DURATION) {
            frameIndex = (frameIndex + 1) % bulletFrames.length;
            lastFrameTime = currentTime;
        }

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
        if (bulletFrames == null || bulletFrames.length == 0) return null;
        return bulletFrames[frameIndex];
    }

    public static void clearCache() {
        referenceCount--;
        if (referenceCount <= 0) {
            referenceCount = 0;
            if (bulletFrames != null) {
                for (Bitmap bmp : bulletFrames) {
                    if (bmp != null && !bmp.isRecycled()) {
                        bmp.recycle();
                    }
                }
                bulletFrames = null;
            }
        }
    }
}