package com.example.cameratest;

import android.Manifest;
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.cameratest.helper.DisplayRotationHelper;
import com.example.cameratest.helper.TapHelper;
import com.example.cameratest.helper.TrackingStateHelper;
import com.example.cameratest.rendering.BackgroundRenderer;
import com.example.cameratest.rendering.ComplexObjectRenderer;
import com.example.cameratest.rendering.ObjectRenderer;
import com.example.cameratest.rendering.PlaneRenderer;
import com.example.cameratest.rendering.PointCloudRenderer;
import com.example.cameratest.sub.Empty;
import com.example.cameratest.sub.Furniturelist;
import com.example.cameratest.sub.FurniturelistButton;
import com.example.cameratest.sub.Submenu;
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

import java.io.File;
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

    // surfaceview??? ?????? ?????????
    private Session mSession;
    private static Session sharedSession;
    private boolean mUserRequestInstall = true;  // ?????? ?????? ??????
    private static GLSurfaceView surfaceView;
    private boolean surfaceCreated;

    // ????????? ?????? ??????????????? view
    public static FrameLayout selectPreview;
    public static TextView textViewPreview;
    public static ImageView imageViewPreview;

    // ????????? ????????? ??? ????????? ?????? ?????????
    private CameraDevice cameraDevice;
    private static SharedCamera sharedCamera;
    private CameraCaptureSession captureSession;
    private boolean captureSessionChangesPossible = true;
    private CaptureRequest.Builder previewCaptureRequestBuilder;
    private final ConditionVariable safeToExitApp = new ConditionVariable();

    // ??????????????? ????????? ????????? ????????? ?????? ?????????
    private boolean saveCheck = false;
    private boolean saveSucess;
    private boolean isNewModel = false;
    private String newModelName = null;

    // ar?????? ??? ????????? ?????? ?????????
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
    private final Map<String, ObjectRenderer> virtualObject = new HashMap<>(); // obj?????? ????????? ?????????
    private final Map<String, ComplexObjectRenderer> complexvirtualObject = new HashMap<>(); // mtl?????? ????????? ?????????
    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();
    private final ObjectRenderer virtualObjectSelect = new ObjectRenderer();
    private boolean isPlaneChanged = false; // plane ????????? ?????????
    private boolean isPlaneshow = true;
    private boolean firstPlanecheck = false;

    // anchor??? ?????????
    private final float[] anchorMatrix = new float[16];
    private static final float[] DEFAULT_COLOR = new float[] {0f, 0f, 0f, 0f};
    public static class ColoredAnchor{
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
    public int selectedAnchor = -1; // ????????? anchor??? ?????????
    public final ArrayList<ColoredAnchor> anchors = new ArrayList<>();
    public String selected = null;
    private ScaleGestureDetector scaleGestureDetector;

    // ???????????? ??????
    // ????????? ??????
    public String IP_ADDRESS;

    // UI????????? ?????? ?????????
    public static MainActivity getInstance;
    private ViewPager2 viewPagerFurniturelist;
    private FragmentStateAdapter pagerAdapterFurniturelist;
    private int oldDragPositionFur = 0;
    private Animator furnitureAnimator = null;
    private ViewPager2 viewPagerSubmenu;
    private FragmentStateAdapter pagerAdapterSubmenu;
    private int oldDragPositionSub = 0;
    private Animator submenuAnimator = null;
    private boolean isUp = false;
    private boolean isSubmenu = false;
    public boolean isDel = false;
    public boolean isUpload = false;
    public boolean isMode = false;
    private boolean isFirst = true;

    // ???????????? ?????????
    SharedPreferences spref;
    private boolean isBattery;
    private boolean isTimer;
    private String whatFont;
    private boolean isPlane;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        // ?????? ??????
        setEnvironment();

        // ?????? ??????
        switch (whatFont){
            case "??????R":
                setTheme(R.style.AppTheme_NanumR);
                break;
            case "??????B":
                setTheme(R.style.AppTheme_NanumB);
                break;
            case "??????":
                setTheme(R.style.AppTheme_Cafe);
                break;
            case "????????????":
                setTheme(R.style.AppTheme_Sc);
                break;
            default:
                setTheme(R.style.AppTheme);
                break;
        }

        // ?????? ?????? ??????
        String[] permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        // ?????? ?????? ?????? ??????
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
        if (savedInstanceState != null){
            isFirst = savedInstanceState.getBoolean("first", true);
            Log.d("first check", isFirst + " restore");
        }
        getInstance = this;

        // ???????????? ??????
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // ?????? ?????? ?????????????????? ??????
        saveInternalModels();

        // ????????? ?????????????????? ??????
        saveInternalPreviews();

        // view ?????? ??????
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

        // ?????????????????? ????????? ??????
        ImageView imageView = findViewById(R.id.gif_plane_tutirial);
        imageView.bringToFront();
        Glide.with(this).load(R.drawable.plane_tutorial).transform(new RoundedCorners(40)).into(imageView);
        TextView textView = findViewById(R.id.gif_plane_tutirial_text);
        textView.bringToFront();

        // ????????? ?????????
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        // AR ??????
        resumeARCore();

        // ????????? ?????? ???????????? ??? ?????????
        setFurniturePreview();

        // ?????????
        ConstraintLayout constraintLayout = findViewById(R.id.layout_timebattery);
        constraintLayout.bringToFront();

        // ???????????? ????????????
        setViewPagerFurniturelist();

        // ???????????? ????????????
        setViewPagerSubmenu();

        // ????????????
        Log.d("firstcheck", "create");
        if (isFirst) {
            Log.d("firstcheck", "hello");
            LayoutInflater inflater = getLayoutInflater();
            View toastDesign = inflater.inflate(R.layout.toast, null);
            TextView toastText = toastDesign.findViewById(R.id.toast_text);
            toastText.setText("?????? ?????????????????? ????????? ????????????!");
            ImageView toastImage = toastDesign.findViewById(R.id.toast_image);
            toastImage.setImageResource(R.drawable.main_character);
            Toast toast = new Toast(getApplicationContext());
            toast.setGravity(Gravity.BOTTOM, 0, 150);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(toastDesign);
            toast.show();
            isFirst = false;
        }

        // ip ??????
        IP_ADDRESS = getString(R.string.server_IP);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // arcore ?????? ??????
        try{
            if(mSession == null){
                switch (ArCoreApk.getInstance().requestInstall(this, mUserRequestInstall)) {
                    case INSTALLED:
                        mSession = new Session(this);
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

        // ?????? ??????
        String[] permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        int check = 0;

        for(String permission : permissions) {
            int result = PermissionChecker.checkSelfPermission(this, permission);
            if (result == PermissionChecker.PERMISSION_GRANTED) {
                //?????????
                check++;
            }
        }

        //?????? ?????? ????????? ?????? ?????? ??????
        waitUntilCameraCaptureSessionIsActive();
        startBackgroundThread();
        surfaceView.onResume();

        // ?????? ????????????????????? ??????????????? ??????
        if (surfaceCreated) {
            openCamera();
        }

        displayRotationHelper.onResume();


        // ????????? ??????
        FrameLayout layout_battary = findViewById(R.id.layout_battery);
        if (isBattery) {
            layout_battary.setVisibility(View.VISIBLE);
            setBatteryTimer();
        }
        else{
            layout_battary.setVisibility(View.GONE);
        }

        // ?????? ??????
        FrameLayout layout_time = findViewById(R.id.layout_time);
        if (isTimer) {
            layout_time.setVisibility(View.VISIBLE);
            setTimeTimer();
        }
        else{
            layout_time.setVisibility(View.GONE);
        }

        checkFirstRun();
    }

    @Override
    protected void onPause() {
        // ?????? ????????? ????????? ?????? ??????
        surfaceView.onPause(); // view
        waitUntilCameraCaptureSessionIsActive(); // ????????? ?????? ?????????
        mSession.close();
        displayRotationHelper.onPause();
        closeCamera();
        stopBackgroundThread();
        shouldUpdateSurfaceTexture.set(false); // ar ?????? ?????????
        pauseARCore();

        // UI??? ????????? ??????
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

        Log.d("first check", "destroyed");
        super.onDestroy();
    }

    private void startBackgroundThread() {
        // ???????????? ????????? ????????????????????? ????????? ????????? ??????
        backgroundThread = new HandlerThread("sharedCameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        // ????????? ????????? ??????
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
            if (isPlaneshow){
                planeRenderer.createOnGlThread(this, "models/trigrid.png");
            }
            else {
                planeRenderer.createOnGlThread(this, "models/transparent.png");
            }
            pointCloudRenderer.createOnGlThread(this);

            // ?????? ???????????? ????????? ????????? ?????? ??????
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

            // ????????? ????????? ???????????? ????????? ??????
            for(int i = 0; i < filenames.size(); i++){
                File check = new File(getFilesDir().getAbsolutePath() + "/models/" + filenames.get(i) + ".mtl");
                // mtl ????????? ??????
                if(check.exists()){
                    complexvirtualObject.put(filenames.get(i), new ComplexObjectRenderer());
                    complexvirtualObject.get(filenames.get(i)).createOnGlThread(this, "models/" + filenames.get(i) + ".obj", "models/" + filenames.get(i) + ".png");
                }
                // obj ????????? ??????
                else {
                    virtualObject.put(filenames.get(i), new ObjectRenderer());
                    virtualObject.get(filenames.get(i)).createOnGlThread(this, "models/" + filenames.get(i) + ".obj", "models/" + filenames.get(i) + ".png");
                    virtualObject.get(filenames.get(i)).setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);
                }
            }

            // ??????????????? ????????? ??????
            virtualObjectSelect.createOnGlThread(this, "models/selected.obj", "models/selected.png");
            virtualObjectSelect.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);
            virtualObjectSelect.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

            // ????????? ??????
            openCamera();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // ????????? ????????? ???????????? ??????
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

        // ?????? ??????
        displayRotationHelper.updateSessionIfNeeded(sharedSession);

        // ar ????????? ??????
        try {
            onDrawFrameARCore();
        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }

        //switch??? ?????? plane ????????? ?????? ??????
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

        //?????? plane ?????? ??? ????????? ????????????
        if(!firstPlanecheck && hasTrackingPlane()){
            ImageView imageView = findViewById(R.id.gif_plane_tutirial);
            imageView.setVisibility(View.INVISIBLE);
            TextView textView = findViewById(R.id.gif_plane_tutirial_text);
            textView.setVisibility(View.INVISIBLE);
            firstPlanecheck = true;
        }

        // ????????? ????????? ????????? ???????????? ??????
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

        // capture ?????? ??????
        try{
            if(saveCheck) {
                saveCheck = false;

                // ???????????? ?????? ????????? ?????? ?????? ??????
                // ?????? ?????? ?????????
                /*
                // plane ??????
                boolean flag = false;
                try {
                    if (isPlaneshow) {
                        Log.d("plane check", ":transparent");
                        planeRenderer.createOnGlThread(this, "models/transparent.png");
                        flag = true;
                    }
                }catch (IOException e){
                    Log.e(TAG, "ar rendering : io error", e);
                }
                 */

                // ?????? ??????
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

                /*
                // plane ??????
                if (flag){
                    try {
                        Log.d("plane check", ":trigrid");
                        planeRenderer.createOnGlThread(this, "models/trigrid.png");
                    }catch (IOException e){
                        Log.e(TAG, "ar rendering : io error", e);
                    }
                }
                 */
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

        // plane ????????? ?????? tracking
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

        // Visualize anchors created by touch.
        for (ColoredAnchor coloredAnchor : anchors) {
            if (coloredAnchor.anchor.getTrackingState() != TrackingState.TRACKING) {
                continue;
            }
            // Get the current pose of an Anchor in world space. The Anchor pose is updated
            // during calls to sharedSession.update() as ARCore refines its estimate of the world.
            coloredAnchor.anchor.getPose().toMatrix(anchorMatrix, 0);

            //????????? ?????? ??????
            float[] temp = new float[16];
            float[] finish = new float[16];
            Matrix.setRotateM(temp, 0, coloredAnchor.angle, 0.0f, 1.0f, 0.0f);
            Matrix.multiplyMM(finish, 0, anchorMatrix, 0, temp, 0);

            // ??????????????? ???????????? ?????? ????????? ??????
            // mtl??? ??????
            if(complexvirtualObject.containsKey(coloredAnchor.name)){
                complexvirtualObject.get(coloredAnchor.name).updateModelMatrix(finish, coloredAnchor.scale);
                complexvirtualObject.get(coloredAnchor.name).draw(viewmtx, projmtx, colorCorrectionRgba);
            }
            // obj??? ??????
            else {
                virtualObject.get(coloredAnchor.name).updateModelMatrix(finish, coloredAnchor.scale);
                virtualObject.get(coloredAnchor.name).draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
            }

            // ????????? ????????? ?????? ?????? ?????????
            if(coloredAnchor.selected) {
                virtualObjectSelect.updateModelMatrix(finish, coloredAnchor.scale);
                virtualObjectSelect.draw(viewmtx, projmtx, colorCorrectionRgba, DEFAULT_COLOR);
            }
        }
    }

    private boolean isARCoreSupportedAndUpToDate(){
        // arcore ?????? ?????? ??? ?????? ??????
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
                AlertDialog.Builder alert  = new AlertDialog.Builder(this, R.style.Dialog);
                alert.setTitle("ARCore ?????? ??????");
                alert.setMessage("????????? ????????? ????????????????????? ?????? ?????? ?????? ??????????????????.");
                alert.setPositiveButton("??????", new DialogInterface.OnClickListener() {
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
        // plane??? ?????? ???????????? ??????
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

                    //?????? ????????????
                    //?????? raycast ?????? ??????????????? ??????
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
                        if (selected != null) {
                            if (selected.equals("tabletest")) {
                                anchors.add(new ColoredAnchor(hit.createAnchor(), objColor, selected, 0.00001f));
                            } else if (selected.equals("andy")) {
                                objColor = new float[]{139.0f, 195.0f, 74.0f, 255.0f};
                                anchors.add(new ColoredAnchor(hit.createAnchor(), objColor, selected, 1.0f));
                            } else {
                                anchors.add(new ColoredAnchor(hit.createAnchor(), objColor, selected, 1.0f));
                            }

                            // ???????????? ?????? ????????? ??????
                            selected = null;
                            selectPreview.setVisibility(View.INVISIBLE);
                        }
                        break;
                    }
                }
            }
        }
        else if(checkStatus == 1){
            //??????????????? ?????? ?????? ??????
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
            //?????? ?????? ???????????? ??????
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
                    //????????? ????????? ?????????
                    tapHelper.setCheckStatus(3);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e){
        //???????????? ?????? ????????? ??????
        scaleGestureDetector.onTouchEvent(e);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
        @Override
        public boolean onScale(ScaleGestureDetector detector){
            //??????????????? ??????
            //?????? ??????????????? ????????? ???????????? ???????????????
            if(selectedAnchor > -1){
                anchors.get(selectedAnchor).scale *= detector.getScaleFactor();

                anchors.get(selectedAnchor).scale = Math.max(0.5f, Math.min(anchors.get(selectedAnchor).scale, 5.0f));
            }
            return true;
        }
    }

    public void saveInternalModels(){
        // ??? ?????? ?????? ????????? ??????
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
        // ??? ?????? ?????? ????????? ??????
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

    public void setEnvironment(){
        spref = PreferenceManager.getDefaultSharedPreferences(this);
        isBattery = spref.getBoolean("battery", true);
        isTimer = spref.getBoolean("timer", true);
        whatFont = spref.getString("font", "??????");
        isPlane = spref.getBoolean("plane", true);
        isPlaneshow = isPlane;
    }

    public void setFurniturePreview(){
        //???????????? ????????? ??????
        selectPreview = findViewById(R.id.select_fur_preview);
        textViewPreview = findViewById(R.id.textView_preview);
        imageViewPreview = findViewById(R.id.imageView_preview);
    }

    public void setSelectedFurniture(String text, Bitmap image){
        selected = text;
        textViewPreview.setText(text + " ??????");
        imageViewPreview.setImageBitmap(image);
        selectPreview.setVisibility(View.VISIBLE);
    }

    public void setViewPagerFurniturelist(){
        viewPagerFurniturelist = findViewById(R.id.furniture_list_viewpager);
        viewPagerFurniturelist.setOffscreenPageLimit(1);
        pagerAdapterFurniturelist = new PagerAdapterFurniturelist(this);
        viewPagerFurniturelist.setAdapter(pagerAdapterFurniturelist);
        viewPagerFurniturelist.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                if (position == 0){
                    isUp = false;
                    viewPagerSubmenu.setUserInputEnabled(false);
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
                }
                else{
                    isUp = true;
                    viewPagerSubmenu.setUserInputEnabled(true);

                    //?????? ?????? ????????? ???????????? ??????
                    if (selectedAnchor >= 0) {
                        anchors.get(selectedAnchor).selected = false;
                        selectedAnchor = -1;
                    }
                    if (selected != null){
                        selected = null;
                        selectPreview.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
        viewPagerFurniturelist.bringToFront();
        viewPagerFurniturelist.setPageTransformer(new DepthPageTransformer());
    }

    public void setViewPagerSubmenu(){
        viewPagerSubmenu = findViewById(R.id.submenu_viewpager);
        viewPagerSubmenu.setOffscreenPageLimit(1);
        pagerAdapterSubmenu = new PagerAdapterSubMenu(this);
        viewPagerSubmenu.setAdapter(pagerAdapterSubmenu);
        viewPagerSubmenu.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                if (position == 0){
                    isSubmenu = false;
                }
                else{
                    isSubmenu = true;
                    // ?????? ??????
                    if (isDel){
                        furDeleteClick(null);
                    }
                    if (isUpload){
                        uploadClick(null);
                    }
                }
            }
        });
        viewPagerSubmenu.setUserInputEnabled(false);
    }

    public void setBatteryTimer(){
        // ?????? ?????????
        TextView textView = findViewById(R.id.textView_battery);
        ImageView imageView = findViewById(R.id.imageView_battery);

        // ?????? ??????
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

        // ????????? ????????? ???????????? ?????? ???????????? ?????? ??????
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

                // ?????? ??????
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

        // ????????? ??????
        TimerTask battery = new TimerTask() {
            @Override
            public void run() {
                Message msg = handler.obtainMessage();
                handler.sendMessage(msg);
            }
        };
        Timer timer = new Timer();
        timer.schedule(battery, 0, 3000);
    }

    public void setTimeTimer(){
        TextView textView = findViewById(R.id.text_time);

        SimpleDateFormat dateFormat = new SimpleDateFormat("aa hh:mm");

        // ????????? ????????? ???????????? ?????? ???????????? ?????? ??????
        @SuppressLint("HandlerLeak")
        Handler handler = new Handler(){
            public void handleMessage(Message msg){
                String time = dateFormat.format(new Date(System.currentTimeMillis()));
                textView.setText(time);
            }
        };

        // ????????? ??????
        TimerTask battery = new TimerTask() {
            @Override
            public void run() {
                Message msg = handler.obtainMessage();
                handler.sendMessage(msg);
            }
        };
        Timer timer = new Timer();
        timer.schedule(battery, 0, 3000);
    }

    public void checkFirstRun(){
        boolean first = spref.getBoolean("isFirstRun", true);
        if(first){
            spref.edit().putBoolean("isFirstRun", false).apply();

            AlertDialog.Builder alert = new AlertDialog.Builder(this, R.style.DialogSmall);
            alert.setTitle("???????????????!");
            alert.setIcon(R.drawable.main_character);
            alert.setMessage("?????? ?????? ?????????????????????.\n???????????? ?????? ???????????? ?????????????");
            alert.setPositiveButton("????????????", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    Intent intent = new Intent(getApplicationContext(), HelpActivity.class);
                    startActivityForResult(intent, 2);
                    overridePendingTransition(R.anim.activity_left_enter, R.anim.activity_left_exit);
                }
            });
            alert.setNeutralButton("????????????", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            alert.setCancelable(false);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    alert.show();
                }
            }, 2000);

            /*
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    LayoutInflater inflater = getLayoutInflater();
                    View toastDesign = inflater.inflate(R.layout.toast_secondline, null);
                    TextView toastText = toastDesign.findViewById(R.id.toast_text);
                    toastText.setText("?????? ?????? ?????????????????????.\n???????????? ?????? ???????????? ?????????????");
                    ImageView toastImage = toastDesign.findViewById(R.id.toast_image);
                    toastImage.setImageResource(R.drawable.main_character);
                    Toast toast = new Toast(getApplicationContext());
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.setView(toastDesign);
                    toast.show();

                    Intent intent = new Intent(getApplicationContext(), HelpActivity.class);
                    startActivityForResult(intent, 2);
                    overridePendingTransition(R.anim.activity_left_enter, R.anim.activity_left_exit);
                }
            }, 2000);
            */
        }
    }

    public class PagerAdapterFurniturelist extends FragmentStateAdapter {

        List<Fragment> fragments = new ArrayList<>();

        public PagerAdapterFurniturelist(@NonNull FragmentActivity fa) {
            super(fa);
            fragments.add(new FurniturelistButton());
            fragments.add(new Furniturelist());
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragments.get(position);
        }

        @Override
        public int getItemCount() {
            return fragments.size();
        }
    }

    private void animatePagerTransitionFur(final boolean forward) {
        if (furnitureAnimator != null){
            furnitureAnimator.cancel();
        }
        furnitureAnimator = getPagerTransitionAnimationFur(forward);
        if (viewPagerFurniturelist.beginFakeDrag()) {
            furnitureAnimator.start();
        }
    }

    private Animator getPagerTransitionAnimationFur(final boolean forward) {
        ValueAnimator animator = ValueAnimator.ofInt(0, viewPagerFurniturelist.getHeight() - 1);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                viewPagerFurniturelist.beginFakeDrag();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                viewPagerFurniturelist.endFakeDrag();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                viewPagerFurniturelist.endFakeDrag();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        animator.setInterpolator(new AccelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int dragPosition = (Integer) animation.getAnimatedValue();
                int dragOffset = dragPosition - oldDragPositionFur;
                oldDragPositionFur = dragPosition;
                viewPagerFurniturelist.fakeDragBy(dragOffset * (forward ? -1 : 1));
            }
        });

        animator.setDuration(500);

        return animator;
    }

    public class DepthPageTransformer implements ViewPager2.PageTransformer {

        public void transformPage(View view, float position) {
            int pageHeight = view.getHeight();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0f);

            } else if (position <= 0) { // [-1,0]
                // Counteract the default slide transition
                view.setTranslationY(pageHeight * -position);
                view.setTranslationZ(-1f);

            } else if (position <= 1) { // (0,1]
                view.setAlpha(1f);
                view.setTranslationX(0f);
                view.setTranslationZ(0f);
                view.setScaleX(1f);
                view.setScaleY(1f);

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0f);
            }
        }
    }

    public class PagerAdapterSubMenu extends FragmentStateAdapter {

        List<Fragment> fragments = new ArrayList<>();

        public PagerAdapterSubMenu(@NonNull FragmentActivity fa) {
            super(fa);
            fragments.add(new Empty());
            fragments.add(new Submenu());
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragments.get(position);
        }

        @Override
        public int getItemCount() {
            return fragments.size();
        }
    }

    private void animatePagerTransitionSub(final boolean forward) {
        if (submenuAnimator != null){
            submenuAnimator.cancel();
        }
        submenuAnimator = getPagerTransitionAnimationSub(forward);
        if (viewPagerSubmenu.beginFakeDrag()) {    // checking that started drag correctly
            submenuAnimator.start();
        }
    }

    private Animator getPagerTransitionAnimationSub(final boolean forward) {
        ValueAnimator animator = ValueAnimator.ofInt(0, viewPagerSubmenu.getWidth() - 1);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                viewPagerSubmenu.endFakeDrag();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                viewPagerSubmenu.endFakeDrag();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                viewPagerSubmenu.endFakeDrag();
                oldDragPositionSub = 0;
                viewPagerSubmenu.beginFakeDrag();
            }
        });

        animator.setInterpolator(new AccelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int dragPosition = (Integer) animation.getAnimatedValue();
                int dragOffset = dragPosition - oldDragPositionSub;
                oldDragPositionSub = dragPosition;
                viewPagerSubmenu.fakeDragBy(dragOffset * (forward ? -1 : 1));
            }
        });

        animator.setDuration(500);

        return animator;
    }

    public void uploadModel(String name){
        if (name.equals("already exist")){
            AlertDialog.Builder alert = new AlertDialog.Builder(this, R.style.Dialog);
            alert.setTitle("?????? ????????? ??????");
            alert.setMessage("?????? ????????? ???????????? ???????????????.");
            alert.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            alert.show();
        }
        else {
            AlertDialog.Builder alert = new AlertDialog.Builder(this, R.style.Dialog);
            alert.setTitle("?????? ?????????");
            alert.setMessage(name + " ?????? ???????????? ??????????????? ???????????????.");
            alert.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            alert.show();
        }
    }

    public void downloadClick(View view){
        // ???????????? ??????????????? ??????
        furnitureMenuClick(null);
        Intent intent = new Intent(this, DownloadActivity.class);

        // ?????????????????? ??????????????? ?????? ??????
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivityForResult(intent, 0);
                overridePendingTransition(R.anim.activity_left_enter, R.anim.activity_left_exit);
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

            LayoutInflater inflater = getLayoutInflater();
            View toastDesign = inflater.inflate(R.layout.toast, null);
            TextView toastText = toastDesign.findViewById(R.id.toast_text);
            toastText.setText("????????? ????????? ???????????????.");
            ImageView toastImage = toastDesign.findViewById(R.id.toast_image);
            toastImage.setVisibility(View.GONE);
            Toast toast = new Toast(getApplicationContext());
            toast.setGravity(Gravity.BOTTOM, 0, 150);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(toastDesign);
            toast.show();
        }

        // ??? ?????? ??? ?????? ??????
        isMode = true;
        Furniturelist.furniturelist.recyclerViewState = Furniturelist.furniturelist.recyclerView.getLayoutManager().onSaveInstanceState();
        Furniturelist.CustomerAdapter adapter = (Furniturelist.CustomerAdapter) Furniturelist.furniturelist.recyclerView.getAdapter();
        adapter.notifyDataSetChanged();
        Furniturelist.furniturelist.recyclerView.setAdapter(adapter);
        Furniturelist.furniturelist.recyclerView.getLayoutManager().onRestoreInstanceState(Furniturelist.furniturelist.recyclerViewState);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isMode = false;
            }
        }, 100);
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

            LayoutInflater inflater = getLayoutInflater();
            View toastDesign = inflater.inflate(R.layout.toast, null);
            TextView toastText = toastDesign.findViewById(R.id.toast_text);
            toastText.setText("?????? ????????? ???????????????.");
            ImageView toastImage = toastDesign.findViewById(R.id.toast_image);
            toastImage.setVisibility(View.GONE);
            Toast toast = new Toast(getApplicationContext());
            toast.setGravity(Gravity.BOTTOM, 0, 150);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(toastDesign);
            toast.show();
        }

        // ??? ?????? ??? ?????? ??????
        isMode = true;
        Furniturelist.furniturelist.recyclerViewState = Furniturelist.furniturelist.recyclerView.getLayoutManager().onSaveInstanceState();
        Furniturelist.CustomerAdapter adapter = (Furniturelist.CustomerAdapter) Furniturelist.furniturelist.recyclerView.getAdapter();
        adapter.notifyDataSetChanged();
        Furniturelist.furniturelist.recyclerView.setAdapter(adapter);
        Furniturelist.furniturelist.recyclerView.getLayoutManager().onRestoreInstanceState(Furniturelist.furniturelist.recyclerViewState);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isMode = false;
            }
        }, 100);
    }

    public void subMenuClick(View view){
        // ?????? ?????? ??????
        if (isSubmenu){
            isSubmenu = false;
            animatePagerTransitionSub(false);
        }
        else {
            isSubmenu = true;
            animatePagerTransitionSub(true);
            // ?????? ??????
            if (isDel){
                furDeleteClick(null);
            }
            if (isUpload){
                uploadClick(null);
            }
        }
    }

    public void furnitureMenuClick(View view){
        if(!isUp){
            //??????
            animatePagerTransitionFur(true);
            viewPagerSubmenu.setUserInputEnabled(true);
            isUp = true;

            //?????? ?????? ????????? ???????????? ??????
            if (selectedAnchor >= 0) {
                anchors.get(selectedAnchor).selected = false;
                selectedAnchor = -1;
            }
            if (selected != null){
                selected = null;
                selectPreview.setVisibility(View.INVISIBLE);
            }
        }
        else{
            //??????
            animatePagerTransitionFur(false);
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
            viewPagerSubmenu.setUserInputEnabled(false);
            isUp = false;
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

    public void buttonCreateClick(View view){
        // create ??????????????? ??????
        furnitureMenuClick(null);
        Intent intent = new Intent(this, CreateActivity.class);

        // ?????????????????? ??????????????? ?????? ??????
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivityForResult(intent, 0);
                overridePendingTransition(R.anim.activity_left_enter, R.anim.activity_left_exit);
            }
        }, 500);
    }

    public void settingClick(View view){
        // ?????? ??????????????? ??????
        furnitureMenuClick(null);
        Intent intent = new Intent(this, SettingActivity.class);

        // ?????????????????? ??????????????? ?????? ??????
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivityForResult(intent, 1);
                overridePendingTransition(R.anim.activity_left_enter, R.anim.activity_left_exit);
            }
        }, 500);
    }

    public void resetClick(View view){
        //?????? ?????? ???????????????
        AlertDialog.Builder alert = new AlertDialog.Builder(this, R.style.Dialog);
        alert.setTitle("Reset");
        alert.setMessage("????????? ?????? ????????? ????????????, ????????? ????????? ????????? ??????????");
        alert.setPositiveButton("???????????? ?????????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // ?????? ??????
                anchors.clear();
                selectedAnchor = -1;

                // ????????????
                if (selected != null){
                    selected = null;
                    selectPreview.setVisibility(View.INVISIBLE);
                }

                // plane ???????????? ?????? ?????? ?????? ?????? ?????? ??????
                surfaceView.onPause();
                waitUntilCameraCaptureSessionIsActive();
                mSession.close();
                displayRotationHelper.onPause();
                closeCamera();
                stopBackgroundThread();
                shouldUpdateSurfaceTexture.set(false);
                pauseARCore();

                // ?????? ?????????
                if (sharedSession != null) {
                    sharedSession.close();
                    sharedSession = null;
                }

                // ?????? ?????? ?????? ?????????
                waitUntilCameraCaptureSessionIsActive();
                startBackgroundThread();
                surfaceView.onResume();
                if (surfaceCreated) {
                    openCamera();
                }
                displayRotationHelper.onResume();

                dialog.cancel();

                LayoutInflater inflater = getLayoutInflater();
                View toastDesign = inflater.inflate(R.layout.toast, null);
                TextView toastText = toastDesign.findViewById(R.id.toast_text);
                toastText.setText("???????????? ??????????????? ???????????????.");
                ImageView toastImage = toastDesign.findViewById(R.id.toast_image);
                toastImage.setVisibility(View.GONE);
                Toast toast = new Toast(getApplicationContext());
                toast.setGravity(Gravity.BOTTOM, 0, 150);
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(toastDesign);
                toast.show();
            }
        });
        alert.setNegativeButton("????????? ??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // ?????? ??????
                anchors.clear();
                selectedAnchor = -1;

                // ????????????
                if (selected != null){
                    selected = null;
                    selectPreview.setVisibility(View.INVISIBLE);
                }

                dialog.cancel();

                LayoutInflater inflater = getLayoutInflater();
                View toastDesign = inflater.inflate(R.layout.toast, null);
                TextView toastText = toastDesign.findViewById(R.id.toast_text);
                toastText.setText("????????? ????????? ??????????????? ???????????????.");
                ImageView toastImage = toastDesign.findViewById(R.id.toast_image);
                toastImage.setVisibility(View.GONE);
                Toast toast = new Toast(getApplicationContext());
                toast.setGravity(Gravity.BOTTOM, 0, 150);
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(toastDesign);
                toast.show();
            }
        });
        alert.setNeutralButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alert.show();
    }

    public void helpClick(View view){
        // ????????? ??????????????? ??????
        furnitureMenuClick(null);
        Intent intent = new Intent(this, HelpActivity.class);

        // ?????????????????? ??????????????? ?????? ??????
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivityForResult(intent, 2);
                overridePendingTransition(R.anim.activity_left_enter, R.anim.activity_left_exit);
            }
        }, 500);
    }

    public void captureClick(View view) throws InterruptedException {
        // ?????? ?????? ??????

        // ?????? ??????
        MediaActionSound sound = new MediaActionSound();
        sound.play(MediaActionSound.SHUTTER_CLICK);

        Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            @Override
            public void run() {
                saveCheck = true;
                saveSucess = false;
            }
        }, 200);

        // ????????? ????????? ????????? ??? ?????? ??????
        Handler handler2 = new Handler();
        handler2.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(saveSucess){
                    LayoutInflater inflater = getLayoutInflater();
                    View toastDesign = inflater.inflate(R.layout.toast, null);
                    TextView toastText = toastDesign.findViewById(R.id.toast_text);
                    toastText.setText("????????? ???????????????.");
                    ImageView toastImage = toastDesign.findViewById(R.id.toast_image);
                    toastImage.setVisibility(View.GONE);
                    Toast toast = new Toast(getApplicationContext());
                    toast.setGravity(Gravity.BOTTOM, 0, 150);
                    toast.setDuration(Toast.LENGTH_SHORT);
                    toast.setView(toastDesign);
                    toast.show();
                }
                else{
                    LayoutInflater inflater = getLayoutInflater();
                    View toastDesign = inflater.inflate(R.layout.toast, null);
                    TextView toastText = toastDesign.findViewById(R.id.toast_text);
                    toastText.setText("????????? ???????????????.");
                    ImageView toastImage = toastDesign.findViewById(R.id.toast_image);
                    toastImage.setVisibility(View.GONE);
                    Toast toast = new Toast(getApplicationContext());
                    toast.setGravity(Gravity.BOTTOM, 0, 150);
                    toast.setDuration(Toast.LENGTH_SHORT);
                    toast.setView(toastDesign);
                    toast.show();
                }
            }
        }, 500);
    }

    public boolean screenShot(Bitmap bm){
        if(bm != null) {
            Bitmap overlay = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), bm.getConfig());
            Canvas canvas = new Canvas(overlay);
            canvas.drawBitmap(bm, 0, 0, null);

            File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/FSCapture");
            //?????? ??????
            if (!path.exists()) {
                path.mkdirs();
            }
            try {
                //???????????? ?????? ??? ?????? ??????
                SimpleDateFormat day = new SimpleDateFormat("yyMMdd_HHmmss");
                Date date = new Date();
                FileOutputStream os = new FileOutputStream(path + "/capture_" + day.format(date) + ".jpg");
                overlay.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.close();
            } catch (IOException e) {
                Log.e("Save_Image", e.getMessage(), e);
            }
            //????????? ??????
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
        // ?????? ??????
        String[] permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        //????????? ?????? ?????? ?????? ??? ??????
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
                    //?????? ?????????
                    waitUntilCameraCaptureSessionIsActive();
                    startBackgroundThread();
                    surfaceView.onResume();
                    if (surfaceCreated) {
                        openCamera();
                    }
                }else {
                    //?????? ?????????
                    AlertDialog.Builder alert  = new AlertDialog.Builder(this, R.style.Dialog);
                    alert.setTitle("APP permissions");
                    alert.setMessage("?????? ?????? ?????????????????? ????????? ???????????????.");
                    alert.setPositiveButton("??????", new DialogInterface.OnClickListener() {
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

        //???????????? ????????? ????????? ?????????
        if(requestCode == 0){
            if(resultCode == RESULT_OK) {
                Furniturelist.furniturelist.setFurnitureList();
                String newModel = data.getExtras().getString("modelname");
                newModelName = newModel;
                isNewModel = true;
            }
        }
        else if (requestCode == 1){
            if(resultCode == RESULT_OK) {
                // ?????? ????????? ????????? ???????????? ??????
                String [] strings = {"andy", "desk", "chair", "lamp"};

                // ?????? ?????? ??????
                Furniturelist.furniturelist.setFurnitureList();

                // ?????? ????????? ????????? ?????? ??? ?????? ?????? ?????? ??? ??????
                for (int i = 0; i < anchors.size(); i++){
                    if (!Arrays.asList(strings).contains(anchors.get(i).name)){
                        anchors.remove(i);
                        i--;
                    }
                }
                selectedAnchor = -1;

                // ????????? ????????? ?????? ????????? ?????? ?????? ??????
                if (selected != null && !Arrays.asList(strings).contains(selected)){
                    selected = null;
                    selectPreview.setVisibility(View.INVISIBLE);
                }

                // ??? ??? ?????? ????????? ???????????? ??????
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
                Boolean battery = pref.getBoolean("battery", true);
                Boolean timer = pref.getBoolean("timer", true);
                String font = pref.getString("font", "??????");
                Boolean plane = pref.getBoolean("plane", true);

                // ??? ????????? ?????? ?????? ??????
                if (isBattery != battery) {
                    isBattery = battery;
                }
                if (isTimer != timer){
                    isTimer = timer;
                }
                if (!whatFont.equals(font)){
                    whatFont = font;

                    TextView tutorialText = findViewById(R.id.gif_plane_tutirial_text);
                    TextView battaryText = findViewById(R.id.textView_battery);
                    TextView timeText = findViewById(R.id.text_time);
                    switch (whatFont){
                        case "??????R":
                            setTheme(R.style.AppTheme_NanumR);
                            textViewPreview.setTypeface(getResources().getFont(R.font.nanumroundr));
                            tutorialText.setTypeface(getResources().getFont(R.font.nanumroundr));
                            battaryText.setTypeface(getResources().getFont(R.font.nanumroundr));
                            timeText.setTypeface(getResources().getFont(R.font.nanumroundr));
                            break;
                        case "??????B":
                            setTheme(R.style.AppTheme_NanumB);
                            textViewPreview.setTypeface(getResources().getFont(R.font.nanumroundb));
                            tutorialText.setTypeface(getResources().getFont(R.font.nanumroundb));
                            battaryText.setTypeface(getResources().getFont(R.font.nanumroundb));
                            timeText.setTypeface(getResources().getFont(R.font.nanumroundb));
                            break;
                        case "??????":
                            setTheme(R.style.AppTheme_Cafe);
                            textViewPreview.setTypeface(getResources().getFont(R.font.cafe24surround));
                            tutorialText.setTypeface(getResources().getFont(R.font.cafe24surround));
                            battaryText.setTypeface(getResources().getFont(R.font.cafe24surround));
                            timeText.setTypeface(getResources().getFont(R.font.cafe24surround));
                            break;
                        case "????????????":
                            setTheme(R.style.AppTheme_Sc);
                            textViewPreview.setTypeface(getResources().getFont(R.font.scdream3));
                            tutorialText.setTypeface(getResources().getFont(R.font.scdream3));
                            battaryText.setTypeface(getResources().getFont(R.font.scdream3));
                            timeText.setTypeface(getResources().getFont(R.font.scdream3));
                            break;
                        default:
                            setTheme(R.style.AppTheme);
                            textViewPreview.setTypeface(Typeface.DEFAULT);
                            tutorialText.setTypeface(Typeface.DEFAULT);
                            battaryText.setTypeface(Typeface.DEFAULT);
                            timeText.setTypeface(Typeface.DEFAULT);
                            break;
                    }

                    //?????????
                    setViewPagerFurniturelist();
                    setViewPagerSubmenu();
                }
                if (isPlane != plane){
                    isPlane = plane;
                    isPlaneChanged = true;
                    isPlaneshow = isPlane;
                }
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
            MainActivity.this.cameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onClosed(@NonNull CameraDevice cameraDevice) {
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
            cameraDevice.close();
            MainActivity.this.cameraDevice = null;
            // Fatal error. Quit application.
            finish();
        }
    };

    CameraCaptureSession.StateCallback cameraSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            captureSession = session;
            setRepeatingCaptureRequest();
        }
        @Override
        public void onSurfacePrepared(@NonNull CameraCaptureSession session, @NonNull Surface surface) {
        }
        @Override
        public void onReady(@NonNull CameraCaptureSession session) {
        }

        @Override
        public void onActive(@NonNull CameraCaptureSession session) {
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
            super.onClosed(session);
            stopBackgroundThread();
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        outState.putBoolean("first", false);
        outPersistentState.putBoolean("first", false);
        super.onSaveInstanceState(outState, outPersistentState);
        Log.d("first check", isFirst + " restore");
    }
}