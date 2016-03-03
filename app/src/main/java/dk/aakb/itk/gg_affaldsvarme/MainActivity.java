package dk.aakb.itk.gg_affaldsvarme;

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
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import dk.aakb.itk.brilleappen.BrilleappenClient;
import dk.aakb.itk.brilleappen.BrilleappenClientListener;
import dk.aakb.itk.brilleappen.ContactPerson;
import dk.aakb.itk.brilleappen.Event;
import dk.aakb.itk.brilleappen.Media;

public class MainActivity extends Activity implements BrilleappenClientListener, GestureDetector.BaseListener {
    public static final String FILE_DIRECTORY = "Affaldvarme";

    private static final String TAG = "affaldvarme_main";
    private static final int TAKE_PICTURE_REQUEST = 101;
    private static final int RECORD_VIDEO_CAPTURE_REQUEST = 102;
    private static final int SCAN_ADDRESS_REQUEST = 103;
    private static final int RECORD_MEMO_REQUEST = 104;
    private static final String STATE_VIDEOS = "videos";
    private static final String STATE_PICTURES = "pictures";
    private static final String STATE_MEMOS = "memos";
    private static final String STATE_EVENT = "url";
    private static final String STATE_ADDRESS = "address";

    private ArrayList<String> imagePaths = new ArrayList<>();
    private ArrayList<String> videoPaths = new ArrayList<>();
    private ArrayList<String> memoPaths = new ArrayList<>();

    private static final int MENU_MAIN = 1;
    private static final int MENU_START = 0;

    private String address = null;
    private String addFileUrl = null;
    private String addressUrl;
    private BrilleappenClient client;
    private Media clientResultMedia;
    private String username;
    private String password;
    private String captionTwitter;
    private String captionInstagram;
    private String uploadFileUrl;
    private Event event;

    int selectedMenu = 0;

    private GestureDetector gestureDetector;
    private Menu panelMenu;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.i(TAG, "onSaveInstanceState");

