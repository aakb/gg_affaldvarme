package dk.aakb.itk.gg_affaldsvarme;

import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.glass.touchpad.Gesture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class PictureActivity extends CameraActivity {
    private byte[] pictureData;

    /**
     * On create.
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        TAG = "PictureActivity";
        contentView = R.layout.activity_camera_picture;

        super.onCreate(savedInstanceState);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textField.setText(R.string.picture_help_text);
            }
        });
    }

    public boolean onGesture(Gesture gesture) {
        if (Gesture.TAP.equals(gesture)) {
            handleSingleTap();
            return true;

        }
        return false;
    }

    private void handleSingleTap() {
        Log.i(TAG, "Single tap.");

        audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK);

        camera.takePicture(null, null, mPicture);
    }

    private void returnPicture(boolean share) {
        outputFile = getOutputFile("image", "jpg");
        if (outputFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions");
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(pictureData);
            fos.close();

            returnFile(share);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error accessing file: " + e.getMessage());
        } catch (Throwable t) {
            Log.e(TAG, t.getClass() + ": " + t.getMessage());
        } finally {
            finish();
        }
    }

    /**
     * Picture callback.
     */
    private PictureCallback mPicture = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            releaseCamera();

            pictureData = data;

            returnPicture(false);
        }
    };
}
