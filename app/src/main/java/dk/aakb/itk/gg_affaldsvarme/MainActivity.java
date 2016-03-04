package dk.aakb.itk.gg_affaldsvarme;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
import dk.aakb.itk.brilleappen.UndeliveredFile;

public class MainActivity extends BaseActivity implements BrilleappenClientListener, GestureDetector.BaseListener {
    public static final String FILE_DIRECTORY = "Affaldvarme";

    private static final String TAG = "affaldvarme_main";

    private static final int TAKE_PICTURE_REQUEST = 101;
    private static final int RECORD_VIDEO_CAPTURE_REQUEST = 102;
    private static final int SCAN_ADDRESS_REQUEST = 103;
    private static final int RECORD_MEMO_REQUEST = 104;

    private static final String STATE_UNDELIVERED_FILES = "undelivered_files";
    private static final String STATE_EVENT = "event";
    private static final String STATE_EVENT_URL = "event_url";
    private static final String STATE_CONTACTS = "contacts";

    private static final int MENU_MAIN = 1;
    private static final int MENU_START = 0;

    private GestureDetector gestureDetector;
    private Menu panelMenu;
    private BrilleappenClient client;
    private String username;
    private String password;

    private Media clientResultMedia;
    private boolean isOffline = false;
    private int numberOfFiles = 0;
    private int selectedMenu = MENU_START;
    private ArrayList<Contact> contacts = new ArrayList<>();
    private ArrayList<UndeliveredFile> undeliveredFiles = new ArrayList<>();

    private Event event;
    private String eventUrl;

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

        restoreState();

        if (event != null) {
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

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        panelMenu = menu;

        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS ||
                featureId == Window.FEATURE_OPTIONS_PANEL) {

            getMenuInflater().inflate(R.menu.main, menu);
        }

        if (updateMenu(menu, featureId)) {
            return true;
        }

        // Pass through to super to setup touch menu.
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        updateMenu(menu, featureId);

