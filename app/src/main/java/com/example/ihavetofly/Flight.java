package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;

public class Flight {

    public int x, y, width, height;
    public int speed = 600; // pixels per second (đổi sang đơn vị per-second)
    public boolean movingLeft = false;
    public boolean movingRight = false;

    private Bitmap flightBitmap;

    public Flight(GameView gameView, int screenY, Resources res){
        flightBitmap = BitmapCache.get(res, R.drawable.space_ships, 1);
        width = flightBitmap.getWidth()/4;
        height = flightBitmap.getHeight()/4;
        flightBitmap = Bitmap.createScaledBitmap(flightBitmap,width,height,true);
        x = 100;
        y = screenY - height - 50;
    }

    /**
     * Cập nhật vị trí theo deltaTime (giây). Giữ nguyên giới hạn biên màn hình.
     */
    public void updatePosition(int backgroundWidth, float deltaTime){
        float move = speed * deltaTime;
        if(movingLeft) x -= (int) move;
        if(movingRight) x += (int) move;

        if(x<0) x=0;
        if(x+width>backgroundWidth) x = backgroundWidth - width;
    }

    // giữ overload cũ để tương thích (nếu có chỗ gọi)
    public void updatePosition(int backgroundWidth){
        updatePosition(backgroundWidth, 1f/60f);
    }

    public Bitmap getFlight(){ return flightBitmap; }
}
