package com.example.emshnio;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MenuActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.goToSingleCaptureButton: goToSingleCapture(); break;
            case R.id.goToStreamCaptureButton: goToStreamCapture(); break;
            case R.id.goToHelpButton: goToHelp(); break;
        }
    }

    private void goToHelp() {

        Intent intent = new Intent(MenuActivity.this, HelpActivity.class);
        startActivity(intent);
    }

    private void goToStreamCapture() {

        Intent intent = new Intent(MenuActivity.this, SingleCaptureActivity.class);
        startActivity(intent);

    }

    private void goToSingleCapture() {

        Intent intent = new Intent(MenuActivity.this, SingleCaptureActivity.class);
        startActivity(intent);

    }
}