        // Save the user's current game state
        savedInstanceState.putStringArrayList(STATE_VIDEOS, videoPaths);
        savedInstanceState.putStringArrayList(STATE_PICTURES, imagePaths);
        savedInstanceState.putStringArrayList(STATE_MEMOS, memoPaths);
        savedInstanceState.putString(STATE_ADDRESS, address);
        savedInstanceState.putString(STATE_EVENT, addFileUrl);

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
            proposeAToast("Cannot read configuration file");
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
            address = savedInstanceState.getString(STATE_ADDRESS);
            addFileUrl = savedInstanceState.getString(STATE_EVENT);

        } else {
            Log.i(TAG, "Restoring state");

            // Probably initialize members with default values for a new instance
            restoreState();
        }

        if (addFileUrl != null) {
            selectedMenu = MENU_MAIN;

            // Set the main activity view.
            setContentView(R.layout.activity_layout);

            updateUI();
        } else {
            selectedMenu = MENU_START;
            // Set the main activity view.
            setContentView(R.layout.activity_layout_init);
        }


        Log.i(TAG, "------------");

        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), MainActivity.FILE_DIRECTORY);
        Log.i(TAG, "Listing files in: " + f.getAbsolutePath());

        getDirectoryListing(f);

        Log.i(TAG, "------------");

        gestureDetector = new GestureDetector(this).setBaseListener(this);
    }


    public boolean onGenericMotionEvent(MotionEvent event) {
        return gestureDetector.onMotionEvent(event);
    }

    public boolean onGesture(Gesture gesture) {
        if (Gesture.TAP.equals(gesture)) {
            openOptionsMenu();

            return true;
        }
        return false;
    }

    /**
     * On create panel menu.
     *
     * @param featureId the feature id
     * @param menu      the menu to create
     * @return boolean
     */
    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS ||
                featureId == Window.FEATURE_OPTIONS_PANEL) {

            getMenuInflater().inflate(R.menu.main, menu);

            panelMenu = menu;

            updatePanelMenu();

            return true;
        }

        // Pass through to super to setup touch menu.
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS ||
                featureId == Window.FEATURE_OPTIONS_PANEL) {

            updatePanelMenu();
        }

        return super.onPreparePanel(featureId, view, menu);
    }

    /**
     * Update what menu is displayed.
     */
    public void updatePanelMenu() {
        panelMenu.setGroupVisible(R.id.main_menu_group_main, selectedMenu == MENU_MAIN);
        panelMenu.setGroupVisible(R.id.main_menu_group_start, selectedMenu == MENU_START);
    }

    /**
     * On menu item selected.
     * <p>
     * Processes the voice commands from the main menu.
     *
     * @param featureId the feature id
     * @param item      the selected menu item
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
                case R.id.notify_menu_item:
                    Log.i(TAG, "menu: Notify by email");

                    notifyByEmail();

                    break;

                case R.id.scan_new_adress_menu_item:
                    Log.i(TAG, "menu: Scan new adress");
                    deleteState();
                    clearMediaArrays();
                    Intent scanNewAdressIntent = new Intent(this, QRActivity.class);
                    startActivityForResult(scanNewAdressIntent, SCAN_ADDRESS_REQUEST);
                    updateUI();

                    break;
                case R.id.confirm_cancel:
                    Log.i(TAG, "menu: Confirm: cancel and exit");

                    cleanDirectory();
                    deleteState();

                    finish();

                    break;

                case R.id.scan_address_menu_item:
                    Intent scanAddressIntent = new Intent(this, QRActivity.class);
                    startActivityForResult(scanAddressIntent, SCAN_ADDRESS_REQUEST);

                    break;
                case R.id.finish_menu_item:
                    deleteState();
                    finish();

                    break;

                case R.id.create_breakdown_menu_item:
                    createBreakdown();
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
        if (clientResultMedia != null) {
            client = new BrilleappenClient(this, addFileUrl, username, password);
            client.notifyFile(clientResultMedia);
        }
    }

    /**
     * Launch the record memo intent.
     */
    private void recordMemo() {
        Intent intent = new Intent(this, MemoActivity.class);
        intent.putExtra("FILE_PREFIX", address);
        startActivityForResult(intent, RECORD_MEMO_REQUEST);
    }

    /**
     * Launch the image capture intent.
     */
    private void takePicture() {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("FILE_PREFIX", address);
        startActivityForResult(intent, TAKE_PICTURE_REQUEST);
    }

    /**
     * Launch the record video intent.
     */
    private void recordVideo() {
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra("FILE_PREFIX", address);
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
        editor.putString(STATE_ADDRESS, address);
        editor.putString(STATE_EVENT, addFileUrl);
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
        addFileUrl = sharedPref.getString(STATE_EVENT, null);
        address = sharedPref.getString(STATE_ADDRESS, null);
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
        } catch (JSONException e) {
            // ignore
        }

        Log.i(TAG, "Restored url: " + addFileUrl);
        Log.i(TAG, "Restored address: " + address);
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
        } else {
            Log.i(TAG, "directory empty or does not exist.");
        }
    }

    private void sendFile(String path) {
        sendFile(path, false);
    }

    private void sendFile(String path, boolean notify) {
        clientResultMedia = null;
        client = new BrilleappenClient(this, addFileUrl, username, password);
        client.sendFile(new File(path), notify);
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
     * <p>
     * When an intent returns, it is intercepted in this method.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String path;
            switch (requestCode) {
                case TAKE_PICTURE_REQUEST:
                    Log.i(TAG, "Received image: " + data.getStringExtra("path"));

                    boolean instaShare = data.getBooleanExtra("instaShare", false);
                    path = data.getStringExtra("path");
                    imagePaths.add(path);
                    saveState();
                    updateUI();
                    sendFile(path);
                    break;
                case RECORD_VIDEO_CAPTURE_REQUEST:
                    Log.i(TAG, "Received video: " + data.getStringExtra("path"));

                    path = data.getStringExtra("path");
                    videoPaths.add(path);
                    saveState();
                    updateUI();
                    sendFile(path);
                    break;
                case RECORD_MEMO_REQUEST:
                    Log.i(TAG, "Received memo: " + data.getStringExtra("path"));

                    memoPaths.add(data.getStringExtra("path"));
                    saveState();
                    updateUI();
                    break;

                case SCAN_ADDRESS_REQUEST:
                    Log.i(TAG, "Received url QR: " + data.getStringExtra("result"));

                    String result = data.getStringExtra("result");

                    try {
                        JSONObject jResult = new JSONObject(result);
                        addressUrl = jResult.getString("url");

                        selectedMenu = MENU_MAIN;

                        updatePanelMenu();

                        if (jResult.has("caption")) {
                            JSONObject caption = jResult.getJSONObject("caption");

                            captionTwitter = caption.getString("twitter");
                            captionInstagram = caption.getString("instagram");
                        }

                        client = new BrilleappenClient(this, addressUrl, username, password);
                        client.getEvent();
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                    }

                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Update a ui text view.
     *
     * @param id    id of the text view
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

        updateTextField(R.id.addressIdentifier, address, address != null ? Color.WHITE : null);
    }


    private void clearMediaArrays() {
        imagePaths.clear();
        videoPaths.clear();
        memoPaths.clear();
    }

    /**
     * Send a toast
     *
     * @param message Message to display
     */
    public void proposeAToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void createBreakdown() {
        String url = "http://teknikogmiljoe.hulk.aakb.dk/brilleappen/event/create";
        client = new BrilleappenClient(this, url, username, password);
        client.createEvent("Breakdown @" + new Date().toString(), "breakdown");
    }

    @Override
    public void createEventDone(BrilleappenClient client, boolean success, String url) {
        Log.i(TAG, "createEventDone");
        try {
            addressUrl = url;

            selectedMenu = MENU_MAIN;

            updatePanelMenu();

            client = new BrilleappenClient(this, addressUrl, username, password);
            client.getEvent();
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                proposeAToast("Event created");
            }
        });
    }

    @Override
    public void getEventDone(BrilleappenClient client, boolean success, Event event) {
        Log.i(TAG, "getEventDone (" + success + "): " + event);

        if (success) {
            try {
                this.event = event;

                this.address = event.title;
                this.captionTwitter = event.twitterCaption;
                this.captionInstagram = event.instagramCaption;

                this.uploadFileUrl = event.addFileUrl;

                saveState();

                // Update the UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (uploadFileUrl != null) {
                            // Set the main activity view.
                            setContentView(R.layout.activity_layout);
                        }

                        updateUI();
                    }
                });
            }
            catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
        }
    }

    @Override
    public void sendFileDone(BrilleappenClient client, boolean success, File file, Media media) {
        Log.i(TAG, "sendFileDone");
        clientResultMedia = media;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                proposeAToast("File sent");
            }
        });
    }

    @Override
    public void sendFileProgress(BrilleappenClient client, File file, int progress, int max) {
        // Not implemented
    }

    @Override
    public void notifyFileDone(BrilleappenClient client, boolean success, Media media) {
        Log.i(TAG, "notifyFileDone");
        clientResultMedia = null;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                proposeAToast("Email sent");
            }
        });
    }
}
