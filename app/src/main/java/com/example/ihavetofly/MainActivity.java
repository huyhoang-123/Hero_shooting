package com.example.ihavetofly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

        // Preload bitmap nhẹ
        preloadBitmaps();
    }

    private void preloadBitmaps() {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        opts.inSampleSize = 2;

        int[] ids = new int[]{
                R.drawable.background,
                R.drawable.space_ships,
                R.drawable.bird1,
                R.drawable.bullet,
                R.drawable.speaker_high_volume,
                R.drawable.volume_speaker
        };

        for (int id : ids) {
            Bitmap b = BitmapFactory.decodeResource(getResources(), id, opts);
            if (b != null) b.recycle();
        }
    }
}
