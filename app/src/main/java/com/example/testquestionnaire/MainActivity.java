package com.example.testquestionnaire;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.example.questionnairelibrary.CameraActivity;
import com.example.questionnairelibrary.DetectorActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(getBaseContext(), DetectorActivity.class);
        startActivity(intent);
        finish();
    }
}