package com.example.ihavetofly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import java.util.Random;

public class Bomb {

    public int x, y;
    public int width, height;
    public boolean active = false;        // Bomb đang hoạt động
    public long spawnTime;                 // Thời gian spawn
    public boolean isRespawnBomb = false; // Bomb mới spawn có thể clear timeout

    private Bitmap bombBitmap;
    private Random random;

    public Bomb(Resources res, int screenX, int screenY){
        bombBitmap = BitmapFactory.decodeResource(res, R.drawable.bomb_4);

        width = bombBitmap.getWidth() / 8; // scale nhỏ
        height = bombBitmap.getHeight() * width / bombBitmap.getWidth();
        bombBitmap = Bitmap.createScaledBitmap(bombBitmap, width, height, true);

        random = new Random();
        x = -width;
        y = -height;
    }

    public void spawn(int screenX, boolean respawnBomb){
        active = true;
        y = -height;  // khởi tạo trên màn hình
        x = random.nextInt(screenX - width);
        spawnTime = System.currentTimeMillis();
        isRespawnBomb = respawnBomb;
    }

    public void update(float deltaTime, int speed){
        if(!active) return;
        y += speed * deltaTime;
    }

    public void clear(){
        active = false;
        x = -width;
        y = -height;
        isRespawnBomb = false;
    }

    public Rect getCollisionShape(){
        return new Rect(x, y, x + width, y + height);
    }

    public Bitmap getBitmap(){
        return bombBitmap;
    }
}
