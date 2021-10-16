package com.example.imageclassifier;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AutoFitTextureView extends TextureView {
    private int ratioWidth = 0;
    private int ratioHeight = 0;

    public AutoFitTextureView(final Context context) {
        super(context, null);
    }

    public AutoFitTextureView(final Context context, final AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public AutoFitTextureView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr, defStyleAttr);
    }
    public void setAspectRatio(final int width, final int height){
        if(width < 0 || height < 0){
            throw new IllegalArgumentException("사이즈는 양수여야 합니다.");
        }
        ratioWidth = width;
        ratioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width = View.MeasureSpec.getSize(widthMeasureSpec);
        final int height = View.MeasureSpec.getSize(heightMeasureSpec);
        if(ratioWidth == 0 || ratioHeight == 0){
            setMeasuredDimension(width,height);
        }
        else{
            if(width < height * ratioWidth / ratioHeight){
                setMeasuredDimension(width, width * ratioHeight /ratioWidth);
            }else{
                setMeasuredDimension(height * ratioWidth / ratioHeight,height);
            }
        }
    }
}
