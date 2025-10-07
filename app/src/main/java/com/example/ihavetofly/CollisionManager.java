package com.example.ihavetofly;

import android.graphics.Rect;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class CollisionManager {

    private final Rect tempRect = new Rect();
    private final Random random = new Random();

    public boolean checkBulletCollisions(List<Bullet> bullets, Bird[] birds, Boss boss,
                                         List<Coin> coins, List<PowerUp> powerUps,
                                         android.content.res.Resources res) {
        if (bullets.isEmpty()) return false;

        boolean scoreIncreased = false;
        Iterator<Bullet> it = bullets.iterator();

        while (it.hasNext()) {
            Bullet bullet = it.next();

            if (bullet.y < -bullet.height) {
                it.remove();
                continue;
            }

            Rect bulletRect = bullet.getCollisionShape();

            if (boss.active && !boss.isExploding && Rect.intersects(boss.getCollisionShape(), bulletRect)) {
                boss.hit();
                it.remove();
                continue;
            }

            for (Bird bird : birds) {
                if (!bird.wasShot && Rect.intersects(bird.getCollisionShape(), bulletRect)) {
                    scoreIncreased = true;
                    bird.wasShot = true;
                    spawnDrops(bird, coins, powerUps, res);
                    it.remove();
                    break;
                }
            }
        }
        return scoreIncreased;
    }

    private void spawnDrops(Bird bird, List<Coin> coins, List<PowerUp> powerUps, android.content.res.Resources res) {
        int dropChance = random.nextInt(100);

        // FIXED: Adjusted drop rates
        // Coins: 70% (was 30%)
        // Double Bullet: 10% (was 25%)
        // Kunai: 10% (was 25%)
        // Shield: 10% (was 20%)

        if (dropChance < 70) {
            // 70% chance to drop coin
            Coin c = new Coin(res);
            int spawnX = bird.x + bird.width / 2 - (c.width / 2);
            int spawnY = bird.y + bird.height / 2 - (c.height / 2);
            c.spawnAt(spawnX, spawnY);
            coins.add(c);
        } else if (dropChance < 80) {
            // 10% chance for double bullet
            spawnPowerUp(bird, PowerUp.TYPE_DOUBLE_BULLET, powerUps, res);
        } else if (dropChance < 90) {
            // 10% chance for kunai
            spawnPowerUp(bird, PowerUp.TYPE_KUNAI, powerUps, res);
        } else {
            // 10% chance for shield
            spawnPowerUp(bird, PowerUp.TYPE_SHIELD, powerUps, res);
        }
    }

    private void spawnPowerUp(Bird bird, int type, List<PowerUp> powerUps, android.content.res.Resources res) {
        PowerUp p = new PowerUp(res, type);
        int spawnX = bird.x + bird.width / 2 - (p.width / 2);
        int spawnY = bird.y + bird.height / 2 - (p.height / 2);
        p.spawnAt(spawnX, spawnY);
        powerUps.add(p);
    }

    public boolean checkBirdCollision(Bird[] birds, Flight flight, int screenY) {
        tempRect.set(flight.x, flight.y, flight.x + flight.width, flight.y + flight.height);

        for (Bird bird : birds) {
            if (bird.y <= screenY && !bird.wasShot && !flight.hasShield() &&
                    Rect.intersects(bird.getCollisionShape(), tempRect)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkBombCollision(Bomb bomb, Flight flight) {
        if (!bomb.active || flight.hasShield()) return false;
        tempRect.set(flight.x, flight.y, flight.x + flight.width, flight.y + flight.height);
        return Rect.intersects(tempRect, bomb.getCollisionShape());
    }

    public boolean checkBossCollisions(Boss boss, List<Rocket> rockets, List<BossBullet> bossBullets, Flight flight) {
        if (flight.hasShield()) return false;

        tempRect.set(flight.x, flight.y, flight.x + flight.width, flight.y + flight.height);

        if (boss.active && !boss.isExploding && Rect.intersects(boss.getCollisionShape(), tempRect)) {
            return true;
        }

        for (Rocket rocket : rockets) {
            if (rocket.active && Rect.intersects(rocket.getCollisionShape(), tempRect)) {
                return true;
            }
        }

        for (BossBullet bullet : bossBullets) {
            if (bullet.active && Rect.intersects(bullet.getCollisionShape(), tempRect)) {
                return true;
            }
        }

        return false;
    }

    public int checkCoinCollisions(List<Coin> coins, Flight flight) {
        tempRect.set(flight.x, flight.y, flight.x + flight.width, flight.y + flight.height);
        int collected = 0;

        Iterator<Coin> it = coins.iterator();
        while (it.hasNext()) {
            Coin c = it.next();
            if (c.active && Rect.intersects(tempRect, c.getCollisionShape())) {
                collected++;
                c.clear();
                it.remove();
            }
        }
        return collected;
    }

    public ShieldEffect checkPowerUpCollisions(List<PowerUp> powerUps, Flight flight) {
        tempRect.set(flight.x, flight.y, flight.x + flight.width, flight.y + flight.height);
        ShieldEffect newShield = null;

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
                    newShield = new ShieldEffect(
                            flight.x + flight.width / 2f,
                            flight.y + flight.height / 2f,
                            shieldRadius
                    );
                }
                p.clear();
                it.remove();
            }
        }
        return newShield;
    }
}