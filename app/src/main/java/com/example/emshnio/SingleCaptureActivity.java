package com.example.emshnio;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Mode;
import com.otaliastudios.cameraview.PictureResult;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class SingleCaptureActivity extends AppCompatActivity implements View.OnClickListener, ControlView.Callback {

    private CameraView camera;
    private ViewGroup controlPanel;

    // To show stuff in the callback
    private long mCaptureTime;

    private Interpreter tflite;

    /* Threading vars */
    View editText;
    Thread imgStreamThread;
    float[][] pictureStreamOutput;
    PictureResult pictureStreamResult;

    boolean onStream;

    public static Bitmap doRotate(Bitmap src, float degree) {

        // create new matrix
        Matrix matrix = new Matrix();

        // setup rotation degree
        matrix.postRotate(degree);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);

    }

    private MappedByteBuffer loadModelFile() throws IOException {

        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("new_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

    }

    private float[][] doInference(PictureResult pictureResult) {

        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inMutable = true;
        Bitmap bmp = BitmapFactory.decodeByteArray(pictureResult.getData(), 0, pictureResult.getData().length, opt);

        float rotation = (float)(pictureResult.getRotation() / 90);

        if (rotation != 0) bmp = doRotate(bmp, rotation * 90);

        float [][] output = new float[1][7];

        Bitmap bmp565 = bmp.copy(Bitmap.Config.RGB_565, false);
        FaceDetector detector = new FaceDetector(bmp565.getWidth(), bmp565.getHeight(), 1);
        FaceDetector.Face[] faces = new FaceDetector.Face[1];
        detector.findFaces(bmp565, faces);

        if (faces[0] != null){
            PointF mid = new PointF();
            faces[0].getMidPoint(mid);
            float dist = faces[0].eyesDistance();
            mid.y = mid.y + 0.3f*dist;

            float cropWidth  = Math.min(Math.min(3f*dist, 2f*mid.x), 2f*(bmp.getWidth() - mid.x));
            float cropHeight = Math.min(Math.min(3f*dist, 2f*mid.y), 2f*(bmp.getHeight() - mid.y));

            float cropDim = Math.min(cropWidth, cropHeight);

            Bitmap cropBmp = bmp;
            Canvas c = new Canvas(cropBmp);
            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(10);
            c.drawRect(mid.x - cropDim/2f,
                    mid.y - cropDim/2f,
                    mid.x - cropDim/2f + cropDim,
                    mid.y - cropDim/2f + cropDim,
                    paint);
            PicturePreviewActivity.setCropBitmap(cropBmp);
            bmp = Bitmap.createBitmap(bmp, (int)(mid.x - cropDim/2f), (int)(mid.y - cropDim/2f), (int)cropDim, (int)cropDim);
        }
        bmp = Bitmap.createScaledBitmap(bmp, 200, 200, false);

        try {
            tflite = new Interpreter(loadModelFile());
//            InputStream is = this.getAssets().open("nikola_happy.jpg");
//            netInput = BitmapFactory.decodeStream(is);


            float [][][][] img = new float[1][200][200][3];
            for (int i = 0; i < 200; i++){
                for (int j = 0; j < 200; j++){
                    int p = bmp.getPixel(j, i);
                    img[0][i][j][0] = ((p >> 16) & 0xff);
                    img[0][i][j][1] = ((p >> 8) & 0xff);
                    img[0][i][j][2] = (p & 0xff);
                }
            }
            tflite.run(img, output);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return output;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);

        camera = findViewById(R.id.camera);
        camera.setLifecycleOwner(this);
        camera.addCameraListener(new CameraListener() {
            public void onCameraOpened(@NonNull CameraOptions options) { onOpened(options); }
            public void onPictureTaken(@NonNull PictureResult result) { onPicture(result); }
//            public void onVideoTaken(@NonNull VideoResult result) { onVideo(result); }
            public void onCameraError(@NonNull CameraException exception) {
                onError(exception);
            }
        });

        findViewById(R.id.edit).setOnClickListener(this);
        findViewById(R.id.capturePicture).setOnClickListener(this);
//        findViewById(R.id.capturePictureSnapshot).setOnClickListener(this);
        findViewById(R.id.captureVideo).setOnClickListener(this);
//        findViewById(R.id.captureVideoSnapshot).setOnClickListener(this);
        findViewById(R.id.toggleCamera).setOnClickListener(this);

        controlPanel = findViewById(R.id.controls);
        ViewGroup group = (ViewGroup) controlPanel.getChildAt(0);
        Control[] controls = Control.values();
        for (Control control : controls) {
            ControlView view = new ControlView(this, control, this);
            group.addView(view,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }

        controlPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
                b.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });


        editText = findViewById(R.id.testVal);
        editText.setVisibility(View.INVISIBLE);

        onStream = false;

    }

    private void message(String content, boolean important) {
        int length = important ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        Toast.makeText(this, content, length).show();
    }

    private void onOpened(CameraOptions options) {
        ViewGroup group = (ViewGroup) controlPanel.getChildAt(0);
        for (int i = 0; i < group.getChildCount(); i++) {
            ControlView view = (ControlView) group.getChildAt(i);
            view.onCameraOpened(camera, options);
        }
    }

    private void onError(@NonNull CameraException exception) {
        message("Got CameraException #" + exception.getReason(), true);
    }

    private void onPicture(PictureResult result) {

        if (onStream) pictureStreamResult = result;

        else {

            if (camera.isTakingVideo()) {
                message("Captured while taking video. Size=" + result.getSize(), false);
                return;
            }

            // This can happen if picture was taken with a gesture.
            long callbackTime = System.currentTimeMillis();
            if (mCaptureTime == 0) mCaptureTime = callbackTime - 300;
            float[][] netOutput = doInference(result);

            PicturePreviewActivity.setPictureResult(result);
            PicturePreviewActivity.setInferenceResult(netOutput);
            ResultsActivity.setInferenceResult(netOutput);


            Intent intent = new Intent(SingleCaptureActivity.this, PicturePreviewActivity.class);
//        Intent intent = new Intent(SingleCaptureActivity.this, ResultsActivity.class);
            intent.putExtra("delay", callbackTime - mCaptureTime);

//            float neutral = netOutput[0][0];
//            float happy = netOutput[0][1];
//            float sad = netOutput[0][2];
//            float surprise = netOutput[0][3];
//            float fear = netOutput[0][4];
//            float disgust = netOutput[0][5];
//            float angry = netOutput[0][6];
//
//            intent.putExtra("neutral", neutral);
//            intent.putExtra("happy", happy);
//            intent.putExtra("sad", sad);
//            intent.putExtra("surprise", surprise);
//            intent.putExtra("fear", fear);
//            intent.putExtra("disgust", disgust);
//            intent.putExtra("angry", angry);

            startActivity(intent);
            mCaptureTime = 0;
        }
    }

