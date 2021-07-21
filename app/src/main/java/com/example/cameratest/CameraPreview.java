package com.example.cameratest;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback{

    private Create2dActivity main;
    private Camera mCamera;
    public List<Camera.Size> listPreviewSizes;
    private Camera.Size previewSize;
    private Context context;

    public CameraPreview(Context context, AttributeSet attrs){
        super(context, attrs);
        this.context = context;
        mCamera = Create2dActivity.getCamera();
        if(mCamera == null){
            mCamera = Camera.open();
        }
        listPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
    }
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder){

        try{
            if(mCamera == null){
                mCamera = Camera.open();
            }
            Camera.Parameters parameters = mCamera.getParameters();

            //화면 회전시 코드
            if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
                parameters.set("orientation", "portrait");
                mCamera.setDisplayOrientation(90);
                parameters.setRotation(90);
            }else{
                parameters.set("orientation", "landscope");
                mCamera.setDisplayOrientation(0);
                parameters.setRotation(0);
            }

            parameters.set("orientation", "portrait");

            //포커스 모드 부분
            List<String> focusModes = parameters.getSupportedFocusModes();
            if(focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            mCamera.setDisplayOrientation(90);
            parameters.setRotation(90);

            mCamera.setParameters(parameters);
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();

            //mCamera.setPreviewCallback(this);

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    //화면크기 변경시 호출
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int w, int h) {
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        try {
            mCamera.stopPreview();
            Camera.Parameters parameters = mCamera.getParameters();

            //회전시 코드
            int rotation = MainActivity.getInstance.getWindowManager().getDefaultDisplay().getRotation();
            if (rotation == Surface.ROTATION_0) {
                mCamera.setDisplayOrientation(90);
                parameters.setRotation(90);
            } else if (rotation == Surface.ROTATION_90) {
                mCamera.setDisplayOrientation(0);
                parameters.setRotation(0);
            } else if (rotation == Surface.ROTATION_180) {
                mCamera.setDisplayOrientation(270);
                parameters.setRotation(270);
            } else {
                mCamera.setDisplayOrientation(180);
                parameters.setRotation(180);
            }

            parameters.setPreviewSize(previewSize.width, previewSize.height);
            mCamera.setParameters(parameters);
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();

            //콜백 활성화를 위하여 중복 처리
            mCamera.setPreviewCallback(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder){
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if(listPreviewSizes != null){
            previewSize = getPreviewSize(listPreviewSizes, width, height);
        }
    }

    public Camera.Size getPreviewSize(List<Camera.Size> sizes, int w, int h){
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if(sizes == null){
            return null;
        }
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;

        for(Camera.Size size : sizes){
            double ratio = (double)size.width / size.height;
            if(Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE){
                continue;
            }
            if(Math.abs(size.height - targetHeight) < minDiff){
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        if(optimalSize == null){
            minDiff = Double.MAX_VALUE;
            for(Camera.Size size : sizes){
                if(Math.abs(size.height - targetHeight) < minDiff){
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera){
        Camera.Parameters parameters = camera.getParameters();
        int w = parameters.getPreviewSize().width;
        int h = parameters.getPreviewSize().height;
        int format = parameters.getPreviewFormat();
        YuvImage image = new YuvImage(data, format, w, h, null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Rect area = new Rect(0, 0, w, h);
        image.compressToJpeg(area, 100, out);
        Bitmap bm = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());

        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap rbm = Bitmap.createBitmap(bm, 0, 0, w, h, matrix, true);
        main.bm = rbm;
    }
}