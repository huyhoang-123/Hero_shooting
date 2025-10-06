package com.example.ihavetofly;

public class GameConfig {
    public static final long SHOOT_INTERVAL = 200;
    public static final int TARGET_FPS = 60;
    public static final long FRAME_TIME = 1000 / TARGET_FPS;
    public static final int MAX_BULLETS = 15;
    public static final long BOMB_SPAWN_INTERVAL = 10000;
    public static final int BASE_BOMB_SPEED = 400;
    public static final int RESUME_COST = 1;

    public static final int BASE_BIRD_MIN_SPEED = 200;
    public static final int BASE_BIRD_SPEED_RANGE = 150;

    public static float getSpeedMultiplier(int level) {
        return level == 2 ? 2.0f : 1.0f;
    }
}