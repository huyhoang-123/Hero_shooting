package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Flight {

    public int x, y;
    public int width, height;
    public boolean movingLeft = false, movingRight = false, movingUp = false, movingDown = false;

    private Bitmap flightBitmap;
    private float speed = 600; // pixels/second
    private int screenX, screenY;

    private int coinScore = 0; // thêm biến đếm coin

    public Flight(int screenX, int screenY, Resources res){
        this.screenX = screenX;
        this.screenY = screenY;

        Bitmap tmp = BitmapFactory.decodeResource(res, R.drawable.space_ships);
        width = screenX / 8;
        height = tmp.getHeight() * width / tmp.getWidth();
        flightBitmap = Bitmap.createScaledBitmap(tmp, width, height, true);
        if(tmp != flightBitmap) tmp.recycle();

        // Vị trí ban đầu: giữa đáy màn hình
        x = screenX / 2 - width / 2;
        y = screenY - height - 50;
    }

    public void updatePosition(float deltaTime){
        float dx = 0, dy = 0;
        if(movingLeft) dx -= speed * deltaTime;
        if(movingRight) dx += speed * deltaTime;
        if(movingUp) dy -= speed * deltaTime;
        if(movingDown) dy += speed * deltaTime;

        x += dx;
        y += dy;

        // Giới hạn trong màn hình
        if(x < 0) x = 0;
        if(x + width > screenX) x = screenX - width;
        if(y < 0) y = 0;
        if(y + height > screenY) y = screenY - height;
    }

    public Bitmap getFlight(){ return flightBitmap; }

    // ----------------------
    // Các phương thức mới cho coin
    // ----------------------
    public void collectCoin(){
        coinScore++;
    }

    public int getCoinScore(){
        return coinScore;
    }

    public void resetCoinScore(){
        coinScore = 0;
    }
}
