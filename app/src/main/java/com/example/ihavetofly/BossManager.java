package com.example.ihavetofly;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BossManager {

    private Boss boss;
    private List<Rocket> rockets;
    private long lastBossSpawnTime;
    private long lastRocketTime;
    private boolean waitingForBirdsToClear = false;
    private boolean wasBossActive = false;
    private boolean bossDefeatedRewardGiven = false;

    private static final long BOSS_SPAWN_INTERVAL = 20000;
    private static final int BOSS_DEFEAT_REWARD = 50;

    public BossManager(Resources res, int screenX, int screenY) {
        boss = new Boss(res, screenX, screenY);
        rockets = new ArrayList<>();
        lastBossSpawnTime = System.currentTimeMillis();
        lastRocketTime = 0;
    }

    public boolean shouldSpawnBoss(long currentTime) {
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

            if (!boss.isExploding && boss.shouldShootRocket(currentTime, lastRocketTime)) {
                Rocket rocket = new Rocket(res);
                rocket.spawn(boss.x + boss.width / 2 - rocket.width / 2, boss.y + boss.height);
                rockets.add(rocket);
                lastRocketTime = currentTime;
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
    }

    public Boss getBoss() {
        return boss;
    }

    public List<Rocket> getRockets() {
        return rockets;
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
        lastBossSpawnTime = System.currentTimeMillis();
        lastRocketTime = 0;
        waitingForBirdsToClear = false;
        wasBossActive = false;
        bossDefeatedRewardGiven = false;
    }

    public static void clearCache() {
        Boss.clearCache();
        Rocket.clearCache();
    }
}