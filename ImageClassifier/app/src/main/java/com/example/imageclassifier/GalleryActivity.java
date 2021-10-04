package com.example.imageclassifier;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;

public class GalleryActivity extends AppCompatActivity {
    private Classifier cls;
    private ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        cls = new Classifier(this);
        Bitmap icon = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.glass103);
        try{
            cls.init();
        } catch(IOException e){
            Log.d("DigitClassifier", "failed to init Classifier",e);
        }
        cls.classify(icon);
        Log.d("끝", "~~");

    }
}