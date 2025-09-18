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

        // 🌌 2 nền để wrap-around
        background1 = new Background(screenX, screenY, getResources(), R.drawable.background);
        background2 = new Background(screenX, screenY, getResources(), R.drawable.background);
        background1.x = 0;
        background2.x = screenX;
        background1.y = 0;
        background2.y = 0;

        flight = new Flight(this, screenX, screenY, getResources());
        bullets = new ArrayList<>();
        paint = new Paint();
        paint.setTextSize(96);
        paint.setColor(Color.WHITE);

        // 🐦 Khởi tạo chim
        birds = new Bird[3];
        random = new Random();
        int spacing = screenY / 4; // khoảng cách Y giữa chim
        for (int i = 0; i < birds.length; i++) {
            birds[i] = new Bird(getResources());

            birds[i].x = random.nextInt(screenX - birds[i].size);
            birds[i].y = -birds[i].size - i * spacing; // cách nhau
            birds[i].speed = random.nextInt((int)(8 * screenRatioY)) + (int)(5 * screenRatioY); // rơi nhanh hơn
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
        // 🌌 Cập nhật background
        updateBackground();

        // ✈️ Cập nhật nhân vật
        flight.updatePosition();

        // 🔫 Bắn tự động khi di chuyển
        boolean moved = flight.movingLeft || flight.movingRight || flight.movingUp || flight.movingDown;
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
                    score++; // ✅ tăng score khi bắn trúng chim
                    respawnBird(bird);
                    bullet.y = -500;
                    bird.wasShot = true;
                }
            }
        }
        bullets.removeAll(trash);

        // 🐦 Cập nhật chim
        for (Bird bird : birds) {
            bird.y += bird.speed;

            if (bird.y > screenY || bird.wasShot) {
                respawnBird(bird);
            }

            if (Rect.intersects(bird.getCollisionShape(), flight.getCollisionShape())) {
                isGameOver = true;
                return;
            }
        }
    }

    private void respawnBird(Bird bird) {
        bird.wasShot = false;
        bird.y = -bird.size - random.nextInt(screenY / 4);
        bird.x = random.nextInt(screenX - bird.size);
        bird.speed = random.nextInt((int)(8 * screenRatioY)) + (int)(5 * screenRatioY);
    }

    private void updateBackground() {
        int scrollX = (int)(5 * screenRatioX);
        int scrollY = (int)(5 * screenRatioY);

        if (flight.movingLeft) {
            background1.x += scrollX;
            background2.x += scrollX;
        }
        if (flight.movingRight) {
            background1.x -= scrollX;
            background2.x -= scrollX;
        }

        // Wrap-around ngang
        if (background1.x + screenX <= 0) background1.x = background2.x + screenX;
        else if (background1.x >= screenX) background1.x = background2.x - screenX;
        if (background2.x + screenX <= 0) background2.x = background1.x + screenX;
        else if (background2.x >= screenX) background2.x = background1.x - screenX;

        // Di chuyển dọc nhưng không vượt quá chiều dài background
        if (flight.movingUp) {
            if (background1.y + scrollY <= 0 && background2.y + scrollY <= 0) {
                background1.y += scrollY;
                background2.y += scrollY;
            }
        }
        if (flight.movingDown) {
            if (background1.y - scrollY >= -background1.background.getHeight() + screenY
                    && background2.y - scrollY >= -background2.background.getHeight() + screenY) {
                background1.y -= scrollY;
                background2.y -= scrollY;
            }
        }
    }

    private void draw() {
        if (!getHolder().getSurface().isValid()) return;

        Canvas canvas = getHolder().lockCanvas();
        canvas.drawColor(Color.BLACK);

        canvas.drawBitmap(background1.background, background1.x, background1.y, paint);
        canvas.drawBitmap(background2.background, background2.x, background2.y, paint);

        canvas.drawBitmap(flight.getFlight(), flight.x, flight.y, paint);
        canvas.drawText(score + "", screenX / 2f, 128, paint);

        if (isGameOver) {
            canvas.drawBitmap(flight.getDead(), flight.x, flight.y, paint);
            getHolder().unlockCanvasAndPost(canvas);
            saveIfHighScore();
            waitBeforeExiting();
            return;
        }

        // Vẽ chim
        for (Bird bird : birds) {
            canvas.drawBitmap(bird.getBird(), bird.x, bird.y, paint);
        }

        // Vẽ đạn
        for (Bullet bullet : bullets) {
            canvas.drawBitmap(bullet.bullet, bullet.x, bullet.y, paint);
        }

        getHolder().unlockCanvasAndPost(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                flight.movingLeft = x < screenX / 2;
                flight.movingRight = x >= screenX / 2;
                flight.movingUp = y < screenY / 2;
                flight.movingDown = y >= screenY / 2;
                break;

            case MotionEvent.ACTION_UP:
                flight.movingLeft = false;
                flight.movingRight = false;
                flight.movingUp = false;
                flight.movingDown = false;
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
