package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Background {

    public Bitmap background;

    public Background(Resources res, int drawableId, int screenY){
        background = BitmapFactory.decodeResource(res, drawableId);
        float ratio = (float)background.getWidth() / background.getHeight();
        int w = (int)(screenY*ratio);
        background = Bitmap.createScaledBitmap(background,w,screenY,true);
    }
}
