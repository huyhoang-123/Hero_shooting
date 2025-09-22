package com.example.ihavetofly;

import static com.example.ihavetofly.GameView.screenRatioX;
import static com.example.ihavetofly.GameView.screenRatioY;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public class Bullet {
    int x, y, speed, width, height;
    Bitmap bullet;
    private Rect collisionRect; // Cache collision rect
    private static Bitmap cachedBulletBitmap; // Static cache cho bullet bitmap

    Bullet(Resources res, int screenX, int screenY) {
        // Tối ưu: Sử dụng cached bitmap nếu có
        if (cachedBulletBitmap == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_4444; // Tiết kiệm bộ nhớ

            Bitmap temp = BitmapFactory.decodeResource(res, R.drawable.bullet, options);

            if (temp == null) {
                throw new RuntimeException("Cannot decode bullet resource");
            }

            // Tối ưu: Tính kích thước một cách hiệu quả hơn
            width = Math.max(1, (int)((screenX / 15.0f) * screenRatioX));
            height = Math.max(1, (int)((screenY / 15.0f) * screenRatioY));

            // Cache bitmap đã scale
            cachedBulletBitmap = Bitmap.createScaledBitmap(temp, width, height, false);

            // Giải phóng bitmap tạm
            if (temp != cachedBulletBitmap) {
                temp.recycle();
            }
        } else {
            // Sử dụng cached size
            width = cachedBulletBitmap.getWidth();
            height = cachedBulletBitmap.getHeight();
        }

        bullet = cachedBulletBitmap;

        // Tối ưu: Tính speed hiệu quả hơn
        speed = Math.max(1, (int)(50 * screenRatioY));

        // Khởi tạo collision rect
        collisionRect = new Rect();
    }

    Rect getCollisionShape() {
        // Tối ưu: Cập nhật collision rect thay vì tạo mới
        collisionRect.set(x, y, x + width, y + height);
        return collisionRect;
    }

    // Static method để giải phóng cached bitmap
    public static void recycleCachedBitmap() {
        if (cachedBulletBitmap != null && !cachedBulletBitmap.isRecycled()) {
            cachedBulletBitmap.recycle();
            cachedBulletBitmap = null;
        }
    }
}