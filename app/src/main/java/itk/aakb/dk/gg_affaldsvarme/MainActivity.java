package itk.aakb.dk.gg_affaldsvarme;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.glass.view.WindowUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

public class MainActivity extends Activity implements BrilleappenClientListener {
    public static final String FILE_DIRECTORY = "Affaldvarme";

    private static final String TAG = "affaldvarme main";
    private static final int EXECUTE_SENDFILE = 1;
    private static final int TAKE_PICTURE_REQUEST = 101;
    private static final int RECORD_VIDEO_CAPTURE_REQUEST = 102;
    private static final int SCAN_ADRESS_REQUEST = 103;
    private static final int RECORD_MEMO_REQUEST = 104;
    private static final int NOTIFY_REQUEST = 105;
    private static final String STATE_VIDEOS = "videos";
    private static final String STATE_PICTURES = "pictures";


    private static final String STATE_MEMOS = "memos";
    private static final String STATE_EVENT = "url";
    private static final String STATE_ADRESS = "adress";

    private ArrayList<String> imagePaths = new ArrayList<>();
    private ArrayList<String> videoPaths = new ArrayList<>();
    private ArrayList<String> memoPaths = new ArrayList<>();

    String adress = null;
    private String url = null;
    BrilleappenClient client;
    private JSONObject clientResult;
    private String username;
    private String password;
    String captionTwitter;
    String captionInstagram;


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.i(TAG, "onSaveInstanceState");

        // Save the user's current game state
        savedInstanceState.putStringArrayList(STATE_VIDEOS, videoPaths);
        savedInstanceState.putStringArrayList(STATE_PICTURES, imagePaths);
        savedInstanceState.putStringArrayList(STATE_MEMOS, memoPaths);
        savedInstanceState.putString(STATE_ADRESS, adress);
        savedInstanceState.putString(STATE_EVENT, url);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * On create.
     *
     * @param savedInstanceState the bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Requests a voice menu on this activity. As for any other
        // window feature, be sure to request this before
        // setContentView() is called
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        getWindow().requestFeature(Window.FEATURE_OPTIONS_PANEL);

        Properties properties = new Properties();
        try {
            AssetManager assetManager = getApplicationContext().getAssets();
            InputStream inputStream = assetManager.open("config.properties");
            properties.load(inputStream);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            finish();
        }

        this.username = properties.getProperty("Username");
        this.password = properties.getProperty("Password");


        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            Log.i(TAG, "Restoring savedInstance");

