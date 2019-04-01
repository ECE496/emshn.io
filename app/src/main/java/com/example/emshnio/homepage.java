package com.example.emshnio;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.appcompat.app.AppCompatActivity;

public class homepage extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);
    }
    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.start) {
            Intent intent = new Intent(homepage.this, CameraActivity.class);
            startActivity(intent);
        }
    }





}
