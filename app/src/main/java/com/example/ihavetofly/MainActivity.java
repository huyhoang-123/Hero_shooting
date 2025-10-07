package com.example.ihavetofly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView highscore;
    private TextView totalCoins;
    private ImageView coinIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.play).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("level", 1);
            startActivity(intent);
        });

        findViewById(R.id.level2Button).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("level", 2);
            startActivity(intent);
        });

        findViewById(R.id.level3Button).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("level", 3);
            startActivity(intent);
        });

        findViewById(R.id.guideButton).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, GuideActivity.class));
        });

        // Exit game button
        findViewById(R.id.exitButton).setOnClickListener(v -> {
            finishAffinity();
        });

        highscore = findViewById(R.id.highScoreTxt);
        totalCoins = findViewById(R.id.totalCoinsTxt);
        coinIcon = findViewById(R.id.coinIcon);

        updateDisplay();
        preloadBitmaps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDisplay();
    }

    private void updateDisplay() {
        SharedPreferences prefs = getSharedPreferences("game", MODE_PRIVATE);

        int level1High = prefs.getInt("high_score_level_1", 0);
        int level2High = prefs.getInt("high_score_level_2", 0);
        int level3High = prefs.getInt("high_score_level_3", 0);

        int maxHigh = Math.max(level1High, Math.max(level2High, level3High));
        highscore.setText(String.valueOf(maxHigh));

        totalCoins.setText(String.valueOf(prefs.getInt("total_coins", 0)));
    }

    private void preloadBitmaps() {
        int[] ids = new int[]{
                R.drawable.space_ships,
                R.drawable.bird1,
                R.drawable.bird2,
                R.drawable.bird3,
                R.drawable.bomb_4,
                R.drawable.bullet,
                R.drawable.boss_lv3,
                R.drawable.boss_bullet
        };

        for (int id : ids) {
            BitmapCache.get(getResources(), id, 2);
        }
    }
}