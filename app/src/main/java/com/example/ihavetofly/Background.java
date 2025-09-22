package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Background {
    public int x, y;
    public Bitmap background;

    public Background(Resources res, int drawableId, int screenY) {
        // Tối ưu: Sử dụng BitmapFactory.Options để decode hiệu quả hơn
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565; // Tiết kiệm bộ nhớ
        options.inSampleSize = 1; // Có thể điều chỉnh nếu ảnh quá lớn

        Bitmap temp = BitmapFactory.decodeResource(res, drawableId, options);

        if (temp == null) {
            throw new RuntimeException("Cannot decode background resource");
        }

        float ratio = (float) screenY / temp.getHeight();
        int newWidth = (int) (temp.getWidth() * ratio);

        // Tối ưu: Sử dụng filter = true để có chất lượng tốt hơn khi scale
        background = Bitmap.createScaledBitmap(temp, newWidth, screenY, true);

        // Giải phóng bitmap tạm
        if (temp != background) {
            temp.recycle();
        }

        x = 0;
        y = 0;
    }

    // Thêm method để giải phóng bộ nhớ
    public void recycle() {
        if (background != null && !background.isRecycled()) {
            background.recycle();
            background = null;
        }
    }
}