        return super.onPreparePanel(featureId, view, menu);
    }

    private boolean updateMenu(Menu menu, int featureId) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS ||
                featureId == Window.FEATURE_OPTIONS_PANEL) {

            // Add contacts menu, if not already added.
            if (menu.findItem(R.id.make_call_menu_item).getSubMenu().size() <= 1) {
                for (int i = 0; i < contacts.size(); i++) {
                    menu.findItem(R.id.make_call_menu_item).getSubMenu().add(R.id.main_menu_group_main, R.id.contacts_menu_item, i, contacts.get(i).getName());
                }
            }

            // Hide menu if no contacts are available.
            menu.findItem(R.id.make_call_menu_item).setVisible(contacts.size() > 0);

            // Update which group is visible.
            setMenuGroupVisibilty(menu);

            // Hide the finish_menu from main_menu_group_main when using voice commands.
            if (featureId == Window.FEATURE_OPTIONS_PANEL && selectedMenu == MENU_MAIN) {
                menu.findItem(R.id.finish_menu_item).setVisible(true);
            }
            else {
                menu.findItem(R.id.finish_menu_item).setVisible(false);
            }

            return true;
        }
        return false;
    }

    /**
     * Update what menu is displayed.
     */
    public void setMenuGroupVisibilty(Menu menu) {
        menu.setGroupVisible(R.id.main_menu_group_main, selectedMenu == MENU_MAIN);
        menu.setGroupVisible(R.id.main_menu_group_start, selectedMenu == MENU_START);
        menu.findItem(R.id.scan_address_menu_item).setVisible(event == null);
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
                case R.id.take_picture_menu_item:
                    Log.i(TAG, "menu: take picture");

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
                case R.id.contacts_menu_item:
                    Log.i(TAG, "menu: make call");

                    Contact contact = contacts.get(item.getOrder());

                    Log.i(TAG, "Calling: (" + item.getOrder() + ") " + contact.getName() + " " + contact.getPhoneNumber());

                    proposeAToast(R.string.calling_name_phone, contact.getName(), contact.getPhoneNumber());

                    makeCall(contact.getPhoneNumber());

                    break;
                case R.id.scan_new_adress_menu_item:
                    Log.i(TAG, "menu: Scan new adress");
                    deleteState();

                    numberOfFiles = 0;

                    Intent scanNewAdressIntent = new Intent(this, QRActivity.class);
                    startActivityForResult(scanNewAdressIntent, SCAN_ADDRESS_REQUEST);

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
                case R.id.offline_event_menu_item:
                    setOfflineEvent();
                    isOffline = true;
                    selectedMenu = MENU_MAIN;

                    setContentView(R.layout.activity_layout);

                    updateUI();

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
            client = new BrilleappenClient(this, clientResultMedia.notifyUrl, username, password);
            client.notifyFile(clientResultMedia);
        }
    }

    public void setOfflineEvent() {
        contacts = new ArrayList<>();
        event = null;
    }

    /**
     * Launch the image capture intent.
     */
    private void takePicture() {
        Intent intent = new Intent(this, PictureActivity.class);
        startActivityForResult(intent, TAKE_PICTURE_REQUEST);
    }

    /**
     * Launch the record video intent.
     */
    private void recordVideo() {
        Intent intent = new Intent(this, VideoActivity.class);
        startActivityForResult(intent, RECORD_VIDEO_CAPTURE_REQUEST);
    }

    /**
     * Call a phone number with an intent
     *
     * @param phoneNumber The phone number to call.
     */
    private void makeCall(String phoneNumber) {
        Intent localIntent = new Intent();
        localIntent.putExtra("com.google.glass.extra.PHONE_NUMBER", phoneNumber);
        localIntent.setAction("com.google.glass.action.CALL_DIAL");
        sendBroadcast(localIntent);
    }

    /*
     * Save state.
     */
    private void saveState() {
        Gson gson = new Gson();

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(STATE_UNDELIVERED_FILES, gson.toJson(undeliveredFiles));
        editor.putString(STATE_CONTACTS, gson.toJson(contacts));
        editor.putString(STATE_EVENT_URL, eventUrl);
        editor.putString(STATE_EVENT, gson.toJson(event));
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
        String serializedEvent = sharedPref.getString(STATE_EVENT, "{}");
        String serializedUndeliveredFiles = sharedPref.getString(STATE_UNDELIVERED_FILES, "[]");
        String serializedContacts = sharedPref.getString(STATE_CONTACTS, "[]");

        undeliveredFiles = new Gson().fromJson(serializedUndeliveredFiles, new TypeToken<ArrayList<UndeliveredFile>>() {}.getType());
        contacts = new Gson().fromJson(serializedContacts, new TypeToken<ArrayList<Contact>>() {}.getType());
        event = new Gson().fromJson(serializedEvent, Event.class);
        eventUrl = sharedPref.getString(STATE_EVENT_URL, null);

        Log.i(TAG, "Restored event: " + event);
        Log.i(TAG, "Restored event url: " + eventUrl);
        Log.i(TAG, "Restored undeliveredFiles: " + undeliveredFiles);
        Log.i(TAG, "Restored contacts: " + contacts);
    }

    private void sendFile(String path) {
        sendFile(path, false);
    }

    private void sendFile(String path, boolean notify) {
        if (isOffline) {
            undeliveredFiles.add(new UndeliveredFile(null, null, path));
            saveState();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    proposeAToast(R.string.is_offline_file_not_sent);
                }
            });
        } else {
            clientResultMedia = null;
            client = new BrilleappenClient(this, eventUrl, username, password);
            client.sendFile(new File(path), notify);
        }
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
                    path = data.getStringExtra("path");

                    numberOfFiles++;

                    saveState();

                    updateUI();
                    sendFile(path);

                    break;
                case RECORD_VIDEO_CAPTURE_REQUEST:
                    Log.i(TAG, "Received video: " + data.getStringExtra("path"));

                    path = data.getStringExtra("path");

                    numberOfFiles++;

                    saveState();
                    updateUI();
                    sendFile(path);
                    break;
                case RECORD_MEMO_REQUEST:
                    Log.i(TAG, "Received memo: " + data.getStringExtra("path"));

                    numberOfFiles++;

                    saveState();
                    updateUI();
                    break;
                case SCAN_ADDRESS_REQUEST:
                    Log.i(TAG, "Received url QR: " + data.getStringExtra("result"));

                    String result = data.getStringExtra("result");

                    try {
                        JSONObject jResult = new JSONObject(result);
                        eventUrl = jResult.getString("url");

                        selectedMenu = MENU_MAIN;

                        setMenuGroupVisibilty(panelMenu);

                        client = new BrilleappenClient(this, eventUrl, username, password);
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
        updateTextField(R.id.filesNumber, String.valueOf(numberOfFiles), numberOfFiles != 0 ? Color.WHITE : null);
        updateTextField(R.id.filesLabel, null, numberOfFiles > 0 ? Color.WHITE : null);

        updateTextField(R.id.addressIdentifier, event != null ? event.title : "offline", event != null ? Color.WHITE : null);

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
            eventUrl = url;

            selectedMenu = MENU_MAIN;

            setMenuGroupVisibilty(panelMenu);

            client = new BrilleappenClient(this, eventUrl, username, password);
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

                this.contacts = new ArrayList<>();
                for (ContactPerson cp : event.contactPersons) {
                    contacts.add(new Contact(cp.name, cp.phone));
                }

                if (isOffline) {
                    isOffline = false;

                    for (UndeliveredFile undeliveredFile : undeliveredFiles) {
                        if (undeliveredFile.isOfflineEvent()) {
                            undeliveredFile.setEvent(event);
                            undeliveredFile.setEventUrl(event.addFileUrl);
                        }

                        // Send previously undelivered files.
                        sendFile(undeliveredFile.getFilePath(), false);
                    }
                }

                saveState();

                if (this.event != null) {
                    // Update the UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Set the main activity view.
                            setContentView(R.layout.activity_layout);

                            updateUI();
                        }
                    });
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
        }
    }

    @Override
    public void sendFileDone(BrilleappenClient client, boolean success, File file, Media media) {
        if (success) {
            // If this is an undelivered file, remove from list
            for (UndeliveredFile undeliveredFile : undeliveredFiles) {
                if (undeliveredFile.getFilePath().equals(file.getPath())) {
                    undeliveredFiles.remove(undeliveredFile);
                }
                else {
                    // Send previously undelivered files.
                    if (!undeliveredFile.isOfflineEvent()) {
                        sendFile(undeliveredFile.getFilePath(), false);
                    }
                }
            }

            saveState();

            Log.i(TAG, "sendFileDone");
            clientResultMedia = media;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    proposeAToast("File sent");
                }
            });
        } else {
            undeliveredFiles.add(new UndeliveredFile(event, event.addFileUrl, file.getPath()));

            saveState();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    proposeAToast(R.string.is_offline_file_not_sent);
                }
            });
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void sendFileProgress(BrilleappenClient client, File file, final int progress, final int max) {
       Log.i(TAG, String.format("sendFileProgress: %d/%d", progress, max));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                progressBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                progressBar.setMax(max);
                progressBar.setProgress(progress);
            }
        });
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
