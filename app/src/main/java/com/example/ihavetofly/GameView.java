package com.example.ihavetofly;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements Runnable {

    private volatile Thread thread;
    private final GameState gameState;
    private final GameActivity activity;
    private final SharedPreferences prefs;

    public int screenX, screenY;

    private final Flight flight;
    private final Background background;
    private final EntityManager entityManager;
    private final CollisionManager collisionManager;
    private final GameRenderer renderer;
    private final ScoreManager scoreManager;

    private final GameAudioManager audioManager;
    private final VolumeButton volumeButton;

    private ShieldEffect shieldEffect;
    private Bitmap gameOverBitmap;
    private GameOverScreen gameOverScreen;
    private GameWinScreen gameWinScreen;

    private long lastShootTime = 0;
    private long lastFrameTime = 0;

    private int totalCoinsCollected = 0;

    private Bitmap scoreIcon, timeIcon, coinIcon;
    private int iconSize;
    private Paint gameplayTextPaint;

    private boolean bossDefeatedThisSession = false;

    public GameView(GameActivity activity, int screenX, int screenY) {
        super(activity);
        this.activity = activity;
        this.screenX = screenX;
        this.screenY = screenY;

        gameState = new GameState();
        prefs = activity.getSharedPreferences("game", GameActivity.MODE_PRIVATE);

        audioManager = GameAudioManager.getInstance(activity);
        background = new Background(getResources(), R.drawable.infinity_bg, screenY);
        flight = new Flight(screenX, screenY, getResources());

        entityManager = new EntityManager(getResources(), screenX, screenY, flight);
        collisionManager = new CollisionManager();
        renderer = new GameRenderer(getResources(), screenX, screenY);
        scoreManager = new ScoreManager(prefs, gameState.currentLevel);

        Bitmap tmp = BitmapCache.get(getResources(), R.drawable.space_ship_bloom, 1);
        gameOverBitmap = Bitmap.createScaledBitmap(tmp, flight.width, flight.height, true);

        volumeButton = new VolumeButton(activity, screenX, screenY);

        gameState.sessionStartHighScore = scoreManager.getSessionStartHighScore();
        totalCoinsCollected = prefs.getInt("total_coins", 0);

        initIcons();
        initPaints();

        gameOverScreen = new GameOverScreen(getResources(), screenX, screenY);
        gameWinScreen = new GameWinScreen(getResources(), screenX, screenY);

        gameState.startTime = System.currentTimeMillis();

        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                audioManager.prepare();
                audioManager.playBackground();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cleanup();
            }
        });
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

    private void initPaints() {
        gameplayTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gameplayTextPaint.setColor(Color.WHITE);
        gameplayTextPaint.setTextAlign(Paint.Align.LEFT);
        gameplayTextPaint.setShadowLayer(4, 2, 2, Color.BLACK);
    }

    public void setLevel(int level) {
        gameState.setLevel(level);
        scoreManager.setLevel(level);
        entityManager.setLevel(level);
        gameState.sessionStartHighScore = scoreManager.getSessionStartHighScore();
    }

    @Override
    public void run() {
        lastFrameTime = System.currentTimeMillis();
        while (gameState.isPlaying) {
            long startTimeLoop = System.currentTimeMillis();
            update();
            draw();

            long targetFrameTime = (gameState.isGameOver || gameState.isWin)
                    ? 100
                    : GameConfig.FRAME_TIME;

            long frameTime = System.currentTimeMillis() - startTimeLoop;
            if (frameTime < targetFrameTime) {
                try {
                    Thread.sleep(targetFrameTime - frameTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void update() {
        if (gameState.isGameOver || gameState.isWin) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        gameState.gameTime = (currentTime - gameState.startTime) / 1000;

        float deltaTime = (currentTime - lastFrameTime) / 1000f;
        if (deltaTime <= 0f) deltaTime = 1f / 60f;
        if (deltaTime > 0.05f) deltaTime = 0.05f;
        lastFrameTime = currentTime;

        flight.updatePosition(deltaTime);
        background.update(deltaTime * gameState.speedMultiplier);

        updateBoss(currentTime, deltaTime);
        handleAutoShoot(currentTime);

        entityManager.updateBullets(deltaTime);

        // Level 3: No birds or bombs
        if (gameState.currentLevel != 3) {
            if (!entityManager.getBossManager().getBoss().active) {
                entityManager.updateBirds(deltaTime, gameState.speedMultiplier);
            }

            int bombSpeed = (int) (GameConfig.BASE_BOMB_SPEED * gameState.speedMultiplier);
            entityManager.updateBomb(currentTime, deltaTime, bombSpeed);
        }

        entityManager.updateCoins(deltaTime);
        entityManager.updatePowerUps(deltaTime);

        checkCollisions();
        checkWinCondition();
    }

    private void updateBoss(long currentTime, float deltaTime) {
        BossManager bossManager = entityManager.getBossManager();

        if (gameState.currentLevel == 3) {
            // Level 3: Boss spawns immediately
            if (!bossManager.getBoss().active && !bossDefeatedThisSession) {
                bossManager.spawnBoss();
            }
        } else {
            // Level 1 & 2: Normal boss spawn logic
            boolean allBirdsGone = entityManager.areAllBirdsGone();

            if (bossManager.shouldSpawnBoss(currentTime)) {
                if (allBirdsGone) {
                    bossManager.spawnBoss();
                } else {
                    bossManager.setWaitingForBirdsToClear(true);
                }
            } else if (bossManager.isWaitingForBirdsToClear() && allBirdsGone) {
                bossManager.spawnBoss();
            }
        }

        bossManager.update(deltaTime, screenX, screenY, getResources());

        int bossScore = bossManager.checkBossDestroyed();
        if (bossScore > 0) {
            gameState.score += bossScore;
            bossDefeatedThisSession = true;
        }
    }

    private void handleAutoShoot(long currentTime) {
        boolean isMoving = flight.movingLeft || flight.movingRight ||
                flight.movingUp || flight.movingDown;

        if (isMoving && currentTime - lastShootTime >= GameConfig.SHOOT_INTERVAL &&
                entityManager.getBullets().size() < GameConfig.MAX_BULLETS) {
            entityManager.addBullet(getResources());
            audioManager.playShootSound();
            lastShootTime = currentTime;
        }
    }

    private void checkCollisions() {
        boolean bulletHit = collisionManager.checkBulletCollisions(
                entityManager.getBullets(),
                entityManager.getBirds(),
                entityManager.getBossManager().getBoss(),
                entityManager.getCoins(),
                entityManager.getPowerUps(),
                getResources()
        );

        if (bulletHit) gameState.score++;

        // Level 3: Only check boss collisions
        if (gameState.currentLevel == 3) {
            if (collisionManager.checkBossCollisions(
                    entityManager.getBossManager().getBoss(),
                    entityManager.getBossManager().getRockets(),
                    entityManager.getBossManager().getBossBullets(),
                    flight)) {
                gameOver();
                return;
            }
        } else {
            // Level 1 & 2: Check all collisions
            if (collisionManager.checkBirdCollision(entityManager.getBirds(), flight, screenY) ||
                    collisionManager.checkBombCollision(entityManager.getBomb(), flight) ||
                    collisionManager.checkBossCollisions(
                            entityManager.getBossManager().getBoss(),
                            entityManager.getBossManager().getRockets(),
                            entityManager.getBossManager().getBossBullets(),
                            flight)) {
                gameOver();
                return;
            }
        }

        int coinsCollected = collisionManager.checkCoinCollisions(entityManager.getCoins(), flight);
        if (coinsCollected > 0) {
            flight.collectCoin();
            totalCoinsCollected += coinsCollected;
            prefs.edit().putInt("total_coins", totalCoinsCollected).apply();
        }

        ShieldEffect newShield = collisionManager.checkPowerUpCollisions(
                entityManager.getPowerUps(), flight);
        if (newShield != null) {
            shieldEffect = newShield;
        }
    }

    private void checkWinCondition() {
        if (!gameState.hasShownWinScreen && bossDefeatedThisSession) {
            gameState.isWin = true;
            gameState.hasShownWinScreen = true;

            if (gameState.currentLevel == 3) {
                // Level 3: Clear all high scores on win
                scoreManager.clearAllHighScores();
            } else {
                scoreManager.saveHighScores(gameState.score, gameState.gameTime);
            }
        }
    }

    private void gameOver() {
        gameState.isGameOver = true;
        gameState.isWin = false;

        if (gameState.currentLevel == 3) {
            // Level 3: Don't save score on loss
        } else {
            scoreManager.saveHighScores(gameState.score, gameState.gameTime);
        }
    }

    private void draw() {
        if (!getHolder().getSurface().isValid()) return;
        Canvas canvas = getHolder().lockCanvas();
        if (canvas == null) return;

        try {
            if (background != null && background.background != null) {
                canvas.drawBitmap(background.background, 0, background.getY1(), renderer.getPaint());
                canvas.drawBitmap(background.background, 0, background.getY2(), renderer.getPaint());
            }

            if (!gameState.isGameOver && !gameState.isWin) {
                drawGameplay(canvas);
            } else if (gameState.isGameOver) {
                boolean canResume = totalCoinsCollected >= GameConfig.RESUME_COST;
                gameOverScreen.draw(canvas, gameState.score, gameOverBitmap,
                        flight.x, flight.y, renderer.getPaint(), canResume);
            } else if (gameState.isWin) {
                if (gameState.currentLevel == 3) {
                    gameWinScreen.draw(canvas, scoreManager.getHighScores(), renderer.getPaint(), true);
                } else {
                    gameWinScreen.draw(canvas, scoreManager.getHighScores(), renderer.getPaint(), false);
                }
            }

            if (volumeButton != null) {
                volumeButton.draw(canvas);
            }
        } finally {
            try {
                getHolder().unlockCanvasAndPost(canvas);
            } catch (Exception ignored) {}
        }
    }

    private void drawGameplay(Canvas canvas) {
        if (flight != null && flight.getFlight() != null) {
            canvas.drawBitmap(flight.getFlight(), flight.x, flight.y, renderer.getPaint());
        }

        if (shieldEffect != null) {
            shieldEffect.updatePosition(flight.x + flight.width / 2f, flight.y + flight.height / 2f);
            if (!shieldEffect.isExpired()) {
                shieldEffect.draw(canvas);
            } else {
                shieldEffect = null;
            }
        }

        renderer.drawGameplay(canvas, background, flight, shieldEffect, entityManager, gameState.score, gameState.gameTime);

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

        canvas.drawBitmap(scoreIcon, scoreX, topMargin, renderer.getPaint());
        canvas.drawText(String.valueOf(gameState.score),
                scoreX + iconSize + 15,
                textBaseY,
                gameplayTextPaint);

        float timeX = leftPadding + spacing;
        canvas.drawBitmap(timeIcon, timeX, topMargin, renderer.getPaint());
        canvas.drawText(gameState.gameTime + "s",
                timeX + iconSize + 15,
                textBaseY,
                gameplayTextPaint);

        float coinX = leftPadding + spacing * 2;
        canvas.drawBitmap(coinIcon, coinX, topMargin, renderer.getPaint());
        canvas.drawText(String.valueOf(flight.getCoinScore()),
                coinX + iconSize + 15,
                textBaseY,
                gameplayTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (volumeButton != null && volumeButton.handleTouch(event.getX(), event.getY())) {
            audioManager.setAllMuted(volumeButton.isSfxMuted());
            audioManager.setMusicMuted(volumeButton.isMusicMuted());
            return true;
        }

        if (gameState.isGameOver) {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP) {
                boolean canResume = totalCoinsCollected >= GameConfig.RESUME_COST;
                boolean handled = gameOverScreen.handleTouch(event.getX(), event.getY(),
                        this::resetGame,
                        this::resumeGame,
                        () -> activity.finish(),
                        canResume);
                if (handled) {
                    return true;
                }
            }
        } else if (gameState.isWin) {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP) {
                boolean handled = gameWinScreen.handleTouch(event.getX(), event.getY(),
                        this::resetGame,
                        this::nextLevel,
                        null);
                if (handled) {
                    return true;
                }
            }
        } else {
            handleGameplayTouch(event);
        }
        return true;
    }

    private void handleGameplayTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                flight.movingLeft = event.getX() < screenX / 2;
                flight.movingRight = event.getX() > screenX / 2;
                flight.movingUp = event.getY() < screenY / 2;
                flight.movingDown = event.getY() > screenY / 2;
                break;
            case MotionEvent.ACTION_UP:
                flight.movingLeft = flight.movingRight =
                        flight.movingUp = flight.movingDown = false;
                break;
        }
    }

    private void resumeGame() {
        if (totalCoinsCollected >= GameConfig.RESUME_COST) {
            totalCoinsCollected -= GameConfig.RESUME_COST;
            prefs.edit().putInt("total_coins", totalCoinsCollected).apply();

            gameState.isGameOver = false;
            gameState.isWin = false;
            gameState.isPlaying = true;
            flight.resetPosition();

            flight.activateShield();
            float shieldRadius = Math.max(flight.width, flight.height) * 1.2f;
            shieldEffect = new ShieldEffect(
                    flight.x + flight.width / 2f,
                    flight.y + flight.height / 2f,
                    shieldRadius
            );

            lastFrameTime = System.currentTimeMillis();

            if (thread == null || !thread.isAlive()) {
                thread = new Thread(this);
                thread.start();
            }
        }
    }

    private void nextLevel() {
        int nextLevel = gameState.currentLevel + 1;
        if (nextLevel <= 3) {
            activity.startLevel(nextLevel);
        } else {
            activity.finish();
        }
    }

    private void resetGame() {
        gameState.isGameOver = false;
        gameState.isWin = false;
        gameState.isPlaying = true;

        gameState.reset();
        flight.resetCoinScore();
        flight.resetPowerUps();
        flight.resetPosition();
        shieldEffect = null;
        entityManager.reset(gameState.speedMultiplier);
        gameState.sessionStartHighScore = scoreManager.getSessionStartHighScore();
        bossDefeatedThisSession = false;
        lastFrameTime = System.currentTimeMillis();

        if (thread == null || !thread.isAlive()) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void resume() {
        gameState.isPlaying = true;
        thread = new Thread(this);
        thread.start();
    }

    public void pause() {
        try {
            gameState.isPlaying = false;
            if (thread != null) thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void cleanup() {
        gameState.isPlaying = false;
        if (audioManager != null) audioManager.release();
        if (gameOverScreen != null) gameOverScreen.cleanup();
        if (gameWinScreen != null) gameWinScreen.cleanup();
        if (renderer != null) renderer.cleanup();
        if (scoreIcon != null && !scoreIcon.isRecycled()) scoreIcon.recycle();
        if (timeIcon != null && !timeIcon.isRecycled()) timeIcon.recycle();
        if (coinIcon != null && !coinIcon.isRecycled()) coinIcon.recycle();
        if (gameOverBitmap != null && !gameOverBitmap.isRecycled()) gameOverBitmap.recycle();

        Bird.clearCache();
        Coin.clearCache();
        Bomb.clearCache();
        PowerUp.clearCache();
        BossManager.clearCache();
        Bullet.clearCache();
    }
}