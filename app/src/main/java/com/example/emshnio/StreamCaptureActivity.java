package com.example.emshnio;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IRadarDataSet;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Mode;
import com.otaliastudios.cameraview.PictureResult;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class StreamCaptureActivity extends AppCompatActivity implements View.OnClickListener, ControlView.Callback {

    private CameraView camera;
    private ViewGroup controlPanel;

    String[] emojiMap = {"\uD83D\uDE10", "\uD83D\uDE00", "\uD83D\uDE22", "\uD83D\uDE32", "\uD83D\uDE31", "\uD83E\uDD22", "\uD83D\uDE21"};

    // To show stuff in the callback
    private long mCaptureTime;

    /* Threading vars */
    View editText;
    Thread imgStreamThread;
    float[][] pictureStreamOutput;
    PictureResult pictureStreamResult;

    boolean onStream;

    Inference predictor;

    private boolean displayMode;

    public static int[] argsort(final float[] a, final boolean ascending) {
        Integer[] indexes = new Integer[a.length];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
        }
        Arrays.sort(indexes, new Comparator<Integer>() {
            @Override
            public int compare(final Integer i1, final Integer i2) {
                return (ascending ? 1 : -1) * Float.compare(a[i1], a[i2]);
            }
        });

        int[] ret = new int[indexes.length];
        for(int i = 0; i  < ret.length; i++)
            ret[i] = indexes[i];

        return ret;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream_capture);
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
//        findViewById(R.id.capturePicture).setOnClickListener(this);
//        findViewById(R.id.capturePictureSnapshot).setOnClickListener(this);
        findViewById(R.id.captureVideo).setOnClickListener(this);
//        findViewById(R.id.captureVideoSnapshot).setOnClickListener(this);
        findViewById(R.id.toggleCamera).setOnClickListener(this);
        findViewById(R.id.modeToggle).setOnClickListener(this);

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

        View radar = findViewById(R.id.radarchart);
        radar.setVisibility(View.INVISIBLE);

        onStream = false;

        predictor = new Inference(this);
        displayMode = false; /* Start on emoji mode ?*/

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
//            case R.id.capturePicture: capturePictureSnapshot(); break;
//            case R.id.capturePictureSnapshot: capturePictureSnapshot(); break;
            case R.id.captureVideo: backgroundTask(); break;
