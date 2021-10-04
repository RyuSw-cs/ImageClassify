package com.example.imageclassifier;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.util.Pair;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/*
* 모델 파일 로드
* 이미지 입력 시 추론하여 결과 값 해석
* */
public class Classifier {
    private static final String MODEL_NAME = "classification.tflite";
    private Context context;
    private Interpreter interpreter = null; //모델에 데이터를 입력하고 추론 결과를 전달받을 수 있는 클래스
    private int modelInputWidth, modelInputHeight, modelInputChannel;
    private TensorImage inputImage;
    private TensorBuffer outputBuffer;
    /*
    클래스가 생성되는 시점에 context를 적재
     */
    public Classifier(Context context){
        this.context = context;
    }

    /*
    모델 초기화 관련 초기화
     */
    public void init() throws IOException{
        ByteBuffer model = FileUtil.loadMappedFile(context,MODEL_NAME); //ByteBuffer 포맷으로 얻은 모델
        model.order(ByteOrder.nativeOrder()); // 시스템의 ByteOrder값과 동일하게 설정
        interpreter = new Interpreter(model); //스레드 개수, delegate 없이 기본적인 인터프리터 생성
        initModelShape();
    }

    /*
    모델 초기화
     */
    private void initModelShape(){
        Tensor inputTensor = interpreter.getInputTensor(0);
        int[] inputShape = inputTensor.shape();
        modelInputChannel = inputShape[0]; //1
        modelInputWidth = inputShape[1];  //320
        modelInputHeight = inputShape[2]; //320
        inputImage = new TensorImage(inputTensor.dataType());
        Tensor outputTensor = interpreter.getOutputTensor(0);
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(),outputTensor.dataType());
    }
    public void classify(Bitmap image) {
        inputImage = loadImage(image);
        interpreter.run(inputImage.getBuffer(), outputBuffer.getBuffer().rewind());
        argmax(outputBuffer.getFloatArray());
    }
    private void argmax(float[] array){
        for(int i = 0 ; i < array.length; i++){
            Log.d(String.valueOf(i), String.valueOf(array[i]));
        }
    }

    /*
    Return Type : TensorImage
    Bitmap 이미지를 받아 전처리 후 TensorImage 형태로 변환
     */
    private TensorImage loadImage(final Bitmap bitmap){
          inputImage.load(bitmap);
        ImageProcessor imageProcessor=
                new ImageProcessor.Builder()
                .add(new ResizeOp(modelInputWidth,modelInputHeight, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .add(new NormalizeOp(0.0f,255.0f))
                .build();
        return imageProcessor.process(inputImage);
    }


    /*
    입력받은 이미지 크기를 모델에 맞게 변환
    parameter : 변환할 이미지, 변환할 가로 크기, 변환할 세로 크기, false : 최근접 보간법 사용(true : 양선형 보간법)
     */
    private Bitmap resizeBitmap(Bitmap bitmap){
        return Bitmap.createScaledBitmap(bitmap,modelInputWidth,modelInputHeight,false);
    }
    /*
    ARGB 채널의 이미지를 GrayScale로 변환하고 ByteBuffer 포맷으로 변경
     */
    private ByteBuffer convertBitmapToGrayByteBuffer(Bitmap bitmap){
        ByteBuffer byteByffer = ByteBuffer.allocateDirect(bitmap.getByteCount());
        byteByffer.order(ByteOrder.nativeOrder());
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
        for(int pixel : pixels){
            int r = pixel >> 16 & 0xFF;
            int g = pixel >> 8 & 0xFF;
            int b = pixel & 0xFF;

            float avgPixelValue = (r+g+b) / 3.0f;
            float nomalizedPixelValue = avgPixelValue/ 255.0f;

            byteByffer.putFloat(nomalizedPixelValue);
        }
        return byteByffer;
    }

}

