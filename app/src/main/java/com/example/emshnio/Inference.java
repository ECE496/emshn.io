package com.example.emshnio;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;

import com.otaliastudios.cameraview.PictureResult;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import androidx.appcompat.app.AppCompatActivity;

class Inference {

    private AppCompatActivity caller;

    Inference(AppCompatActivity caller) {
        this.caller = caller;
    }

    private static Bitmap doRotate(Bitmap src, float degree) {

        // create new matrix
        Matrix matrix = new Matrix();

        // setup rotation degree
        matrix.postRotate(degree);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);

    }

    private MappedByteBuffer loadModelFile() throws IOException {

        AssetFileDescriptor fileDescriptor = caller.getAssets().openFd("new_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

    }

    float[][] doInference(PictureResult pictureResult) {

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
            Interpreter tflite = new Interpreter(loadModelFile());
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

}
