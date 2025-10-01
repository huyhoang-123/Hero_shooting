package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.util.HashMap;

public class BitmapCache {
    private static final HashMap<Integer, Bitmap> cache = new HashMap<>();

    /**
     * Lấy bitmap từ cache nếu có, nếu chưa có -> decode và lưu vào cache.
     * sampleSize tương tự inSampleSize (1 = full, 2 = giảm 2 lần...)
     */
    public static Bitmap get(Resources res, int resId, int sampleSize) {
        Bitmap bmp = cache.get(resId);
        if (bmp != null && !bmp.isRecycled()) {
            return bmp;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        opts.inSampleSize = Math.max(1, sampleSize);
        bmp = BitmapFactory.decodeResource(res, resId, opts);

        if (bmp != null) {
            cache.put(resId, bmp);
        }
        return bmp;
    }

    /**
     * Giải phóng tất cả bitmap trong cache (gọi khi cần clear memory, ví dụ onDestroy của GameActivity).
     */
    public static void clear() {
        for (Bitmap b : cache.values()) {
            if (b != null && !b.isRecycled()) {
                b.recycle();
            }
        }
        cache.clear();
    }
}
