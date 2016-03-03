package dk.aakb.itk.gg_affaldsvarme;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.glass.touchpad.GestureDetector;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class CameraActivity extends BaseActivity implements GestureDetector.BaseListener {
    protected static String TAG = "CameraActivity";

    protected int state;

    protected static Camera camera;
    protected CameraPreview cameraPreview;
    protected TextView textField;
    protected String filePrefix;
    protected File outputFile;

    protected AudioManager audioManager;
    protected GestureDetector gestureDetector;

    protected int contentView = 0;

    /**
     * On create.
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Launching activity");

        // Get file prefix
        Intent intent = getIntent();
        filePrefix = intent.getStringExtra("FILE_PREFIX");

        Log.i(TAG, "Setting content view: " + contentView);
        setContentView(contentView);
        textField = (TextView)findViewById(R.id.text_camera_helptext);

        if (!checkCameraHardware(this)) {
            Log.i(TAG, "no camera");
            finish();
            return;
        }

        getCameraInstance();
        if (camera == null) {
            Log.i(TAG, "Cannot get camera instance");
            finish();
            return;
        }

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        gestureDetector = new GestureDetector(this).setBaseListener(this);

        // Create our Preview view and set it as the content of our activity.
        cameraPreview = new CameraPreview(this, camera);
        FrameLayout preview = (FrameLayout)findViewById(R.id.camera_preview);
        preview.addView(cameraPreview);
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        return gestureDetector.onMotionEvent(event);
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    protected void getCameraInstance() {
        Log.i(TAG, "getting camera instance...");
        // http://stackoverflow.com/a/19154438

        if (mThread == null) {
            mThread = new CameraHandlerThread();
        }

        synchronized (mThread) {
            mThread.openCamera();
        }
    }

    private static void oldOpenCamera() {
        try {
            camera = Camera.open();
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to open front camera");
        }
    }

    private CameraHandlerThread mThread = null;

    private static class CameraHandlerThread extends HandlerThread {
        Handler mHandler = null;

        CameraHandlerThread() {
            super("CameraHandlerThread");
            start();
            mHandler = new Handler(getLooper());
        }

        synchronized void notifyCameraOpened() {
            notify();
        }

        void openCamera() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    oldOpenCamera();
                    notifyCameraOpened();
                }
            });
            try {
                wait();
            } catch (InterruptedException e) {
                Log.w(TAG, "wait was interrupted");
            }
        }
    }

    /**
     * Check if this device has a camera
     */
    protected boolean checkCameraHardware(Context context) {
        // this device has a camera
        // no camera on this device
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /**
     * On pause.
     */
    @Override
    protected void onPause() {
        // http://stackoverflow.com/questions/18149964/best-use-of-handlerthread-over-other-similar-classes/19154438#comment33324681_19154438
        releaseCamera();

        super.onPause();
    }

    /**
     * On destroy.
     */
    @Override
    protected void onDestroy() {
        releaseCamera();

        super.onDestroy();
    }

    /**
     * Release the camera resources.
     */
    protected void releaseCamera() {
        Log.i(TAG, "releasing camera");

        if (camera != null) {
            camera.stopPreview();
            camera.release();        // release the camera for other applications
            camera = null;
        }
    }

    /**
     * Create a File for saving an image
     */
    protected File getOutputFile(String type, String extension) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), MainActivity.FILE_DIRECTORY);

        Log.i(TAG, mediaStorageDir.getAbsolutePath());

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                filePrefix + "_" + type + "_" + timeStamp + "." + extension);
        return mediaFile;
    }

    protected void returnFile(boolean instaShare) {
        try {
            // Add path to file as result
            Intent returnIntent = new Intent();
            returnIntent.putExtra("path", outputFile.getAbsolutePath());
            returnIntent.putExtra("instaShare", instaShare);
            setResult(RESULT_OK, returnIntent);

            // Finish activity
            finish();
        } catch (Throwable t) {
            Log.e(TAG, t.getClass() + ": " + t.getMessage());
        }
    }
}
