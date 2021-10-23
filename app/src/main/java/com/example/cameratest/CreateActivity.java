package com.example.cameratest;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Handler;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
// androidx 이전
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cameratest.sub.CameraPreview;
import com.example.cameratest.sub.PagerAdapter;
import com.google.android.material.tabs.TabLayout;

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
import java.util.ArrayList;
import java.util.List;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjWriter;

import static android.graphics.Bitmap.Config.RGB_565;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2RGB;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.rectangle;

public class CreateActivity extends AppCompatActivity {

    private static CameraPreview surfaceView;
    private SurfaceHolder holder;
    private static Camera mCamera;
    public static CreateActivity getInstance;
    public static Bitmap bm;

    private ViewPager viewPager;
    private TabLayout tabLayout;
    TabLayout.TabLayoutOnPageChangeListener listener;

    // 생성 도중 터치 무효화
    View.OnTouchListener disable_touch = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return true;
        }
    };
    View.OnTouchListener enable_touch = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return false;
        }
    };

    private int mode = 0;


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
        // 폰트 설정
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        switch (pref.getString("font", "기본")){
            case "나눔R":
                setTheme(R.style.AppTheme_NanumR);
                break;
            case "나눔B":
                setTheme(R.style.AppTheme_NanumB);
                break;
            case "카페":
                setTheme(R.style.AppTheme_Cafe);
                break;
            case "에스코드":
                setTheme(R.style.AppTheme_Sc);
                break;
            default:
                setTheme(R.style.AppTheme);
                break;
        }

        super.onCreate(savedInstanceState);

        // 풀 스크린 적용
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_create);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setInit();

        tabLayout = (TabLayout)findViewById(R.id.tab_layout);
        tabLayout.bringToFront();
        tabLayout.addTab(tabLayout.newTab().setText("2D"));
        tabLayout.addTab(tabLayout.newTab().setText("3D"));
        listener = new TabLayout.TabLayoutOnPageChangeListener(tabLayout);


        viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setAdapter(new PagerAdapter(getSupportFragmentManager()));

        viewPager.addOnPageChangeListener(listener);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                mode = tab.getPosition();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
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
        overridePendingTransition(R.anim.activity_right_enter, R.anim.activity_right_exit);
    }

    public void captureClick(View view){
        if (mode == 0) {
            //소리 재생
            MediaActionSound sound = new MediaActionSound();
            sound.play(MediaActionSound.SHUTTER_CLICK);

            // 콜백함수의 진행을 기다린 뒤 저장 시도
            mCamera.setPreviewCallback(surfaceView);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    screenShot(bm);
                    mCamera.setPreviewCallback(null);
                }
            }, 300);

            //캡쳐버튼 비활성화
            Button button = (Button) findViewById(R.id.capture);
            button.setEnabled(false);

            viewPager.removeOnPageChangeListener(listener);
            LinearLayout tab = (LinearLayout) tabLayout.getChildAt(0);
            tab.getChildAt(0).setOnTouchListener(disable_touch);
            tab.getChildAt(1).setOnTouchListener(disable_touch);
        }
        else{
            AlertDialog.Builder alert  = new AlertDialog.Builder(this, R.style.Dialog);
            alert.setTitle("3D Create");
            alert.setMessage("해당 기능은 준비 중입니다.");
            alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            alert.show();
        }
    }

    @Override
    public void onBackPressed() {
        backbuttonClick(null);
        super.onBackPressed();
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
            frameLayout.bringToFront();
            frameLayout.setVisibility(View.VISIBLE);
        }
        else{
            AlertDialog.Builder alert  = new AlertDialog.Builder(this, R.style.Dialog);
            alert.setMessage("촬영에 실패했어요.\n다시 시도해주세요.");
            alert.show();
        }
    }

    public void openCVobjectCheck(){
        // 이미지에서 사물인식

        // 이미지 형식 변환
        Mat rgb = new Mat();
        Utils.bitmapToMat(bm, rgb);
        Mat imageMat = new Mat();
        Utils.bitmapToMat(bm, imageMat);

        // 회색조로 변경
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

        // 감지 못한 경우 체크
        TextView textView = (TextView)findViewById(R.id.text_opencv_check);
        Button button = (Button)findViewById(R.id.button_ok);

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

            textView.setText("감지한 사물을 빨간색 네모로 표기했어요.");
            button.setVisibility(View.VISIBLE);
        }
        else {
            textView.setText("사물을 감지하지 못했어요. 다시 찍어주세요.");
            button.setVisibility(View.INVISIBLE);
        }

        Utils.matToBitmap(imageMat, bm);
    }

    public boolean collisionCheck(Rect r1, Rect r2){
        // 충돌 체크
        if(r1.x < r2.x + r2.width && r1.y < r2.y + r2.height && r1.x + r1.width > r2.x && r1.y + r1.height > r2.y){
            return true;
        }
        else{
            return false;
        }
    }

    public Rect mergeRect(Rect r1, Rect r2){
        // 사각형 합치기
        int x = Math.min(r1.x, r2.x);
        int y = Math.min(r1.y, r2.y);
        int right = Math.max(r1.x + r1.width, r2.x + r2.width);
        int bottom = Math.max(r1.y + r1.height, r2.y + r2.height);
        Rect r3 = new Rect(x, y, right - x, bottom - y);
        return r3;
    }

    public void checkOClick(View view){
        //이름 입력받기
        AlertDialog.Builder alert  = new AlertDialog.Builder(this, R.style.Dialog);
        alert.setTitle("모델 이름 설정");
        alert.setMessage("모델 이름을 정해주세요.");

        EditText name = new EditText(this);
        name.setPadding(20, 20, 20, 20);
        name.setHighlightColor(Color.TRANSPARENT);
        alert.setView(name);

        alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                modelName = name.getText().toString();
                //모델생성부분으로 전달
                createModel();
                dialog.cancel();
            }
        });
        alert.setNeutralButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alert.setCancelable(false);
        alert.show();
    }

    public void createModel(){
        //이름 체크
        if(modelName.length() < 2){
            AlertDialog.Builder alert  = new AlertDialog.Builder(this, R.style.Dialog);
            alert.setMessage("이름이 너무 짧아요.");
            alert.show();
            return;
        }
        File path = new File(getFilesDir().getAbsolutePath() + "/models/" + modelName + ".obj");

        if(path.exists()){
            AlertDialog.Builder alert  = new AlertDialog.Builder(this, R.style.Dialog);
            alert.setMessage("이미 있는 이름이에요.");
            alert.show();
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
        AlertDialog.Builder alert  = new AlertDialog.Builder(this, R.style.Dialog);
        alert.setMessage("모델 생성 완료!");
        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialog.cancel();
                Intent data = new Intent();
                data.putExtra("modelname", modelName);
                setResult(RESULT_OK, data);
                modelName = null;
                finish();
                overridePendingTransition(R.anim.activity_right_enter, R.anim.activity_right_exit);
            }
        });
        alert.show();
    }

    public void checkXClick(View view){
        // 다시 촬영시도
        FrameLayout frameLayout = (FrameLayout)findViewById(R.id.objectpreview);
        frameLayout.setVisibility(View.INVISIBLE);

        Button button = (Button)findViewById(R.id.capture);
        button.setEnabled(true);

        viewPager.addOnPageChangeListener(listener);
        viewPager.setCurrentItem(mode);
        LinearLayout tab = (LinearLayout) tabLayout.getChildAt(0);
        tab.getChildAt(0).setOnTouchListener(enable_touch);
        tab.getChildAt(1).setOnTouchListener(enable_touch);
    }

    private void setInit(){
        // 카메라 세팅
        getInstance = this;
        mCamera = Camera.open();

        setContentView(R.layout.activity_create);
        surfaceView = (CameraPreview)findViewById(R.id.preview);

        holder = surfaceView.getHolder();
        holder.addCallback(surfaceView);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public static Camera getCamera(){
        return mCamera;
    }
}