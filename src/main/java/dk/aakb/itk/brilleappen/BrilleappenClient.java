package dk.aakb.itk.brilleappen;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class BrilleappenClient extends AsyncTask<Object, Void, Boolean> {
    private static final String TAG = "brilleappen_client";

    private enum Execute {
        CREATE_EVENT,
        GET_EVENT,
        SEND_FILE,
        NOTIFY_FILE
    }

    private String url;
    private String username;
    private String password;
    private BrilleappenClientListener listener;

    public BrilleappenClient(BrilleappenClientListener listener, String url, String username, String password) {
        this.listener = listener;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public BrilleappenClient(String url, String username, String password) {
        this(null, url, username, password);
    }

    protected Boolean doInBackground(Object... args) {
        Execute action = (Execute)args[0];

        switch (action) {
            case CREATE_EVENT:
                String title = (String)args[1];
                String type = (String)args[2];
                double lat = (double)args[3];
                double lng = (double)args[4];
                _createEvent(title, type, lat, lng);
                break;
            case GET_EVENT:
                _getEvent();
                break;
            case SEND_FILE:
                File file = (File)args[1];
                boolean share = (boolean)args[2];
                _sendFile(file, share);
                break;
            case NOTIFY_FILE:
                Media media = (Media)args[1];
                String[] types = (String[])args[2];
                _notifyFile(media, types);
                break;
        }
        return true;
    }

    public void createEvent(String title, String type, double lat, double lng) {
        execute(Execute.CREATE_EVENT, title, type, lat, lng);
    }

    public void createEvent(String title, String type) {
        createEvent(title, type, Double.MIN_VALUE, Double.MIN_VALUE);
    }

    public void getEvent() {
        execute(Execute.GET_EVENT);
    }

    public void sendFile(File file, boolean share) {
        execute(Execute.SEND_FILE, file, share);
    }

    public void notifyFile(Media media, String[] types) {
        execute(Execute.NOTIFY_FILE, media, types);
    }

    public void notifyFile(Media media) {
        notifyFile(media, null);
    }

    private void _createEvent(final String title, final String type, final double lat, final double lng) {
        try {
            URL url = new URL(this.url);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();

            String authString = username + ":" + password;
            String authStringEnc = Base64.encodeToString(authString.getBytes(), Base64.DEFAULT);

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
            HashMap<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("type", type);
            if (lat != Double.MIN_VALUE && lng != Double.MIN_VALUE) {
                data.put("geolocation", new double[]{lat, lng});
            }
            dos.writeBytes(Util.toJson(data));
            dos.flush();
            dos.close();

            String response = getResponse(connection, 201);

            String eventUrl = null;
            boolean success = false;

            try {
                Map<String, Object> values = Util.getValues(response);
                eventUrl = values.containsKey("url") ? (String)values.get("url") : null;
                success = values.containsKey("status") && values.get("status") == "OK";
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }

            if (listener != null) {
                listener.createEventDone(this, success, eventUrl);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void _getEvent() {
        try {
            URL url = new URL(this.url);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();

            String authString = username + ":" + password;
            String authStringEnc = Base64.encodeToString(authString.getBytes(), Base64.DEFAULT);

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String response = getResponse(connection);

            boolean success = false;
            Event event = null;
            try {
                event = new Event(response);
                success = true;
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }

            if (listener != null) {
                listener.getEventDone(this, success, event);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void _sendFile(File file, final boolean share) {
        try {
            final String mimeType = URLConnection.guessContentTypeFromName(file.getName());

            Map<String, Object> query = new HashMap<String, Object>() {
                {
                    put("type", mimeType);
                    put("share", (share ? "yes" : "no"));
                }
            };

            URL url = getUrl(this.url, query);

            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setFixedLengthStreamingMode(file.length());

            String authString = username + ":" + password;
            String authStringEnc = Base64.encodeToString(authString.getBytes(), Base64.DEFAULT);

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
            writeFile(dos, file);
            dos.flush();
            dos.close();

            // Response from the server (code and message)
            int serverResponseCode = connection.getResponseCode();
            String response = getResponse(connection);

            Media media = null;
            boolean success = false;
            try {
                if (serverResponseCode == 200) {
                    media = new Media(response);
                    success = true;
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }

            if (listener != null) {
                listener.sendFileDone(this, success, file, media);
            }

            Log.i(TAG, serverResponseCode + ": " + response);
        } catch (Throwable t) {
            Log.e(TAG, t.getClass() + "\t" + t.getMessage());
        }
    }

    private void _notifyFile(Media media, final String[] types) {
        try {
            String notifyUrl = media.notifyUrl;
            Map<String, Object> query = new HashMap<String, Object>() {{
                put("types", types);
            }};
            URL url = getUrl(notifyUrl, query);

            HttpURLConnection connection = (HttpURLConnection)url.openConnection();

            String authString = username + ":" + password;
            String authStringEnc = Base64.encodeToString(authString.getBytes(), Base64.DEFAULT);

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Basic " + authStringEnc);

            // Response from the server (code and message)
            int serverResponseCode = connection.getResponseCode();
            String response = getResponse(connection);

            Map<String, Object> values = Util.getValues(response);
            boolean success = values.containsKey("status") && "OK".equals(values.get("status"));

            if (listener != null) {
                listener.notifyFileDone(this, success, media);
            }

            Log.i(TAG, serverResponseCode + ": " + response);
        } catch (Throwable t) {
            Log.e(TAG, t.getMessage());
        }
    }

    private void writeFile(DataOutputStream dos, File file) throws Throwable {
        int maxBufferSize = 1024 * 1024;

        FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());

        int bytesAvailable = fileInputStream.available();
        int bufferSize = Math.min(bytesAvailable, maxBufferSize);
        byte[] buffer = new byte[bufferSize];

        int totalBytesAvailable = bytesAvailable;
        int totalBytesRead = 0;
        int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

        while (bytesRead > 0) {
            totalBytesRead += bytesRead;
            dos.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            if (listener != null) {
                listener.sendFileProgress(this, file, totalBytesRead, totalBytesAvailable);
            }
        }

        fileInputStream.close();
    }

    private String getResponse(HttpURLConnection connection) {
        return getResponse(connection, 200);
    }

    private String getResponse(HttpURLConnection connection, int expectedResponseCode) {
        try {
            InputStream responseStream = connection.getResponseCode() == expectedResponseCode ? connection.getInputStream() : connection.getErrorStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(responseStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            return sb.toString();
        } catch (IOException ex) {
            // @TODO: handle this!
            Log.e(TAG, ex.getMessage());
        }

        return null;
    }

    private URL getUrl(String url, Map<String, Object> query) {
        try {
            StringBuilder queryString = new StringBuilder();
            for (Map.Entry<String, Object> entry : query.entrySet()) {
                if (queryString.length() > 0) {
                    queryString.append('&');
                }
                Object value = entry.getValue();
                queryString.append(entry.getKey()).append('=').append(value == null ? "" : URLEncoder.encode(value.toString(), StandardCharsets.UTF_8.name()));
            }

            return new URL(url + (url.indexOf('?') == -1 ? '?' : '&') + queryString.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    // The synchronous stuff
    public String createEventSync(String title, String type, double lat, double lng) {
        BrilleappenClientListener originalListener = this.listener;

        BrilleappenClientListenerSync syncListener = new BrilleappenClientListenerSync();
        this.listener = syncListener;

        _createEvent(title, type, lat, lng);

        this.listener = originalListener;

        return syncListener.url;
    }

    public String createEventSync(String title, String type) {
        return createEventSync(title, type, Double.MIN_VALUE, Double.MIN_VALUE);
    }

    public Event getEventSync() {
        BrilleappenClientListener originalListener = this.listener;

        BrilleappenClientListenerSync syncListener = new BrilleappenClientListenerSync();
        this.listener = syncListener;

        _getEvent();

        this.listener = originalListener;

        return syncListener.event;
    }

    public Media sendFileSync(File file, boolean share) {
        BrilleappenClientListener originalListener = this.listener;

        BrilleappenClientListenerSync syncListener = new BrilleappenClientListenerSync();
        this.listener = syncListener;

        _sendFile(file, share);

        this.listener = originalListener;

        return syncListener.media;
    }

    private boolean successResult;

    public boolean notifyFileSync(Media media, String[] types) {
        BrilleappenClientListener originalListener = this.listener;

        BrilleappenClientListenerSync syncListener = new BrilleappenClientListenerSync();
        this.listener = syncListener;

        _notifyFile(media, types);

        this.listener = originalListener;

        return syncListener.success;
    }

    public boolean notifyFileSync(Media media) {
        return notifyFileSync(media, null);
    }

    class BrilleappenClientListenerSync implements BrilleappenClientListener {
        public boolean success;
        public String url;
        public Event event;
        public Media media;
        public File file;

        @Override
        public void createEventDone(BrilleappenClient client, boolean success, String url) {
            this.success = success;
            this.url = url;
        }

        @Override
        public void getEventDone(BrilleappenClient client, boolean success, Event event) {
            this.success = success;
            this.event = event;
        }

        @Override
        public void sendFileDone(BrilleappenClient client, boolean success, File file, Media media) {
            this.success = success;
            this.media = media;
            this.file = file;
        }

        @Override
        public void sendFileProgress(BrilleappenClient client, File file, int progress, int max) {
        }

        @Override
        public void notifyFileDone(BrilleappenClient client, boolean success, Media media) {
            this.success = success;
            this.media = media;
        }
    }
}
