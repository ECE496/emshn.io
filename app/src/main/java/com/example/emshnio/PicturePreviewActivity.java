package com.example.emshnio;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.otaliastudios.cameraview.AspectRatio;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.PictureResult;

import java.lang.ref.WeakReference;

import androidx.annotation.Nullable;


public class PicturePreviewActivity extends Activity implements View.OnClickListener {



    private static WeakReference<PictureResult> image;
    private static Bitmap cropBmp;

    public static void setPictureResult(@Nullable PictureResult im) {
        image = im != null ? new WeakReference<>(im) : null;
    }

    public static void setCropBitmap(Bitmap bmp) {
        cropBmp = bmp;
    }

//    @Override
//    public boolean onTouch (View view, MotionEvent event) {
//        Intent intent = new Intent(PicturePreviewActivity.this, ResultsActivity.class);
//
//        Bundle extras = getIntent().getExtras();
//
//        float neutral = extras.getFloat("neutral");
//        float happy = extras.getFloat("happy");
//        float sad = extras.getFloat("sad");
//        float surprise = extras.getFloat("surprise");
//        float fear = extras.getFloat("fear");
//        float disgust = extras.getFloat("disgust");
//        float anger = extras.getFloat("angry");
//
//        intent.putExtra("neutral", neutral);
//        intent.putExtra("happy", happy);
//        intent.putExtra("sad", sad);
//        intent.putExtra("surprise", surprise);
//        intent.putExtra("fear", fear);
//        intent.putExtra("disgust", disgust);
//        intent.putExtra("angry", anger);
//
//        startActivity(intent);
//
//        return true;
//    }
    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.edit) {

            Intent intent = new Intent(PicturePreviewActivity.this, ResultsActivity.class);

            Bundle extras = getIntent().getExtras();

            float neutral = extras.getFloat("neutral");
            float happy = extras.getFloat("happy");
            float sad = extras.getFloat("sad");
            float surprise = extras.getFloat("surprise");
            float fear = extras.getFloat("fear");
            float disgust = extras.getFloat("disgust");
            float anger = extras.getFloat("angry");

            intent.putExtra("neutral", neutral);
            intent.putExtra("happy", happy);
            intent.putExtra("sad", sad);
            intent.putExtra("surprise", surprise);
            intent.putExtra("fear", fear);
            intent.putExtra("disgust", disgust);
            intent.putExtra("angry", anger);

            startActivity(intent);
        }

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_preview);
        final ImageView imageView = findViewById(R.id.image);
        final MessageView captureResolution = findViewById(R.id.nativeCaptureResolution);
        final MessageView captureLatency = findViewById(R.id.captureLatency);
        final MessageView exifRotation = findViewById(R.id.exifRotation);
        PictureResult result = image == null ? null : image.get();
        if (result == null) {
            finish();
            return;
        }
        final long delay = getIntent().getLongExtra("delay", 0);
        AspectRatio ratio = AspectRatio.of(result.getSize());
        captureLatency.setTitleAndMessage("Approx. latency", delay + " milliseconds");
        captureResolution.setTitleAndMessage("Resolution", result.getSize() + " (" + ratio + ")");
        exifRotation.setTitleAndMessage("EXIF rotation", result.getRotation() + "");
        imageView.setImageBitmap(cropBmp);

        if (result.isSnapshot()) {
            // Log the real size for debugging reason.
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(result.getData(), 0, result.getData().length, options);
            if (result.getRotation() % 180 != 0) {
                Log.e("PicturePreview", "The picture full size is " + result.getSize().getHeight() + "x" + result.getSize().getWidth());
            } else {
                Log.e("PicturePreview", "The picture full size is " + result.getSize().getWidth() + "x" + result.getSize().getHeight());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            setPictureResult(null);
        }
    }
}
