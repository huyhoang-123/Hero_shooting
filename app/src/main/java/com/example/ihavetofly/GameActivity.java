package com.example.ihavetofly;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class GameActivity extends Activity {

    private GameView gameView;
    private int currentLevel = 1;

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
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (gameView != null) {
            gameView.cleanup();
        }
    }
}