package dk.aakb.itk.gg_affaldsvarme;

import android.app.Activity;
import android.widget.Toast;

public abstract class BaseActivity extends Activity {

    /**
     * Send a toast
     *
     * @param message Message to display
     */
    private void proposeAToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    protected void proposeAToast(int resId) {
        proposeAToast(getString(resId));
    }

    protected void proposeAToast(int resId, Object... formatArgs) {
        proposeAToast(getString(resId, formatArgs));
    }
}
