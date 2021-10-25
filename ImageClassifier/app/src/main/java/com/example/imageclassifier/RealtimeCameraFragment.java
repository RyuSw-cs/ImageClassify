package com.example.imageclassifier;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.app.Fragment;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@SuppressLint("ValidFragment")
public class RealtimeCameraFragment extends Fragment {

    private ConnectionCallback connectionCallback;
    private ImageReader.OnImageAvailableListener imageAvailableListener;
    private Size inputSize;
    private String cameraId;
    private AutoFitTextureView autoFitTextureView = null;
    private int orientation;
    private Size previewSize;
    private Semaphore cameraOpenCloseLock = new Semaphore(1);
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewRequestBuilder;
    private CameraCaptureSession captureSession;
    private ImageReader previewReader;
    private HandlerThread backgroundthread = null;
    private Handler backgroundHandler = null;

    @SuppressLint("ValidFragment")
    private RealtimeCameraFragment(final ConnectionCallback callback,
                                   final ImageReader.OnImageAvailableListener imageAvailableListener,
                                   final Size inputSize,
                                   final String cameraId) {
        this.connectionCallback= callback;
        this.imageAvailableListener = imageAvailableListener;
        this.inputSize = inputSize;
        this.cameraId = cameraId;
    }



    public static RealtimeCameraFragment newInstance(ConnectionCallback callback,
                                                     final ImageReader.OnImageAvailableListener imageAvailableListener,
                                                     final Size inputSize,
                                                     final String cameraId) {
        return new RealtimeCameraFragment(callback,imageAvailableListener,inputSize,cameraId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_realtime_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        autoFitTextureView = view.findViewById(R.id.autoFitTextureView);
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if(!autoFitTextureView.isAvailable()) autoFitTextureView.setSurfaceTextureListener(surfaceTextureListener);
        else{
            openCamera(autoFitTextureView.getWidth(),autoFitTextureView.getHeight());
        }

    }
    private void startBackgroundThread(){
        backgroundthread = new HandlerThread("ImageListener");
        backgroundthread.start();
        backgroundHandler = new Handler(backgroundthread.getLooper());
    }
    private void stopBackgroundThread(){
        backgroundthread.quitSafely();
        try{
            backgroundthread.join();
            backgroundthread = null;
            backgroundHandler = null;
        }catch (final InterruptedException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        startBackgroundThread();
        super.onPause();
    }
    private void closeCamera(){
        try{
            cameraOpenCloseLock.acquire();
            if(captureSession != null){
                captureSession.close();
                captureSession = null;
            }
            if(cameraDevice != null){
                cameraDevice.close();
                cameraDevice = null;
            }
            if(previewReader != null){
                previewReader.close();
                previewReader = null;
            }
        }catch (final InterruptedException e){
            throw new RuntimeException("카메라 닫기중 인터럽트 발생",e);
        }finally {
            cameraOpenCloseLock.release();
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(final CameraDevice cd) {
            cameraOpenCloseLock.release();
            cameraDevice = cd;
            createCameraPreviewSession();

        }

        @Override
        public void onDisconnected(final CameraDevice cd) {
              cameraOpenCloseLock.release();
              cd.close();
              cameraDevice = null;
        }

        @Override
        public void onError(final CameraDevice cd, int i) {
            cameraOpenCloseLock.release();
            cd.close();
            cameraDevice = null;
            final Activity activity = getActivity();
            if(activity != null){
                activity.finish();
            }
        }
    };
    private final TextureView.SurfaceTextureListener surfaceTextureListener =
       new TextureView.SurfaceTextureListener(){
           @Override
           public void onSurfaceTextureAvailable(final SurfaceTexture surfaceTexture, final int width, final int height) {
                openCamera(width,height);
           }
           @Override
           public void onSurfaceTextureSizeChanged(final SurfaceTexture surfaceTexture, final int width, final int height) {
                configureTransform(width,height);
           }
           @Override
           public boolean onSurfaceTextureDestroyed(final SurfaceTexture surfaceTexture) {
               return true;
           }
           @Override
           public void onSurfaceTextureUpdated(final SurfaceTexture surfaceTexture) {

           }
    };
    private final CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            if(null == cameraDevice) return;
            captureSession = cameraCaptureSession;
            try{
                captureSession.setRepeatingRequest(previewRequestBuilder.build(),null,null);
            }catch (CameraAccessException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
             Toast.makeText(getActivity(),"카메라 캡쳐 세션 실패",Toast.LENGTH_SHORT).show();
        }
    };


    @SuppressLint("MissingPermission")
    private void openCamera(final int width, final int height){
        final Activity activity = getActivity();
        final CameraManager manager = (CameraManager)activity.getSystemService(Context.CAMERA_SERVICE);
        setupCameraOutputs(manager);
        configureTransform(width,height);

        try{
            if(!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)){
                Toast.makeText(getContext(),"카메라 락 타임아웃",Toast.LENGTH_LONG).show();
                activity.finish();
            }else{
                manager.openCamera(cameraId,stateCallback,null);
            }
        }catch (final InterruptedException | CameraAccessException e){
            e.printStackTrace();
        }

    }

    private void createCameraPreviewSession(){
        try{
            final SurfaceTexture texture = autoFitTextureView.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize.getWidth(),previewSize.getHeight());
            final Surface surface = new Surface(texture);
            previewReader = ImageReader.newInstance(previewSize.getWidth(),previewSize.getHeight(), ImageFormat.YUV_420_888,2);
            previewReader.setOnImageAvailableListener(imageAvailableListener,null);
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);
            previewRequestBuilder.addTarget(previewReader.getSurface());
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            cameraDevice.createCaptureSession(Arrays.asList(surface,previewReader.getSurface()),sessionStateCallback,null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setupCameraOutputs(CameraManager manager){
        try{
            final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            final StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            final int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),inputSize.getWidth(),inputSize.getHeight());
            orientation = getResources().getConfiguration().orientation;
            if(orientation == Configuration.ORIENTATION_LANDSCAPE){
                autoFitTextureView.setAspectRatio(previewSize.getWidth(),previewSize.getHeight());
            } else {
                autoFitTextureView.setAspectRatio(previewSize.getHeight(),previewSize.getWidth());
            }
            connectionCallback.onPreviewSizeChosen(previewSize,sensorOrientation);
        }catch (final CameraAccessException e){
            e.printStackTrace();
        }
    }
    protected Size chooseOptimalSize(final Size[] choices, final int width, final int height){
        final int minSize = Math.min(width,height);
        final Size desiredSize = new Size(width, height);

        final List<Size> bigEnough = new ArrayList<>();
        final List<Size> tooSmall = new ArrayList<>();
        for(final Size option : choices){
            if(option.equals(desiredSize)){
                return desiredSize;
            }
            if(option.getHeight() >= minSize && option.getWidth() >= minSize){
                bigEnough.add(option);
            }else{
                tooSmall.add(option);
            }
        }
        if(bigEnough.size() > 0){
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else{
            return Collections.min(tooSmall, new CompareSizeByArea());
        }
    }

    private void configureTransform(final int viewWidth, final int viewHeight){
        final Activity activity = getActivity();
        if(autoFitTextureView == null || previewSize == null || activity == null) return;
        final int rotation = activity.getDisplay().getRotation();
        final Matrix matrix = new Matrix();
        final RectF viewRect = new RectF(0,0, viewWidth,viewHeight);
        final RectF bufferRect = new RectF(0,0,previewSize.getHeight(),previewSize.getWidth());
        final float centerX = viewRect.centerX();
        final float centerY = viewRect.centerY();
        if(Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation){
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect,bufferRect,Matrix.ScaleToFit.FILL);
            final float scale = Math.max((float)viewHeight / previewSize.getHeight(),(float)viewWidth/previewSize.getWidth());
            matrix.postScale(scale,scale,centerX,centerY);
            matrix.postRotate(90 * (rotation -2), centerX, centerY);
        }else if (Surface.ROTATION_180 == rotation){
            matrix.postRotate(180,centerX,centerY);
        }
        autoFitTextureView.setTransform(matrix);
    }
    public interface  ConnectionCallback{
        void onPreviewSizeChosen(Size size, int cameraRotation);
    }

    static class CompareSizeByArea implements Comparator<Size>{
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long)lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

}