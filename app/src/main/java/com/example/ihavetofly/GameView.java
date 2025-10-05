package com.example.ihavetofly;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
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

    public GameView(GameActivity activity, int screenX, int screenY) {
        super(activity);
        this.activity = activity;
        this.screenX = screenX;
        this.screenY = screenY;

        prefs = activity.getSharedPreferences("game", GameActivity.MODE_PRIVATE);

        audioManager = GameAudioManager.getInstance(activity);
        audioManager.prepare();
        audioManager.playBackground();

        background = new Background(getResources(), R.drawable.background, screenY);
        flight = new Flight(screenX, screenY, getResources());

        Bitmap tmp = BitmapFactory.decodeResource(getResources(), R.drawable.space_ship_bloom);
        gameOverBitmap = Bitmap.createScaledBitmap(tmp, flight.width, flight.height, true);
        if (tmp != gameOverBitmap) tmp.recycle();

        volumeButton = new VolumeButton(activity, screenX, screenY);
        bullets = new ArrayList<>(MAX_BULLETS);
        random = new Random();

        initPaints();
        initIcons();
        initBirds();

        bomb = new Bomb(getResources(), screenX, screenY);
        coins = new ArrayList<>();

        loadHighScores();

        gameOverScreen = new GameOverScreen(getResources(), screenX, screenY);
        gameWinScreen = new GameWinScreen(getResources(), screenX, screenY);

        startTime = System.currentTimeMillis();

        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
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

        Bitmap scoreTmp = BitmapFactory.decodeResource(getResources(), R.drawable.score_nobg);
        scoreIcon = Bitmap.createScaledBitmap(scoreTmp, iconSize, iconSize, true);
        if (scoreTmp != scoreIcon) scoreTmp.recycle();

        Bitmap timeTmp = BitmapFactory.decodeResource(getResources(), R.drawable.clock);
        timeIcon = Bitmap.createScaledBitmap(timeTmp, iconSize, iconSize, true);
        if (timeTmp != timeIcon) timeTmp.recycle();

        Bitmap coinTmp = BitmapFactory.decodeResource(getResources(), R.drawable.coin1);
        coinIcon = Bitmap.createScaledBitmap(coinTmp, iconSize, iconSize, true);
        if (coinTmp != coinIcon) coinTmp.recycle();
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

        handleAutoShoot(currentTime);
        updateBullets(deltaTime);
        updateBirds(deltaTime);
        updateBomb(currentTime, deltaTime);
        updateCoins(deltaTime);
        checkCoinCollision();
        checkWinCondition();
    }

    private void handleAutoShoot(long currentTime) {
        boolean isMoving = flight.movingLeft || flight.movingRight || flight.movingUp || flight.movingDown;
        if (isMoving && currentTime - lastShootTime >= SHOOT_INTERVAL && bullets.size() < MAX_BULLETS) {
            newBullet();
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

                    Coin c = new Coin(getResources());
                    int spawnX = bird.x + bird.width / 2 - (c.width / 2);
                    int spawnY = bird.y + bird.height / 2 - (c.height / 2);
                    c.spawnAt(spawnX, spawnY);
                    coins.add(c);

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

            if (!isGameOver && !bird.wasShot && Rect.intersects(bird.getCollisionShape(), tempRect)) {
                isGameOver = true;
                isWin = false;
                saveHighScores();
                return;
            }
        }
    }

    private void respawnBird(Bird bird) {
        bird.wasShot = false;
        bird.y = -bird.height - random.nextInt(screenY / 3);
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

            if (!isGameOver && Rect.intersects(tempRect, bomb.getCollisionShape())) {
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

    private void checkCoinCollision() {
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

    private void newBullet() {
        Bullet b = new Bullet(getResources(), flight.x + flight.width / 2, flight.y, flight.width);
        bullets.add(b);
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

        for (Bird bird : birds)
            if (!bird.wasShot && bird.getBird() != null)
                canvas.drawBitmap(bird.getBird(), bird.x, bird.y, paint);

        for (Bullet bullet : bullets)
            if (bullet.bullet != null)
                canvas.drawBitmap(bullet.bullet, bullet.x, bullet.y, paint);

        if (bomb.active && bomb.getBitmap() != null)
            canvas.drawBitmap(bomb.getBitmap(), bomb.x, bomb.y, paint);

        for (Coin c : coins)
            if (c.active && c.getBitmap() != null)
                canvas.drawBitmap(c.getBitmap(), c.x, c.y, paint);

        drawGameplayHUD(canvas);
    }

    private void drawGameplayHUD(Canvas canvas) {
        float topMargin = 100;
        float leftPadding = screenX * 0.05f;
        float spacing = (screenX - 2 * leftPadding) / 3;

        gameplayTextPaint.setTextSize(screenY * 0.03f);
        gameplayTextPaint.setTextAlign(Paint.Align.LEFT);

        // Get font metrics for vertical centering
        Paint.FontMetrics fm = gameplayTextPaint.getFontMetrics();
        float textHeight = fm.bottom - fm.top;
        float textOffset = textHeight / 2 - fm.bottom; // to align text center with icon

        // --- SCORE ---
        float scoreX = leftPadding;
        float iconCenterY = topMargin + iconSize / 2f;
        float textBaseY = iconCenterY + textOffset;

        canvas.drawBitmap(scoreIcon, scoreX, topMargin, paint);
        canvas.drawText(String.valueOf(score),
                scoreX + iconSize + 15,
                textBaseY,
                gameplayTextPaint
        );

        // --- TIME ---
        float timeX = leftPadding + spacing;
        canvas.drawBitmap(timeIcon, timeX, topMargin, paint);
        canvas.drawText(gameTime + "s",
                timeX + iconSize + 15,
                textBaseY,
                gameplayTextPaint
        );

        // --- COINS ---
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
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void cleanup() {
        isPlaying = false;
        audioManager.release();
        if (gameOverScreen != null) gameOverScreen.cleanup();
        if (gameWinScreen != null) gameWinScreen.cleanup();
        if (scoreIcon != null && !scoreIcon.isRecycled()) scoreIcon.recycle();
        if (timeIcon != null && !timeIcon.isRecycled()) timeIcon.recycle();
        if (coinIcon != null && !coinIcon.isRecycled()) coinIcon.recycle();
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
        startTime = System.currentTimeMillis();
        bullets.clear();
        coins.clear();
        for (Bird b : birds) respawnBird(b);
    }
}