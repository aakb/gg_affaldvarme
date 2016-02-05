package itk.aakb.dk.gg_saarpleje;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MemoActivity extends Activity {
    private static final String TAG = "MemoActivity";

    private MediaRecorder mRecorder;
    private TextView durationText;

    private Timer timer;
    private int timerExecutions = 0;
    private boolean recording = false;

    private String outputPath;
    private String filePrefix;

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
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Launching memo activity");

        // Get file prefix
        Intent intent = getIntent();
        filePrefix = intent.getStringExtra("FILE_PREFIX");

        setContentView(R.layout.activity_record_memo);

        durationText = (TextView) findViewById(R.id.text_memo_duration);
        outputPath = getOutputVideoFile().toString();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mSensorManager.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mSensorEventListener, mMagnetometer, SensorManager.SENSOR_DELAY_UI);

        startRecording();
    }

    /**
     * On pause.
     */
    @Override
    protected void onPause() {
        super.onPause();

        timer.cancel();
        stopRecording();

        mSensorManager.unregisterListener(mSensorEventListener);
    }

    /**
     * On destroy.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        timer.cancel();
        stopRecording();

        mSensorManager.unregisterListener(mSensorEventListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLastAccelerometerSet = false;
        mLastMagnetometerSet = false;
    }

    private void startRecording() {
        durationText.setText("0 sec");

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(outputPath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        (new Timer()).schedule(new TimerTask() {
            @Override
            public void run() {

                mRecorder.start();

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

    private void stopRecording() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
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
                            stopRecording();

                            // Add path to file as result
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("path", outputPath);
                            setResult(RESULT_OK, returnIntent);

                            recording = false;

                            // Finish activity
                            finish();
                        } catch (Exception e) {
                            Log.d(TAG, "Exception stopping recording: " + e.getMessage());
                            stopRecording();
                            finish();
                        }
                    }
                }
            }
        }
    };

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
                filePrefix + "_audio_" + timeStamp + ".mp3");
        return mediaFile;
    }
}