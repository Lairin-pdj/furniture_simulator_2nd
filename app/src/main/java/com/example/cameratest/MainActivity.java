package com.example.cameratest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
// androidx 이전
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.PermissionChecker;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.LinearLayoutManager;
//import android.support.v7.widget.RecyclerView;
import android.os.Message;
import android.os.Parcelable;
import android.util.JsonWriter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cameratest.rendering.BackgroundRenderer;
import com.example.cameratest.rendering.ObjectRenderer;
import com.example.cameratest.rendering.PlaneRenderer;
import com.example.cameratest.rendering.PointCloudRenderer;
import com.example.cameratest.rendering.ComplexObjectRenderer;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Session;
import com.google.ar.core.SharedCamera;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
//sceneform code
//import com.google.ar.sceneform.ux.ArFragment;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import de.javagl.obj.Mtl;
import de.javagl.obj.MtlReader;
import de.javagl.obj.MtlWriter;
import de.javagl.obj.Obj;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjWriter;

public class MainActivity extends AppCompatActivity implements GLSurfaceView.Renderer, ImageReader.OnImageAvailableListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    // surfaceview를 위한 변수들
    private Session mSession;
    private static Session sharedSession;
    private boolean mUserRequestInstall = true;  // 설치 여부 체크
    private static GLSurfaceView surfaceView;
    private boolean surfaceCreated;

    // 선택된 가구 보여주기용 view
    private static FrameLayout selectPreview;
    private static TextView textViewPreview;
    private static ImageView imageViewPreview;

    // 카메라 프리뷰 및 캡쳐를 위한 변수들
    private CameraDevice cameraDevice;
    private static SharedCamera sharedCamera;
    private CameraCaptureSession captureSession;
    private boolean captureSessionChangesPossible = true;
    private CaptureRequest.Builder previewCaptureRequestBuilder;
    private final ConditionVariable safeToExitApp = new ConditionVariable();

    // 생성되거나 다운한 모델의 저장을 위한 변수들
    private boolean saveCheck = false;
    private boolean saveSucess;
    private boolean isNewModel = false;
    private String newModelName = null;

    // ar코어 및 렌더링 관련 변수들
    private boolean arcoreActive;
    private static final Short AUTOMATOR_DEFAULT = 0;
    private static final String AUTOMATOR_KEY = "automator";
    private final AtomicBoolean automatorRun = new AtomicBoolean(false);
    private final AtomicBoolean shouldUpdateSurfaceTexture = new AtomicBoolean(false);
    private TapHelper tapHelper;
    private DisplayRotationHelper displayRotationHelper;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final Map<String, ObjectRenderer> virtualObject = new HashMap<>(); // obj모델 저장용 해시맵
    private final Map<String, ComplexObjectRenderer> complexvirtualObject = new HashMap<>(); // mtl모델 저장용 해시맵
    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();
    private final ObjectRenderer virtualObjectSelect = new ObjectRenderer();
    private boolean isPlaneChanged = false; // plane 체크용 변수들
    private boolean isPlaneshow = true;
    private boolean firstPlanecheck = false;

    // anchor용 변수들
    private final float[] anchorMatrix = new float[16];
    private static final float[] DEFAULT_COLOR = new float[] {0f, 0f, 0f, 0f};
    private static class ColoredAnchor{
        public Anchor anchor;
        public float[] color;
        public String name;
        public float scale;
        public boolean selected;
        public float angle;

        public ColoredAnchor(Anchor a, float[] color4f, String name, float scale) {
            this.anchor = a;
            this.color = color4f;
            this.name = name;
            this.scale = scale;
            this.angle = 0;
            this.selected = false;
        }
    }
    private int selectedAnchor = -1; // 선택된 anchor의 순서값
    private final ArrayList<ColoredAnchor> anchors = new ArrayList<>();
    private String togled = null;
    private ScaleGestureDetector scaleGestureDetector;

    // 업로드용 변수
    // 캡슐화 필요
    private final static String IP_ADDRESS = "13.125.254.183";

    // UI구현을 위한 변수들
    public static MainActivity getInstance;
    private RecyclerView recyclerView = null;
    private static class subMenuData{
        public int picture;
        public String name;
        public String text;

        public subMenuData(int picture, String name, String text) {
            this.picture = picture;
            this.name = name;
            this.text = text;
        }
    }
    private Parcelable recyclerViewState;
    private boolean isUp = false;
    private boolean isSubmenu = false;
    private boolean isDeco = false;
    private boolean isDel = false;
    private boolean isUpload = false;

    // 애니메이션 변수들
    private Animation translate_up;
    private Animation translate_down;
    private Animation sub_up;
    private Animation sub_down;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        // 멀티 권한 설정
        String[] permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        // 권한 승인 여부 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            for(String permission : permissions) {
                int result = PermissionChecker.checkSelfPermission(this, permission);
                if (!(result == PermissionChecker.PERMISSION_GRANTED)) {
                    doRequestPermissions();
                    break;
                }
            }
        }

        super.onCreate(savedInstanceState);
        getInstance = this;

        // 풀스크린 적용
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // 애니메이션 변수 적용
        translate_up = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.translate_up);
        translate_down = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.translate_down);
        sub_up = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.sub_up);
        sub_down = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.sub_down);

        // 내장 모델 내부저장소에 저장
        saveInternalModels();

        // 프리뷰 내부저장소에 저장
        saveInternalPreviews();

        // glsurfaceview 설정 코드
        setContentView(R.layout.activity_main);

        Bundle extraBundle = getIntent().getExtras();
        if (extraBundle != null && 1 == extraBundle.getShort(AUTOMATOR_KEY, AUTOMATOR_DEFAULT)) {
            automatorRun.set(true);
        }

        // GL surface view that renders camera preview image.
        surfaceView = findViewById(R.id.glsurfaceview);
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Helpers, see hello_ar_java sample to learn more.
        displayRotationHelper = new DisplayRotationHelper(this);
        tapHelper = new TapHelper(this);
        surfaceView.setOnTouchListener(tapHelper);

        // plane스위치 표시
        Switch planeSwitch = findViewById(R.id.switch1);
        planeSwitch.bringToFront();
        planeSwitch.setOnCheckedChangeListener(new planeChangedListener());

        // 초기튜토리얼 이미지 표시
        ImageView imageView = findViewById(R.id.imageView);
        imageView.bringToFront();

        // 핀치줌 체크용
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        // AR 시작
        resumeARCore();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // arcore 다운 체크
        try{
            if(mSession == null){
                switch (ArCoreApk.getInstance().requestInstall(this, mUserRequestInstall)) {
                    case INSTALLED:
                        mSession = new Session(this);
                        //Toast.makeText(this, "session created", Toast.LENGTH_SHORT).show();
                        break;
                    case INSTALL_REQUESTED:
                        mUserRequestInstall = false;
                        break;
                }
            }
        }catch (UnavailableUserDeclinedInstallationException e){
            Log.e(TAG, "downcheck : UnavailableUserDeclinedInstallationException", e);
        }catch (Exception e){
            Log.e(TAG, "downcheck : Exception", e);
        }

        // 권한 체크
        String[] permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        int check = 0;

        for(String permission : permissions) {
            int result = PermissionChecker.checkSelfPermission(this, permission);
            if (result == PermissionChecker.PERMISSION_GRANTED) {
                //성공시
                check++;
            }
        }

        //모든 권한 획득시 화면 출력 진행
        waitUntilCameraCaptureSessionIsActive();
        startBackgroundThread();
        surfaceView.onResume();

        // 뷰에 카메라프리뷰를 띄우기위한 작업
        if (surfaceCreated) {
            openCamera();
        }

        displayRotationHelper.onResume();

        // 선택된 가구 보여주는 뷰 초기화
        setFurniturePreview();

        // 가구목록 셋팅
        setFurnitureList();

        // 배터리 표시
        setBatteryTimer();
    }

    @Override
    protected void onPause() {
        // 실행 중이던 내용들 모두 정지
        surfaceView.onPause(); // view
        waitUntilCameraCaptureSessionIsActive(); // 카메라 관련 내용들
        mSession.close();
        displayRotationHelper.onPause();
        closeCamera();
        stopBackgroundThread();
        shouldUpdateSurfaceTexture.set(false); // ar 관련 내용들
        pauseARCore();

        // UI의 통일을 위해
        isUp = false;

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (sharedSession != null) {
            // Explicitly close ARCore Session to release native resources.
            // Review the API reference for important considerations before calling close() in apps with
            // more complicated lifecycle requirements:
            // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
            sharedSession.close();
            sharedSession = null;
        }

        super.onDestroy();
    }

    private void startBackgroundThread() {
        // 쓰레드를 이용해 백그라운드에서 카메라 프리뷰 진행
        backgroundThread = new HandlerThread("sharedCameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        // 카메라 프리뷰 정지
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while trying to join background handler thread", e);
            }
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        surfaceCreated = true;

        // Set GL clear color to black.
        GLES20.glClearColor(0f, 0f, 0f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the camera preview image texture. Used in non-AR and AR mode.
            backgroundRenderer.createOnGlThread(this);
            planeRenderer.createOnGlThread(this, "models/trigrid.png");
            pointCloudRenderer.createOnGlThread(this);

            // 내부 파일에서 모델링 가능한 목록 파싱
            File path = new File(getFilesDir().getAbsolutePath() + "/models");
            File[] files = path.listFiles();
            List<String> filenames = new ArrayList<>();
            for(int i = 0; i < files.length; i++){
                String temp = files[i].getName();
                int idx = temp.lastIndexOf(".");
                if(temp.substring(idx + 1).equals("obj")){
                    filenames.add(temp.substring(0, idx));
                }
            }

            // 파싱된 결과를 기준으로 렌더링 생성
            for(int i = 0; i < filenames.size(); i++){
                File check = new File(getFilesDir().getAbsolutePath() + "/models/" + filenames.get(i) + ".mtl");
                // mtl 파일인 경우
                if(check.exists()){
                    complexvirtualObject.put(filenames.get(i), new ComplexObjectRenderer());
                    complexvirtualObject.get(filenames.get(i)).createOnGlThread(this, "models/" + filenames.get(i) + ".obj", "models/" + filenames.get(i) + ".png");
                }
                // obj 파일인 경우
                else {
                    virtualObject.put(filenames.get(i), new ObjectRenderer());
                    virtualObject.get(filenames.get(i)).createOnGlThread(this, "models/" + filenames.get(i) + ".obj", "models/" + filenames.get(i) + ".png");
                    virtualObject.get(filenames.get(i)).setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);
                }
            }

            // 선택표시용 렌더러 생성
            virtualObjectSelect.createOnGlThread(this, "models/selected.obj", "models/selected.png");
            virtualObjectSelect.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);
            virtualObjectSelect.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

            // 카메라 진행
            openCamera();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // 화면의 이동이 발생함을 캐치
        GLES20.glViewport(0, 0, width, height);
        displayRotationHelper.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Use the cGL clear color specified in onSurfaceCreated() to erase the GL surface.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (!shouldUpdateSurfaceTexture.get()) {
            // Not ready to draw.
            return;
        }

        // 이동 감지
        displayRotationHelper.updateSessionIfNeeded(sharedSession);

        // ar 렌더링 진행
        try {
            onDrawFrameARCore();
        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }

        //switch에 따른 plane 텍스처 변경 코드
        try {
            if (isPlaneChanged) {
                isPlaneChanged = false;
                if (isPlaneshow) {
                    planeRenderer.createOnGlThread(this, "models/trigrid.png");
                } else {
                    planeRenderer.createOnGlThread(this, "models/transparent.png");
                }
            }
        }catch (IOException e){
            Log.e(TAG, "ar rendering : io error", e);
        }

        //초기 plane 탐지 전 이미지 보여주기
        if(!firstPlanecheck && hasTrackingPlane()){
            ImageView imageView;
            imageView = findViewById(R.id.imageView);
            imageView.setVisibility(View.INVISIBLE);
            firstPlanecheck = true;
        }

        // 새로운 모델이 있으면 렌더러에 추가
        if(isNewModel){
            isNewModel = false;
            if(newModelName != null){
                try {
                    virtualObject.put(newModelName, new ObjectRenderer());
                    virtualObject.get(newModelName).createOnGlThread(this, "models/" + newModelName + ".obj", "models/" + newModelName + ".png");
                    virtualObject.get(newModelName).setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);
                }
                catch (Exception e){
                    Log.e(TAG, "new model : error", e);
                }
                newModelName = null;
            }
        }

        //capture 기능 코드
        try{
            if(saveCheck) {
                saveCheck = false;
                int width = MainActivity.surfaceView.getWidth();
                int height = MainActivity.surfaceView.getHeight();

                int[] bitmapBuffer = new int[(int)(width * height)];
                int[] bitmapSource = new int[(int)(width * height)];

                IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
                intBuffer.position(0);

                gl.glReadPixels(0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer);

                int offset1, offset2;
                for (int i = 0; i < height; i++) {
                    offset1 = i * width;
                    offset2 = (height - i - 1) * width;
                    for (int j = 0; j < width; j++) {
                        int texturePixel = bitmapBuffer[offset1 + j];
                        int blue = (texturePixel >> 16) & 0xff;
                        int red = (texturePixel << 16) & 0x00ff0000;
                        int pixel = (texturePixel & 0xff00ff00) | red | blue;
                        bitmapSource[offset2 + j] = pixel;
                    }
                }

                Bitmap bm = Bitmap.createBitmap(bitmapSource, width, height, Bitmap.Config.ARGB_8888);

                saveSucess = screenShot(bm);
            }
        }catch (GLException e){
            Log.e(TAG, "capture : error", e);
        }
    }

    public void onDrawFrameARCore() throws CameraNotAvailableException {
        if (!arcoreActive) {
            // ARCore not yet active, so nothing to draw yet.
            return;
        }

        // Perform ARCore per-frame update.
        Frame frame = sharedSession.update();
        Camera camera = frame.getCamera();

        // Handle screen tap.
        handleTap(frame, camera);

        // If frame is ready, render camera preview image to the GL surface.
        backgroundRenderer.draw(frame);

        // plane 생성을 위한 tracking
        trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

        // If not tracking, don't draw 3D objects.
        if (camera.getTrackingState() == TrackingState.PAUSED) {
            return;
        }

        // Get projection matrix.
        float[] projmtx = new float[16];
        camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

        // Get camera matrix and draw.
        float[] viewmtx = new float[16];
        camera.getViewMatrix(viewmtx, 0);

        // Compute lighting from average intensity of the image.
        // The first three components are color scaling factors.
        // The last one is the average pixel intensity in gamma space.
        final float[] colorCorrectionRgba = new float[4];
        frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

        // Visualize tracked points.
        // Use try-with-resources to automatically release the point cloud.
        try (PointCloud pointCloud = frame.acquirePointCloud()) {
            pointCloudRenderer.update(pointCloud);
            pointCloudRenderer.draw(viewmtx, projmtx);
        }

        // Visualize planes.
        //플레인 제거시 수정 필요 예상
        planeRenderer.drawPlanes(
                sharedSession.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);

        // Visualize anchors created by touch.
        for (ColoredAnchor coloredAnchor : anchors) {
            if (coloredAnchor.anchor.getTrackingState() != TrackingState.TRACKING) {
                continue;
            }
            // Get the current pose of an Anchor in world space. The Anchor pose is updated
            // during calls to sharedSession.update() as ARCore refines its estimate of the world.
            coloredAnchor.anchor.getPose().toMatrix(anchorMatrix, 0);

            //각도에 따른 회전
            float[] temp = new float[16];
            float[] finish = new float[16];
            Matrix.setRotateM(temp, 0, coloredAnchor.angle, 0.0f, 1.0f, 0.0f);
            Matrix.multiplyMM(finish, 0, anchorMatrix, 0, temp, 0);

            // 해시맵에서 렌더러를 찾아 렌더링 진행
            // mtl의 경우
            if(complexvirtualObject.containsKey(coloredAnchor.name)){
                complexvirtualObject.get(coloredAnchor.name).updateModelMatrix(finish, coloredAnchor.scale);
                complexvirtualObject.get(coloredAnchor.name).draw(viewmtx, projmtx, colorCorrectionRgba);
            }
            // obj의 경우
            else {
                virtualObject.get(coloredAnchor.name).updateModelMatrix(finish, coloredAnchor.scale);
                virtualObject.get(coloredAnchor.name).draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
            }

            // 선택된 가구는 추가 마크 렌더링
            if(coloredAnchor.selected) {
                virtualObjectSelect.updateModelMatrix(finish, coloredAnchor.scale);
                virtualObjectSelect.draw(viewmtx, projmtx, colorCorrectionRgba, DEFAULT_COLOR);
            }
        }
    }

    private boolean isARCoreSupportedAndUpToDate(){
        // arcore 설치 여부 및 설치 진행
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        switch (availability){
            case SUPPORTED_INSTALLED:
                return true;
            case SUPPORTED_APK_TOO_OLD:
            case SUPPORTED_NOT_INSTALLED:
                try{
                    ArCoreApk.InstallStatus installStatus = ArCoreApk.getInstance().requestInstall(this, true);
                    switch (installStatus){
                        case INSTALL_REQUESTED:
                            return false;
                        case INSTALLED:
                            return true;
                    }
                }catch (UnavailableException e){
                    Log.e(TAG, "ar install check : error", e);
                }
                return false;
            case UNSUPPORTED_DEVICE_NOT_CAPABLE:
                return false;
            case UNKNOWN_CHECKING:
            case UNKNOWN_ERROR:
            case UNKNOWN_TIMED_OUT:
                AlertDialog.Builder alert  = new AlertDialog.Builder(this);
                alert.setTitle("ARCore 연결 실패");
                alert.setMessage("인터넷 연결을 확인해주시거나 잠시 후에 다시 시도해주시길 바랍니다.");
                alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        finish();
                    }
                });
                alert.show();
                return false;
            default:
                return false;
        }
    }

    private synchronized void waitUntilCameraCaptureSessionIsActive() {
        while (!captureSessionChangesPossible) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                Log.e(TAG, "Unable to wait for a safe time to make changes to the capture session", e);
            }
        }
    }

    private void openCamera() {
        // Don't open camera if already opened.
        if (cameraDevice != null) {
            return;
        }

        // Make sure that ARCore is installed, up to date, and supported on this device.
        if (!isARCoreSupportedAndUpToDate()) {
            return;
        }

        if (sharedSession == null) {
            try {
                // Create ARCore session that supports camera sharing.
                sharedSession = new Session(this, EnumSet.of(Session.Feature.SHARED_CAMERA));
            } catch (Exception e) {
                Log.e(TAG, "Failed to create ARCore session that supports camera sharing", e);
                return;
            }


            // Enable auto focus mode while ARCore is running.
            Config config = sharedSession.getConfig();
            config.setFocusMode(Config.FocusMode.AUTO);
            sharedSession.configure(config);
        }

        // Store the ARCore shared camera reference.
        sharedCamera = sharedSession.getSharedCamera();

        // Store the ID of the camera used by ARCore.
        String cameraId = sharedSession.getCameraConfig().getCameraId();


        try {
            // Wrap our callback in a shared camera callback.
            CameraDevice.StateCallback wrappedCallback =
                    sharedCamera.createARDeviceStateCallback(cameraDeviceCallback, backgroundHandler);

            // Store a reference to the camera system service.
            CameraManager cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);

            // Get the characteristics for the ARCore camera.
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

            // On Android P and later, get list of keys that are difficult to apply per-frame and can
            // result in unexpected delays when modified during the capture session lifetime.
            if (Build.VERSION.SDK_INT >= 28) {
                List<CaptureRequest.Key<?>> keysThatCanCauseCaptureDelaysWhenModified = characteristics.getAvailableSessionKeys();
                if (keysThatCanCauseCaptureDelaysWhenModified == null) {
                    // Initialize the list to an empty list if getAvailableSessionKeys() returns null.
                    keysThatCanCauseCaptureDelaysWhenModified = new ArrayList<>();
                }
            }
            // Prevent app crashes due to quick operations on camera open / close by waiting for the
            // capture session's onActive() callback to be triggered.
            captureSessionChangesPossible = false;

            // Open the camera device using the ARCore wrapped callback.
            cameraManager.openCamera(cameraId, wrappedCallback, backgroundHandler);
        } catch (CameraAccessException | IllegalArgumentException | SecurityException e) {
            Log.e(TAG, "Failed to open camera", e);
        }
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            waitUntilCameraCaptureSessionIsActive();
            safeToExitApp.close();
            cameraDevice.close();
            safeToExitApp.block();
        }
    }

    private void resumeARCore() {
        // Ensure that session is valid before triggering ARCore resume. Handles the case where the user
        // manually uninstalls ARCore while the app is paused and then resumes.
        if (sharedSession == null) {
            return;
        }

        if (!arcoreActive) {
            try {
                // To avoid flicker when resuming ARCore mode inform the renderer to not suppress rendering
                // of the frames with zero timestamp.
                backgroundRenderer.suppressTimestampZeroRendering(false);
                // Resume ARCore.
                sharedSession.resume();
                arcoreActive = true;

                // Set capture session callback while in AR mode.
                sharedCamera.setCaptureCallback(cameraCaptureCallback, backgroundHandler);
            } catch (CameraNotAvailableException e) {
                Log.e(TAG, "Failed to resume ARCore session", e);
            }
        }
    }

    private void pauseARCore() {
        if (arcoreActive) {
            // Pause ARCore.
            sharedSession.pause();
            arcoreActive = false;
        }
    }

    private boolean hasTrackingPlane() {
        // plane이 생성 되었는지 감지
        for (Plane plane : sharedSession.getAllTrackables(Plane.class)) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                return true;
            }
        }
        return false;
    }

    private void handleTap(Frame frame, Camera camera) {
        int checkStatus = tapHelper.getCheckStatus();
        if(checkStatus == 0) {
            MotionEvent tap = tapHelper.poll();
            if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
                for (HitResult hit : frame.hitTest(tap)) {
                    // Check if any plane was hit, and if it was hit inside the plane polygon
                    Trackable trackable = hit.getTrackable();

                    //객체 클릭체크
                    //추후 raycast 코드 변경가능성 존재
                    boolean check = false;
                    for (ColoredAnchor anchor : anchors) {
                        float anchorX, anchorY, anchorZ, hitX, hitY, hitZ;
                        anchorX = anchor.anchor.getPose().tx();
                        anchorY = anchor.anchor.getPose().ty();
                        anchorZ = anchor.anchor.getPose().tz();
                        hitX = hit.getHitPose().tx();
                        hitY = hit.getHitPose().ty();
                        hitZ = hit.getHitPose().tz();
                        double distance = Math.sqrt(Math.pow((anchorX - hitX), 2) + Math.pow((anchorY - hitY), 2) + Math.pow((anchorZ - hitZ), 2));

                        if (distance < 0.15) {
                            if (selectedAnchor >= 0) {
                                anchors.get(selectedAnchor).selected = false;
                            }
                            selectedAnchor = anchors.indexOf(anchor);
                            anchor.selected = true;
                            check = true;
                            return;
                        }
                    }
                    if (!check) {
                        if (selectedAnchor >= 0) {
                            anchors.get(selectedAnchor).selected = false;
                            selectedAnchor = -1;
                        }
                    }

                    // Creates an anchor if a plane or an oriented point was hit.
                    if ((trackable instanceof Plane
                            && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                            && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
                            || (trackable instanceof Point
                            && ((Point) trackable).getOrientationMode()
                            == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)) {
                        // Hits are sorted by depth. Consider only closest hit on a plane or oriented point.
                        // Cap the number of objects created. This avoids overloading both the
                        // rendering system and ARCore.
                        if (anchors.size() >= 20) {
                            anchors.get(0).anchor.detach();
                            anchors.remove(0);
                        }
                        // Assign a color to the object for rendering based on the trackable type
                        // this anchor attached to. For AR_TRACKABLE_POINT, it's blue color, and
                        // for AR_TRACKABLE_PLANE, it's green color.
                        float[] objColor;
                        if (trackable instanceof Point) {
                            objColor = new float[]{66.0f, 133.0f, 244.0f, 255.0f};
                        } else {
                            objColor = DEFAULT_COLOR;
                        }

                        // Adding an Anchor tells ARCore that it should track this position in
                        // space. This anchor is created on the Plane to place the 3D model
                        // in the correct position relative both to the world and to the plane.
                        if (togled != null) {
                            if (togled.equals("tabletest")) {
                                anchors.add(new ColoredAnchor(hit.createAnchor(), objColor, togled, 0.00001f));
                            } else if (togled.equals("andy")) {
                                objColor = new float[]{139.0f, 195.0f, 74.0f, 255.0f};
                                anchors.add(new ColoredAnchor(hit.createAnchor(), objColor, togled, 1.0f));
                            } else {
                                anchors.add(new ColoredAnchor(hit.createAnchor(), objColor, togled, 1.0f));
                            }

                            // 배치하고 선택 모델은 해제
                            togled = null;
                            selectPreview.setVisibility(View.INVISIBLE);
                        }
                        break;
                    }
                }
            }
        }
        else if(checkStatus == 1){
            //더블클릭을 통해 사물 이동
            if(selectedAnchor > -1) {
                MotionEvent tap = tapHelper.poll();
                if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
                    for (HitResult hit : frame.hitTest(tap)) {
                        // Check if any plane was hit, and if it was hit inside the plane polygon
                        Trackable trackable = hit.getTrackable();

                        // Creates an anchor if a plane or an oriented point was hit.
                        if ((trackable instanceof Plane
                                && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                                && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
                                || (trackable instanceof Point
                                && ((Point) trackable).getOrientationMode()
                                == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)) {

                            anchors.get(selectedAnchor).anchor = hit.createAnchor();
                        }
                    }
                }
            }
        }
        else if(checkStatus == 2){
            //회전 감지 스크롤을 통해
            if(selectedAnchor > -1){
                MotionEvent check = tapHelper.poll();
                if(check.getAction() != MotionEvent.ACTION_UP) {
                    float temp = tapHelper.getDistance();
                    if (temp != 0) {
                        anchors.get(selectedAnchor).angle -= temp;
                        if (anchors.get(selectedAnchor).angle >= 360 || anchors.get(selectedAnchor).angle <= -360) {
                            anchors.get(selectedAnchor).angle = anchors.get(selectedAnchor).angle % 360;
                        }
                    }
                }
                else{
                    //가속도 제거를 위해서
                    tapHelper.setCheckStatus(3);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e){
        //액티비티 터치 체크를 위해
        scaleGestureDetector.onTouchEvent(e);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
        @Override
        public boolean onScale(ScaleGestureDetector detector){
            //확대축소용 코드
            //아직 반응속도가 느려서 최적화가 필요해보임
            if(selectedAnchor > -1){
                anchors.get(selectedAnchor).scale *= detector.getScaleFactor();

                anchors.get(selectedAnchor).scale = Math.max(0.5f, Math.min(anchors.get(selectedAnchor).scale, 5.0f));
            }
            return true;
        }
    }

    public void saveInternalModels(){
        // 첫 기본 모델 초기화 과정
        File path = new File(getFilesDir().getAbsolutePath() + "/models");
        if(!path.exists()){
            path.mkdirs();
        }
        try {
            AssetManager am = getResources().getAssets();
            InputStream inputStream = null;
            OutputStream outputStream = null;
            Obj obj = null;
            Bitmap bitmap = null;
            File check = null;

            check = new File(path + "/andy.obj");
            if(!check.exists()) {
                inputStream = am.open("models/andy.obj");
                obj = ObjReader.read(inputStream);
                outputStream = new FileOutputStream(path + "/andy.obj");
                ObjWriter.write(obj, outputStream);
                inputStream = am.open("models/andy.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
                outputStream = new FileOutputStream(path + "/andy.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

                inputStream = am.open("models/desk.obj");
                obj = ObjReader.read(inputStream);
                outputStream = new FileOutputStream(path + "/desk.obj");
                ObjWriter.write(obj, outputStream);
                inputStream = am.open("models/desk.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
                outputStream = new FileOutputStream(path + "/desk.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                inputStream = am.open("models/desk1.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
                outputStream = new FileOutputStream(path + "/desk1.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                inputStream = am.open("models/desk2.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
                outputStream = new FileOutputStream(path + "/desk2.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                inputStream = am.open("models/desk3.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
                outputStream = new FileOutputStream(path + "/desk3.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                inputStream = am.open("models/desk.mtl");
                List<Mtl> mtl1 = MtlReader.read(inputStream);
                outputStream = new FileOutputStream(path + "/desk.mtl");
                MtlWriter.write(mtl1, outputStream);

                inputStream = am.open("models/chair.obj");
                obj = ObjReader.read(inputStream);
                outputStream = new FileOutputStream(path + "/chair.obj");
                ObjWriter.write(obj, outputStream);
                inputStream = am.open("models/chair.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
                outputStream = new FileOutputStream(path + "/chair.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                inputStream = am.open("models/chair1.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
                outputStream = new FileOutputStream(path + "/chair1.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                inputStream = am.open("models/chair2.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
                outputStream = new FileOutputStream(path + "/chair2.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                inputStream = am.open("models/chair.mtl");
                List<Mtl> mtl2 = MtlReader.read(inputStream);
                outputStream = new FileOutputStream(path + "/chair.mtl");
                MtlWriter.write(mtl2, outputStream);

                inputStream = am.open("models/lamp.obj");
                obj = ObjReader.read(inputStream);
                outputStream = new FileOutputStream(path + "/lamp.obj");
                ObjWriter.write(obj, outputStream);
                inputStream = am.open("models/lamp.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
                outputStream = new FileOutputStream(path + "/lamp.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                inputStream = am.open("models/lamp1.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
                outputStream = new FileOutputStream(path + "/lamp1.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                inputStream = am.open("models/lamp2.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
                outputStream = new FileOutputStream(path + "/lamp2.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                inputStream = am.open("models/lamp3.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
                outputStream = new FileOutputStream(path + "/lamp3.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                inputStream = am.open("models/lamp.mtl");
                List<Mtl> mtl3 = MtlReader.read(inputStream);
                outputStream = new FileOutputStream(path + "/lamp.mtl");
                MtlWriter.write(mtl3, outputStream);
            }
        }catch (IOException e){
            Log.e(TAG, "savemodel : io error", e);
        }
    }

    public void saveInternalPreviews(){
        // 첫 기본 모델 초기화 과정
        File path = new File(getFilesDir().getAbsolutePath() + "/previews");
        if(!path.exists()){
            path.mkdirs();
        }
        try {
            AssetManager am = getResources().getAssets();
            InputStream inputStream = null;
            OutputStream outputStream = null;
            Bitmap bitmap = null;
            File check = null;

            check = new File(path + "/andypreview.png");
            if(!check.exists()) {
                inputStream = am.open("models/andyview.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
                outputStream = new FileOutputStream(path + "/andypreview.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

                inputStream = am.open("models/deskview.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
                outputStream = new FileOutputStream(path + "/deskpreview.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

                inputStream = am.open("models/chairview.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
                outputStream = new FileOutputStream(path + "/chairpreview.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

                inputStream = am.open("models/lampview.png");
                bitmap = BitmapFactory.decodeStream(inputStream);
                outputStream = new FileOutputStream(path + "/lamppreview.png");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            }
        }catch (IOException e){
            Log.e(TAG, "savemodel : io error", e);
        }
    }

    public void setFurniturePreview(){
        //미리보기 세팅용 코드
        selectPreview = (FrameLayout) findViewById(R.id.select_fur_preview);
        textViewPreview = (TextView)findViewById(R.id.textView_preview);
        imageViewPreview = (ImageView)findViewById(R.id.imageView_preview);
    }

    public void setSubmenu(){
        // 서브메뉴의 생성
        RecyclerView subRecyclerView = findViewById(R.id.submenu_View);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        subRecyclerView.setLayoutManager(layoutManager);

        // 구분자 적용
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL);
        subRecyclerView.addItemDecoration(dividerItemDecoration);

        SubAdapter subAdapter = new SubAdapter(getApplicationContext());

        subAdapter.addItem(new subMenuData(R.drawable.title2d, "2D Create", "2D 가구 모델을 만들 수 있습니다."));
        subAdapter.addItem(new subMenuData(R.drawable.title3d, "3D Create", "3D 가구 모델을 만들 수 있습니다."));
        subAdapter.addItem(new subMenuData(R.drawable.download_icon, "Download", "서버에서 다양한 가구 모델을 받을 수 있습니다."));
        subAdapter.addItem(new subMenuData(R.drawable.upload_icon, "Upload", "가구 모델을 서버로 올릴 수 있습니다."));
        subAdapter.addItem(new subMenuData(R.drawable.del_icon, "Model Delete", "필요 없는 가구를 삭제할 수 있습니다."));
        subAdapter.addItem(new subMenuData(R.drawable.setting_icon, "Settings", "여러가지 설정을 조절할 수 있습니다."));

        subRecyclerView.setAdapter(subAdapter);
    }

    public void setFurnitureList(){
        // 가구목록 생성
        recyclerView = findViewById(R.id.furniture_list);

        // 위치의 유지를 위해
        boolean flag = false;
        if (recyclerView.getLayoutManager() != null) {
            recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
            flag = true;
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        // 데코레이터 적용
        if (!isDeco) {
            recyclerView.addItemDecoration(new MainActivity.SpacingItemDecoration(8));
            isDeco = true;
        }

        CustomerAdapter adapter = new CustomerAdapter(getApplicationContext());

        // 내부 저장소 파싱을 통해 모델 체크
        File path = new File(getFilesDir().getAbsolutePath() + "/models");
        File[] files = path.listFiles();
        List<String> filenames = new ArrayList<>();
        for(int i = 0; i < files.length; i++){
            String temp = files[i].getName();
            int idx = temp.lastIndexOf(".");
            if(temp.substring(idx + 1).equals("obj")){
                filenames.add(temp.substring(0, idx));
            }
        }

        // 체크된 모델 가구목록에 삽입
        for(int i = 0; i < filenames.size(); i++){
            adapter.addItem(new String(filenames.get(i)));
        }

        // 가구 목록 적용
        recyclerView.setAdapter(adapter);
        if (flag) {
            recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        }
    }

    public void setBatteryTimer(){
        // 순서 재조정
        FrameLayout frameLayout = findViewById(R.id.layout_battery);
        TextView textView = findViewById(R.id.textView_battery);
        ImageView imageView = findViewById(R.id.imageView_battery);
        frameLayout.bringToFront();
        imageView.bringToFront();
        textView.bringToFront();

        // 필터 설정
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

        // 스레드 에러를 방지하기 위해 핸들러로 대신 처리
        @SuppressLint("HandlerLeak")
        Handler handler = new Handler(){
            public void handleMessage(Message msg){
                Intent batteryStatus = registerReceiver(null, ifilter);
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

                float batteryPct = level / (float)scale;

                String value = String.valueOf((int)(batteryPct * 100));
                int check = (int)(batteryPct * 100);

                // 상태 체크
                if (status == BatteryManager.BATTERY_STATUS_CHARGING){
                    imageView.setImageResource(R.drawable.battery_charge);
                    textView.setTextColor(Color.rgb(0x0, 0x99, 0x0));
                }
                else if (check >= 50){
                    imageView.setImageResource(R.drawable.battery_high);
                    textView.setTextColor(Color.rgb(0x0, 0x99, 0x0));
                }
                else{
                    imageView.setImageResource(R.drawable.battery_low);
                    textView.setTextColor(Color.rgb(0x99, 0x0, 0x0));
                }

                textView.setText(value + "%");
            }
        };

        // 주기적 갱신
        TimerTask battery = new TimerTask() {
            @Override
            public void run() {
                Log.v(TAG,"timer run");
                Message msg = handler.obtainMessage();
                handler.sendMessage(msg);
            }
        };
        Timer timer = new Timer();
        timer.schedule(battery, 0, 3000);
    }

    public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.ViewHolder>{
        ArrayList<String> items = new ArrayList<>();
        Context context;

        public CustomerAdapter(Context context){
            this.context = context;
        }

        @Override
        public int getItemCount(){
            return items.size();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
            LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View itemView = vi.inflate(R.layout.list_view, parent, false);

            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position){
            String item = items.get(position);
            holder.setItem(item);
        }

        public void addItem(String item){
            items.add(item);
        }

        class ViewHolder extends RecyclerView.ViewHolder{
            ImageButton imageButton;
            Button delButton;
            Button uploadButton;
            TextView textView;

            public ViewHolder(@NonNull View itemView){
                super(itemView);

                textView = (TextView)itemView.findViewById(R.id.textView);

                // 삭제버튼
                delButton = (Button)itemView.findViewById((R.id.delete));
                if (isDel){
                    delButton.setVisibility(View.VISIBLE);
                }
                else{
                    delButton.setVisibility(View.INVISIBLE);
                }
                delButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                        alert.setTitle("가구 모델 삭제");
                        alert.setMessage(textView.getText() + " 모델을 삭제하시겠습니까?");
                        alert.setPositiveButton("제거", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                //제거 부분
                                File path = new File(getFilesDir().getAbsolutePath() + "/models");
                                File[] files = path.listFiles();

                                // 목록에 있는 파일 중 해당하는 이름의 파일 삭제
                                for(int i = 0; i < files.length; i++){
                                    String temp = files[i].getName();
                                    int idx = temp.lastIndexOf(".");
                                    if (temp.substring(0, idx).equals(textView.getText())) {
                                        files[i].delete();
                                    }
                                }

                                //프리뷰 제거
                                path = new File(getFilesDir().getAbsolutePath() + "/previews/" + textView.getText() + "preview.png");
                                path.delete();

                                // 이미 렌더링 되있는 앵커 제거
                                for (int i = 0; i < anchors.size(); i++){
                                    if (anchors.get(i).name.equals(textView.getText())){
                                       anchors.remove(i);
                                       i--;
                                    }
                                }
                                selectedAnchor = -1;

                                //선택된 가구일 경우 제거
                                if (togled != null && togled.equals(textView.getText())){
                                    togled = null;
                                    selectPreview.setVisibility(View.INVISIBLE);
                                }

                                // 가구 목록의 변화가 있으므로 갱신
                                setFurnitureList();
                            }
                        });
                        alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        alert.show();
                    }
                });

                // 업로드 버튼
                uploadButton = (Button)itemView.findViewById((R.id.upload_check));
                if (isUpload){
                    uploadButton.setVisibility(View.VISIBLE);
                }
                else{
                    uploadButton.setVisibility(View.INVISIBLE);
                }
                uploadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                        alert.setTitle("가구 모델 업로드");
                        alert.setMessage(textView.getText() + " 모델을 업로드하시겠습니까?");
                        alert.setPositiveButton("업로드", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 업로드 진행
                                UpData task = new UpData();
                                task.execute("http://" + IP_ADDRESS + "/upload.php", textView.getText().toString());

                                dialog.cancel();
                            }
                        });
                        alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        alert.show();
                    }
                });

                // 가구이미지
                imageButton = (ImageButton)itemView.findViewById(R.id.button);
                imageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 모드가 적용되지 않은 경우 가구 선택
                        if (!isDel && !isUpload) {
                            Toast.makeText(MainActivity.this, textView.getText(), Toast.LENGTH_SHORT).show();
                            togled = textView.getText().toString();
                            textViewPreview.setText(textView.getText() + " 선택됨");
                            BitmapDrawable bd = (BitmapDrawable) imageButton.getDrawable();
                            Bitmap temp = bd.getBitmap();
                            imageViewPreview.setImageBitmap(temp);
                            selectPreview.setVisibility(View.VISIBLE);
                            furnitureMenuClick(null);
                        }
                        // 업로드 모드일 경우
                        else if (isUpload){
                            uploadButton.callOnClick();
                        }
                        // 삭제 모드일 경우
                        else {
                            // 기본 가구는 삭제하지 못하도록
                            String [] strings = {"andy", "desk", "chair", "lamp"};

                            if (!Arrays.asList(strings).contains(textView.getText())) {
                                delButton.callOnClick();
                            }
                        }
                    }
                });
            }

            public void setItem(String item){
                // 이름에 맞춰 파일 탐색 및 적용
                File path = new File(getFilesDir().getAbsolutePath() + "/previews");
                File[] files = path.listFiles();
                InputStream is = null;
                Bitmap bitmap;
                try {
                    File check = new File(path + "/" + item + "preview.png");
                    if(check.exists()){
                        is = new FileInputStream(check);
                        bitmap = BitmapFactory.decodeStream(is);
                        imageButton.setImageBitmap(bitmap);
                    }else {
                        imageButton.setImageResource(R.drawable.furniture_vector_icons_111434);
                    }
                }catch (IOException e){

                }
                textView.setText(item);

                // 기본 모델은 지울 수 없도록
                String [] strings = {"andy", "desk", "chair", "lamp"};

                delButton = (Button)itemView.findViewById((R.id.delete));
                if (Arrays.asList(strings).contains(textView.getText())) {
                    delButton.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    public class SpacingItemDecoration extends RecyclerView.ItemDecoration {
        // 주어진 숫자 만큼 공간을 띄워주는 데코레이터
        private final int spacing;

        public SpacingItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position

            if (position != 0) {
                outRect.left = spacing;
            }
        }
    }

    public class SubAdapter extends RecyclerView.Adapter<SubAdapter.ViewHolder>{
        ArrayList<subMenuData> items = new ArrayList<>();
        Context context;

        public SubAdapter(Context context){
            this.context = context;
        }

        @Override
        public int getItemCount(){
            return items.size();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
            LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View itemView = vi.inflate(R.layout.submenu_view, parent, false);

            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position){
            subMenuData item = items.get(position);
            holder.setItem(item);
        }

        public void addItem(subMenuData item){
            items.add(item);
        }

        @Override
        public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
            // marquee의 시작 시점
            super.onViewAttachedToWindow(holder);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    holder.text.setSelected(true);
                }
            }, 3000);
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
            // marquee의 종료 시점
            super.onViewDetachedFromWindow(holder);
            holder.text.setSelected(false);
        }

        class ViewHolder extends RecyclerView.ViewHolder{
            FrameLayout line;
            ImageView picture;
            TextView name;
            TextView text;

            public ViewHolder(@NonNull View itemView){
                super(itemView);

                // 메뉴의 선택
                line = (FrameLayout)itemView.findViewById(R.id.submenu_area);
                line.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String temp = name.getText().toString();
                        switch (temp){
                            case "2D Create":
                                button2dClick(null);
                                break;
                            case "3D Create":
                                button3dClick(null);
                                break;
                            case "Download":
                                downloadClick(null);
                                break;
                            case "Upload":
                                uploadClick(null);
                                break;
                            case "Model Delete":
                                furDeleteClick(null);
                                break;
                            case "Settings":
                                Toast.makeText(getApplicationContext(), "Settings", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });
                picture = (ImageView)itemView.findViewById(R.id.menu_image);
                name = (TextView)itemView.findViewById(R.id.menu_name);
                text = (TextView)itemView.findViewById(R.id.menu_text);
            }

            public void setItem(subMenuData item){
                picture.setImageResource(item.picture);
                name.setText(item.name);
                text.setText(item.text);
            }
        }
    }

    private class UpData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(MainActivity.this, "Please Wait", "가구 데이터를 올리는 중입니다.", true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();
            Log.d(TAG, "response - " + result);

            // 결과가 도착한 경우
            if (result != null){
                // 내용물이 없는 경우
                if (result.equals("")) {
                    Log.d(TAG, "UpData : error");
                }
                // 내용물이 있는 경우
                else {
                    // 성공적인 진행
                    uploadModel(result);
                }
            }
            // 연결에 실패한 경우
            else{
                Log.d(TAG, "connect failed");
            }
        }

        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];
            String name = params[1];

            try {
                HttpURLConnection conn = null;
                DataOutputStream dos = null;
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;
                File sourceFile;
                FileInputStream fileInputStream;

                URL url = new URL(serverURL);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());


                // 텍스트 데이터
                wr.writeBytes("\r\n--" + boundary + "\r\n");
                wr.writeBytes("Content-Disposition: form-data; name=\"name\"\r\n\r\n" + name);
                wr.writeBytes("\r\n--" + boundary + "\r\n");


                // obj 전송
                sourceFile = new File(getFilesDir().getAbsolutePath() + "/models/" + name + ".obj");
                fileInputStream = new FileInputStream(sourceFile);

                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"obj\";filename=\"" + name + ".obj\"" + lineEnd);
                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


                // 텍스쳐 전송
                sourceFile = new File(getFilesDir().getAbsolutePath() + "/models/" + name + ".png");
                fileInputStream = new FileInputStream(sourceFile);

                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"tex\";filename=\"" + name + ".png\"" + lineEnd);
                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


                // 프리뷰 전송
                sourceFile = new File(getFilesDir().getAbsolutePath() + "/previews/" + name + "preview.png");
                fileInputStream = new FileInputStream(sourceFile);

                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"pre\";filename=\"" + name + "_preview.png\"" + lineEnd);
                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


                // 대답 체크
                int responseStatusCode = conn.getResponseCode();
                Log.d(TAG, "response code - " + responseStatusCode);

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = conn.getInputStream();
                }
                else{
                    inputStream = conn.getErrorStream();
                }

                // 통신 정보를 저장
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }

                bufferedReader.close();

                return sb.toString().trim();

            } catch (Exception e) {
                Log.d(TAG, "UpData : Error ", e);
                errorString = e.toString();

                return null;
            }
        }
    }

    public void uploadModel(String name){
        if (name.equals("already exist")){
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("모델 업로드 실패");
            alert.setMessage(name + " 은(는) 이미 서버에 존재하는 모델입니다.");
            alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            alert.show();
        }
        else {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("모델 업로드");
            alert.setMessage(name + " 모델 업로드 완료!");
            alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            alert.show();
        }
    }

    public void downloadClick(View view){
        // 다운로드 액티비티로 이동
        furnitureMenuClick(null);
        Intent intent = new Intent(this, DownloadActivity.class);

        // 지연시간으로 애니메이션 효과 지속
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivityForResult(intent, 0);
            }
        }, 500);
    }

    public void uploadClick(View view){
        ImageView icon = findViewById(R.id.mode_icon);
        TextView text = findViewById(R.id.mode_text);

        if (isUpload){
            icon.setVisibility(View.GONE);
            text.setVisibility(View.GONE);
            isUpload = false;
        }
        else{
            if (isDel){
               furDeleteClick(null);
            }
            isUpload = true;
            subMenuClick(null);
            icon.setImageResource(R.drawable.upload_icon);
            icon.setVisibility(View.VISIBLE);
            text.setVisibility(View.VISIBLE);
        }

        // 뷰 갱신 및 위치 고정
        recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
        CustomerAdapter adapter = (CustomerAdapter)recyclerView.getAdapter();
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
        recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
    }

    public void furDeleteClick(View view){
        ImageView icon = findViewById(R.id.mode_icon);
        TextView text = findViewById(R.id.mode_text);

        if (isDel){
            icon.setVisibility(View.GONE);
            text.setVisibility(View.GONE);
            isDel = false;
        }
        else{
            if (isUpload){
                uploadClick(null);
            }
            isDel = true;
            subMenuClick(null);
            icon.setImageResource(R.drawable.del_icon);
            icon.setVisibility(View.VISIBLE);
            text.setVisibility(View.VISIBLE);
        }

        // 뷰 갱신 및 위치 고정
        recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
        CustomerAdapter adapter = (CustomerAdapter)recyclerView.getAdapter();
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
        recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
    }

    public void subMenuClick(View view){
        // 서브 메뉴 클릭
        FrameLayout submenu = (FrameLayout)findViewById(R.id.furniture_list_submenu);
        if (isSubmenu){
            isSubmenu = false;
            submenu.setVisibility(View.GONE);
            submenu.startAnimation(sub_down);
        }
        else {
            isSubmenu = true;
            setSubmenu();
            submenu.setVisibility(View.VISIBLE);
            submenu.startAnimation(sub_up);

            // 모드 정리
            if (isDel){
                furDeleteClick(null);
            }
            if (isUpload){
                uploadClick(null);
            }
        }
    }

    public void furnitureMenuClick(View view){
        FrameLayout listView = (FrameLayout)findViewById(R.id.furniture_list_frame);
        listView.bringToFront();

        if(!isUp){
            //열기
            listView.setVisibility(View.VISIBLE);
            listView.startAnimation(translate_up);
            isUp = true;
        }
        else{
            //닫기
            listView.setVisibility(View.GONE);
            listView.startAnimation(translate_down);
            if (isSubmenu){
                subMenuClick(null);
            }
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isDel){
                        furDeleteClick(null);
                    }
                    if (isUpload){
                        uploadClick(null);
                    }
                }
            }, 500);
            isUp = false;
        }

        //선택 상태 유지를 막기위해 해제
        if (selectedAnchor >= 0) {
            anchors.get(selectedAnchor).selected = false;
            selectedAnchor = -1;
        }
    }

    public void modeIconClick(View view){
        if (isDel){
            furDeleteClick(null);
        }
        if (isUpload){
            uploadClick(null);
        }
    }

    public void furListExitClick(View view){
        furnitureMenuClick(null);
    }

    public void button2dClick(View view){
        // 2d 액티비티로 이동
        furnitureMenuClick(null);
        Intent intent = new Intent(this, Create2dActivity.class);

        // 지연시간으로 애니메이션 효과 지속
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivityForResult(intent, 0);
            }
        }, 500);
    }

    public void button3dClick(View view){
        Toast.makeText(getApplicationContext(), "3D 기능은 준비중입니다.", Toast.LENGTH_SHORT).show();
    }

    public void resetClick(View view){
        //리셋 확인 다이얼로그
        AlertDialog.Builder alert  = new AlertDialog.Builder(this);
        alert.setTitle("Reset");
        alert.setMessage("배치된 모든 모델을 제거하고, \nPlane을 재설정 합니다.");
        alert.setPositiveButton("진행", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                anchors.clear();
                selectedAnchor = -1;

                // plane 초기화를 위해 세션 관련 작업 전부 정지
                surfaceView.onPause();
                waitUntilCameraCaptureSessionIsActive();
                mSession.close();
                displayRotationHelper.onPause();
                closeCamera();
                stopBackgroundThread();
                shouldUpdateSurfaceTexture.set(false);
                pauseARCore();

                // 세션 초기화
                if (sharedSession != null) {
                    sharedSession.close();
                    sharedSession = null;
                }

                // 관련 작업 전부 재실행
                waitUntilCameraCaptureSessionIsActive();
                startBackgroundThread();
                surfaceView.onResume();
                if (surfaceCreated) {
                    openCamera();
                }
                displayRotationHelper.onResume();
            }
        });
        alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alert.show();
    }

    class planeChangedListener implements CompoundButton.OnCheckedChangeListener{
        // 플레인 설정이 바뀐 것을 감지
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked){
                Toast.makeText(getApplicationContext(), "plane visible!", Toast.LENGTH_SHORT).show();
                isPlaneChanged = true;
                isPlaneshow = true;
            }
            else{
                Toast.makeText(getApplicationContext(), "plane invisible!", Toast.LENGTH_SHORT).show();
                isPlaneChanged = true;
                isPlaneshow = false;
            }
        }
    }

    public void captureClick(View view) throws InterruptedException {
        // 화면 캡쳐 기능

        // 소리 출력
        MediaActionSound sound = new MediaActionSound();
        sound.play(MediaActionSound.SHUTTER_CLICK);

        saveCheck = true;
        saveSucess = false;

        // 콜백함수의 진행을 기다린 뒤 결과 출력
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(saveSucess){
                    Toast.makeText(getApplicationContext(), "Screen Captured", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Screen Capture failed", Toast.LENGTH_SHORT).show();
                }

            }
        }, 300);
    }

    public boolean screenShot(Bitmap bm){
        if(bm != null) {
            Bitmap overlay = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), bm.getConfig());
            Canvas canvas = new Canvas(overlay);
            canvas.drawBitmap(bm, 0, 0, null);

            File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/FSCapture");
            //폴더 생성
            if (!path.exists()) {
                path.mkdirs();
            }
            try {
                //날짜서식 지정 및 파일 저장
                SimpleDateFormat day = new SimpleDateFormat("yyMMdd_HHmmss");
                Date date = new Date();
                FileOutputStream os = new FileOutputStream(path + "/capture_" + day.format(date) + ".jpg");
                overlay.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.close();
            } catch (IOException e) {
                Log.e("Save_Image", e.getMessage(), e);
            }
            //미디어 스캔
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(path));
            sendBroadcast(intent);

            return true;
        }
        else{
            return false;
        }
    }

    public void doRequestPermissions() {
        // 권한 목록
        String[] permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        //거부된 권한 목록 파악 후 요청
        ArrayList<String> notGrantedPermissions = new ArrayList<>();
        for(String perm : permissions){
            if(PermissionChecker.checkSelfPermission(this, perm) == PermissionChecker.PERMISSION_DENIED){
                notGrantedPermissions.add(perm);
            }
        }
        ActivityCompat.requestPermissions(this, notGrantedPermissions.toArray(new String[]{}), 101);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] gramtResults){
        switch(requestCode){
            case 101:
                if(gramtResults.length > 0 && gramtResults[0] == PackageManager.PERMISSION_GRANTED){
                    //권한 허가시
                    waitUntilCameraCaptureSessionIsActive();
                    startBackgroundThread();
                    surfaceView.onResume();
                    if (surfaceCreated) {
                        openCamera();
                    }
                }else {
                    //권한 거부시
                    AlertDialog.Builder alert  = new AlertDialog.Builder(this);
                    alert.setTitle("APP permissions");
                    alert.setMessage("해당 앱을 이용하시려면 권한이 필요합니다.");
                    alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            finish();
                        }
                    });
                    alert.show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, gramtResults);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        //모델생성 확인시 표시를 위해서
        if(requestCode == 0){
            if(resultCode == RESULT_OK) {
                setFurnitureList();
                String newModel = data.getExtras().getString("modelname");
                newModelName = newModel;
                isNewModel = true;
            }
        }
    }

    private void createCameraPreviewSession() {
        try {
            // Note that isGlAttached will be set to true in AR mode in onDrawFrame().
            sharedSession.setCameraTextureName(backgroundRenderer.getTextureId());

            // Create an ARCore compatible capture request using `TEMPLATE_RECORD`.
            previewCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            // Build surfaces list, starting with ARCore provided surfaces.
            List<Surface> surfaceList = sharedCamera.getArCoreSurfaces();

            // Add ARCore surfaces and CPU image surface targets.
            for (Surface surface : surfaceList) {
                previewCaptureRequestBuilder.addTarget(surface);
            }

            // Wrap our callback in a shared camera callback.
            CameraCaptureSession.StateCallback wrappedCallback =
                    sharedCamera.createARSessionStateCallback(cameraSessionStateCallback, backgroundHandler);

            // Create camera capture session for camera preview using ARCore wrapped callback.
            cameraDevice.createCaptureSession(surfaceList, wrappedCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException", e);
        }
    }

    private final CameraDevice.StateCallback cameraDeviceCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "Camera device ID " + cameraDevice.getId() + " opened.");
            MainActivity.this.cameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onClosed(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "Camera device ID " + cameraDevice.getId() + " closed.");
            MainActivity.this.cameraDevice = null;
            safeToExitApp.open();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.w(TAG, "Camera device ID " + cameraDevice.getId() + " disconnected.");
            cameraDevice.close();
            MainActivity.this.cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.e(TAG, "Camera device ID " + cameraDevice.getId() + " error " + error);
            cameraDevice.close();
            MainActivity.this.cameraDevice = null;
            // Fatal error. Quit application.
            finish();
        }
    };

    CameraCaptureSession.StateCallback cameraSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "Camera capture session configured.");
            captureSession = session;
            setRepeatingCaptureRequest();
        }
        @Override
        public void onSurfacePrepared(@NonNull CameraCaptureSession session, @NonNull Surface surface) {
            Log.d(TAG, "Camera capture surface prepared.");
        }
        @Override
        public void onReady(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "Camera capture session ready.");
        }

        @Override
        public void onActive(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "Camera capture session active.");
            if (!arcoreActive) {
                resumeARCore();
            }
            synchronized (MainActivity.this) {
                captureSessionChangesPossible = true;
                MainActivity.this.notify();
            }
        }
        @Override
        public void onCaptureQueueEmpty(@NonNull CameraCaptureSession session) {
            Log.w(TAG, "Camera capture queue empty.");
        }
        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "Camera capture session closed.");
        }
        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "Failed to configure camera capture session.");
        }
    };

    private final CameraCaptureSession.CaptureCallback cameraCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            shouldUpdateSurfaceTexture.set(true);
        }
        @Override
        public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
            Log.e(TAG, "onCaptureBufferLost: " + frameNumber);
        }
        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            Log.e(TAG, "onCaptureFailed: " + failure.getFrameNumber() + " " + failure.getReason());
        }
        @Override
        public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
            Log.e(TAG, "onCaptureSequenceAborted: " + sequenceId + " " + session);
        }
    };

    @Override
    public void onImageAvailable(ImageReader imageReader) {
        Image image = imageReader.acquireLatestImage();
        if (image == null) {
            Log.w(TAG, "onImageAvailable: Skipping null image.");
            return;
        }

        image.close();
    }

    private void setRepeatingCaptureRequest() {
        try {
            captureSession.setRepeatingRequest(
                    previewCaptureRequestBuilder.build(), cameraCaptureCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to set repeating request", e);
        }
    }
}