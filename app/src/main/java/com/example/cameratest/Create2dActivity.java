package com.example.cameratest;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
// androidx 이전
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjWriter;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.graphics.Bitmap.Config.RGB_565;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2RGB;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.rectangle;

public class Create2dActivity extends AppCompatActivity {

    private static CameraPreview surfaceView;
    private SurfaceHolder holder;
    private static Camera mCamera;
    public static Create2dActivity getInstance;
    public static Bitmap bm;

    String modelName = null;

    static{
        if(!OpenCVLoader.initDebug()){
            Log.d("2D", "OpenCV isn't loaded");
        }else{
            Log.d("2D", "OpenCV is loaded successfully");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_create2d);
    }
    @Override
    protected void onResume() {
        super.onResume();
        setInit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mCamera != null){
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void backbuttonClick(View view){
        finish();
    }

    public void captureClick(View view){
        //끊김 해결필요
        MediaActionSound sound = new MediaActionSound();
        sound.play(MediaActionSound.SHUTTER_CLICK);

        //원랜 콜백 활성화를 위해 하였는데 성능상 항시 체크해도 문제가 없는거같아서 추후 테스트 예정
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                screenShot(bm);
            }
        }, 200);

        //캡쳐버튼 비활성화
        Button button = (Button)findViewById(R.id.capture);
        button.setEnabled(false);
    }

    public void screenShot(Bitmap bm){
        if(bm != null) {
            openCVobjectCheck();

            //자른 이미지까지 저장
            Bitmap overlay = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), RGB_565);
            Canvas canvas = new Canvas(overlay);
            canvas.drawBitmap(bm, 0, 0, null);

            //Bitmap overlayOb = Bitmap.createBitmap(cutBm.getWidth(), cutBm.getHeight(), RGB_565);
            //Canvas canvasOb = new Canvas(overlayOb);
            //canvasOb.drawBitmap(cutBm, 0, 0, null);

            File path = new File(getFilesDir().getAbsolutePath() + "/TEMP");
            //폴더 생성
            if (!path.exists()) {
                path.mkdirs();
            }
            try {
                //파일 저장
                FileOutputStream os = new FileOutputStream(path + "/temp.png");
                overlay.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.close();

                //FileOutputStream osOb = new FileOutputStream(path + "/tempObject.png");
                //overlayOb.compress(Bitmap.CompressFormat.PNG, 100, osOb);
                //osOb.close();
            } catch (IOException e) {
                Log.e("Save_Image", e.getMessage(), e);
            }

            //미디어 스캔
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(path));
            sendBroadcast(intent);

            //이미지 확인을 위한 표시
            ImageView imageView = (ImageView)findViewById(R.id.objectdetect);
            imageView.setImageBitmap(bm);

            FrameLayout frameLayout = (FrameLayout)findViewById(R.id.objectpreview);
            frameLayout.setVisibility(View.VISIBLE);
        }
        else{
            Toast.makeText(getApplicationContext(), "capture failed", Toast.LENGTH_SHORT).show();
        }
    }

    public void openCVobjectCheck(){
        Mat rgb = new Mat();
        Utils.bitmapToMat(bm, rgb);
        Mat imageMat = new Mat();
        Utils.bitmapToMat(bm, imageMat);

        Mat grayImage = new Mat();  //grey color matrix
        Imgproc.cvtColor(rgb, grayImage, Imgproc.COLOR_RGB2GRAY);

        //이미지 윤곽 체크
        Mat morpy = new Mat();
        Mat gradThresh = new Mat();  //matrix for threshold
        Mat hierarchy = new Mat();    //matrix for contour hierachy
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.adaptiveThreshold(grayImage, morpy, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 3, 12);  //block size 3
        Imgproc.dilate(morpy, gradThresh, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
        Imgproc.findContours(gradThresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        //작은 사각형 지우기
        List<Rect> contoursRemoved = new ArrayList<Rect>();
        if(contours.size()>0) {
            for(int idx = 0; idx < contours.size(); idx++) {
                Rect rect = Imgproc.boundingRect(contours.get(idx));
                if (rect.height > 100 && rect.width > 100){
                    contoursRemoved.add(rect);
                }
            }
        }

        //겹치는 사각형 합치기
        if(contoursRemoved.size()>0) {
            boolean check = true;
            boolean collision = false;
            while(check) {
                for (int idx = 0; idx < contoursRemoved.size() - 1; idx++) {
                    for (int idy = idx + 1; idy < contoursRemoved.size(); idy++) {
                        Rect rect1 = contoursRemoved.get(idx);
                        Rect rect2 = contoursRemoved.get(idy);
                        if (collisionCheck(rect1, rect2)) {
                            contoursRemoved.add(mergeRect(rect1, rect2));
                            contoursRemoved.remove(idy);
                            contoursRemoved.remove(idx);
                            collision = true;
                            break;
                        }
                    }
                    if(collision){
                        break;
                    }
                }
                if(collision){
                    collision = false;
                    continue;
                }
                else{
                    check = false;
                }
            }
        }

        //제일 큰 사각형  출력
        if(contoursRemoved.size()>0) {
            Rect rect = null;
            double areaMax = 0;
            for(int idx = 0; idx < contoursRemoved.size(); idx++) {
                Rect r = contoursRemoved.get(idx);
                if(r.area() > areaMax){
                    rect = r;
                    areaMax = r.area();
                }
            }
            if(rect != null) {
                File path = new File(getFilesDir().getAbsolutePath() + "/TEMP");
                //폴더 생성
                if (!path.exists()) {
                    path.mkdirs();
                }
                Mat object = new Mat(imageMat, rect);
                cvtColor(object, object, COLOR_BGR2RGB);
                Imgcodecs.imwrite(path.getAbsolutePath() + "/tempObject.png", object);
                rectangle(imageMat, new Point(rect.br().x - rect.width, rect.br().y - rect.height)
                        , rect.br()
                        , new Scalar(255, 0, 0), 5);
            }
        }

        Utils.matToBitmap(imageMat, bm);
    }

    public boolean collisionCheck(Rect r1, Rect r2){
        if(r1.x < r2.x + r2.width && r1.y < r2.y + r2.height && r1.x + r1.width > r2.x && r1.y + r1.height > r2.y){
            return true;
        }
        else{
            return false;
        }
    }

    public Rect mergeRect(Rect r1, Rect r2){
        int x = Math.min(r1.x, r2.x);
        int y = Math.min(r1.y, r2.y);
        int right = Math.max(r1.x + r1.width, r2.x + r2.width);
        int bottom = Math.max(r1.y + r1.height, r2.y + r2.height);
        Rect r3 = new Rect(x, y, right - x, bottom - y);
        return r3;
    }

    public void checkOClick(View view){
        //이름 입력받기
        AlertDialog.Builder alert  = new AlertDialog.Builder(this);
        alert.setTitle("모델 이름 설정");
        alert.setMessage("모델 이름을 정해주세요.");

        EditText name = new EditText(this);
        alert.setView(name);

        alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                modelName= name.getText().toString();
                //모델생성부분으로 전달
                createModel();
                dialog.cancel();
            }
        });
        alert.show();
    }

    public void createModel(){
        //이름 체크
        if(modelName.length() < 2){
            Toast.makeText(getApplicationContext(), "이름이 너무 짧습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        File path = new File(getFilesDir().getAbsolutePath() + "/models/" + modelName + ".obj");

        if(path.exists()){
            Toast.makeText(getApplicationContext(), "이미 있는 이름입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        //모델 생성
        File pathModel = new File(getFilesDir().getAbsolutePath() + "/models");
        File pathPreviews = new File(getFilesDir().getAbsolutePath() + "/previews");
        File pathTemp = new File(getFilesDir().getAbsolutePath() + "/TEMP/tempObject.png");
        AssetManager am = getResources().getAssets();
        InputStream inputStream = null;
        OutputStream outputStream = null;
        Obj obj = null;
        Bitmap bitmap = null;

        try {
            //obj 저장
            inputStream = am.open("models/create2d.obj");
            obj = ObjReader.read(inputStream);
            outputStream = new FileOutputStream(pathModel + "/" + modelName +".obj");
            ObjWriter.write(obj, outputStream);

            //jpg 저장
            inputStream = new FileInputStream(pathTemp);
            bitmap = BitmapFactory.decodeStream(inputStream);
            outputStream = new FileOutputStream(pathModel + "/" + modelName + ".png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

            //priview 저장
            inputStream = new FileInputStream(pathTemp);
            bitmap = BitmapFactory.decodeStream(inputStream);
            outputStream = new FileOutputStream(pathPreviews + "/" + modelName + "preview.png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        }
        catch (Exception e){
            return;
        }

        //모델생성 완료 표시
        AlertDialog.Builder alert  = new AlertDialog.Builder(this);
        alert.setTitle("모델 생성");
        alert.setMessage("모델 생성 완료!");

        alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                Intent data = new Intent();
                data.putExtra("modelname", modelName);
                setResult(RESULT_OK, data);
                modelName = null;
                finish();
            }
        });
        alert.show();
    }

    public void checkXClick(View view){
        FrameLayout frameLayout = (FrameLayout)findViewById(R.id.objectpreview);
        frameLayout.setVisibility(View.INVISIBLE);

        Button button = (Button)findViewById(R.id.capture);
        button.setEnabled(true);
    }

    private void setInit(){
        getInstance = this;
        mCamera = Camera.open();

        setContentView(R.layout.activity_create2d);
        surfaceView = (CameraPreview)findViewById(R.id.preview);

        holder = surfaceView.getHolder();
        holder.addCallback(surfaceView);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public static Camera getCamera(){
        return mCamera;
    }
}