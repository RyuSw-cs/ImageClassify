package com.example.imageclassifier;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

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
        cls = new Classifier(this);
        try{
            cls.init();
        }catch(IOException e){
            e.printStackTrace();
        }
        if(savedInstanceState != null){
            Uri uri = savedInstanceState.getParcelable(KEY_SELECTED_URI);
            if(uri != null) selectedImageUri = uri;
        }
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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK && requestCode == CAMERA_IMAGE_REQUEST_CODE){
            Bitmap bitmap = null;
            try{
                if(Build.VERSION.SDK_INT>=29){
                    ImageDecoder.Source src = ImageDecoder.createSource(getContentResolver(),selectedImageUri);
                    bitmap = ImageDecoder.decodeBitmap(src);
                }else{ //
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),selectedImageUri);
                }
            }catch(IOException e){
                Log.e("Failed to read Image", e.toString());
            }
            if(bitmap != null){
                Pair<String,Float> output = cls.classify(bitmap);
                String resultStr = String.format(Locale.ENGLISH,"클래스 : %s\n정확도 : %.2f%%",output.first,output.second*100);
                imageView.setImageBitmap(bitmap);
                resultText.setText(resultStr);
            }
        }
    }
}
