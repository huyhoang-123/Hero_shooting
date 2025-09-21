package com.example.ihavetofly;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable {

    private Thread thread;
    private boolean isPlaying, isGameOver = false;
    private Background background1, background2;
    public int screenX, screenY;
    public static float screenRatioX, screenRatioY;

    private List<Bullet> bullets;
    private Bird[] birds;
    private SharedPreferences prefs;
    private Random random;
    private SoundPool soundPool;
    private Paint paint;
    private int sound;
    private Flight flight;
    private GameActivity activity;
    private int score = 0;

    private long lastShootTime = 0;
    private long shootInterval = 300; // 300ms giữa 2 viên đạn

    public float cameraX = 0; // camera để cuộn background theo player (đổi từ private thành public)

    public GameView(GameActivity activity, int screenX, int screenY) {
        super(activity);
        this.activity = activity;
        prefs = activity.getSharedPreferences("game", Context.MODE_PRIVATE);

        // Setup SoundPool
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        }
        sound = soundPool.load(activity, R.raw.shoot, 1);

        this.screenX = screenX;
        this.screenY = screenY;
        screenRatioX = 1920f / screenX;
        screenRatioY = 1080f / screenY;

        // 🌌 Background (tile ngang)
        background1 = new Background(screenX, screenY, getResources(), R.drawable.bg_space);
        background2 = new Background(screenX, screenY, getResources(), R.drawable.bg_space);

        flight = new Flight(this, screenX, screenY, getResources());
        bullets = new ArrayList<>();
        paint = new Paint();
        paint.setTextSize(96);
        paint.setColor(Color.WHITE);

        // 🐦 Khởi tạo chim nhiều hơn và rơi nhanh hơn
        birds = new Bird[8];  // 8 chim
        random = new Random();
        int spacing = screenY / 8;  // giảm spacing để chim rơi dày hơn
        for (int i = 0; i < birds.length; i++) {
            birds[i] = new Bird(getResources());
            respawnBird(birds[i]);
            birds[i].y -= i * spacing;
        }
    }

    @Override
    public void run() {
        while (isPlaying) {
            update();
            draw();
            sleep();
        }
    }

    private void update() {
        // ✈️ Cập nhật nhân vật
        flight.updatePosition();

        // 📷 Camera follow nhân vật để background di chuyển theo
        cameraX = flight.x - screenX / 2f + flight.width / 2f;

        // 🔫 Bắn tự động khi di chuyển (trái/phải)
        boolean moved = flight.movingLeft || flight.movingRight;
        long now = System.currentTimeMillis();
        if (moved && now - lastShootTime >= shootInterval) {
            flight.toShoot++;
            lastShootTime = now;
        }

        // 📍 Cập nhật đạn
        List<Bullet> trash = new ArrayList<>();
        for (Bullet bullet : bullets) {
            bullet.y -= bullet.speed;
            if (bullet.y < 0) trash.add(bullet);

            for (Bird bird : birds) {
                if (Rect.intersects(bird.getCollisionShape(), bullet.getCollisionShape())) {
                    score++;
                    respawnBird(bird);
                    bullet.y = -500;
                    bird.wasShot = true;
                }
            }
        }
        bullets.removeAll(trash);

        // 🐦 Cập nhật chim (chim rơi nhanh hơn)
        for (Bird bird : birds) {
            bird.y += bird.speed;

            if (bird.y > screenY || bird.wasShot) {
                respawnBird(bird);
            }

            if (Rect.intersects(bird.getCollisionShape(),
                    new Rect(flight.x, flight.y, flight.x + flight.width, flight.y + flight.height))) {
                isGameOver = true;
                return;
            }
        }
    }

    private void respawnBird(Bird bird) {
        bird.wasShot = false;

        int marginX = 50; // khoảng cách từ mép màn hình
        int minX = (int) cameraX + marginX;
        int maxX = (int) (cameraX + screenX - bird.size - marginX);

        bird.x = minX + random.nextInt(Math.max(1, maxX - minX));
        bird.y = -bird.size - random.nextInt(screenY / 4);

        // Tăng tốc độ bird
        bird.speed = random.nextInt((int)(12 * screenRatioY)) + (int)(6 * screenRatioY); // nhanh hơn trước
    }

    private void draw() {
        if (!getHolder().getSurface().isValid()) return;

        Canvas canvas = getHolder().lockCanvas();
        canvas.drawColor(Color.BLACK);

        // Vẽ background (tile ngang lặp vô hạn)
        int bgWidth = background1.background.getWidth();
        int startX = (int)(-cameraX % bgWidth);
        if (startX > 0) startX -= bgWidth;

        for (int x = startX; x < screenX; x += bgWidth) {
            canvas.drawBitmap(background1.background, x, 0, paint);
        }

        // Vẽ player (luôn ở giữa màn hình, background di chuyển theo)
        canvas.drawBitmap(flight.getFlight(), screenX / 2f - flight.width / 2f, flight.y, paint);

        // Vẽ điểm
        canvas.drawText(score + "", screenX / 2f, 128, paint);

        if (isGameOver) {
            canvas.drawBitmap(flight.getDead(), screenX / 2f - flight.width / 2f, flight.y, paint);
            getHolder().unlockCanvasAndPost(canvas);
            saveIfHighScore();
            waitBeforeExiting();
            return;
        }

        // Vẽ chim
        for (Bird bird : birds) {
            canvas.drawBitmap(bird.getBird(), bird.x - cameraX, bird.y, paint);
        }

        // Vẽ đạn
        for (Bullet bullet : bullets) {
            canvas.drawBitmap(bullet.bullet, bullet.x - cameraX, bullet.y, paint);
        }

        getHolder().unlockCanvasAndPost(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (x < screenX / 2f) {
                    flight.movingLeft = true;
                    flight.movingRight = false;
                } else {
                    flight.movingRight = true;
                    flight.movingLeft = false;
                }
                break;

            case MotionEvent.ACTION_UP:
                flight.movingLeft = false;
                flight.movingRight = false;
                break;
        }
        return true;
    }

    public void newBullet() {
        if (!prefs.getBoolean("isMute", false) && soundPool != null) {
            soundPool.play(sound, 1, 1, 0, 0, 1);
        }

        Bullet bullet = new Bullet(getResources(), screenX, screenY);
        bullet.x = flight.x + flight.width / 2 - bullet.width / 2;
        bullet.y = flight.y;
        bullets.add(bullet);
    }

    private void waitBeforeExiting() {
        try {
            Thread.sleep(3000);
            activity.startActivity(new Intent(activity, MainActivity.class));
            activity.finish();
        } catch (InterruptedException e) { e.printStackTrace(); }
    }

    private void saveIfHighScore() {
        if (prefs.getInt("highscore", 0) < score) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("highscore", score);
            editor.apply();
        }
    }

    private void sleep() {
        try { Thread.sleep(17); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    public void resume() {
        isPlaying = true;
        thread = new Thread(this);
        thread.start();
    }

    public void pause() {
        try {
            isPlaying = false;
            thread.join();
        } catch (InterruptedException e) { e.printStackTrace(); }
    }
}