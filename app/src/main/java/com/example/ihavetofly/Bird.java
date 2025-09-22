package com.example.ihavetofly;

import static com.example.ihavetofly.GameView.screenRatioX;
import static com.example.ihavetofly.GameView.screenRatioY;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public class Bird {

    public int speed;
    public boolean wasShot = true;
    public int x, y;
    private final Bitmap[] frames;
    private int frameIndex = 0;
    private int frameCounter = 0; // Tối ưu: Điều khiển tốc độ animation
    private static final int FRAME_DELAY = 3; // Thay đổi frame sau 3 lần update

    public final int size;
    private Rect collisionRect; // Cache collision rect

    Bird(Resources res) {
        // Tối ưu: Sử dụng options để giảm bộ nhớ
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_4444; // Tiết kiệm bộ nhớ cho ảnh có alpha

        Bitmap b1 = BitmapFactory.decodeResource(res, R.drawable.bird1, options);
        Bitmap b2 = BitmapFactory.decodeResource(res, R.drawable.bird2, options);
        Bitmap b3 = BitmapFactory.decodeResource(res, R.drawable.bird3, options);

        // Kiểm tra null
        if (b1 == null || b2 == null || b3 == null) {
            throw new RuntimeException("Cannot decode bird resources");
        }

        int width = b1.getWidth() / 12;
        int height = b1.getHeight() / 12;

        width = Math.max(1, (int)(width * screenRatioX));
        height = Math.max(1, (int)(height * screenRatioY));

        size = height;

        // Tối ưu: Scale một lần và lưu trữ
        b1 = Bitmap.createScaledBitmap(b1, size, size, false);
        b2 = Bitmap.createScaledBitmap(b2, size, size, false);
        b3 = Bitmap.createScaledBitmap(b3, size, size, false);

        frames = new Bitmap[]{b1, b2, b3};

        speed = (int)(7 * screenRatioY);
        y = -size;
        x = 0;

        // Khởi tạo collision rect
        collisionRect = new Rect();
    }

    public Bitmap getBird() {
        // Tối ưu: Chỉ thay đổi frame sau một khoảng thời gian
        frameCounter++;
        if (frameCounter >= FRAME_DELAY) {
            frameIndex = (frameIndex + 1) % frames.length;
            frameCounter = 0;
        }
        return frames[frameIndex];
    }

    public Rect getCollisionShape() {
        // Tối ưu: Cập nhật collision rect thay vì tạo mới
        collisionRect.set(x, y, x + size, y + size);
        return collisionRect;
    }

    // Thêm method để giải phóng bộ nhớ
    public void recycle() {
        for (Bitmap frame : frames) {
            if (frame != null && !frame.isRecycled()) {
                frame.recycle();
            }
        }
    }
}