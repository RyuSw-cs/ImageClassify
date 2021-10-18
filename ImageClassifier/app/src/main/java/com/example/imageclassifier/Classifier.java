package com.example.imageclassifier;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.util.Pair;
import android.util.Size;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.model.Model;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
* 모델 파일 로드
* 이미지 입력 시 추론하여 결과 값 해석
* */
public class Classifier {
    private static final String MODEL_NAME = "model.tflite";
    private static final String LABEL_FILE = "labels.txt";
    private Context context;
    private Model model;//모델에 데이터를 입력하고 추론 결과를 전달받을 수 있는 클래스 (인터프리터의 확장판)
    private int modelInputWidth, modelInputHeight, modelInputChannel;
    private TensorImage inputImage;
    private TensorBuffer outputBuffer;
    private List<String> labels;
    private boolean isInitialized = false;
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
        model = Model.createModel(context,MODEL_NAME);
        initModelShape();
        labels = FileUtil.loadLabels(context, LABEL_FILE);
        isInitialized = true;
    }

    public boolean isInitialized(){
        return isInitialized;
    }
    public Size getModelInputSize(){
        if(!isInitialized){
            return new Size(0,0);
        }
        return new Size(modelInputWidth,modelInputHeight);
    }
    /*
    모델 초기화
     */
    private void initModelShape(){
        Tensor inputTensor = model.getInputTensor(0);
        int[] inputShape = inputTensor.shape();
        modelInputChannel = inputShape[0]; //1
        modelInputWidth = inputShape[1];  //320
        modelInputHeight = inputShape[2]; //320
        inputImage = new TensorImage(inputTensor.dataType());
        Tensor outputTensor = model.getOutputTensor(0);
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(),outputTensor.dataType());
    }
    public Pair<String, Float> classify(Bitmap image) {
        inputImage = loadImage(image);
        Object[] inputs = new Object[]{inputImage.getBuffer()};
        Map<Integer, Object> outputs = new HashMap();
        outputs.put(0, outputBuffer.getBuffer().rewind());
        model.run(inputs,outputs);
        Map<String,Float> output = new TensorLabel(labels,outputBuffer).getMapWithFloatValue(); //클래스 레이블과 결과 실수를 매핑
        return argmax(output);
    }
    private Pair argmax(Map<String,Float> map){
        String maxKey = "";
        float maxVal = -1;
        for(Map.Entry<String,Float> entry : map.entrySet()){
            float f = entry.getValue();
            if(f > maxVal){
                maxKey = entry.getKey();
                maxVal = f;

            }
        }
        return new Pair<>(maxKey,maxVal);
    }

    /*
    Return Type : TensorImage
    Bitmap 이미지를 받아 전처리 후 TensorImage 형태로 변환
     */
    private TensorImage loadImage(final Bitmap bitmap){
        if(bitmap.getConfig() != Bitmap.Config.ARGB_8888){
            inputImage.load(convertBitmapToARGB8888(bitmap));
        }
        else{
            inputImage.load(bitmap);
        }
        ImageProcessor imageProcessor=
                new ImageProcessor.Builder()
                .add(new ResizeOp(modelInputWidth,modelInputHeight, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .add(new NormalizeOp(0.0f,255.0f))
                .build();
        return imageProcessor.process(inputImage);
    }
    private Bitmap convertBitmapToARGB8888(Bitmap bitmap){
        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }

    /*
    입력받은 이미지 크기를 모델에 맞게 변환
    parameter : 변환할 이미지, 변환할 가로 크기, 변환할 세로 크기, false : 최근접 보간법 사용(true : 양선형 보간법)
     */
    private Bitmap resizeBitmap(Bitmap bitmap){
        return Bitmap.createScaledBitmap(bitmap,modelInputWidth,modelInputHeight,false);
    }

    public void finish(){
        if(model != null) {
            model.close();
            isInitialized = false;
        }

    }


}