//    private void onVideo(VideoResult video) {
//        VideoPreviewActivity.setVideoResult(video);
//        Intent intent = new Intent(SingleCaptureActivity.this, VideoPreviewActivity.class);
//        startActivity(intent);
//    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.edit: edit(); break;
            case R.id.capturePicture: capturePictureSnapshot(); break;
//            case R.id.capturePictureSnapshot: capturePictureSnapshot(); break;
            case R.id.captureVideo: backgroundTask(); break;
//            case R.id.captureVideoSnapshot: captureVideoSnapshot(); break;
            case R.id.toggleCamera: toggleCamera(); break;
        }
    }


    @Override
    public void onBackPressed() {
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        if (b.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            b.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }
        super.onBackPressed();
    }

    private void edit() {
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        b.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void capturePicture() {
        if (camera.getMode() == Mode.VIDEO) {
            message("Can't take HQ pictures while in VIDEO mode.", false);
            return;
        }
        if (camera.isTakingPicture()) return;

        mCaptureTime = System.currentTimeMillis();
//        message("Capturing picture...", false);
        camera.takePicture();
    }

    private void capturePictureSnapshot() {
        if (camera.isTakingPicture()) return;
        mCaptureTime = System.currentTimeMillis();
//        message("Capturing picture snapshot...", false);
        camera.takePictureSnapshot();
    }

    private void captureVideo() {
        if (camera.getMode() == Mode.PICTURE) {
            message("Can't record HQ videos while in PICTURE mode.", false);
            return;
        }
        if (camera.isTakingPicture() || camera.isTakingVideo()) return;
        message("Recording for 5 seconds...", true);
        camera.takeVideo(new File(getFilesDir(), "video.mp4"), 5000);
    }

    private void captureVideoSnapshot() {
        if (camera.isTakingVideo()) {
            message("Already taking video.", false);
            return;
        }
        message("Recording snapshot for 5 seconds...", true);
        camera.takeVideoSnapshot(new File(getFilesDir(), "video.mp4"), 5000);
    }

    private void toggleCamera() {
        if (camera.isTakingPicture() || camera.isTakingVideo()) return;
        switch (camera.toggleFacing()) {
            case BACK:
                message("Switched to back camera!", false);
                break;

            case FRONT:
                message("Switched to front camera!", false);
                break;
        }
    }

    @Override
    public boolean onValueChanged(Control control, Object value, String name) {
        if (!camera.isHardwareAccelerated() && (control == Control.WIDTH || control == Control.HEIGHT)) {
            if ((Integer) value > 0) {
                message("This device does not support hardware acceleration. " +
                        "In this case you can not change width or height. " +
                        "The view will act as WRAP_CONTENT by default.", true);
                return false;
            }
        }
        control.applyValue(camera, value);
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        b.setState(BottomSheetBehavior.STATE_HIDDEN);
        message("Changed " + control.getName() + " to " + name, false);
        return true;
    }

    //region Permissions

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean valid = true;
        for (int grantResult : grantResults) {
            valid = valid && grantResult == PackageManager.PERMISSION_GRANTED;
        }
        if (valid && !camera.isOpened()) {
            camera.open();
        }
    }

    //endregion

    /* Image stream background task*/
    private void backgroundTask() {

        if (editText.getVisibility() == View.VISIBLE) {

            /* Turning off image stream */
            editText.setVisibility(View.INVISIBLE);
            onStream = false;
            imgStreamThread.interrupt();

        } else if (editText.getVisibility() == View.INVISIBLE) {

            /* Turning on image stream */
            editText.setVisibility(View.VISIBLE);

            /* Background thread --> */
            imgStreamThread = new Thread("PictureStream") {
                private int count = 0;

                @Override
                public void run() {

                    count = 0;
                    /* While interrupt not received ... */
                    while (!this.isInterrupted()) {

                        /* Get a picture, and do inference on it*/
                        capturePictureSnapshot();
                        if (pictureStreamResult != null) pictureStreamOutput = doInference(pictureStreamResult);

                        /* Put "real-time" results into UI */
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                if (pictureStreamOutput != null) {

                                    TextView neutral = findViewById(R.id.neutral);
                                    TextView happy = findViewById(R.id.happy);
                                    TextView sad = findViewById(R.id.sad);
                                    TextView surprise = findViewById(R.id.surprise);
                                    TextView fear = findViewById(R.id.fear);
                                    TextView disgust = findViewById(R.id.disgust);
                                    TextView anger = findViewById(R.id.anger);


                                    neutral.setText(Float.toString(pictureStreamOutput[0][0]));
                                    happy.setText(Float.toString(pictureStreamOutput[0][1]));
                                    sad.setText(Float.toString(pictureStreamOutput[0][2]));
                                    surprise.setText(Float.toString(pictureStreamOutput[0][3]));
                                    fear.setText(Float.toString(pictureStreamOutput[0][4]));
                                    disgust.setText(Float.toString(pictureStreamOutput[0][5]));
                                    anger.setText(Float.toString(pictureStreamOutput[0][6]));

                                }
                            }
                        });

                    }

                }
            };

            /* Turn on image stream */
            onStream = true;
            imgStreamThread.start();
        }
    }


}
