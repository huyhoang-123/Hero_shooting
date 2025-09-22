package com.example.ihavetofly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private boolean isMusicMuted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Play button
        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, GameActivity.class));
            }
        });

        // Setup high score display
        TextView highscore = findViewById(R.id.highScoreTxt);
        SharedPreferences prefs = getSharedPreferences("game", MODE_PRIVATE);
        highscore.setText("Highscore: " + prefs.getInt("highscore", 0));

        // Get music mute status - CHỈ đọc từ SharedPreferences
        isMusicMuted = prefs.getBoolean("isMusicMute", false);

        // Setup volume control button
        final ImageView volumeCtrl = findViewById(R.id.volumeCtrl);
        updateVolumeIcon(volumeCtrl);

        volumeCtrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle CHINHỈ nhạc nền
                isMusicMuted = !isMusicMuted;

                android.util.Log.d("MainActivity", "ONLY Music button clicked. isMusicMuted: " + isMusicMuted);

                // Lưu vào SharedPreferences
                SharedPreferences prefs = getSharedPreferences("game", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isMusicMute", isMusicMuted);
                editor.apply();

                updateVolumeIcon(volumeCtrl);
            }
        });
    }

    private void updateVolumeIcon(ImageView volumeCtrl) {
        if (isMusicMuted) {
            volumeCtrl.setImageResource(R.drawable.volume_speaker); // mute icon
        } else {
            volumeCtrl.setImageResource(R.drawable.speaker_high_volume); // volume on icon
        }
        android.util.Log.d("MainActivity", "Icon updated. isMusicMuted: " + isMusicMuted);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update UI
        SharedPreferences prefs = getSharedPreferences("game", MODE_PRIVATE);
        isMusicMuted = prefs.getBoolean("isMusicMute", false);
        ImageView volumeCtrl = findViewById(R.id.volumeCtrl);
        updateVolumeIcon(volumeCtrl);
    }
}