package com.example.ihavetofly;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
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
    public volatile boolean isGameOver = false;
    private boolean isWin = false;

    public int screenX, screenY;

    private List<Bullet> bullets;
    private Bird[] birds;
    private SharedPreferences prefs;
    private Random random;
    private Paint paint;
    private Flight flight;
    private GameActivity activity;
    private int score = 0;

    private GameAudioManager audioManager;
    private VolumeButton volumeButton;

    private long lastShootTime = 0;
    private static final long SHOOT_INTERVAL = 200;
    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME = 1000 / TARGET_FPS;

    private Background background;
    private static final int MAX_BULLETS = 15;

    private Bomb bomb;
    private long lastBombSpawnTime = 0;
    private static final long BOMB_SPAWN_INTERVAL = 10000;
    private static final int bombSpeed = 400;

    private List<Coin> coins;
    private List<PowerUp> powerUps;
    private ShieldEffect shieldEffect;

    private Bitmap gameOverBitmap;
    private Paint gameplayTextPaint;

    private final Rect tempRect = new Rect();
    private long lastFrameTime = 0;

    private long gameTime = 0;
    private long startTime = 0;

    private List<Integer> highScores;

    private GameOverScreen gameOverScreen;
    private GameWinScreen gameWinScreen;

    private Bitmap scoreIcon, timeIcon, coinIcon;
    private int iconSize;

    private BossManager bossManager;

    public GameView(GameActivity activity, int screenX, int screenY) {
        super(activity);
        this.activity = activity;
        this.screenX = screenX;
        this.screenY = screenY;

        prefs = activity.getSharedPreferences("game", GameActivity.MODE_PRIVATE);

        audioManager = GameAudioManager.getInstance(activity);

        background = new Background(getResources(), R.drawable.background, screenY);
        flight = new Flight(screenX, screenY, getResources());

        Bitmap tmp = BitmapCache.get(getResources(), R.drawable.space_ship_bloom, 1);
        gameOverBitmap = Bitmap.createScaledBitmap(tmp, flight.width, flight.height, true);

        volumeButton = new VolumeButton(activity, screenX, screenY);
        bullets = new ArrayList<>(MAX_BULLETS);
        random = new Random();

        initPaints();
        initIcons();

        // IMPORTANT: bossManager must exist before initBirds() because respawnBird() uses it
        bossManager = new BossManager(getResources(), screenX, screenY);

        initBirds();

        bomb = new Bomb(getResources(), screenX, screenY);
        coins = new ArrayList<>();
        powerUps = new ArrayList<>();

        loadHighScores();

        gameOverScreen = new GameOverScreen(getResources(), screenX, screenY);
        gameWinScreen = new GameWinScreen(getResources(), screenX, screenY);

        startTime = System.currentTimeMillis();

        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                audioManager.prepare();
                audioManager.playBackground();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cleanup();
            }
        });
    }

    private void initPaints() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);

        gameplayTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gameplayTextPaint.setColor(Color.WHITE);
        gameplayTextPaint.setTextAlign(Paint.Align.LEFT);
        gameplayTextPaint.setShadowLayer(4, 2, 2, Color.BLACK);
    }

    private void initIcons() {
        iconSize = (int) (screenY * 0.04f);

        Bitmap scoreTmp = BitmapCache.get(getResources(), R.drawable.score_nobg, 1);
        scoreIcon = Bitmap.createScaledBitmap(scoreTmp, iconSize, iconSize, true);

        Bitmap timeTmp = BitmapCache.get(getResources(), R.drawable.clock, 1);
        timeIcon = Bitmap.createScaledBitmap(timeTmp, iconSize, iconSize, true);

        Bitmap coinTmp = BitmapCache.get(getResources(), R.drawable.coin1, 1);
        coinIcon = Bitmap.createScaledBitmap(coinTmp, iconSize, iconSize, true);
    }

    private void initBirds() {
        birds = new Bird[6];
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
            long startTimeLoop = System.currentTimeMillis();
            update();
            draw();
            long frameTime = System.currentTimeMillis() - startTimeLoop;
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
        if (isGameOver || isWin) return;

        long currentTime = System.currentTimeMillis();
        gameTime = (currentTime - startTime) / 1000;

        float deltaTime = (currentTime - lastFrameTime) / 1000f;
        if (deltaTime <= 0f) deltaTime = 1f / 60f;
        lastFrameTime = currentTime;

        flight.updatePosition(deltaTime);
        background.update(deltaTime);

        // --- FIXED Boss spawn logic ---
        // Compute whether all birds are off-screen (call this every frame)
        boolean allBirdsGone = true;
        if (birds != null) {
            for (Bird bird : birds) {
                if (bird != null && bird.y < screenY) {
                    allBirdsGone = false;
                    break;
                }
            }
        }

        // If it's time to attempt spawning (BossManager decides on time)
        boolean timeToSpawn = bossManager != null && bossManager.shouldSpawnBoss(currentTime);

        if (timeToSpawn) {
            if (allBirdsGone) {
                // birds are clear, spawn boss now
                bossManager.spawnBoss();
            } else {
                // not clear yet -> wait until they are cleared
                bossManager.setWaitingForBirdsToClear(true);
            }
        } else {
            // If we were waiting earlier, and birds are now cleared -> spawn immediately
            if (bossManager != null && bossManager.isWaitingForBirdsToClear() && allBirdsGone) {
                bossManager.spawnBoss();
            }
        }
        // --- end fixed boss spawn logic ---

        // Debug: Log boss status every 5 seconds
        if (gameTime > 0 && gameTime % 5 == 0) {
            Boss boss = bossManager.getBoss();
            android.util.Log.d("BOSS_DEBUG", "GameTime=" + gameTime +
                    ", BossActive=" + (boss != null && boss.active) +
                    ", Waiting=" + (bossManager != null && bossManager.isWaitingForBirdsToClear()));
        }

        // Update boss system
        if (bossManager != null)
            bossManager.update(deltaTime, screenX, screenY, getResources());

        // Check boss score reward
        int bossScore = bossManager != null ? bossManager.checkBossDestroyed() : 0;
        if (bossScore > 0) {
            score += bossScore;
            android.util.Log.d("BOSS_DEBUG", "Boss destroyed! Added " + bossScore + " points");
        }

        handleAutoShoot(currentTime);
        updateBullets(deltaTime);

        // Only update birds if boss is not active
        if (bossManager == null || !bossManager.getBoss().active) {
            updateBirds(deltaTime);
        } else {
            android.util.Log.d("BOSS_DEBUG", "Boss is active, birds not updating");
        }

        updateBomb(currentTime, deltaTime);
        updateCoins(deltaTime);
        updatePowerUps(deltaTime);
        checkCoinCollision();
        checkPowerUpCollision();
        checkBossCollisions();
        checkWinCondition();
    }

    private void handleAutoShoot(long currentTime) {
        boolean isMoving = flight.movingLeft || flight.movingRight || flight.movingUp || flight.movingDown;
        if (isMoving && currentTime - lastShootTime >= SHOOT_INTERVAL && bullets.size() < MAX_BULLETS) {
            newBullet();
            audioManager.playShootSound();
            lastShootTime = currentTime;
        }
    }

    private void updateBullets(float deltaTime) {
        if (bullets.isEmpty()) return;

        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet bullet = it.next();
            bullet.y -= (int) (bullet.speed * deltaTime);
            if (bullet.y < -bullet.height) {
                it.remove();
                continue;
            }

            Rect bulletRect = bullet.getCollisionShape();
            for (Bird bird : birds) {
                if (!bird.wasShot && Rect.intersects(bird.getCollisionShape(), bulletRect)) {
                    score++;
                    bird.wasShot = true;

                    int dropChance = random.nextInt(100);
                    if (dropChance < 30) {
                        Coin c = new Coin(getResources());
                        int spawnX = bird.x + bird.width / 2 - (c.width / 2);
                        int spawnY = bird.y + bird.height / 2 - (c.height / 2);
                        c.spawnAt(spawnX, spawnY);
                        coins.add(c);
                    } else if (dropChance < 55) {
                        PowerUp p = new PowerUp(getResources(), PowerUp.TYPE_DOUBLE_BULLET);
                        int spawnX = bird.x + bird.width / 2 - (p.width / 2);
                        int spawnY = bird.y + bird.height / 2 - (p.height / 2);
                        p.spawnAt(spawnX, spawnY);
                        powerUps.add(p);
                    } else if (dropChance < 80) {
                        PowerUp p = new PowerUp(getResources(), PowerUp.TYPE_KUNAI);
                        int spawnX = bird.x + bird.width / 2 - (p.width / 2);
                        int spawnY = bird.y + bird.height / 2 - (p.height / 2);
                        p.spawnAt(spawnX, spawnY);
                        powerUps.add(p);
                    } else {
                        PowerUp p = new PowerUp(getResources(), PowerUp.TYPE_SHIELD);
                        int spawnX = bird.x + bird.width / 2 - (p.width / 2);
                        int spawnY = bird.y + bird.height / 2 - (p.height / 2);
                        p.spawnAt(spawnX, spawnY);
                        powerUps.add(p);
                    }

                    respawnBird(bird);
                    it.remove();
                    break;
                }
            }
        }
    }

    private void updateBirds(float deltaTime) {
        tempRect.set(flight.x, flight.y, flight.x + flight.width, flight.y + flight.height);

        for (Bird bird : birds) {
            bird.y += (int) (bird.speed * deltaTime);
            bird.updateFrame();

            if (bird.y > screenY || bird.wasShot)
                respawnBird(bird);

            if (!isGameOver && !bird.wasShot && !flight.hasShield() && Rect.intersects(bird.getCollisionShape(), tempRect)) {
                isGameOver = true;
                isWin = false;
                saveHighScores();
                return;
            }
        }
    }

    private void respawnBird(Bird bird) {
        // Don't respawn birds if we're waiting for boss to spawn
        if (bossManager != null && bossManager.isWaitingForBirdsToClear()) {
            bird.wasShot = false;
            bird.y = screenY + bird.height; // Move far off screen
            return;
        }

        // Don't respawn birds if boss is active
        if (bossManager != null && bossManager.getBoss() != null && bossManager.getBoss().active) {
            bird.wasShot = false;
            bird.y = screenY + bird.height; // Move far off screen
            return;
        }

        bird.wasShot = false;
        bird.y = -bird.height - random.nextInt(Math.max(1, screenY / 3));
        int range = screenX / 4;
        int spawnX = flight.x + random.nextInt(range * 2) - range;
        spawnX = Math.max(0, Math.min(spawnX, screenX - bird.width));
        bird.x = spawnX;
        bird.speed = 200 + random.nextInt(150);
    }

    private void updateBomb(long currentTime, float deltaTime) {
        if (!bomb.active && currentTime - lastBombSpawnTime >= BOMB_SPAWN_INTERVAL) {
            bomb.spawn(screenX, false);
            lastBombSpawnTime = currentTime;
        }

        if (bomb.active) {
            bomb.update(deltaTime, bombSpeed);

            if (bomb.y > screenY) {
                bomb.clear();
            }

            if (!isGameOver && !flight.hasShield() && Rect.intersects(tempRect, bomb.getCollisionShape())) {
                isGameOver = true;
                isWin = false;
                saveHighScores();
            }
        }
    }

    private void updateCoins(float deltaTime) {
        Iterator<Coin> it = coins.iterator();
        while (it.hasNext()) {
            Coin c = it.next();
            c.update(deltaTime, screenY);
            if (!c.active) it.remove();
        }
    }

    private void updatePowerUps(float deltaTime) {
        Iterator<PowerUp> it = powerUps.iterator();
        while (it.hasNext()) {
            PowerUp p = it.next();
            p.update(deltaTime, screenY);
            if (!p.active) it.remove();
        }
    }

    private void checkCoinCollision() {
        tempRect.set(flight.x, flight.y, flight.x + flight.width, flight.y + flight.height);
        Iterator<Coin> it = coins.iterator();
        while (it.hasNext()) {
            Coin c = it.next();
            if (c.active && Rect.intersects(tempRect, c.getCollisionShape())) {
                flight.collectCoin();
                c.clear();
                it.remove();
            }
        }
    }

    private void checkPowerUpCollision() {
        tempRect.set(flight.x, flight.y, flight.x + flight.width, flight.y + flight.height);
        Iterator<PowerUp> it = powerUps.iterator();
        while (it.hasNext()) {
            PowerUp p = it.next();
            if (p.active && Rect.intersects(tempRect, p.getCollisionShape())) {
                if (p.type == PowerUp.TYPE_DOUBLE_BULLET) {
                    flight.activateDoubleBullet();
                } else if (p.type == PowerUp.TYPE_KUNAI) {
                    flight.activateKunai();
                } else if (p.type == PowerUp.TYPE_SHIELD) {
                    flight.activateShield();
                    float shieldRadius = Math.max(flight.width, flight.height) * 1.2f;
                    shieldEffect = new ShieldEffect(
                            flight.x + flight.width / 2f,
                            flight.y + flight.height / 2f,
                            shieldRadius
                    );
                }
                p.clear();
                it.remove();
            }
        }
    }

    private void checkBossCollisions() {
        Boss boss = bossManager.getBoss();

        if (!boss.active || boss.isExploding) return;

        tempRect.set(flight.x, flight.y, flight.x + flight.width, flight.y + flight.height);

        // Check flight collision with boss
        if (!flight.hasShield() && Rect.intersects(boss.getCollisionShape(), tempRect)) {
            isGameOver = true;
            isWin = false;
            saveHighScores();
            return;
        }

        // Check flight collision with rockets
        for (Rocket rocket : bossManager.getRockets()) {
            if (rocket.active && !flight.hasShield() && Rect.intersects(rocket.getCollisionShape(), tempRect)) {
                isGameOver = true;
                isWin = false;
                saveHighScores();
                return;
            }
        }
    }

    private void newBullet() {
        boolean useKunai = flight.hasKunai();

        if (flight.hasDoubleBullet()) {
            int offset = flight.width / 4;
            Bullet b1 = new Bullet(getResources(), flight.x + offset, flight.y, flight.width, useKunai);
            Bullet b2 = new Bullet(getResources(), flight.x + flight.width - offset, flight.y, flight.width, useKunai);
            bullets.add(b1);
            bullets.add(b2);
        } else {
            Bullet b = new Bullet(getResources(), flight.x + flight.width / 2, flight.y, flight.width, useKunai);
            bullets.add(b);
        }
    }

    private void saveHighScores() {
        int highScore = prefs.getInt("high_score", 0);
        if (score > highScore) {
            prefs.edit().putInt("high_score", score).apply();
        }

        String scoresStr = prefs.getString("high_scores", "");
        List<Integer> list = new ArrayList<>();
        if (!scoresStr.isEmpty()) {
            for (String s : scoresStr.split(",")) list.add(Integer.parseInt(s));
        }
        list.add(score);
        list.sort((a, b) -> b - a);
        if (list.size() > 6) list = list.subList(0, 6);

        highScores = new ArrayList<>(list);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i));
        }
        prefs.edit().putString("high_scores", sb.toString()).apply();
    }

    private void loadHighScores() {
        String scoresStr = prefs.getString("high_scores", "");
        highScores = new ArrayList<>();
        if (!scoresStr.isEmpty()) {
            for (String s : scoresStr.split(",")) highScores.add(Integer.parseInt(s));
        }
    }

    private void checkWinCondition() {
        int highScore = prefs.getInt("high_score", 0);
        if (score > highScore) {
            isWin = true;
            saveHighScores();
        }
    }

    private void draw() {
        if (!getHolder().getSurface().isValid()) return;
        Canvas canvas = getHolder().lockCanvas();
        if (canvas == null) return;

        try {
            if (background != null && background.background != null) {
                canvas.drawBitmap(background.background, 0, background.getY1(), paint);
                canvas.drawBitmap(background.background, 0, background.getY2(), paint);
            }

            if (!isGameOver && !isWin) {
                drawGameplay(canvas);
            } else if (isGameOver) {
                gameOverScreen.draw(canvas, score, gameOverBitmap, flight.x, flight.y, paint);
            } else if (isWin) {
                gameWinScreen.draw(canvas, highScores, paint);
            }

            if (volumeButton != null)
                volumeButton.draw(canvas);

        } finally {
            try {
                getHolder().unlockCanvasAndPost(canvas);
            } catch (Exception ignored) {
            }
        }
    }

    private void drawGameplay(Canvas canvas) {
        if (flight != null && flight.getFlight() != null)
            canvas.drawBitmap(flight.getFlight(), flight.x, flight.y, paint);

        if (shieldEffect != null) {
            shieldEffect.updatePosition(
                    flight.x + flight.width / 2f,
                    flight.y + flight.height / 2f
            );
            shieldEffect.draw(canvas);
            if (shieldEffect.isExpired()) {
                shieldEffect = null;
            }
        }

        // Only draw birds if boss is not active
        if (!bossManager.getBoss().active) {
            for (Bird bird : birds)
                if (!bird.wasShot && bird.getBird() != null)
                    canvas.drawBitmap(bird.getBird(), bird.x, bird.y, paint);
        }

        // Draw boss and rockets
        Boss boss = bossManager.getBoss();
        if (boss.active && boss.getBitmap() != null) {
            canvas.drawBitmap(boss.getBitmap(), boss.x, boss.y, paint);
        }

        for (Rocket rocket : bossManager.getRockets()) {
            if (rocket.active && rocket.getBitmap() != null) {
                canvas.drawBitmap(rocket.getBitmap(), rocket.x, rocket.y, paint);
            }
        }

        for (Bullet bullet : bullets)
            if (bullet.bullet != null)
                canvas.drawBitmap(bullet.bullet, bullet.x, bullet.y, paint);

        if (bomb.active && bomb.getBitmap() != null)
            canvas.drawBitmap(bomb.getBitmap(), bomb.x, bomb.y, paint);

        for (Coin c : coins)
            if (c.active && c.getBitmap() != null)
                canvas.drawBitmap(c.getBitmap(), c.x, c.y, paint);

        for (PowerUp p : powerUps)
            if (p.active && p.getBitmap() != null)
                canvas.drawBitmap(p.getBitmap(), p.x, p.y, paint);

        drawGameplayHUD(canvas);
    }

    private void drawGameplayHUD(Canvas canvas) {
        float topMargin = 100;
        float leftPadding = screenX * 0.05f;
        float spacing = (screenX - 2 * leftPadding) / 3;

        gameplayTextPaint.setTextSize(screenY * 0.03f);
        gameplayTextPaint.setTextAlign(Paint.Align.LEFT);

        Paint.FontMetrics fm = gameplayTextPaint.getFontMetrics();
        float textHeight = fm.bottom - fm.top;
        float textOffset = textHeight / 2 - fm.bottom;

        float scoreX = leftPadding;
        float iconCenterY = topMargin + iconSize / 2f;
        float textBaseY = iconCenterY + textOffset;

        canvas.drawBitmap(scoreIcon, scoreX, topMargin, paint);
        canvas.drawText(String.valueOf(score),
                scoreX + iconSize + 15,
                textBaseY,
                gameplayTextPaint
        );

        float timeX = leftPadding + spacing;
        canvas.drawBitmap(timeIcon, timeX, topMargin, paint);
        canvas.drawText(gameTime + "s",
                timeX + iconSize + 15,
                textBaseY,
                gameplayTextPaint
        );

        float coinX = leftPadding + spacing * 2;
        canvas.drawBitmap(coinIcon, coinX, topMargin, paint);
        canvas.drawText(String.valueOf(flight.getCoinScore()),
                coinX + iconSize + 15,
                textBaseY,
                gameplayTextPaint
        );
    }

    public void resume() {
        isPlaying = true;
        thread = new Thread(this);
        thread.start();
    }

    public void pause() {
        try {
            isPlaying = false;
            if (thread != null) thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void cleanup() {
        isPlaying = false;
        if (audioManager != null) audioManager.release();
        if (gameOverScreen != null) gameOverScreen.cleanup();
        if (gameWinScreen != null) gameWinScreen.cleanup();
        if (scoreIcon != null && !scoreIcon.isRecycled()) scoreIcon.recycle();
        if (timeIcon != null && !timeIcon.isRecycled()) timeIcon.recycle();
        if (coinIcon != null && !coinIcon.isRecycled()) coinIcon.recycle();
        if (gameOverBitmap != null && !gameOverBitmap.isRecycled()) gameOverBitmap.recycle();

        Bird.clearCache();
        Coin.clearCache();
        Bomb.clearCache();
        PowerUp.clearCache();
        BossManager.clearCache();
        BitmapCache.clear();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (volumeButton != null) {
            if (volumeButton.handleTouch(event.getX(), event.getY())) {
                audioManager.setAllMuted(volumeButton.isSfxMuted());
                audioManager.setMusicMuted(volumeButton.isMusicMuted());
                return true;
            }
        }

        if (isGameOver) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                return gameOverScreen.handleTouch(event.getX(), event.getY(),
                        this::resetGame,
                        () -> {
                            isGameOver = false;
                            isWin = false;
                        },
                        () -> activity.finish());
            }
        } else if (isWin) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                return gameWinScreen.handleTouch(event.getX(), event.getY(),
                        this::resetGame,
                        () -> {
                            isGameOver = false;
                            isWin = false;
                        },
                        () -> activity.finish());
            }
        } else {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    flight.movingLeft = event.getX() < screenX / 2;
                    flight.movingRight = event.getX() > screenX / 2;
                    flight.movingUp = event.getY() < screenY / 2;
                    flight.movingDown = event.getY() > screenY / 2;
                    break;
                case MotionEvent.ACTION_UP:
                    flight.movingLeft = flight.movingRight = flight.movingUp = flight.movingDown = false;
                    break;
            }
        }
        return true;
    }

    private void resetGame() {
        isGameOver = false;
        isWin = false;
        score = 0;
        flight.resetCoinScore();
        flight.resetPowerUps();
        shieldEffect = null;
        startTime = System.currentTimeMillis();
        bullets.clear();
        coins.clear();
        powerUps.clear();
        if (bossManager != null) bossManager.reset();
        for (Bird b : birds) respawnBird(b);
    }
}
