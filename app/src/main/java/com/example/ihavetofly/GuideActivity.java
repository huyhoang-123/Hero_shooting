package com.example.ihavetofly;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class GuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        ImageView exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> finish());
    }
}