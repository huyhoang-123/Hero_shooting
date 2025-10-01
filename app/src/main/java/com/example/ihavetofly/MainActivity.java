package com.example.ihavetofly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Nút Play
        findViewById(R.id.play).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, GameActivity.class));
        });

        // Hiển thị high score
        TextView highscore = findViewById(R.id.highScoreTxt);
        SharedPreferences prefs = getSharedPreferences("game", MODE_PRIVATE);
        highscore.setText("Highscore: " + prefs.getInt("highscore", 0));

        // Preload bitmap nhẹ (bây giờ thực sự cache, không recycle ngay)
        preloadBitmaps();
    }

    private void preloadBitmaps() {
        int[] ids = new int[]{
                R.drawable.background,
                R.drawable.space_ships,
                R.drawable.bird1,
                R.drawable.bird2,
                R.drawable.bird3,
                R.drawable.bomb_4,
                R.drawable.bullet,
                R.drawable.speaker_high_volume,
                R.drawable.volume_speaker
        };

        for (int id : ids) {
            // sampleSize = 2 (giảm resolution khi preload) -> nhanh & ít tốn RAM
            BitmapCache.get(getResources(), id, 2);
        }
    }
}
