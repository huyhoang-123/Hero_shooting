package com.example.ihavetofly;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class EntityManager {

    private final List<Bullet> bullets;
    private final Bird[] birds;
    private final Bomb bomb;
    private final List<Coin> coins;
    private final List<PowerUp> powerUps;
    private final BossManager bossManager;
    private final Flight flight;
    private final Resources resources;

    private long lastBombSpawnTime = 0;
    private final Random random;
    private final int screenX, screenY;
    private int currentLevel = 1;

    public EntityManager(Resources res, int screenX, int screenY, Flight flight) {
        this.resources = res;
        this.screenX = screenX;
        this.screenY = screenY;
        this.flight = flight;
        this.random = new Random();

        bullets = new ArrayList<>(GameConfig.MAX_BULLETS);
        coins = new ArrayList<>();
        powerUps = new ArrayList<>();
        bomb = new Bomb(res, screenX, screenY);
        bossManager = new BossManager(res, screenX, screenY);

        birds = new Bird[6];
        initBirds();
    }

    public void setLevel(int level) {
        this.currentLevel = level;
        bossManager.setLevel(level);
    }

    private void initBirds() {
        int spacing = screenY / 6;
        for (int i = 0; i < birds.length; i++) {
            birds[i] = new Bird(resources);
            respawnBird(birds[i], 1.0f);
            birds[i].y -= i * spacing;
        }
    }

    public void updateBullets(float deltaTime) {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet bullet = it.next();
            bullet.y -= (int) (bullet.speed * deltaTime);
        }
    }

    public void updateBirds(float deltaTime, float speedMultiplier) {
        for (Bird bird : birds) {
            bird.y += (int) (bird.speed * deltaTime * speedMultiplier);
            bird.updateFrame();

            if (bird.y > screenY || bird.wasShot) {
                respawnBird(bird, speedMultiplier);
            }
        }
    }

    public void updateBomb(long currentTime, float deltaTime, int bombSpeed) {
        if (!bomb.active && currentTime - lastBombSpawnTime >= GameConfig.BOMB_SPAWN_INTERVAL) {
            bomb.spawn(screenX, false);
            lastBombSpawnTime = currentTime;
        }

        if (bomb.active) {
            bomb.update(deltaTime, bombSpeed);
            if (bomb.y > screenY) {
                bomb.clear();
            }
        }
    }

    public void updateCoins(float deltaTime) {
        Iterator<Coin> it = coins.iterator();
        while (it.hasNext()) {
            Coin c = it.next();
            c.update(deltaTime, screenY);
            if (!c.active) it.remove();
        }
    }

    public void updatePowerUps(float deltaTime) {
        Iterator<PowerUp> it = powerUps.iterator();
        while (it.hasNext()) {
            PowerUp p = it.next();
            p.update(deltaTime, screenY);
            if (!p.active) it.remove();
        }
    }

    public void addBullet(Resources res) {
        if (bullets.size() >= GameConfig.MAX_BULLETS) return;

        boolean useKunai = flight.hasKunai();

        if (flight.hasDoubleBullet()) {
            int offset = flight.width / 4;
            bullets.add(new Bullet(res, flight.x + offset, flight.y, flight.width, useKunai));
            bullets.add(new Bullet(res, flight.x + flight.width - offset, flight.y, flight.width, useKunai));
        } else {
            bullets.add(new Bullet(res, flight.x + flight.width / 2, flight.y, flight.width, useKunai));
        }
    }

    public void respawnBird(Bird bird, float speedMultiplier) {
        if (bossManager.isWaitingForBirdsToClear() || bossManager.getBoss().active) {
            bird.wasShot = false;
            bird.y = screenY + bird.height;
            return;
        }

        bird.wasShot = false;
        bird.y = -bird.height - random.nextInt(Math.max(1, screenY / 3));
        bird.x = random.nextInt(Math.max(1, screenX - bird.width));
        bird.speed = (int) ((GameConfig.BASE_BIRD_MIN_SPEED +
                random.nextInt(GameConfig.BASE_BIRD_SPEED_RANGE)) * speedMultiplier);
    }

    public boolean areAllBirdsGone() {
        for (Bird bird : birds) {
            if (bird != null && bird.y < screenY) {
                return false;
            }
        }
        return true;
    }

    public void reset(float speedMultiplier) {
        bullets.clear();
        coins.clear();
        powerUps.clear();
        bomb.clear();
        lastBombSpawnTime = System.currentTimeMillis();
        bossManager.reset();

        if (currentLevel != 3) {
            for (Bird b : birds) respawnBird(b, speedMultiplier);
        }
    }

    public List<Bullet> getBullets() { return bullets; }
    public Bird[] getBirds() { return birds; }
    public Bomb getBomb() { return bomb; }
    public List<Coin> getCoins() { return coins; }
    public List<PowerUp> getPowerUps() { return powerUps; }
    public BossManager getBossManager() { return bossManager; }
}