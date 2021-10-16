package com.example.imageclassifier;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.io.IOException;

public class RealtimeCameraActivity extends AppCompatActivity {
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int PERMISSION_REQUEST_CODE = 1;

    private TextView textView;
    private Classifier cls;

    private int previewWidth = 0;
    private int previewHeight = 0;
    private int sensorOrientation = 0;

    private Bitmap rgbFrameBitmap = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_realtime_camera);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        init();
        cls = new Classifier(this);
        try{
            cls.init();
        }catch (IOException e){
            e.printStackTrace();
        }
        if(checkSelfPermission(CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED){

        }else{
            requestPermissions(new String[]{CAMERA_PERMISSION},PERMISSION_REQUEST_CODE);
        }

    }


    private void init(){
        textView = findViewById(R.id.result);
    }
    
    protected void setFragment(){
        Size inputSize = cls.getModelInputSize();
        String cameraId = chooseCamera();
        if(inputSize.getWidth() > 0 && inputSize.getHeight() > 0 && cameraId != null){
            //프래그먼트 생성
        }

    }

    protected int getScreenOrientation(){
        switch (getDisplay().getRotation()){
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    @Nullable
    private String chooseCamera(){
        final CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            for(final String cameraId : manager.getCameraIdList()){
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if(facing != null && facing == CameraCharacteristics.LENS_FACING_BACK){
                    return cameraId;
                }
            }
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSION_REQUEST_CODE){
            if(grantResults.length >0 && allPermissionsGranted(grantResults)){

            }
            else{
                Toast.makeText(this,"권한 거부", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    private boolean allPermissionsGranted(final int[] grantResults){
        for(int result : grantResults){
            if(result != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }
}
