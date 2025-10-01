package com.example.ihavetofly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    private Paint scorePaint;
    private Flight flight;
    private GameActivity activity;
    private int score = 0;

    private GameAudioManager audioManager;
    private VolumeButton volumeButton;

    private long lastShootTime = 0;
    private static final long SHOOT_INTERVAL = 200; // ms
    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME = 1000 / TARGET_FPS;

    public float cameraX = 0;
    private Background background;
    private static final int MAX_BULLETS = 15;

    private final Rect tempRect = new Rect();
    private long lastFrameTime = 0;

    private Bitmap gameOverBitmap;
    private Rect exitButtonRect;
    private Paint gameOverTextPaint;
    private Paint exitButtonPaint;

    // --- Bomb ---
    private Bomb bomb;
    private long lastBombSpawnTime = 0;
    private static final long BOMB_SPAWN_INTERVAL = 10000; // 10s
    private static final long BOMB_DURATION = 5000;         // 5s
    private static final int bombSpeed = 400;              // pixels/s

    public GameView(GameActivity activity, int screenX, int screenY) {
        super(activity);
        this.activity = activity;
        prefs = activity.getSharedPreferences("game", GameActivity.MODE_PRIVATE);

        audioManager = GameAudioManager.getInstance(activity);
        audioManager.prepare();
        audioManager.playBackground();

        this.screenX = screenX;
        this.screenY = screenY;

        screenRatioX = 1920f / screenX;
        screenRatioY = 1080f / screenY;

        background = new Background(getResources(), R.drawable.background, screenY);
        flight = new Flight(this, screenY, getResources());

        Bitmap tmp = BitmapFactory.decodeResource(getResources(), R.drawable.space_ship_bloom);
        gameOverBitmap = Bitmap.createScaledBitmap(tmp, flight.width, flight.height, true);
        if(tmp != gameOverBitmap) tmp.recycle();

        volumeButton = new VolumeButton(activity, screenX, screenY);

        bullets = new ArrayList<>(MAX_BULLETS);
        random = new Random();

        initPaints();
        initBirds();
        initGameOverUI();

        bomb = new Bomb(getResources(), screenX, screenY);

        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) { }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cleanup();
            }
        });
    }

    private void initPaints() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);

        scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scorePaint.setTextSize(screenY * 0.06f);
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextAlign(Paint.Align.CENTER);
        scorePaint.setShadowLayer(4, 2, 2, Color.BLACK);
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

    private void initGameOverUI() {
        gameOverTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gameOverTextPaint.setColor(Color.WHITE);
        gameOverTextPaint.setTextAlign(Paint.Align.CENTER);
        gameOverTextPaint.setShadowLayer(6, 2, 2, Color.BLACK);

        exitButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        exitButtonPaint.setColor(Color.rgb(200, 60, 60));

        int btnW = (int) (screenX * 0.45f);
        int btnH = (int) (screenY * 0.10f);
        int left = screenX / 2 - btnW / 2;
        int top = (int) (screenY * 0.62f);
        exitButtonRect = new Rect(left, top, left + btnW, top + btnH);
    }

    @Override
    public void run() {
        lastFrameTime = System.currentTimeMillis();
        while (isPlaying) {
            long startTime = System.currentTimeMillis();
            update();
            draw();
            long frameTime = System.currentTimeMillis() - startTime;
            if (frameTime < FRAME_TIME) {
                try { Thread.sleep(FRAME_TIME - frameTime); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        }
    }

    private void update() {
        if (isGameOver) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastFrameTime) / 1000f;
        if (deltaTime <= 0f) deltaTime = 1f / 60f;
        lastFrameTime = currentTime;

        // Không gọi volumeButton.draw(null) ở đây nữa (chỉ vẽ trong draw())

        // Cập nhật flight với deltaTime (đã đổi trong Flight)
        flight.updatePosition(background.background.getWidth(), deltaTime);

        updateCamera();
        handleAutoShoot(currentTime);
        updateBullets(deltaTime);
        updateBirds(deltaTime);
        updateBomb(currentTime, deltaTime);
    }

    private void updateCamera() {
        float centerStart = screenX / 2f;
        float targetCameraX;

        if (flight.x < centerStart) targetCameraX = 0;
        else if (flight.x + flight.width > background.background.getWidth() - centerStart)
            targetCameraX = background.background.getWidth() - screenX;
        else targetCameraX = flight.x - centerStart + flight.width / 2f;

        cameraX += (targetCameraX - cameraX) * 0.1f;
    }

    private void handleAutoShoot(long currentTime) {
        boolean isMoving = flight.movingLeft || flight.movingRight;
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
            // di chuyển theo pixels/second * deltaTime
            bullet.y -= (int)(bullet.speed * deltaTime);

            if (bullet.y < -bullet.height) {
                it.remove();
                continue;
            }

            Rect bulletRect = bullet.getCollisionShape();
            for (Bird bird : birds) {
                if (!bird.wasShot && Rect.intersects(bird.getCollisionShape(), bulletRect)) {
                    score++;
                    respawnBird(bird);
                    bird.wasShot = true;
                    it.remove();
                    break;
                }
            }
        }
    }

    private void updateBirds(float deltaTime) {
        tempRect.set(flight.x, flight.y, flight.x + flight.width, flight.y + flight.height);

        for (Bird bird : birds) {
            // di chuyển theo speed (đã set là pixels/second trong respawnBird)
            bird.y += (int)(bird.speed * deltaTime);
            bird.updateFrame();

            if (bird.y > screenY || bird.wasShot)
                respawnBird(bird);

            if (!isGameOver && !bird.wasShot && Rect.intersects(bird.getCollisionShape(), tempRect)) {
                isGameOver = true;
                saveIfHighScore();
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

        int baseSpeed = 200; // pixels/second base
        int extraSpeed = random.nextInt(150);
        bird.speed = (int)((baseSpeed + extraSpeed) * screenRatioY);

        if (Math.abs(bird.x - flight.x) < flight.width)
            bird.speed = (int)(bird.speed*1.5f);
    }

    private void updateBomb(long currentTime, float deltaTime){
        // Spawn bomb mới mỗi 10s nếu chưa active
        if(!bomb.active && currentTime - lastBombSpawnTime >= BOMB_SPAWN_INTERVAL){
            bomb.spawn(screenX, true); // bomb mới spawn có timeout
            lastBombSpawnTime = currentTime;
        }

        if(bomb.active){
            bomb.update(deltaTime, bombSpeed);

            // Bomb mới spawn chưa rơi xuống màn hình → clear sau 5s
            if(bomb.isRespawnBomb && bomb.y < 0 && currentTime - bomb.spawnTime >= BOMB_DURATION){
                bomb.clear();
            }

            // Bomb đi hết màn hình → tự clear
            if(bomb.y > screenY){
                bomb.clear();
            }

            // Va chạm với flight → game over
            if(bomb.active && bomb.y >= 0 && Rect.intersects(bomb.getCollisionShape(),
                    new Rect(flight.x, flight.y, flight.x + flight.width, flight.y + flight.height))){
                isGameOver = true;
                saveIfHighScore();
            }
        }
    }

    private void draw() {
        if (!getHolder().getSurface().isValid()) return;
        Canvas canvas = getHolder().lockCanvas();
        if (canvas == null) return;

        try {
            canvas.drawColor(Color.BLACK);
            if(background!=null && background.background!=null)
                canvas.drawBitmap(background.background,-cameraX,0,paint);

            if(!isGameOver){
                if(flight!=null && flight.getFlight()!=null)
                    canvas.drawBitmap(flight.getFlight(), flight.x - cameraX, flight.y, paint);

                for(Bird bird: birds)
                    if(!bird.wasShot && bird.getBird()!=null)
                        canvas.drawBitmap(bird.getBird(), bird.x - cameraX, bird.y, paint);

                for(Bullet bullet: bullets)
                    if(bullet.bullet!=null) canvas.drawBitmap(bullet.bullet, bullet.x - cameraX, bullet.y, paint);

                if(bomb.active && bomb.getBitmap()!=null){
                    canvas.drawBitmap(bomb.getBitmap(), bomb.x - cameraX, bomb.y, paint);
                }

                canvas.drawText(String.valueOf(score), screenX/2f, screenY*0.08f, scorePaint);
            }
            else{
                canvas.drawBitmap(gameOverBitmap, flight.x - cameraX, flight.y, paint);
                gameOverTextPaint.setTextSize(screenY*0.06f);
                canvas.drawText("GAME OVER", screenX/2f, screenY*0.45f, gameOverTextPaint);
                gameOverTextPaint.setTextSize(screenY*0.06f);
                canvas.drawText("Score: "+score, screenX/2f, screenY*0.52f, gameOverTextPaint);
                canvas.drawRect(exitButtonRect, exitButtonPaint);
                gameOverTextPaint.setTextSize(screenY*0.05f);
                canvas.drawText("EXIT", exitButtonRect.centerX(),
                        exitButtonRect.centerY() + (gameOverTextPaint.getTextSize()/3f), gameOverTextPaint);
            }

            if(volumeButton!=null) volumeButton.draw(canvas);

        } finally {
            try{ getHolder().unlockCanvasAndPost(canvas); }catch(Exception ignored){}
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        float x = event.getX();
        float y = event.getY();

        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(volumeButton!=null && volumeButton.handleTouch(x,y)){
                    audioManager.setAllMuted(volumeButton.isSfxMuted());
                    audioManager.setMusicMuted(volumeButton.isMusicMuted());
                    return true;
                }

                if(isGameOver && exitButtonRect.contains((int)x,(int)y)){
                    activity.finish();
                    return true;
                }

                if(x<screenX/2f) flight.movingLeft=true;
                else flight.movingRight=true;
                break;

            case MotionEvent.ACTION_UP:
                flight.movingLeft=false;
                flight.movingRight=false;
                break;
        }
        return true;
    }

    private void saveIfHighScore(){
        int prevHigh = prefs.getInt("highscore",0);
        if(score>prevHigh) prefs.edit().putInt("highscore",score).apply();
    }

    private void newBullet(){
        if(volumeButton != null && volumeButton.isSfxMuted()) return;
        Bullet bullet = new Bullet(getResources(), flight.x + flight.width/2, flight.y, flight.width);
        bullets.add(bullet);
        audioManager.playShootSound();
    }

    public void cleanup(){
        isPlaying=false;
        audioManager.release();
    }

    public void pause(){ isPlaying=false; }
    public void resume(){
        isPlaying=true;
        thread = new Thread(this);
        thread.start();
    }
}
