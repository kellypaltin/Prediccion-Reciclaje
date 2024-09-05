package com.example.projectap;

import static org.opencv.core.Core.flip;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2RGB;
import static org.opencv.imgproc.Imgproc.cvtColor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class Camara extends CameraActivity {

    String TAG = "CameraActivity";
    Mat mRgba;
    CameraBridgeViewBase cameraBridgeViewBase;
    int frameHeight, frameWhit;
    ImageView take_photo_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camara);

        getPermission();
        cameraBridgeViewBase = findViewById(R.id.cameraView);
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                frameHeight = height;
                frameWhit = width;

            }

            @Override
            public void onCameraViewStopped() {

            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                mRgba = inputFrame.rgba().t();
                flip(mRgba, mRgba, 1);
                return mRgba;
            }
        });

        if(OpenCVLoader.initDebug()){
            cameraBridgeViewBase.enableView();
        }

        take_photo_btn = findViewById(R.id.take_photo_btn);
        take_photo_btn.setOnClickListener(v -> save(mRgba,"Predicciones"));

    }
    private void save(Mat img, String name) {

        File filter = new File(getExternalFilesDir(null), name);
        if (!filter.exists()) filter.mkdirs();
        File[] files = filter.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        cvtColor(img, img, COLOR_BGR2RGB);

        Intent intent = new Intent(this, SecondActivity.class);
        String filename = "Prediccion.jpg";
        File file = new File(filter, filename);
        boolean bool = Imgcodecs.imwrite(file.toString(), img);
        if (bool) {
            Log.i(TAG, "SUCCESS writing image to external storage ");
            intent.putExtra("imagePath", file.getAbsolutePath());
        } else {
            Log.i(TAG, "Fail writing image to external storage");
        }
        startActivity(intent);
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }


    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }

    void getPermission(){
        if(checkSelfPermission(android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{android.Manifest.permission.CAMERA},101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0]!= PackageManager.PERMISSION_GRANTED){
            getPermission();
        }
    }
}