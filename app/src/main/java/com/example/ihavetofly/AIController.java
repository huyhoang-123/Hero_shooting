package com.example.ihavetofly;

import android.graphics.Rect;
import java.util.List;

public class AIController {

    private Flight flight;
    private EntityManager entityManager;private int screenX, screenY;

    private boolean isAIActive = false;

    private static final float DANGER_ZONE_MULTIPLIER = 2f;
    private static final int SAFE_MARGIN = 50;
    private static final int TARGET_Y_POSITION = 200;
    private static final float PREDICTION_TIME = 0.5f;

    private AITarget currentTarget = null;
    private AIState currentState = AIState.IDLE;
    private long targetSetTime = 0;
    private static final long TARGET_RECONSIDER_TIME = 50;

    private enum AIState {
        IDLE,
        AVOIDING_DANGER,
        COLLECTING_ITEM,
        ATTACKING_ENEMY,
        REPOSITIONING
    }

    private static class AITarget {
        float x, y;
        int priority;
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

        makeDecision(currentTime);

        if (currentTarget != null) {
            moveTowardsTarget();
        }
    }

    private void makeDecision(long currentTime) {
        boolean shouldReconsider = (currentTime - targetSetTime) >= TARGET_RECONSIDER_TIME;

        AITarget dangerTarget = findNearestDanger();
        if (dangerTarget != null && (shouldReconsider || currentState != AIState.AVOIDING_DANGER)) {
            setNewTarget(dangerTarget, currentTime);
            return;
        }

        if (currentTarget != null && !shouldReconsider && currentState == AIState.AVOIDING_DANGER) {
            return;
        }

        AITarget powerUpTarget = findNearestPowerUp();
        if (powerUpTarget != null && powerUpTarget.priority >= 8) {
            setNewTarget(powerUpTarget, currentTime);
            return;
        }

        AITarget coinTarget = findNearestCoin();
        if (coinTarget != null && (currentState != AIState.COLLECTING_ITEM || shouldReconsider)) {
            setNewTarget(coinTarget, currentTime);
            return;
        }

        if (powerUpTarget != null && (currentState != AIState.COLLECTING_ITEM || shouldReconsider)) {
            setNewTarget(powerUpTarget, currentTime);
            return;
        }

        AITarget optimalPosition = getOptimalShootingPosition();
        if (optimalPosition != null && shouldReconsider) {
            setNewTarget(optimalPosition, currentTime);
            return;
        }

        if (currentTarget == null) {
            currentState = AIState.IDLE;
        }
    }

    private void setNewTarget(AITarget target, long currentTime) {
        currentTarget = target;
        currentState = target.targetType;
        targetSetTime = currentTime;
    }

    private AITarget findNearestDanger() {
        AITarget nearest = null;
        float minDangerScore = Float.MAX_VALUE;

        float flightCenterX = flight.x + flight.width / 2f;
        float flightCenterY = flight.y + flight.height / 2f;

        for (Bird bird : entityManager.getBirds()) {
            if (bird.wasShot) continue;

            float predictedY = bird.y + bird.speed * PREDICTION_TIME;
            float distance = getDistance(flightCenterX, flightCenterY, bird.x + bird.width / 2f, predictedY);
            float dangerZone = (bird.width + bird.height) * DANGER_ZONE_MULTIPLIER;

            if (distance < dangerZone && bird.y < screenY) {
                float dangerScore = distance / (bird.speed + 1);

                if (dangerScore < minDangerScore) {
                    minDangerScore = dangerScore;

                    float escapeX = flightCenterX;
                    float escapeY = flightCenterY;

                    if (bird.x + bird.width / 2f > flightCenterX) {
                        escapeX = Math.max(SAFE_MARGIN + flight.width / 2f, flightCenterX - 200);
                    } else {
                        escapeX = Math.min(screenX - SAFE_MARGIN - flight.width / 2f, flightCenterX + 200);
                    }

                    if (predictedY > flightCenterY) {
                        escapeY = Math.max(SAFE_MARGIN + flight.height / 2f, flightCenterY - 150);
                    } else {
                        escapeY = Math.min(screenY - SAFE_MARGIN - flight.height / 2f, flightCenterY + 100);
                    }

                    nearest = new AITarget(escapeX - flight.width / 2f, escapeY - flight.height / 2f, 10, AIState.AVOIDING_DANGER);
                }
            }
        }

        Bomb bomb = entityManager.getBomb();
        if (bomb.active) {
            float distance = getDistance(flightCenterX, flightCenterY, bomb.x + bomb.width / 2f, bomb.y + bomb.height / 2f);
            float dangerZone = (bomb.width + bomb.height) * DANGER_ZONE_MULTIPLIER;

            if (distance < dangerZone) {
                float escapeX = (bomb.x > flight.x)
                        ? Math.max(SAFE_MARGIN, flight.x - 250)
                        : Math.min(screenX - SAFE_MARGIN - flight.width, flight.x + 250);

                if (nearest == null || dangerZone > minDangerScore) {
                    nearest = new AITarget(escapeX, flight.y, 10, AIState.AVOIDING_DANGER);
                }
            }
        }

        for (Rocket rocket : entityManager.getBossManager().getRockets()) {
            if (!rocket.active) continue;

            float distance = getDistance(flightCenterX, flightCenterY, rocket.x + rocket.width / 2f, rocket.y + rocket.height / 2f);
            float dangerZone = (rocket.width + rocket.height) * DANGER_ZONE_MULTIPLIER;

            if (distance < dangerZone) {
                float escapeX = (rocket.x > flight.x)
                        ? Math.max(SAFE_MARGIN, flight.x - 180)
                        : Math.min(screenX - SAFE_MARGIN - flight.width, flight.x + 180);

                if (nearest == null) {
                    nearest = new AITarget(escapeX, flight.y, 10, AIState.AVOIDING_DANGER);
                }
            }
        }

        for (BossBullet bullet : entityManager.getBossManager().getBossBullets()) {
            if (!bullet.active) continue;

            float distance = getDistance(flightCenterX, flightCenterY, bullet.x + bullet.width / 2f, bullet.y + bullet.height / 2f);
            float dangerZone = (bullet.width + bullet.height) * DANGER_ZONE_MULTIPLIER;

            if (distance < dangerZone) {
                float escapeX = (bullet.x > flight.x)
                        ? Math.max(SAFE_MARGIN, flight.x - 160)
                        : Math.min(screenX - SAFE_MARGIN - flight.width, flight.x + 160);

                if (nearest == null) {
                    nearest = new AITarget(escapeX, flight.y, 10, AIState.AVOIDING_DANGER);
                }
            }
        }

        return nearest;
    }