            // Restore state members from saved instance
            imagePaths = savedInstanceState.getStringArrayList(STATE_PICTURES);
            videoPaths = savedInstanceState.getStringArrayList(STATE_VIDEOS);
            memoPaths = savedInstanceState.getStringArrayList(STATE_MEMOS);
            adress = savedInstanceState.getString(STATE_ADRESS);
            url = savedInstanceState.getString(STATE_EVENT);

        } else {
            Log.i(TAG, "Restoring state");

            // Probably initialize members with default values for a new instance
            restoreState();
        }

        if (url != null) {
            client = new BrilleappenClient(this, url, username, password);

            // Set the main activity view.
            setContentView(R.layout.activity_layout);

            updateUI();
        }
        else {
            // Set the main activity view.
            setContentView(R.layout.activity_layout_init);
        }


        Log.i(TAG, "------------");

        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), MainActivity.FILE_DIRECTORY);
        Log.i(TAG, "Listing files in: " + f.getAbsolutePath());

        getDirectoryListing(f);

        Log.i(TAG, "------------");
    }

    /**
     * On create panel menu.
     *
     * @param featureId the feature id
     * @param menu the menu to create
     *
     * @return boolean
     */
    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS ||
                featureId == Window.FEATURE_OPTIONS_PANEL) {
            if (url != null) {
                getMenuInflater().inflate(R.menu.main, menu);
            } else {
                getMenuInflater().inflate(R.menu.start, menu);
            }

            return true;
        }

        // Pass through to super to setup touch menu.
        return super.onCreatePanelMenu(featureId, menu);
    }

    /**
     * On create options menu.
     *
     * @param menu The menu to create
     * @return boolean
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (url != null) {
            getMenuInflater().inflate(R.menu.main, menu);
        }
        else {
            getMenuInflater().inflate(R.menu.start, menu);
        }

        return true;
    }

    /**
     * On menu item selected.
     * <p/>
     * Processes the voice commands from the main menu.
     *
     * @param featureId the feature id
     * @param item the selected menu item
     *
     * @return boolean
     */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS ||
                featureId == Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                case R.id.take_image_menu_item:
                    Log.i(TAG, "menu: take before image");

                    takePicture();

                    break;
                case R.id.record_video_menu_item:
                    Log.i(TAG, "menu: record video");

                    recordVideo();

                    break;
                case R.id.record_memo_menu_item:
                    Log.i(TAG, "menu: record memo");

                    recordMemo();

                    break;
                case R.id.notify_menu_item:
                    Log.i(TAG, "menu: Notify by email");

                    notifyByEmail();

                    break;
                case R.id.confirm_cancel:
                    Log.i(TAG, "menu: Confirm: cancel and exit");

                    cleanDirectory();
                    deleteState();

                    finish();

                    break;

                case R.id.scan_patient_menu_item:
                    Intent scanPatientIntent = new Intent(this, QRActivity.class);
                    startActivityForResult(scanPatientIntent, SCAN_ADRESS_REQUEST);

                    break;
                case R.id.finish_menu_item:
                    deleteState();
                    finish();

                    break;
                default:
                    return true;
            }
            return true;
        }

        // Pass through to super if not handled
        return super.onMenuItemSelected(featureId, item);
    }

    private void notifyByEmail() {
        if (clientResult != null){
            client = new BrilleappenClient(this, url, username, password);
            client.notifyFile(clientResult);
        }
    }

    /**
     * Launch the record memo intent.
     *
     */
    private void recordMemo() {
        Intent intent = new Intent(this, MemoActivity.class);
        intent.putExtra("FILE_PREFIX", adress);
        startActivityForResult(intent, RECORD_MEMO_REQUEST);
    }

    /**
     * Launch the image capture intent.
     */
    private void takePicture() {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("FILE_PREFIX", adress);
        startActivityForResult(intent, TAKE_PICTURE_REQUEST);
    }

    /**
     * Launch the record video intent.
     */
    private void recordVideo() {
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra("FILE_PREFIX", adress);
        startActivityForResult(intent, RECORD_VIDEO_CAPTURE_REQUEST);
    }

    /*
     * Save state.
     */
    private void saveState() {
        String serializedVideoPaths = (new JSONArray(videoPaths)).toString();
        String serializedImagePaths = (new JSONArray(imagePaths)).toString();
        String serializedMemoPaths = (new JSONArray(memoPaths)).toString();

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(STATE_VIDEOS, serializedVideoPaths);
        editor.putString(STATE_PICTURES, serializedImagePaths);
        editor.putString(STATE_MEMOS, serializedMemoPaths);
        editor.putString(STATE_ADRESS, adress);
        editor.putString(STATE_EVENT, url);
        editor.apply();
    }

    /**
     * Remove state.
     */
    private void deleteState() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Restore state.
     */
    private void restoreState() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        url = sharedPref.getString(STATE_EVENT, null);
        adress = sharedPref.getString(STATE_ADRESS, null);
        String serializedVideoPaths = sharedPref.getString(STATE_VIDEOS, "[]");
        String serializedImagePaths = sharedPref.getString(STATE_PICTURES, "[]");
        String serializedMemoPaths = sharedPref.getString(STATE_MEMOS, "[]");

        imagePaths = new ArrayList<>();
        videoPaths = new ArrayList<>();
        memoPaths = new ArrayList<>();

        try {
            JSONArray jsonArray = new JSONArray(serializedVideoPaths);
            for (int i = 0; i < jsonArray.length(); i++) {
                videoPaths.add(jsonArray.getString(i));
            }

            jsonArray = new JSONArray(serializedImagePaths);
            for (int i = 0; i < jsonArray.length(); i++) {
                imagePaths.add(jsonArray.getString(i));
            }

            jsonArray = new JSONArray(serializedMemoPaths);
            for (int i = 0; i < jsonArray.length(); i++) {
                memoPaths.add(jsonArray.getString(i));
            }
        }
        catch (JSONException e) {
            // ignore
        }

        Log.i(TAG, "Restored patient: " + adress);
        Log.i(TAG, "Restored imagePaths: " + imagePaths);
        Log.i(TAG, "Restored videoPaths: " + videoPaths);
        Log.i(TAG, "Restored memoPaths: " + memoPaths);
    }

    /**
     * Empty the directory.
     */
    private void cleanDirectory() {
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), FILE_DIRECTORY);
        Log.i(TAG, "Cleaning directory: " + f.getAbsolutePath());

        File[] files = f.listFiles();
        if (files != null && files.length > 0) {
            for (File inFile : files) {
                boolean success = inFile.delete();
                if (!success) {
                    Log.e(TAG, "file: " + inFile + " was not deleted (continuing).");
                }
            }
        }
        else {
            Log.i(TAG, "directory empty or does not exist.");
        }
    }

    public void sendFileDone(BrilleappenClient client, JSONObject result){
        clientResult = result;
    }

    public void notifyFileDone(BrilleappenClient client, JSONObject result){
        clientResult = null;
        proposeAToast("Email sent");
    }

    /**
     * List all files in f.
     *
     * @param f file to list
     */
    private void getDirectoryListing(File f) {
        File[] files = f.listFiles();
        if (files != null && files.length > 0) {
            for (File inFile : files) {
                if (inFile.isDirectory()) {
                    Log.i(TAG, "(dir) " + inFile);
                    getDirectoryListing(inFile);
                } else {
                    Log.i(TAG, "" + inFile);
                }
            }
        } else {
            Log.i(TAG, "directory empty or does not exist.");
        }
    }

    /**
     * On activity result.
     * <p/>
     * When an intent returns, it is intercepted in this method.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received image: " + data.getStringExtra("path"));

            imagePaths.add(data.getStringExtra("path"));
            saveState();
            updateUI();
            clientResult = null;
            client = new BrilleappenClient(this, url, username, password);

            client.execute(EXECUTE_SENDFILE ,new File(data.getStringExtra("path")), false);
        }
        else if (requestCode == RECORD_VIDEO_CAPTURE_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received video: " + data.getStringExtra("path"));

            videoPaths.add(data.getStringExtra("path"));
            saveState();
            updateUI();
        } else if (requestCode == RECORD_MEMO_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received memo: " + data.getStringExtra("path"));

            memoPaths.add(data.getStringExtra("path"));
            saveState();
            updateUI();

        } else if (requestCode == SCAN_ADRESS_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received url QR: " + data.getStringExtra("result"));

            String result = data.getStringExtra("result");

            try {
                JSONObject jResult = new JSONObject(result);
                JSONObject caption = jResult.getJSONObject("caption");

                url = jResult.getString("url");
                adress = jResult.getString("title");
                captionTwitter = caption.getString("twitter");
                captionInstagram = caption.getString("instagram");

            }
            catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }

            // Set the main activity view.
            setContentView(R.layout.activity_layout);

            saveState();
            updateUI();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Update a ui text view.
     * @param id id of the text view
     * @param value value to assign
     * @param color the color to set for the text field
     */
    private void updateTextField(int id, String value, Integer color) {
        TextView v = (TextView) findViewById(id);
        if (value != null) {
            v.setText(value);
        }
        if (color != null) {
            v.setTextColor(color);
        }
        v.invalidate();
    }

    /**
     * Update the UI.
     */
    private void updateUI() {
        updateTextField(R.id.imageNumber, String.valueOf(imagePaths.size()), imagePaths.size() > 0 ? Color.WHITE : null);
        updateTextField(R.id.imageLabel, null, imagePaths.size() > 0 ? Color.WHITE : null);

        updateTextField(R.id.videoNumber, String.valueOf(videoPaths.size()), videoPaths.size() > 0 ? Color.WHITE : null);
        updateTextField(R.id.videoLabel, null, videoPaths.size() > 0 ? Color.WHITE : null);

        updateTextField(R.id.memoNumber, String.valueOf(memoPaths.size()), memoPaths.size() > 0 ? Color.WHITE : null);
        updateTextField(R.id.memoLabel, null, memoPaths.size() > 0 ? Color.WHITE : null);

        updateTextField(R.id.patientIdentifier, adress, adress != null ? Color.WHITE : null);
    }

    /**
     * Send a toast
     *
     * @param message Message to display
     */
    public void proposeAToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
