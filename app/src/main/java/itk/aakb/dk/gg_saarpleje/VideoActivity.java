package itk.aakb.dk.gg_saarpleje;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class VideoActivity extends Activity {
    private static final String TAG = "VideoActivity";

    private Camera camera;
    private CameraPreview cameraPreview;
    private MediaRecorder mediaRecorder;
    private TextView durationText;

    private Timer timer;
    private int timerExecutions = 0;
    private String filePrefix;
    private boolean recording = false;
    private String outputPath;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];

    /**
     * On create.
     *
     * @param savedInstanceState save instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Launching video activity");

        // Get file prefix
        Intent intent = getIntent();
        filePrefix = intent.getStringExtra("FILE_PREFIX");

        setContentView(R.layout.activity_camera_video);

        durationText = (TextView) findViewById(R.id.text_camera_duration);

        if (!checkCameraHardware(this)) {
            Log.i(TAG, "no camera");
            finish();
        }

        Log.i(TAG, "get camera instance");
        camera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        cameraPreview = new CameraPreview(this, camera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(cameraPreview);

        // Reset timer executions.
        timerExecutions = 0;

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mSensorManager.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mSensorEventListener, mMagnetometer, SensorManager.SENSOR_DELAY_UI);

        launchUnlimitedVideo();
    }

    private void launchUnlimitedVideo() {
        durationText.setText("0 sec");

        // Catch all errors, and release camera on error.
        try {
            Log.i(TAG, "start preparing video recording");

            Log.i(TAG, "Setting camera hint");
            Camera.Parameters params = camera.getParameters();
            params.setRecordingHint(true);
            camera.setParameters(params);

            Log.i(TAG, "new media recorder");
            mediaRecorder = new MediaRecorder();

            Log.i(TAG, "setting up error listener");
            mediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                public void onError(MediaRecorder mediarecorder1, int k, int i1) {
                    Log.e(TAG, String.format("Media Recorder error: k=%d, i1=%d", k, i1));
                }

            });

            // Step 1: Unlock and set camera to MediaRecorder. Clear preview.
            Log.i(TAG, "unlock and set camera to MediaRecorder");
            camera.unlock();
            mediaRecorder.setCamera(camera);

            // Step 2: Set sources
            Log.i(TAG, "set sources");
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
            Log.i(TAG, "set camcorder profile");
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

            // Step 4: Set output file
            Log.i(TAG, "set output file");
            outputPath = getOutputVideoFile().toString();
            mediaRecorder.setOutputFile(outputPath);

            (new Timer()).schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        // Step 5: Set the preview output
                        Log.i(TAG, "set preview");
                        mediaRecorder.setPreviewDisplay(cameraPreview.getHolder().getSurface());

                        Log.i(TAG, "finished configuration.");

                        // Step 6: Prepare configured MediaRecorder
                        mediaRecorder.prepare();
                    }
                    catch (IOException e) {
                        Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
                        releaseMediaRecorder();
                        releaseCamera();
                        finish();
                    }

                    Log.i(TAG, "prepare successful");

                    // Camera is available and unlocked, MediaRecorder is prepared,
                    // now you can start recording
                    mediaRecorder.start();

                    recording = true;

                    Log.i(TAG, "is recording");

                    // Count down from videoLength seconds, then take picture.
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            timerExecutions++;

                            Log.i(TAG, "" + timerExecutions);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    durationText.setText(timerExecutions + " sec");
                                }
                            });
                        }
                    }, 1000, 1000);
                }
            }, 1000);
        }
        catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder (" + e.getCause() + "): " + e.getMessage());
            releaseMediaRecorder();
            releaseCamera();
            finish();
        }
        catch (Exception e) {
            Log.d(TAG, "Exception preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            releaseCamera();
            finish();
        }
    }

    private final SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Nothing to do here.
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor == mAccelerometer) {
                System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
                mLastAccelerometerSet = true;
            } else if (event.sensor == mMagnetometer) {
                System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
                mLastMagnetometerSet = true;
            }
            if (mLastAccelerometerSet && mLastMagnetometerSet) {
                SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
                SensorManager.getOrientation(mR, mOrientation);

                Log.i(TAG, "o: " + mOrientation[1]);

                if (Math.abs(mOrientation[1]) < 0.15) {
                    if (recording) {
                        Log.i(TAG, "Stop recording!");

                        timer.cancel();
                        mSensorManager.unregisterListener(mSensorEventListener);

                        try {
                            mediaRecorder.stop();  // stop the recording
                            releaseMediaRecorder(); // release the MediaRecorder object
                            releaseCamera();

                            // Add path to file as result
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("path", outputPath);
                            setResult(RESULT_OK, returnIntent);

                            recording = false;

                            // Finish activity
                            finish();
                        }
                        catch (Exception e) {
                            Log.d(TAG, "Exception stopping recording: " + e.getMessage());
                            releaseMediaRecorder();
                            releaseCamera();
                            finish();
                        }
                    }
                }
            }
        }
    };


    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;

        Log.i(TAG, "getting camera instance...");
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            Log.e(TAG, "could not getCameraInstance");
            throw e;
            // Camera is not available (in use or does not exist)
            // @TODO: Throw Toast!
        }

        return c; // returns null if camera is unavailable
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**
     * On pause.
     */
    @Override
    protected void onPause() {
        super.onPause();

        timer.cancel();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();

        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorEventListener);
        }
    }

    /**
     * On destroy.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        timer.cancel();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();

        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorEventListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLastAccelerometerSet = false;
        mLastMagnetometerSet = false;
    }

    /**
     * Release the media recorder.
     */
    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            camera.lock();           // lock camera for later use
        }
    }

    /**
     * Release the camera.
     */
    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            cameraPreview.release();
            camera.release();        // release the camera for other applications
            camera = null;
        }
    }

    /**
     * Create a File for saving a video
     */
    private File getOutputVideoFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), MainActivity.FILE_DIRECTORY);

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
                    filePrefix + "_video_" + timeStamp + ".mp4");
        return mediaFile;
    }
}