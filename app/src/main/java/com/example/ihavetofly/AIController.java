package com.example.ihavetofly;

import android.graphics.Rect;
import java.util.List;

public class AIController {

    private Flight flight;
    private EntityManager entityManager;
    private int screenX, screenY;

    private boolean isAIActive = false;
    private long lastDecisionTime = 0;
    private static final long DECISION_INTERVAL = 100; // AI decides every 100ms

    // AI behavior settings
    private static final float DANGER_ZONE_MULTIPLIER = 2.0f;
    private static final int SAFE_MARGIN = 50;
    private static final int TARGET_Y_POSITION = 200; // Preferred Y position from bottom

    private AITarget currentTarget = null;
    private AIState currentState = AIState.IDLE;

    private enum AIState {
        IDLE,
        AVOIDING_DANGER,
        COLLECTING_ITEM,
        ATTACKING_ENEMY,
        REPOSITIONING
    }

    private static class AITarget {
        float x, y;
        int priority; // Higher = more important
        AIState targetType;

        AITarget(float x, float y, int priority, AIState type) {
            this.x = x;
            this.y = y;
            this.priority = priority;
            this.targetType = type;
        }
    }

    public AIController(Flight flight, EntityManager entityManager, int screenX, int screenY) {
        this.flight = flight;
        this.entityManager = entityManager;
        this.screenX = screenX;
        this.screenY = screenY;
    }

    public void setAIActive(boolean active) {
        this.isAIActive = active;
        if (!active) {
            stopMovement();
        }
    }

    public boolean isAIActive() {
        return isAIActive;
    }

    public void update(long currentTime) {
        if (!isAIActive) return;

        // Make decisions at intervals
        if (currentTime - lastDecisionTime >= DECISION_INTERVAL) {
            makeDecision();
            lastDecisionTime = currentTime;
        }

        // Execute current target movement
        if (currentTarget != null) {
            moveTowardsTarget();
        }
    }

    private void makeDecision() {
        // Priority 1: Avoid immediate dangers
        AITarget dangerTarget = findNearestDanger();
        if (dangerTarget != null) {
            currentTarget = dangerTarget;
            currentState = AIState.AVOIDING_DANGER;
            return;
        }

        // Priority 2: Collect power-ups (shield is highest priority)
        AITarget powerUpTarget = findNearestPowerUp();
        if (powerUpTarget != null && powerUpTarget.priority >= 8) {
            currentTarget = powerUpTarget;
            currentState = AIState.COLLECTING_ITEM;
            return;
        }

        // Priority 3: Collect coins
        AITarget coinTarget = findNearestCoin();
        if (coinTarget != null) {
            currentTarget = coinTarget;
            currentState = AIState.COLLECTING_ITEM;
            return;
        }

        // Priority 4: Lower priority power-ups
        if (powerUpTarget != null) {
            currentTarget = powerUpTarget;
            currentState = AIState.COLLECTING_ITEM;
            return;
        }

        // Priority 5: Position for optimal shooting
        AITarget optimalPosition = getOptimalShootingPosition();
        if (optimalPosition != null) {
            currentTarget = optimalPosition;
            currentState = AIState.REPOSITIONING;
            return;
        }

        currentState = AIState.IDLE;
        currentTarget = null;
    }

