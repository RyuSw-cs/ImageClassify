package com.example.imageclassifier;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class RealtimeCameraActivity extends AppCompatActivity {
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int PERMISSION_REQUEST_CODE = 1;

    private TextView textView;
    private Classifier cls;

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
    void init(){
        textView = findViewById(R.id.result);
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
