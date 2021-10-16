package com.example.imageclassifier;

import android.media.ImageReader;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class RealtimeCameraFragment extends Fragment {

    private ConnectionCallback connectionCallback;
    private ImageReader.OnImageAvailableListener imageAvailableListener;
    private Size inputSize;
    private String cameraId;
    private AutoFitTextureView autoFitTextureView = null;
    public RealtimeCameraFragment(ConnectionCallback callback,
                                  ImageReader.OnImageAvailableListener imageAvailableListener,
                                  Size inputSize,
                                  String cameraId) {
         this.connectionCallback= callback;
         this.imageAvailableListener = imageAvailableListener;
         this.inputSize = inputSize;
         this.cameraId = cameraId;
    }


    public static RealtimeCameraFragment newInstance(final ConnectionCallback callback,
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

    public interface  ConnectionCallback{
        void onPreviewSizeChosen(Size size, int cameraRotation);
    }
}