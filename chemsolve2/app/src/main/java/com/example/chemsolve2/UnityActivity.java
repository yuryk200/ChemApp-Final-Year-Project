package com.example.chemsolve2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.unity3d.player.UnityPlayerActivity;

public class UnityActivity extends AppCompatActivity {

    private String sdfFilePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unity);

        sdfFilePath = getIntent().getStringExtra("sdfFilePath");

        Intent intent = new Intent(UnityActivity.this, UnityPlayerActivity.class);
        startActivity(intent);
    }
}