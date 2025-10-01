package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;

public class Background {

    public Bitmap background;

    public Background(Resources res, int drawableId, int screenY){
        // lấy bitmap từ cache (nếu preload đã gọi ở MainActivity thì sẽ nhanh)
        Bitmap bmp = BitmapCache.get(res, drawableId, 1);
        // giữ logic tỉ lệ như ban đầu
        float ratio = (float) bmp.getWidth() / bmp.getHeight();
        int w = (int)(screenY * ratio);
        background = Bitmap.createScaledBitmap(bmp, w, screenY, true);
    }
}
