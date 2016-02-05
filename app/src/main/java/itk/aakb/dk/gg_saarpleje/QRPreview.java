package itk.aakb.dk.gg_saarpleje;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * A basic Camera preview class
 */
public class QRPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private static final String TAG = "QR Preview";
    private Camera.PreviewCallback previewCallback;
    private int previewWidth = 640;
    private int previewHeight = 360;

    public QRPreview(Context context, Camera camera, Camera.PreviewCallback callback) {
        super(context);
        mCamera = camera;
        previewCallback = callback;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surface created");

        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            Camera.Parameters params = mCamera.getParameters();
            params.setPreviewFpsRange(30000, 30000);
            params.setPreviewSize(previewWidth, previewHeight);
            mCamera.setParameters(params);

            mCamera.setPreviewCallback(previewCallback);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void release() {
        getHolder().getSurface().release();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.i(TAG, "surface changed");
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        Camera.Parameters params = mCamera.getParameters();
        params.setPreviewFpsRange(30000, 30000);
        params.setPreviewSize(previewWidth, previewHeight);
        mCamera.setParameters(params);

        // start preview with new settings
        try {
            mCamera.setPreviewCallback(previewCallback);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
}