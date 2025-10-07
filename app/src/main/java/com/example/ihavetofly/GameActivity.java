package com.example.ihavetofly;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class GameActivity extends Activity {

    private GameView gameView;
    private int currentLevel = 1;
    private long audioSessionId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        int screenX = getResources().getDisplayMetrics().widthPixels;
        int screenY = getResources().getDisplayMetrics().heightPixels;

        currentLevel = getIntent().getIntExtra("level", 1);

        gameView = new GameView(this, screenX, screenY);
        gameView.setLevel(currentLevel);
        setContentView(gameView);
    }

    public void startLevel(int level) {
        if (gameView != null) {
            gameView.pause();
        }

        android.content.Intent intent = new android.content.Intent(this, GameActivity.class);
        intent.putExtra("level", level);
        // Start fresh activity for next level to avoid reusing stale state
        try {
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Keep for backward compatibility
    public void startLevel2() {
        startLevel(2);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.resume();
        }
        // Start new audio session to avoid previous Activity pausing this one
        audioSessionId = GameAudioManager.getInstance(this).startSession();
        // Ensure background music is unmuted and playing for new level
        GameAudioManager audio = GameAudioManager.getInstance(this);
        audio.setAllMuted(false);
        audio.setMusicMuted(false);
        audio.playBackground();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.pause();
        }
        // End audio session; only pause if this is the active session
        GameAudioManager.getInstance(this).endSession(audioSessionId);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        int level = intent.getIntExtra("level", 1);
        currentLevel = level;

        if (gameView != null) {
            gameView.pause();
        }

        int screenX = getResources().getDisplayMetrics().widthPixels;
        int screenY = getResources().getDisplayMetrics().heightPixels;

        gameView = new GameView(this, screenX, screenY);
        gameView.setLevel(currentLevel);
        setContentView(gameView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (gameView != null) {
            gameView.cleanup();
        }
    }
}