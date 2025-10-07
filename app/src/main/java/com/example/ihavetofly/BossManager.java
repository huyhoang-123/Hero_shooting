package com.example.ihavetofly;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BossManager {

    private Boss boss;
    private List<Rocket> rockets;
    private List<BossBullet> bossBullets;
    private long lastBossSpawnTime;
    private long lastRocketTime;
    private long lastBulletTime;
    private boolean waitingForBirdsToClear = false;
    private boolean wasBossActive = false;
    private boolean bossDefeatedRewardGiven = false;
    private int currentLevel = 1;

    private static final long BOSS_SPAWN_INTERVAL = 20000;
    private static final int BOSS_DEFEAT_REWARD = 50;
    private static final long BULLET_SHOOT_INTERVAL = 1500;

    private final Resources resources;
    private final int screenY;

    public BossManager(Resources res, int screenX, int screenY) {
        this.resources = res;
        this.screenY = screenY;
        boss = new Boss(res, screenX, screenY);
        rockets = new ArrayList<>();
        bossBullets = new ArrayList<>();
        lastBossSpawnTime = System.currentTimeMillis();
        lastRocketTime = 0;
        lastBulletTime = 0;
    }

    public void setLevel(int level) {
        this.currentLevel = level;
        boss.setLevel(level);
    }

    public boolean shouldSpawnBoss(long currentTime) {
        if (currentLevel == 3) {
            return !boss.active && !waitingForBirdsToClear;
        }
        return !boss.active && !waitingForBirdsToClear &&
                (currentTime - lastBossSpawnTime >= BOSS_SPAWN_INTERVAL);
    }

    public void setWaitingForBirdsToClear(boolean waiting) {
        waitingForBirdsToClear = waiting;
    }

    public boolean isWaitingForBirdsToClear() {
        return waitingForBirdsToClear;
    }

    public void spawnBoss() {
        boss.spawn();
        waitingForBirdsToClear = false;
        wasBossActive = true;
        bossDefeatedRewardGiven = false;
    }

    public void update(float deltaTime, int screenX, int screenY, Resources res) {
        long currentTime = System.currentTimeMillis();

        if (boss.active) {
            boss.update(deltaTime, screenX);

            if (currentLevel == 3) {
                if (!boss.isExploding && currentTime - lastBulletTime >= BULLET_SHOOT_INTERVAL) {
                    BossBullet bullet = new BossBullet(res);
                    bullet.spawn(boss.x + boss.width / 2 - bullet.width / 2, boss.y + boss.height);
                    bossBullets.add(bullet);
                    lastBulletTime = currentTime;
                }
            } else {
                if (!boss.isExploding && boss.shouldShootRocket(currentTime, lastRocketTime)) {
                    Rocket rocket = new Rocket(res);
                    rocket.spawn(boss.x + boss.width / 2 - rocket.width / 2, boss.y + boss.height);
                    rockets.add(rocket);
                    lastRocketTime = currentTime;
                }
            }

            wasBossActive = true;
        } else if (wasBossActive) {
            lastBossSpawnTime = currentTime;
            wasBossActive = false;
        }

        Iterator<Rocket> it = rockets.iterator();
        while (it.hasNext()) {
            Rocket rocket = it.next();
            rocket.update(deltaTime, screenY);
            if (!rocket.active) {
                it.remove();
            }
        }

        Iterator<BossBullet> bit = bossBullets.iterator();
        while (bit.hasNext()) {
            BossBullet bullet = bit.next();
            bullet.update(deltaTime, screenY);
            if (!bullet.active) {
                bit.remove();
            }
        }
    }

    public Boss getBoss() {
        return boss;
    }

    public List<Rocket> getRockets() {
        return rockets;
    }

    public List<BossBullet> getBossBullets() {
        return bossBullets;
    }

    public int checkBossDestroyed() {
        if (!boss.active && wasBossActive && !bossDefeatedRewardGiven) {
            bossDefeatedRewardGiven = true;
            return BOSS_DEFEAT_REWARD;
        }
        return 0;
    }

    public void reset() {
        boss.clear();
        rockets.clear();
        bossBullets.clear();
        lastBossSpawnTime = System.currentTimeMillis();
        lastRocketTime = 0;
        lastBulletTime = 0;
        waitingForBirdsToClear = false;
        wasBossActive = false;
        bossDefeatedRewardGiven = false;
    }

    public static void clearCache() {
        Boss.clearCache();
        Rocket.clearCache();
        BossBullet.clearCache();
    }
}