    private AITarget findNearestDanger() {
        AITarget nearest = null;
        float minDangerScore = Float.MAX_VALUE;

        Rect flightRect = new Rect(flight.x, flight.y,
                flight.x + flight.width, flight.y + flight.height);

        // Check birds
        for (Bird bird : entityManager.getBirds()) {
            if (bird.wasShot) continue;

            float distance = getDistance(flight.x + flight.width / 2f,
                    flight.y + flight.height / 2f,
                    bird.x + bird.width / 2f,
                    bird.y + bird.height / 2f);

            float dangerZone = (bird.width + bird.height) * DANGER_ZONE_MULTIPLIER;

            if (distance < dangerZone && bird.y < screenY) {
                float dangerScore = distance / (bird.speed + 1);

                if (dangerScore < minDangerScore) {
                    minDangerScore = dangerScore;
                    // Move away from bird
                    float escapeX = flight.x + flight.width / 2f;
                    float escapeY = flight.y + flight.height / 2f;

                    if (bird.x > flight.x) {
                        escapeX = Math.max(SAFE_MARGIN, flight.x - 150);
                    } else {
                        escapeX = Math.min(screenX - SAFE_MARGIN, flight.x + 150);
                    }

                    if (bird.y > flight.y) {
                        escapeY = Math.max(SAFE_MARGIN, flight.y - 100);
                    }

                    nearest = new AITarget(escapeX, escapeY, 10, AIState.AVOIDING_DANGER);
                }
            }
        }

        // Check bomb
        Bomb bomb = entityManager.getBomb();
        if (bomb.active) {
            float distance = getDistance(flight.x + flight.width / 2f,
                    flight.y + flight.height / 2f,
                    bomb.x + bomb.width / 2f,
                    bomb.y + bomb.height / 2f);

            float dangerZone = (bomb.width + bomb.height) * DANGER_ZONE_MULTIPLIER;

            if (distance < dangerZone) {
                float escapeX = (bomb.x > flight.x)
                        ? Math.max(SAFE_MARGIN, flight.x - 200)
                        : Math.min(screenX - SAFE_MARGIN - flight.width, flight.x + 200);

                nearest = new AITarget(escapeX, flight.y, 10, AIState.AVOIDING_DANGER);
            }
        }

        // Check boss rockets
        for (Rocket rocket : entityManager.getBossManager().getRockets()) {
            if (!rocket.active) continue;

            float distance = getDistance(flight.x + flight.width / 2f,
                    flight.y + flight.height / 2f,
                    rocket.x + rocket.width / 2f,
                    rocket.y + rocket.height / 2f);

            float dangerZone = (rocket.width + rocket.height) * DANGER_ZONE_MULTIPLIER;

            if (distance < dangerZone) {
                float escapeX = (rocket.x > flight.x)
                        ? Math.max(SAFE_MARGIN, flight.x - 150)
                        : Math.min(screenX - SAFE_MARGIN - flight.width, flight.x + 150);

                nearest = new AITarget(escapeX, flight.y, 10, AIState.AVOIDING_DANGER);
            }
        }

        // Check boss bullets
        for (BossBullet bullet : entityManager.getBossManager().getBossBullets()) {
            if (!bullet.active) continue;

            float distance = getDistance(flight.x + flight.width / 2f,
                    flight.y + flight.height / 2f,
                    bullet.x + bullet.width / 2f,
                    bullet.y + bullet.height / 2f);

            float dangerZone = (bullet.width + bullet.height) * DANGER_ZONE_MULTIPLIER;

            if (distance < dangerZone) {
                float escapeX = (bullet.x > flight.x)
                        ? Math.max(SAFE_MARGIN, flight.x - 150)
                        : Math.min(screenX - SAFE_MARGIN - flight.width, flight.x + 150);

                nearest = new AITarget(escapeX, flight.y, 10, AIState.AVOIDING_DANGER);
            }
        }

        return nearest;
    }

    private AITarget findNearestPowerUp() {
        AITarget nearest = null;
        float minDistance = Float.MAX_VALUE;

        for (PowerUp powerUp : entityManager.getPowerUps()) {
            if (!powerUp.active) continue;

            float distance = getDistance(flight.x + flight.width / 2f,
                    flight.y + flight.height / 2f,
                    powerUp.x + powerUp.width / 2f,
                    powerUp.y + powerUp.height / 2f);

            // Only consider if reachable
            if (distance < screenY / 2f && distance < minDistance) {
                minDistance = distance;
                int priority = getPowerUpPriority(powerUp.type);
                nearest = new AITarget(
                        powerUp.x + powerUp.width / 2f - flight.width / 2f,
                        powerUp.y + powerUp.height / 2f - flight.height / 2f,
                        priority,
                        AIState.COLLECTING_ITEM
                );
            }
        }

        return nearest;
    }

