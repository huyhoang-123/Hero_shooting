package com.example.ihavetofly;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable {

    private Thread thread;
    private volatile boolean isPlaying = false;
    private volatile boolean isGameOver = false;
    public int screenX, screenY;
    public static float screenRatioX, screenRatioY;

    private List<Bullet> bullets;
    private Bird[] birds;
    private SharedPreferences prefs;
    private Random random;
    private Paint paint;
    private Paint scorePaint; // Paint riêng cho score
    private Flight flight;
    private GameActivity activity;
    private int score = 0;

    // Audio and UI components
    private GameAudioManager audioManager;
    private VolumeButton volumeButton;

    private long lastShootTime = 0;
    private static final long SHOOT_INTERVAL = 200; // Giảm interval
    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME = 1000 / TARGET_FPS;

    public float cameraX = 0;
    private Background background;
    private static final int MAX_BULLETS = 15; // Giảm số bullet tối đa

    // Cache objects để tránh GC
    private final Rect tempRect = new Rect();
    private long lastFrameTime = 0;

    public GameView(GameActivity activity, int screenX, int screenY) {
        super(activity);
        this.activity = activity;
        prefs = activity.getSharedPreferences("game", Context.MODE_PRIVATE);

        // Initialize audio manager
        audioManager = GameAudioManager.getInstance(activity);

        this.screenX = screenX;
        this.screenY = screenY;
        screenRatioX = 1920f / screenX;
        screenRatioY = 1080f / screenY;

        background = new Background(getResources(), R.drawable.background, screenY);
        flight = new Flight(this, getResources(), screenX, screenY);

        // Initialize volume button
        volumeButton = new VolumeButton(activity, screenX, screenY);

        // Sử dụng ArrayList với capacity cố định
        bullets = new ArrayList<>(MAX_BULLETS);

        // Khởi tạo Random trước khi dùng
        random = new Random();

        // Tối ưu Paint objects
        initPaints();

        // Tối ưu birds array
        initBirds();

        // Tối ưu SurfaceHolder
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {}

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cleanup();
            }
        });
    }

    // Remove the initSoundPool method since we're using AudioManager now

    private void initPaints() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true); // Smooth scaling

        scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scorePaint.setTextSize(screenY * 0.08f); // Responsive text size
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextAlign(Paint.Align.CENTER);
        scorePaint.setShadowLayer(4, 2, 2, Color.BLACK); // Text shadow
    }

    private void initBirds() {
        birds = new Bird[6]; // Giảm số birds
        int spacing = screenY / 6;
        for (int i = 0; i < birds.length; i++) {
            birds[i] = new Bird(getResources());
            respawnBird(birds[i]);
            birds[i].y -= i * spacing;
        }
    }

    @Override
    public void run() {
        lastFrameTime = System.currentTimeMillis();

        while (isPlaying) {
            long startTime = System.currentTimeMillis();

            update();
            draw();

            // Tối ưu frame rate
            long frameTime = System.currentTimeMillis() - startTime;
            if (frameTime < FRAME_TIME) {
                try {
                    Thread.sleep(FRAME_TIME - frameTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void update() {
        if (isGameOver) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastFrameTime) / 1000f;
        lastFrameTime = currentTime;

        // Update volume button
        volumeButton.update();

        // Update flight position
        flight.updatePosition(background.background.getWidth());

        // Update camera với smooth follow
        updateCamera();

        // Auto shoot khi di chuyển
        handleAutoShoot(currentTime);

        // Update bullets với Iterator để tránh ConcurrentModificationException
        updateBullets();

        // Update birds
        updateBirds();
    }

    private void updateCamera() {
        float centerStart = screenX / 2f;
        float targetCameraX;

        if (flight.x < centerStart) {
            targetCameraX = 0;
        } else if (flight.x + flight.width > background.background.getWidth() - centerStart) {
            targetCameraX = background.background.getWidth() - screenX;
        } else {
            targetCameraX = flight.x - centerStart + flight.width / 2f;
        }

        // Smooth camera follow
        cameraX += (targetCameraX - cameraX) * 0.1f;
    }

    private void handleAutoShoot(long currentTime) {
        boolean moved = flight.movingLeft || flight.movingRight;
        if (moved && currentTime - lastShootTime >= SHOOT_INTERVAL && bullets.size() < MAX_BULLETS) {
            flight.toShoot = 1;
            lastShootTime = currentTime;
        }
    }

    private void updateBullets() {
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            bullet.y -= bullet.speed;

            if (bullet.y < -bullet.height) {
                bulletIterator.remove();
                continue;
            }

            // Check collision với birds
            Rect bulletRect = bullet.getCollisionShape();
            for (Bird bird : birds) {
                if (!bird.wasShot && Rect.intersects(bird.getCollisionShape(), bulletRect)) {
                    score++;
                    respawnBird(bird);
                    bird.wasShot = true;
                    bulletIterator.remove();
                    break;
                }
            }
        }
    }

    private void updateBirds() {
        tempRect.set(flight.x, flight.y, flight.x + flight.width, flight.y + flight.height);

        for (Bird bird : birds) {
            bird.y += bird.speed;

            if (bird.y > screenY || bird.wasShot) {
                respawnBird(bird);
            }

            if (!isGameOver && !bird.wasShot &&
                    Rect.intersects(bird.getCollisionShape(), tempRect)) {
                isGameOver = true;
                return;
            }
        }
    }

    private void respawnBird(Bird bird) {
        bird.wasShot = false;
        int marginX = bird.size;
        int visibleWidth = Math.max(screenX, bird.size * 2);

        int minX = Math.max(0, (int) cameraX + marginX);
        int maxX = Math.min(background.background.getWidth() - bird.size,
                (int) (cameraX + visibleWidth - marginX));

        if (maxX > minX) {
            bird.x = minX + random.nextInt(maxX - minX);
        } else {
            bird.x = minX;
        }

        bird.y = -bird.size - random.nextInt(screenY / 3);
        bird.speed = random.nextInt(Math.max(1, (int)(8 * screenRatioY))) +
                Math.max(1, (int)(4 * screenRatioY));
    }

    private void draw() {
        if (!getHolder().getSurface().isValid()) return;

        Canvas canvas = getHolder().lockCanvas();
        if (canvas == null) return;

        try {
            // Clear với black
            canvas.drawColor(Color.BLACK);

            // Draw background
            canvas.drawBitmap(background.background, -cameraX, 0, paint);

            if (!isGameOver) {
                // Draw flight
                canvas.drawBitmap(flight.getFlight(), flight.x - cameraX, flight.y, paint);

                // Draw birds
                for (Bird bird : birds) {
                    if (!bird.wasShot) {
                        canvas.drawBitmap(bird.getBird(), bird.x - cameraX, bird.y, paint);
                    }
                }

                // Draw bullets
                for (Bullet bullet : bullets) {
                    canvas.drawBitmap(bullet.bullet, bullet.x - cameraX, bullet.y, paint);
                }
            } else {
                // Draw dead flight
                canvas.drawBitmap(flight.getDead(), flight.x - cameraX, flight.y, paint);
            }

            // Draw score
            canvas.drawText(String.valueOf(score), screenX / 2f, screenY * 0.1f, scorePaint);

            // Draw volume button - cố định trên màn hình
            volumeButton.draw(canvas);
            android.util.Log.d("GameView", "Volume button drawn");

        } finally {
            getHolder().unlockCanvasAndPost(canvas);
        }

        if (isGameOver) {
            handleGameOver();
        }
    }

    private void handleGameOver() {
        saveIfHighScore();
        cleanup();

        // Delay trước khi thoát
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                activity.runOnUiThread(() -> {
                    activity.startActivity(new Intent(activity, MainActivity.class));
                    activity.finish();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isGameOver) return false;

        float x = event.getX();
        float y = event.getY();
        int action = event.getAction();

        // Check volume button first - không cần cameraX
        if (volumeButton.handleTouch(x, y, action)) {
            android.util.Log.d("GameView", "Volume button handled touch");
            return true; // Volume button handled the touch
        }

        android.util.Log.d("GameView", "Touch not handled by volume button, handling flight movement");

        // Handle flight movement
        switch (action) {
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
        if (bullets.size() >= MAX_BULLETS) return;

        // Play shoot sound through audio manager
        audioManager.playShootSound();

        Bullet bullet = new Bullet(getResources(), screenX, screenY);
        bullet.x = flight.x + flight.width / 2 - bullet.width / 2;
        bullet.y = flight.y;
        bullets.add(bullet);
    }

    private void saveIfHighScore() {
        if (prefs.getInt("highscore", 0) < score) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("highscore", score);
            editor.apply();
        }
    }

    public void resume() {
        isPlaying = true;
        isGameOver = false;

        // Start background music
        audioManager.startBackgroundMusic();

        thread = new Thread(this);
        thread.start();
    }

    public void pause() {
        isPlaying = false;

        // Pause background music
        audioManager.pauseBackgroundMusic();

        if (thread != null) {
            try {
                thread.join(1000); // Timeout 1 giây
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void cleanup() {
        // Cleanup audio
        if (audioManager != null) {
            audioManager.cleanup();
        }

        // Cleanup volume button
        if (volumeButton != null) {
            volumeButton.cleanup();
        }

        // Clear bullets
        bullets.clear();

        // Cleanup static cached bitmaps
        Bullet.recycleCachedBitmap();

        // Cleanup background
        if (background != null) {
            background.recycle();
        }

        // Cleanup birds
        if (birds != null) {
            for (Bird bird : birds) {
                if (bird != null) {
                    bird.recycle();
                }
            }
        }
    }
}