    private AITarget findNearestPowerUp() {
        AITarget nearest = null;
        float minDistance = Float.MAX_VALUE;
        float flightCenterX = flight.x + flight.width / 2f;
        float flightCenterY = flight.y + flight.height / 2f;

        for (PowerUp powerUp : entityManager.getPowerUps()) {
            if (!powerUp.active) continue;

            float distance = getDistance(flightCenterX, flightCenterY, powerUp.x + powerUp.width / 2f, powerUp.y + powerUp.height / 2f);

            if (distance < screenY * 0.6f && distance < minDistance) {
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
                return flight.hasShield() ? 3 : 9;
            case PowerUp.TYPE_KUNAI:
                return flight.hasKunai() ? 3 : 7;
            case PowerUp.TYPE_DOUBLE_BULLET:
                return flight.hasDoubleBullet() ? 3 : 6;
            default:
                return 3;
        }
    }

    private AITarget findNearestCoin() {
        AITarget nearest = null;
        float minDistance = Float.MAX_VALUE;
        float flightCenterX = flight.x + flight.width / 2f;
        float flightCenterY = flight.y + flight.height / 2f;

        for (Coin coin : entityManager.getCoins()) {
            if (!coin.active) continue;

            float distance = getDistance(flightCenterX, flightCenterY, coin.x + coin.width / 2f, coin.y + coin.height / 2f);

            if (distance < screenY * 0.4f && distance < minDistance) {
                minDistance = distance;
                nearest = new AITarget(
                        coin.x + coin.width / 2f - flight.width / 2f,
                        coin.y + coin.height / 2f - flight.height / 2f,
                        5,
                        AIState.COLLECTING_ITEM
                );
            }
        }

        return nearest;
    }

    private AITarget getOptimalShootingPosition() {
        Bird targetBird = null;
        float lowestY = Float.MAX_VALUE;

        for (Bird bird : entityManager.getBirds()) {
            if (!bird.wasShot && bird.y < screenY && bird.y < lowestY) {
                lowestY = bird.y;
                targetBird = bird;
            }
        }

        if (targetBird != null) {
            float targetX = targetBird.x + targetBird.width / 2f - flight.width / 2f;
            targetX = Math.max(SAFE_MARGIN, Math.min(screenX - flight.width - SAFE_MARGIN, targetX));
            float targetY = screenY - flight.height - TARGET_Y_POSITION;

            return new AITarget(targetX, targetY, 4, AIState.ATTACKING_ENEMY);
        }

        Boss boss = entityManager.getBossManager().getBoss();
        if (boss.active && !boss.isExploding) {
            float targetX = boss.x + boss.width / 2f - flight.width / 2f;
            targetX = Math.max(SAFE_MARGIN, Math.min(screenX - flight.width - SAFE_MARGIN, targetX));
            float targetY = screenY - flight.height - TARGET_Y_POSITION;

            return new AITarget(targetX, targetY, 7, AIState.ATTACKING_ENEMY);
        }

        return new AITarget(
                screenX / 2f - flight.width / 2f,
                screenY - flight.height - TARGET_Y_POSITION,
                2,
                AIState.REPOSITIONING
        );
    }

    private void moveTowardsTarget() {
        if (currentTarget == null) return;

        float currentX = flight.x + flight.width / 2f;
        float currentY = flight.y + flight.height / 2f;

        float dx = currentTarget.x + flight.width / 2f - currentX;
        float dy = currentTarget.y + flight.height / 2f - currentY;

        float threshold = 15;

        flight.movingLeft = false;
        flight.movingRight = false;
        flight.movingUp = false;
        flight.movingDown = false;

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
