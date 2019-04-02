package com.example.emshnio;

import android.graphics.Color;
import android.os.Bundle;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ResultsActivity extends AppCompatActivity {

    private static WeakReference<float[][]> inferenceResult;

    public static void setInferenceResult(@Nullable float[][] netOutput) {
        inferenceResult = netOutput != null ? new WeakReference<>(netOutput) : null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //PieChart pieChart = findViewById(R.id.piechart);
        //pieChart.setUsePercentValues(true);
        BarChart barChart = findViewById(R.id.barchart);
        barChart.setNoDataText("");
        barChart.getDescription().setEnabled(false);

        //List<PieEntry> value = new ArrayList<>();
        List<BarEntry> entries= new ArrayList<>();

        Bundle extras = getIntent().getExtras();

//        assert extras != null;
//        float neutral = extras.getFloat("neutral");
//        float happy = extras.getFloat("happy");
//        float sad = extras.getFloat("sad");
//        float surprise = extras.getFloat("surprise");
//        float fear = extras.getFloat("fear");
//        float disgust = extras.getFloat("disgust");
//        float anger = extras.getFloat("angry");
//
        float neutral = inferenceResult.get()[0][0];
        float happy = inferenceResult.get()[0][1];
        float sad = inferenceResult.get()[0][2];
        float surprise = inferenceResult.get()[0][3];
        float fear = inferenceResult.get()[0][4];
        float disgust = inferenceResult.get()[0][5];
        float anger = inferenceResult.get()[0][6];

        float values[] = new float[]{neutral, happy, sad, surprise, fear, disgust, anger};
        final String[] emotions = new String[]{"Neutral", "Happy", "Sad", "Surprise","Fear","Disgust","Anger"};
        int max1_index = 0;
        int max2_index = 0;
        int max3_index = 0;
        float maxValue1 = 0;
        float maxValue2 = 0;
        float maxValue3 = 0;

//        for(int i=0;i < values.length;i++) {
//            if (values[i] > maxValue1) {
//                maxValue1 = values[i];
//                max1_index = i;
//            }
//        }
//        for(int i=0;i < values.length;i++) {
//            if (values[i] > maxValue2 && values[i] < maxValue1) {
//                maxValue2 = values[i];
//                max2_index = i;
//            }
//        }
//        for(int i=0;i < values.length;i++) {
//            if (values[i] > maxValue3 && values[i] < maxValue2) {
//                maxValue3 = values[i];
//                max3_index = i;
//            }
//        }

//        entries.add(new BarEntry(0f, maxValue1));
//        entries.add(new BarEntry(1f, maxValue2));
//        entries.add(new BarEntry(2f, maxValue3));

        ArrayList<BarEntry> neutralValue = new ArrayList<>();
        ArrayList<BarEntry> happyValue = new ArrayList<>();
        ArrayList<BarEntry> sadValue = new ArrayList<>();
        ArrayList<BarEntry> surpriseValue = new ArrayList<>();
        ArrayList<BarEntry> fearValue = new ArrayList<>();
        ArrayList<BarEntry> disgustValue = new ArrayList<>();
        ArrayList<BarEntry> angerValue = new ArrayList<>();

//        entries.add(new BarEntry(0f, neutral));
//        entries.add(new BarEntry(1f, happy));
//        entries.add(new BarEntry(2f, sad));
//        entries.add(new BarEntry(3f, surprise));
//        entries.add(new BarEntry(4f, fear));
//        entries.add(new BarEntry(5f, disgust));
//        entries.add(new BarEntry(6f, anger));
        neutralValue.add(new BarEntry(0f, neutral));
        happyValue.add(new BarEntry(1f, happy));
        sadValue.add(new BarEntry(2f, sad));
        surpriseValue.add(new BarEntry(3f, surprise));
        fearValue.add(new BarEntry(4f, fear));
        disgustValue.add(new BarEntry(5f, disgust));
        angerValue.add(new BarEntry(6f, anger));

//        BarDataSet set = new BarDataSet(entries, "Emotion Distribution");

        BarDataSet neutralSet = new BarDataSet(neutralValue, "Neutral");
        BarDataSet happySet = new BarDataSet(happyValue, "Happy");
        BarDataSet sadSet = new BarDataSet(sadValue, "Sad");
        BarDataSet surpriseSet = new BarDataSet(surpriseValue, "Surprise");
        BarDataSet fearSet = new BarDataSet(fearValue, "Fear");
        BarDataSet disgustSet = new BarDataSet(disgustValue, "Disgust");
        BarDataSet angerSet = new BarDataSet(angerValue, "Anger");

        neutralSet.setColors(ColorTemplate.JOYFUL_COLORS[0]);
        happySet.setColors(ColorTemplate.JOYFUL_COLORS[1]);
        sadSet.setColors(ColorTemplate.JOYFUL_COLORS[2]);
        surpriseSet.setColors(ColorTemplate.JOYFUL_COLORS[3]);
        fearSet.setColors(ColorTemplate.JOYFUL_COLORS[4]);
        disgustSet.setColors(ColorTemplate.COLORFUL_COLORS[0]);
        angerSet.setColors(ColorTemplate.COLORFUL_COLORS[1]);

        //set.setColors(new int[] {Color.GRAY, Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.BLUE, Color.MAGENTA, Color.DKGRAY});
        BarData data = new BarData(neutralSet, happySet, sadSet, surpriseSet, fearSet, disgustSet, angerSet);
        data.setBarWidth(0.9f); // set custom bar width
//        data.setValueTextSize(17f);
        barChart.setData(data);
        barChart.setFitBars(true); // make the x-axis fit exactly all bars
        barChart.setDrawValueAboveBar(true);
//        set.setColors(ColorTemplate.JOYFUL_COLORS);
        Legend l = barChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDirection(Legend.LegendDirection.LEFT_TO_RIGHT);
        l.setDrawInside(true);
//        l.setTypeface(tfLight);
        l.setYOffset(20f);
        l.setXOffset(20f);
        l.setYEntrySpace(0f);
        l.setTextSize(8f);
        barChart.animateXY(1500,1500);
//        barChart.invalidate(); // refresh


//        Legend legend = barChart.getLegend();
//        legend.setEnabled(false);
//        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
//        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
//        legend.set
        // the labels that should be drawn on the XAxis
        //final String[] quarters = new String[] { "Neutral", "Happy", "Sad", "Surprise","Fear","Disgust","Anger" };
//        final String[] quarters = new String[] {emotions[max1_index], emotions[max2_index], emotions[max3_index]};
        final String[] quarters = new String[] { "Neutral", "Happy", "Sad", "Surprise","Fear","Disgust","Anger" };
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new MyXAxisValueFormatter(quarters));
        xAxis.setGranularity(1); // minimum axis-step (interval) is 1
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);

        YAxis axisLeft = barChart.getAxisLeft();
        axisLeft.setValueFormatter(new MyYAxisValueFormatter("%"));
        axisLeft.setAxisMinimum(0);
        axisLeft.setDrawGridLines(true);

        YAxis axisRight = barChart.getAxisRight();
        axisRight.setDrawGridLines(false);
        axisRight.setDrawAxisLine(false);
        axisRight.setDrawLabels(false);
    }

    class MyXAxisValueFormatter implements IAxisValueFormatter {

        private String[] mValues;

        MyXAxisValueFormatter(String[] values) {
            this.mValues = values;
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            // "value" represents the position of the label on the axis (x or y)
            return mValues[(int) value];
        }
    }

    class MyYAxisValueFormatter implements IAxisValueFormatter {

        private String suffix;

        MyYAxisValueFormatter(String suffix) {
            this.suffix = suffix;
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            // "value" represents the position of the label on the axis (x or y)
            return Float.toString((int)(value * 100)) + suffix;
        }
    }

}