//            case R.id.captureVideoSnapshot: captureVideoSnapshot(); break;
            case R.id.toggleCamera: toggleCamera(); break;
            case R.id.modeToggle: switchDisplayMode(); break;
        }
    }

    private void switchDisplayMode() {

        View radar = findViewById(R.id.radarchart);

        if (radar.getVisibility() == View.VISIBLE) {

            /* Turning off image stream */
            radar.setVisibility(View.INVISIBLE);
            displayMode = false;

        } else if (radar.getVisibility() == View.INVISIBLE) {

            /* Turning on image stream */
            radar.setVisibility(View.VISIBLE);
            displayMode = true;


            RadarChart chart = (RadarChart) radar;
            chart.setBackgroundColor(Color.argb(0x00, 0x00, 0x85, 0x77));

            chart.getDescription().setEnabled(false);

            chart.setWebLineWidth(1f);
            chart.setWebColor(Color.LTGRAY);
            chart.setWebLineWidthInner(1f);
            chart.setWebColorInner(Color.LTGRAY);
            chart.setWebAlpha(50);

            MarkerView mv = new RadarMarkerView(this, R.layout.radar_markerview);
            mv.setChartView(chart); // For bounds control
            chart.setMarker(mv); // Set the marker to the chart

            setData(null);

            chart.animateXY(400, 400, Easing.EaseInOutQuad);

            XAxis xAxis = chart.getXAxis();
//            xAxis.setTypeface(tfLight);
            xAxis.setTextSize(9f);
            xAxis.setYOffset(0f);
            xAxis.setXOffset(0f);
            xAxis.setValueFormatter(new MyXAxisValueFormatter());
            xAxis.setTextColor(Color.WHITE);

            YAxis yAxis = chart.getYAxis();
//            yAxis.setTypeface(tfLight);
            yAxis.setLabelCount(5, false);
            yAxis.setTextSize(9f);
            yAxis.setAxisMinimum(0f);
            yAxis.setAxisMaximum(00f);
            yAxis.setDrawLabels(false);

            Legend l = chart.getLegend();
            l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
            l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            l.setDrawInside(false);
//            l.setTypeface(tfLight);
            l.setXEntrySpace(7f);
            l.setYEntrySpace(5f);
            l.setTextColor(Color.WHITE);

        }

    }

    class MyXAxisValueFormatter implements IAxisValueFormatter {


        private final String[] mActivities = new String[]{"Neutral", "Happy", "Sad", "Surprise", "Fear", "Disgust", "Anger"};

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return mActivities[(int) value % mActivities.length];
        }
    }

    private void setData(float[] emoData) {

        boolean nullInp = (emoData == null);
        ArrayList<RadarEntry> entries1 = new ArrayList<>();

        // NOTE: The order of the entries when being added to the entries array determines their position around the center of
        // the chart.
        for (int i = 0; i < 7; i++) {
            if (nullInp) entries1.add(new RadarEntry(0f));
            else entries1.add(new RadarEntry(emoData[i] *100f));
        }

        RadarDataSet set1 = new RadarDataSet(entries1, "Emotion Distribution");
//        set1.setColor(Color.rgb(121, 162, 175));
//        set1.setFillColor(Color.rgb(121, 162, 175));
        set1.setColor(Color.rgb(0xD8, 0x1B, 0x60));
        set1.setFillColor(Color.rgb(0xD8, 0x1B, 0x60));
        set1.setDrawFilled(true);
        set1.setFillAlpha(180);
        set1.setLineWidth(2f);
        set1.setDrawHighlightCircleEnabled(true);
        set1.setDrawHighlightIndicators(false);


        ArrayList<IRadarDataSet> sets = new ArrayList<>();
        sets.add(set1);

        RadarData data = new RadarData(sets);
//        data.setValueTypeface(tfLight);
        data.setValueTextSize(8f);
        data.setDrawValues(false);
        data.setValueTextColor(Color.WHITE);

        RadarChart chart = findViewById(R.id.radarchart);
        chart.setData(data);
        chart.invalidate();
    }

    private void updateData(float[] emoData) {

        RadarChart chart = findViewById(R.id.radarchart);
        chart.clearValues();
        chart.invalidate();
        chart.clear();
        setData(emoData);
//        ArrayList<RadarEntry> entries1 = new ArrayList<>();
//
//        // NOTE: The order of the entries when being added to the entries array determines their position around the center of
//        // the chart.
//        for (int i = 0; i < 7; i++) {
//            entries1.add(new RadarEntry(emoData[i] *100f));
//        }
//
//        RadarDataSet set1 = new RadarDataSet(entries1, "Emotion Distribution");
//        set1.setColor(Color.rgb(121, 162, 175));
//        set1.setFillColor(Color.rgb(121, 162, 175));
//        set1.setDrawFilled(true);
//        set1.setFillAlpha(180);
//        set1.setLineWidth(2f);
//        set1.setDrawHighlightCircleEnabled(true);
//        set1.setDrawHighlightIndicators(false);
//
//
//        ArrayList<IRadarDataSet> sets = new ArrayList<>();
//        sets.add(set1);
//
//        RadarData data = new RadarData(sets);
////        data.setValueTypeface(tfLight);
//        data.setValueTextSize(8f);
//        data.setDrawValues(false);
//        data.setValueTextColor(Color.WHITE);
//
//
//        chart.setData(data);
//        chart.invalidate();
    }


    @Override
    public void onBackPressed() {
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        if (b.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            b.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }
        if (imgStreamThread != null) imgStreamThread.interrupt();

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

//        if (editText.getVisibility() == View.VISIBLE) {

        if (onStream) {

            /* Turning off image stream */
            editText.setVisibility(View.INVISIBLE);
            onStream = false;
            imgStreamThread.interrupt();
            ImageButton imageButton = (ImageButton)(findViewById(R.id.captureVideo));
            imageButton.setImageResource(R.drawable.ic_video);

//        } else if (editText.getVisibility() == View.INVISIBLE) {
        } else {

            /* Turning on image stream */
            if (!displayMode) editText.setVisibility(View.VISIBLE);

            /* Background thread --> */
            imgStreamThread = new Thread("PictureStream") {
                private int count = 0;
                private int[] emoArgs;

                private boolean mode = displayMode; /* Mode can't change during operation */

                @Override
                public void run() {
                    ImageButton imageButton = (ImageButton)(findViewById(R.id.captureVideo));
                    imageButton.setImageResource(R.drawable.ic_video_record);
                    count = 0;
                    /* While interrupt not received ... */
                    while (!this.isInterrupted()) {

                        /* Get a picture, and do inference on it*/
                        capturePictureSnapshot();
                        if (pictureStreamResult != null) {

                            pictureStreamOutput = predictor.doInference(pictureStreamResult);
                            emoArgs = argsort(pictureStreamOutput[0], false);

                        }

                        /* Put "real-time" results into UI */
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                if (pictureStreamOutput != null) {

                                    if (mode) {

                                        RadarChart chart = findViewById(R.id.radarchart);

                                        /* set graph data */
                                        updateData(pictureStreamOutput[0]);

                                        chart.animateXY(200, 200, Easing.EaseInOutQuad);

                                        XAxis xAxis = chart.getXAxis();
//            xAxis.setTypeface(tfLight);
                                        xAxis.setTextSize(9f);
                                        xAxis.setYOffset(0f);
                                        xAxis.setXOffset(0f);
                                        xAxis.setValueFormatter(new MyXAxisValueFormatter());
                                        xAxis.setTextColor(Color.WHITE);

                                        YAxis yAxis = chart.getYAxis();
//            yAxis.setTypeface(tfLight);
                                        yAxis.setLabelCount(5, false);
                                        yAxis.setTextSize(9f);
                                        yAxis.setAxisMinimum(0f);
                                        yAxis.setAxisMaximum(80f);
                                        yAxis.setDrawLabels(false);

                                        Legend l = chart.getLegend();
                                        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
                                        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
                                        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
                                        l.setDrawInside(false);
//            l.setTypeface(tfLight);
                                        l.setXEntrySpace(7f);
                                        l.setYEntrySpace(5f);
                                        l.setTextColor(Color.WHITE);


                                    } else {
                                        setEmoji(emoArgs);
                                    }

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


    private void setEmoji(int[] emoArgs) {
        TextView first = findViewById(R.id.first);
        TextView second = findViewById(R.id.second);
        TextView third = findViewById(R.id.third);

        first.setText(emojiMap[emoArgs[0]]);
        second.setText(emojiMap[emoArgs[1]]);
        third.setText(emojiMap[emoArgs[2]]);
    }

    private void launchGraph(int[] emoArgs) {
        TextView first = findViewById(R.id.first);
        TextView second = findViewById(R.id.second);
        TextView third = findViewById(R.id.third);

        first.setText(emojiMap[emoArgs[0]]);
        second.setText(emojiMap[emoArgs[1]]);
        third.setText(emojiMap[emoArgs[2]]);
    }

}
