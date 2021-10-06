package com.example.imageclassifier;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.Locale;

public class GalleryActivity extends AppCompatActivity {
    public static final int GALLERY_IMAGE_REQUEST_CODE = 1;
    private Classifier cls;
    private ImageView imageView;
    private Button classifyButton;
    private TextView resultText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        init();


        cls = new Classifier(this);
        Bitmap icon = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.paper15);
        try{
            cls.init();
        } catch(IOException e){
            Log.d("DigitClassifier", "failed to init Classifier",e);
        }
        classifyButton.setOnClickListener(view->{
            getImageFromGallery();
        });

    }
    private void init(){
         imageView = findViewById(R.id.imageView);
         classifyButton = findViewById(R.id.classifyButton);
         resultText = findViewById(R.id.resultText);
    }
    private void getImageFromGallery(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"); // 기기에 저장된 모든 이미지를 가져올 수 있음.
        startActivityForResult(intent,GALLERY_IMAGE_REQUEST_CODE); //데이터를 전달 받기 위함, 상수 코드 사용
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK && requestCode == GALLERY_IMAGE_REQUEST_CODE){
            if(data == null) return;
            Uri selectedImage = data.getData();
            Bitmap bitmap = null;
            try{
                if(Build.VERSION.SDK_INT>=29){
                    ImageDecoder.Source src = ImageDecoder.createSource(getContentResolver(),selectedImage);
                    bitmap = ImageDecoder.decodeBitmap(src);
                }else{ //
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),selectedImage);
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