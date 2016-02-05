package itk.aakb.dk.gg_saarpleje;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class QRActivity extends Activity {
    private static final String TAG = "QRActivity";
    private static final int   SCANS_PER_SEC = 6;

    private Camera camera;
    private QRPreview qrPreview;
    private int framesSinceLastScan = 0;
    private MultiFormatReader multiFormatReader = new MultiFormatReader();

    /**
     * PreviewCallback.
     */
    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        /**
         * Preview frame
         *
         * @param data preview data
         * @param camera the camera
         */
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Size size = camera.getParameters().getPreviewSize();

            // Only scan SCANS_PER_SEC
            if( ++framesSinceLastScan % (30 / SCANS_PER_SEC) == 0 ) {
                Log.i(TAG, "trigger");
                scan(data, size.width, size.height);
                framesSinceLastScan = 0;
            }
        }

        /**
         * Scan the data for a QR code.
         *
         * @param data
         * @param width
         * @param height
         */
        private void scan(byte[] data, int width, int height) {
            Log.d(TAG, "scan");
            PlanarYUVLuminanceSource luminanceSource = new PlanarYUVLuminanceSource(data,
                    width, height, 0, 0, width, height, false);
            // new ScanTask().execute(luminanceSource); // uncomment to use ScanTask
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(luminanceSource));
            Result result = null;

            Map<DecodeHintType, Object> tmpHintsMap = new EnumMap<DecodeHintType, Object>(
                    DecodeHintType.class);
            tmpHintsMap.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            List<BarcodeFormat> formats = new ArrayList<>();
            formats.add(BarcodeFormat.QR_CODE);
            tmpHintsMap.put(DecodeHintType.POSSIBLE_FORMATS, formats);

            try {
                result = multiFormatReader.decode(bitmap, tmpHintsMap);
            } catch (ReaderException re) {
                Log.e(TAG, re.toString());
            } finally {
                multiFormatReader.reset();
            }
            if (result != null) {
                Log.i(TAG, "Result: " + result.getText());

                // Add path to file as result
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result", result.getText());
                setResult(RESULT_OK, returnIntent);

                finish();
            }
        }

    };

    /**
     * On create.
     *
     * @param savedInstanceState Saved Instance State
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Launching activity");

        setContentView(R.layout.activity_qr);

        if (!checkCameraHardware(this)) {
            Log.i(TAG, "no camera");
            finish();
        }

        // Create an instance of Camera
        camera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        qrPreview = new QRPreview(this, camera, previewCallback);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(qrPreview);
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c;

        Log.i(TAG, "getting camera instance...");
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            Log.e(TAG, "could not getCameraInstance");
            throw e;
        }

        return c; // returns null if camera is unavailable
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /**
     * On pause.
     */
    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    /**
     * On destroy.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    /**
     * Release the camera resources.
     */
    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            qrPreview.release();
            camera.release();        // release the camera for other applications
            camera = null;
        }
    }
}
