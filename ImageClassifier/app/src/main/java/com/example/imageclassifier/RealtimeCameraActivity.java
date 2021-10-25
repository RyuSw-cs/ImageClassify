package com.example.imageclassifier;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Fragment;
import android.app.FragmentManager;

import java.io.IOException;
import java.util.Locale;

public class RealtimeCameraActivity extends AppCompatActivity {
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int PERMISSION_REQUEST_CODE = 1;

    private TextView textView;
    private Classifier cls;

    private int previewWidth = 0;
    private int previewHeight = 0;
    private int sensorOrientation = 0;

    private Bitmap rgbFrameBitmap = null;

    private boolean isProcessingFrame = false;

    private HandlerThread handlerThread;
    private Handler handler;

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
            setFragment();
        }else{
            requestPermissions(new String[]{CAMERA_PERMISSION},PERMISSION_REQUEST_CODE);
        }

    }

    @Override
    protected synchronized void onResume() {
        super.onResume();
        handlerThread = new HandlerThread("InfereneceThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

    }

    @Override
    protected void onPause() {
        super.onPause();
        try{
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e){
            e.printStackTrace();
        }
        super.onPause();
    }
    protected synchronized void runInBackground(final Runnable r){
        if(handler != null){
            handler.post(r);
        }
    }

    private void init(){
        textView = findViewById(R.id.result);
    }

    protected void setFragment(){
        Size inputSize = cls.getModelInputSize();
        String cameraId = chooseCamera();
        if(inputSize.getWidth() > 0 && inputSize.getHeight() > 0 && cameraId != null){
             Fragment fragment = RealtimeCameraFragment.newInstance(
                     (size,rotation) ->{
                         previewHeight = size.getHeight();
                         previewWidth = size.getWidth();
                         sensorOrientation = rotation - getScreenOrientation();
                     },
                     reader->processImage(reader),
                     inputSize, 
                     cameraId);
             getFragmentManager().beginTransaction().replace(R.id.fragment,fragment).commit();
        }else{
            Toast.makeText(this,"카메라를 찾을 수 없습니다",Toast.LENGTH_SHORT).show();
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
                 setFragment();
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

    protected  void processImage(ImageReader reader){
        if(previewWidth == 0 || previewHeight == 0) return;
        if(rgbFrameBitmap == null){
            rgbFrameBitmap = Bitmap.createBitmap(previewWidth,previewHeight, Bitmap.Config.ARGB_8888);
        }
        if(isProcessingFrame) return;
        isProcessingFrame = true;
        final Image image = reader.acquireNextImage();
        if(image == null){
            isProcessingFrame = false;
            return;
        }
        YuvToRgbConverter.yuvToRgb(this,image,rgbFrameBitmap);
        runInBackground(()->{
            if(cls != null && cls.isInitialized()){
                final Pair<String,Float> output = cls.classify(rgbFrameBitmap,sensorOrientation);
                runOnUiThread(()->{
                    String resultStr = String.format(Locale.ENGLISH,"클래스 : %s\n정확도 : %.2f%%",output.first,output.second*100);
                    textView.setText(resultStr);
                });

            }
            image.close();
            isProcessingFrame = false;
        });

    }
}
