package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Flight {

    public int x, y, width, height;
    public int speed = 15;
    public boolean movingLeft = false;
    public boolean movingRight = false;

    private Bitmap flightBitmap;

    public Flight(GameView gameView, int screenY, Resources res){
        flightBitmap = BitmapFactory.decodeResource(res, R.drawable.space_ships);
        width = flightBitmap.getWidth()/4;
        height = flightBitmap.getHeight()/4;
        flightBitmap = Bitmap.createScaledBitmap(flightBitmap,width,height,true);
        x = 100;
        y = screenY - height - 50;
    }

    public void updatePosition(int backgroundWidth){
        if(movingLeft) x -= speed;
        if(movingRight) x += speed;

        if(x<0) x=0;
        if(x+width>backgroundWidth) x = backgroundWidth - width;
    }

    public Bitmap getFlight(){ return flightBitmap; }
}
