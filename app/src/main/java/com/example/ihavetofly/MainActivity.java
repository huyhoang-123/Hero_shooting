package com.example.ihavetofly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private  boolean isMute ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Kết nối với layout activity_main.xml

        setContentView(R.layout.activity_main);

        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, GameActivity.class));
            }

        });
        TextView highscore = findViewById(R.id.highScoreTxt);
        SharedPreferences prefs = getSharedPreferences("game", MODE_PRIVATE);
        highscore.setText("Highscore: " + prefs.getInt("highscore", 0));
        isMute = prefs.getBoolean("isMute",false);

        ImageView volume = findViewById(R.id.volumeCtrl);
        final ImageView volumeCtrl = findViewById(R.id.volumeCtrl);

        if(isMute){
            volumeCtrl.setImageResource(R.drawable.mute);
        }
        else{
            volumeCtrl.setImageResource(R.drawable.baseline_volume_up_24);
        }
        volumeCtrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isMute = !isMute;
                if(isMute){
                    volumeCtrl.setImageResource(R.drawable.mute);
                }
                else{
                    volumeCtrl.setImageResource(R.drawable.baseline_volume_up_24);
                }
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isMute", isMute);
                editor.apply();
            }
        });
    }
}
