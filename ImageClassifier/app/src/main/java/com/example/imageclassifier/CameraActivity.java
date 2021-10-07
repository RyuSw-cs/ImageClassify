package com.example.imageclassifier;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;

public class CameraActivity extends AppCompatActivity {
    public static final String TAG = "[IC]cameraActivity";
    public static final int CAMERA_IMAGE_REQUEST_CODE = 1;
    private static final String KEY_SELECTED_URI = "KEY_SELECTED_URI";
    private Classifier cls;
    private ImageView imageView;
    private TextView resultText;
    private Button cameraButton;
    Uri selectedImageUri;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        init();
        cameraButton.setOnClickListener(view -> {
            getImageFromCamera();
        });

    }
    private void init(){
        imageView = findViewById(R.id.imageView);
        cameraButton = findViewById(R.id.cameraButton);
        resultText = findViewById(R.id.resultText);
    }
    private void getImageFromCamera(){
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"picture.jpg");
        if(file.exists()) file.delete();
        selectedImageUri = FileProvider.getUriForFile(this, getPackageName(), file);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,selectedImageUri);
        startActivityForResult(intent,CAMERA_IMAGE_REQUEST_CODE);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_SELECTED_URI, selectedImageUri);
    }
}
