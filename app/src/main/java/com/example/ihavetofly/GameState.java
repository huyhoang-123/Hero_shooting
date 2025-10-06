package com.example.ihavetofly;

public class GameState {
    public volatile boolean isPlaying = false;
    public volatile boolean isGameOver = false;
    public boolean isWin = false;
    public boolean hasShownWinScreen = false;

    public int score = 0;
    public int sessionStartHighScore = 0;
    public long gameTime = 0;
    public long startTime = 0;

    public int currentLevel = 1;
    public float speedMultiplier = 1.0f;

    public void reset() {
        isGameOver = false;
        isWin = false;
        isPlaying = true;  // Ensure game is playing after reset
        score = 0;
        startTime = System.currentTimeMillis();
        hasShownWinScreen = false;
    }

    public void setLevel(int level) {
        currentLevel = level;
        speedMultiplier = GameConfig.getSpeedMultiplier(level);
    }
}