    private int getPowerUpPriority(int type) {
        switch (type) {
            case PowerUp.TYPE_SHIELD:
                return flight.hasShield() ? 3 : 9; // High priority if don't have shield
            case PowerUp.TYPE_KUNAI:
                return flight.hasKunai() ? 2 : 6;
            case PowerUp.TYPE_DOUBLE_BULLET:
                return flight.hasDoubleBullet() ? 2 : 5;
            default:
                return 3;
        }
    }

    private AITarget findNearestCoin() {
        AITarget nearest = null;
        float minDistance = Float.MAX_VALUE;

        for (Coin coin : entityManager.getCoins()) {
            if (!coin.active) continue;

            float distance = getDistance(flight.x + flight.width / 2f,
                    flight.y + flight.height / 2f,
                    coin.x + coin.width / 2f,
                    coin.y + coin.height / 2f);

            // Only consider coins that are reachable
            if (distance < screenY / 3f && distance < minDistance) {
                minDistance = distance;
                nearest = new AITarget(
                        coin.x + coin.width / 2f - flight.width / 2f,
                        coin.y + coin.height / 2f - flight.height / 2f,
                        4,
                        AIState.COLLECTING_ITEM
                );
            }
        }

        return nearest;
    }

    private AITarget getOptimalShootingPosition() {
        // Find enemy with lowest Y (closest to top, best to shoot)
        Bird targetBird = null;
        float lowestY = Float.MAX_VALUE;

        for (Bird bird : entityManager.getBirds()) {
            if (!bird.wasShot && bird.y < screenY && bird.y < lowestY) {
                lowestY = bird.y;
                targetBird = bird;
            }
        }

        // Position under the target
        if (targetBird != null) {
            float targetX = targetBird.x + targetBird.width / 2f - flight.width / 2f;
            targetX = Math.max(SAFE_MARGIN, Math.min(screenX - flight.width - SAFE_MARGIN, targetX));

            float targetY = screenY - flight.height - TARGET_Y_POSITION;

            return new AITarget(targetX, targetY, 3, AIState.ATTACKING_ENEMY);
        }

        // Check boss
        Boss boss = entityManager.getBossManager().getBoss();
        if (boss.active && !boss.isExploding) {
            float targetX = boss.x + boss.width / 2f - flight.width / 2f;
            targetX = Math.max(SAFE_MARGIN, Math.min(screenX - flight.width - SAFE_MARGIN, targetX));

            float targetY = screenY - flight.height - TARGET_Y_POSITION;

            return new AITarget(targetX, targetY, 7, AIState.ATTACKING_ENEMY);
        }

        // Default: center bottom
        return new AITarget(
                screenX / 2f - flight.width / 2f,
                screenY - flight.height - TARGET_Y_POSITION,
                1,
                AIState.REPOSITIONING
        );
    }

    private void moveTowardsTarget() {
        if (currentTarget == null) return;

        float currentX = flight.x + flight.width / 2f;
        float currentY = flight.y + flight.height / 2f;

        float dx = currentTarget.x + flight.width / 2f - currentX;
        float dy = currentTarget.y + flight.height / 2f - currentY;

        float threshold = 20; // Pixels threshold to consider "reached"

        // Reset all movement
        flight.movingLeft = false;
        flight.movingRight = false;
        flight.movingUp = false;
        flight.movingDown = false;

        // Set movement directions
        if (Math.abs(dx) > threshold) {
            if (dx < 0) {
                flight.movingLeft = true;
            } else {
                flight.movingRight = true;
            }
        }

        if (Math.abs(dy) > threshold) {
            if (dy < 0) {
                flight.movingUp = true;
            } else {
                flight.movingDown = true;
            }
        }

        // Clear target if reached
        if (Math.abs(dx) <= threshold && Math.abs(dy) <= threshold) {
            currentTarget = null;
        }
    }

    private void stopMovement() {
        flight.movingLeft = false;
        flight.movingRight = false;
        flight.movingUp = false;
        flight.movingDown = false;
    }

    private float getDistance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public String getCurrentState() {
        return currentState.toString();
    }
}