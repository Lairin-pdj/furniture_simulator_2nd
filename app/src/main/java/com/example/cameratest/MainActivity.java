package com.example.cameratest;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import android.os.Parcelable;
import android.util.Log;
import android.util.TypedValue;
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

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private Session mSession;
    private static Session sharedSession;
    private boolean mUserRequestInstall = true;
    private static GLSurfaceView surfaceView;
    private boolean surfaceCreated;
    private static TextView textViewPreview;
    private static ImageView imageViewPreview;

    private CameraManager cameraManager;
    private List<CaptureRequest.Key<?>> keysThatCanCauseCaptureDelaysWhenModified;
    private CameraDevice cameraDevice;
    private static SharedCamera sharedCamera;
    private String cameraId;
    private CameraCaptureSession captureSession;
    private boolean arcoreActive;
    private boolean isGlAttached;
    private boolean captureSessionChangesPossible = true;
    private CaptureRequest.Builder previewCaptureRequestBuilder;
    private boolean saveCheck = false;
    private boolean saveSucess;
    private boolean isNewModel = false;
    private String newModelName = null;

    private final AtomicBoolean shouldUpdateSurfaceTexture = new AtomicBoolean(false);
    private TapHelper tapHelper;
    private DisplayRotationHelper displayRotationHelper;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final Map<String, ObjectRenderer> virtualObject = new HashMap<>();
    private final Map<String, ComplexObjectRenderer> complexvirtualObject = new HashMap<>();
    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();
    private final ObjectRenderer virtualObjectSelect = new ObjectRenderer();
    private boolean isPlaneChanged = false;
    private boolean isPlaneshow = true;
    private boolean firstPlanecheck = false;

    private static final Short AUTOMATOR_DEFAULT = 0;
    private static final String AUTOMATOR_KEY = "automator";
    private final AtomicBoolean automatorRun = new AtomicBoolean(false);

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
    private int selectedAnchor = -1;

    private final ArrayList<ColoredAnchor> anchors = new ArrayList<>();
    private String togled = null;
    private ScaleGestureDetector scaleGestureDetector;

    private final ConditionVariable safeToExitApp = new ConditionVariable();

    public static MainActivity getInstance;
    private RecyclerView recyclerView = null;
    private RecyclerView subRecyclerView = null;

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
    private boolean isCreateUp = false;
    private boolean isDel = false;
    private boolean isUpload = false;
    private Animation translate_up;
    private Animation translate_down;
    private Animation button2d_up;
    private Animation button2d_down;
    private Animation button3d_up;
    private Animation button3d_down;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        //멀티 권한 설정
        String[] permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        //권한 승인 여부 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            for(String permission : permissions){
                int result = PermissionChecker.checkSelfPermission(this, permission);
                if (result == PermissionChecker.PERMISSION_GRANTED){
                    //성공시
                }
                else{
                    doRequestPermissions();
                    break;
                }
            }
        }

        super.onCreate(savedInstanceState);
        getInstance = this;
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        translate_up = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.translate_up);
        translate_down = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.translate_down);
        button2d_up = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button2d_up);
        button2d_down = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button2d_down);
        button3d_up = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button3d_up);
        button3d_down = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button3d_down);

        //내장 모델 내부저장소에 저장
        saveInternalModels();

        //프리뷰 내부저장소에 저장
        saveInternalPreviews();

        //glsurfaceview 설정 코드
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

        //plane스위치 표시
        Switch planeSwitch = findViewById(R.id.switch1);
        planeSwitch.bringToFront();
        planeSwitch.setOnCheckedChangeListener(new planeChangedListener());

        //초기튜토리얼 이미지 표시
        ImageView imageView = findViewById(R.id.imageView);
        imageView.bringToFront();

        //핀치줌 체크용
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        resumeARCore();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //arcore 다운 체크
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
            //에러시
        }catch (Exception e){

        }

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
        //모든 권한 획득시 화면 출력
        waitUntilCameraCaptureSessionIsActive();
        startBackgroundThread();
        surfaceView.onResume();

        if (surfaceCreated) {
            openCamera();
        }

        displayRotationHelper.onResume();

        setFurniturePreview();
        setFurnitureList();
        setSubmenu();
    }

    @Override
    protected void onPause() {
        shouldUpdateSurfaceTexture.set(false);
        surfaceView.onPause();
        waitUntilCameraCaptureSessionIsActive();
        displayRotationHelper.onPause();
        pauseARCore();
        closeCamera();
        stopBackgroundThread();

        isUp = false;
        mSession.close();

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
        backgroundThread = new HandlerThread("sharedCameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
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

            for(int i = 0; i < filenames.size(); i++){
                File check = new File(getFilesDir().getAbsolutePath() + "/models/" + filenames.get(i) + ".mtl");
                if(check.exists()){
                    complexvirtualObject.put(filenames.get(i), new ComplexObjectRenderer());
                    complexvirtualObject.get(filenames.get(i)).createOnGlThread(this, "models/" + filenames.get(i) + ".obj", "models/" + filenames.get(i) + ".png");
                }
                else {
                    virtualObject.put(filenames.get(i), new ObjectRenderer());
                    virtualObject.get(filenames.get(i)).createOnGlThread(this, "models/" + filenames.get(i) + ".obj", "models/" + filenames.get(i) + ".png");
                    virtualObject.get(filenames.get(i)).setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);
                }
            }

            virtualObjectSelect.createOnGlThread(this, "models/selected.obj", "models/selected.png");
            virtualObjectSelect.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);
            virtualObjectSelect.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

            openCamera();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
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

        displayRotationHelper.updateSessionIfNeeded(sharedSession);

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

        }

        //초기 plane 탐지 전 이미지 보여주기
        if(!firstPlanecheck && hasTrackingPlane()){
            ImageView imageView;
            imageView = findViewById(R.id.imageView);
            imageView.setVisibility(View.INVISIBLE);
            firstPlanecheck = true;
        }

        if(isNewModel){
            isNewModel = false;
            if(newModelName != null){
                try {
                    virtualObject.put(newModelName, new ObjectRenderer());
                    virtualObject.get(newModelName).createOnGlThread(this, "models/" + newModelName + ".obj", "models/" + newModelName + ".png");
                    virtualObject.get(newModelName).setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);
                }
                catch (Exception e){
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

                int bitmapBuffer[] = new int[(int)(width * height)];
                int bitmapSource[] = new int[(int)(width * height)];

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

        // ARCore attached the surface to GL context using the texture ID we provided
        // in createCameraPreviewSession() via sharedSession.setCameraTextureName(…).
        isGlAttached = true;

        // Handle screen tap.
        handleTap(frame, camera);

        // If frame is ready, render camera preview image to the GL surface.
        backgroundRenderer.draw(frame);

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
        planeRenderer.drawPlanes(
                sharedSession.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);
        //플레인 제거시 수정 필요

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

            if(complexvirtualObject.containsKey(coloredAnchor.name)){
                complexvirtualObject.get(coloredAnchor.name).updateModelMatrix(finish, coloredAnchor.scale);
                complexvirtualObject.get(coloredAnchor.name).draw(viewmtx, projmtx, colorCorrectionRgba);
            }
            else {
                // Update and draw the model and its shadow.
                virtualObject.get(coloredAnchor.name).updateModelMatrix(finish, coloredAnchor.scale);
                virtualObject.get(coloredAnchor.name).draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
            }

            if(coloredAnchor.selected == true) {
                virtualObjectSelect.updateModelMatrix(finish, coloredAnchor.scale);
                virtualObjectSelect.draw(viewmtx, projmtx, colorCorrectionRgba, DEFAULT_COLOR);
            }
        }
    }

    private boolean isARCoreSupportedAndUpToDate(){
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
        cameraId = sharedSession.getCameraConfig().getCameraId();


        try {
            // Wrap our callback in a shared camera callback.
            CameraDevice.StateCallback wrappedCallback =
                    sharedCamera.createARDeviceStateCallback(cameraDeviceCallback, backgroundHandler);

            // Store a reference to the camera system service.
            cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);

            // Get the characteristics for the ARCore camera.
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(this.cameraId);

            // On Android P and later, get list of keys that are difficult to apply per-frame and can
            // result in unexpected delays when modified during the capture session lifetime.
            if (Build.VERSION.SDK_INT >= 28) {
                keysThatCanCauseCaptureDelaysWhenModified = characteristics.getAvailableSessionKeys();
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
                return;
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
                        } else if (trackable instanceof Plane) {
                            objColor = DEFAULT_COLOR;
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

                            //선택되면 다시 선택 해제제
                            togled = null;
                            TextView textViewpreview;
                            ImageView imageView;
                            textViewpreview = getInstance.textViewPreview;
                            imageView = getInstance.imageViewPreview;
                            textViewpreview.setVisibility(View.INVISIBLE);
                            imageView.setVisibility(View.INVISIBLE);
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
        File path = new File(getFilesDir().getAbsolutePath() + "/models");
        if(!path.exists()){
            path.mkdirs();
        }
        //최적화 필요
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

        }
    }

    public void saveInternalPreviews(){
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

        }
    }

    public void setFurniturePreview(){
        //미리보기 세팅용 코드
        textViewPreview = (TextView) findViewById(R.id.textView_preview);
        imageViewPreview = (ImageView) findViewById(R.id.imageView_preview);
    }

    public void setSubmenu(){
        subRecyclerView = findViewById(R.id.submenu_View);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        subRecyclerView.setLayoutManager(layoutManager);

        SubAdapter subAdapter = new SubAdapter(getApplicationContext());

        subAdapter.addItem(new subMenuData(R.drawable.title2d, "2D create", "2d 가구모델을 만들 수 있습니다."));
        subAdapter.addItem(new subMenuData(R.drawable.title3d, "3D create", "3d 가구모델을 만들 수 있습니다."));

        subRecyclerView.setAdapter(subAdapter);
    }

    public void setFurnitureList(){
        recyclerView = findViewById(R.id.furniture_list);

        boolean flag = false;
        if (recyclerView.getLayoutManager() != null) {
            recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
            flag = true;
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        CustomerAdapter adapter = new CustomerAdapter(getApplicationContext());

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

        for(int i = 0; i < filenames.size(); i++){
            adapter.addItem(new String(filenames.get(i)));
        }

        recyclerView.setAdapter(adapter);
        if (flag) {
            recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        }
    }

    public void furnitureMenuClick(View view){
        RecyclerView listView = (RecyclerView)findViewById(R.id.furniture_list);
        Button button = (Button)findViewById(R.id.button_list);
        Button sub = (Button)findViewById(R.id.button_sub_menu);
        TextView textView = (TextView)findViewById(R.id.furniture_text);
        Resources r = getResources();
        if(!isUp){
            //열기
            listView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            textView.bringToFront();
            listView.startAnimation(translate_up);
            textView.startAnimation(translate_up);
            button.setVisibility(View.GONE);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    button.setY(button.getY() - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 235, r.getDisplayMetrics()));
                    button.setX(button.getX() - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, r.getDisplayMetrics()));
                    button.setVisibility(View.VISIBLE);
                    sub.setVisibility(View.VISIBLE);
                }
            }, 500);
            isUp = true;
        }
        else{
            //닫기
            listView.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
            listView.startAnimation(translate_down);
            textView.startAnimation(translate_down);
            button.setVisibility(View.GONE);
            sub.setVisibility(View.GONE);
            if (isSubmenu){
                subMenu(null);
            }
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    button.setY(button.getY() + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 235, r.getDisplayMetrics()));
                    button.setX(button.getX() + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, r.getDisplayMetrics()));
                    button.setVisibility(View.VISIBLE);
                    if (isDel){
                        furDelete(null);
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
            TextView textViewpreview;
            ImageView imageView;

            public ViewHolder(@NonNull View itemView){
                super(itemView);

                textView = (TextView)itemView.findViewById(R.id.textView);

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
                                    TextView textViewpreview = getInstance.textViewPreview;
                                    ImageView imageView = getInstance.imageViewPreview;
                                    textViewpreview.setVisibility(View.INVISIBLE);
                                    imageView.setVisibility(View.INVISIBLE);
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

                uploadButton = (Button)itemView.findViewById((R.id.upload_check));
                if (isUpload){
                    uploadButton.setVisibility(View.VISIBLE);
                }
                else{
                    uploadButton.setVisibility(View.INVISIBLE);
                }


                imageButton = (ImageButton)itemView.findViewById(R.id.button);
                imageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isDel) {
                            Toast.makeText(MainActivity.this, textView.getText(), Toast.LENGTH_SHORT).show();
                            togled = textView.getText().toString();
                            textViewpreview = getInstance.textViewPreview;
                            textViewpreview.setText(textView.getText());
                            textViewpreview.setVisibility(View.VISIBLE);
                            imageView = getInstance.imageViewPreview;
                            BitmapDrawable bd = (BitmapDrawable) imageButton.getDrawable();
                            Bitmap temp = bd.getBitmap();
                            imageView.setImageBitmap(temp);
                            imageView.setVisibility(View.VISIBLE);
                            furnitureMenuClick(null);
                        }
                        else {
                            String [] strings = {"andy", "desk", "chair", "lamp"};

                            if (!Arrays.asList(strings).contains(textView.getText())) {
                                delButton.callOnClick();
                            }
                        }
                    }
                });
            }

            public void setItem(String item){
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

        class ViewHolder extends RecyclerView.ViewHolder{
            FrameLayout line;
            ImageView picture;
            TextView name;
            TextView text;


            public ViewHolder(@NonNull View itemView){
                super(itemView);

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

    public void downloadClick(View view){
        furnitureMenuClick(view);
        Intent intent = new Intent(this, DownloadActivity.class);
        startActivityForResult(intent, 0);
    }

    public void uploadClick(View view){
        //Button up = (Button)findViewById(R.id.button_fur_del);

        if (isUpload){
            isUpload = false;
            //up.setBackgroundResource(R.drawable.del_noncheck);
        }
        else{
            isUpload = true;
            //up.setBackgroundResource(R.drawable.del_check);
        }

        recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
        CustomerAdapter adapter = (CustomerAdapter)recyclerView.getAdapter();
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
        recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
    }

    public void furDelete(View view){
        Button del = (Button)findViewById(R.id.button_fur_del);

        if (isDel){
            isDel = false;
            del.setBackgroundResource(R.drawable.del_noncheck);
        }
        else{
            isDel = true;
            del.setBackgroundResource(R.drawable.del_check);
        }

        recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
        CustomerAdapter adapter = (CustomerAdapter)recyclerView.getAdapter();
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
        recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
    }

    public void subMenu(View view){
        FrameLayout submenu = (FrameLayout)findViewById(R.id.furniture_list_submenu);
        if (isSubmenu){
            isSubmenu = false;
            submenu.setVisibility(View.INVISIBLE);
        }
        else {
            isSubmenu = true;
            submenu.setVisibility(View.VISIBLE);
        }
    }

    public void createMenuClick(View view){
        Button bt2d = (Button)findViewById(R.id.button2d);
        Button bt3d = (Button)findViewById(R.id.button3d);
        if(!isCreateUp){
            isCreateUp = true;
            bt2d.setVisibility(View.VISIBLE);
            bt3d.setVisibility(View.VISIBLE);
            bt2d.startAnimation(button2d_up);
            bt3d.startAnimation(button3d_up);
        }
        else{
            isCreateUp = false;
            bt2d.setVisibility(View.INVISIBLE);
            bt3d.setVisibility(View.INVISIBLE);
            bt2d.startAnimation(button2d_down);
            bt3d.startAnimation(button3d_down);
        }
    }

    public void button2dClick(View view){
        createMenuClick(view);
        Intent intent = new Intent(this, Create2dActivity.class);
        startActivityForResult(intent, 0);
    }

    public void button3dClick(View view){
        createMenuClick(view);
        Toast.makeText(getApplicationContext(), "3D 기능은 준비중입니다.", Toast.LENGTH_SHORT).show();
    }

    public void resetClick(View view){
        //리셋 확인 다이얼로그
        AlertDialog.Builder alert  = new AlertDialog.Builder(this);
        alert.setTitle("RESET Anchor");
        alert.setMessage("모든 모델을 제거합니다.");
        alert.setPositiveButton("제거", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                anchors.clear();
                selectedAnchor = -1;
            }
        });
        alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alert.show();

        //플레인 재설정 관련 코드
        //sharedSession.close();
        //onResume();
    }

    class planeChangedListener implements CompoundButton.OnCheckedChangeListener{
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
        MediaActionSound sound = new MediaActionSound();
        sound.play(MediaActionSound.SHUTTER_CLICK);

        saveCheck = true;
        saveSucess = false;

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

    private <T> boolean checkIfKeyCanCauseDelay(CaptureRequest.Key<T> key) {
        if (Build.VERSION.SDK_INT >= 28) {
            // On Android P and later, return true if key is difficult to apply per-frame.
            return keysThatCanCauseCaptureDelaysWhenModified.contains(key);
        } else {
            // On earlier Android versions, log a warning since there is no API to determine whether
            // the key is difficult to apply per-frame. Certain keys such as CONTROL_AE_TARGET_FPS_RANGE
            // are known to cause a noticeable delay on certain devices.
            // If avoiding unexpected capture delays when switching between non-AR and AR modes is
            // important, verify the runtime behavior on each pre-Android P device on which the app will
            // be distributed. Note that this device-specific runtime behavior may change when the
            // device's operating system is updated.
            Log.w(
                    TAG,
                    "Changing "
                            + key
                            + " may cause a noticeable capture delay. Please verify actual runtime behavior on"
                            + " specific pre-Android P devices that this app will be distributed on.");
            // Allow the change since we're unable to determine whether it can cause unexpected delays.
            return false;
        }
    }
}