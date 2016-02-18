package dk.aakb.itk.gg_affaldsvarme;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

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
    private static final String TAG = "bibliotek Brilleappen";

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
        this.url = url.replaceFirst("/+$", "");
        this.username = username;
        this.password = password;
        this.listener = listener;
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
                return true;
            case GET_EVENT:
                _getEvent();
                return true;
            case SEND_FILE:
                File file = (File)args[1];
                boolean share = (boolean)args[2];
                _sendFile(file, share);
                return true;
            case NOTIFY_FILE:
                JSONObject result = (JSONObject)args[1];
                String[] types = (String[])args[2];
                _notifyFile(result, types);
                return true;
        }
        return false;
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

    public void notifyFile(JSONObject result, String[] types) {
        execute(Execute.NOTIFY_FILE, result, types);
    }

    public void notifyFile(JSONObject result) {
        notifyFile(result, null);
    }

    private void _createEvent(String title, String type, double lat, double lng) {
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
            JSONObject data = new JSONObject();
            data.put("title", title);
            data.put("type", type);
            if (lat != Double.MIN_VALUE && lng != Double.MIN_VALUE) {
                data.put("geolocation", new double[]{lat, lng});
            }
            dos.writeBytes(data.toString());
            dos.flush();
            dos.close();

            String response = getResponse(connection, 201);

            JSONObject result = null;
            try {
                result = new JSONObject(response);
            }
            catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }

            listener.createEventDone(this, result);
        }
        catch (Exception e) {
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

            JSONObject result = null;
            try {
                result = new JSONObject(response);
            }
            catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }

            listener.getEventDone(this, result);
        }
        catch (Exception e) {
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

            JSONObject mainObject = new JSONObject(response);

            listener.sendFileDone(this, mainObject);

            Log.i(TAG, serverResponseCode + ": " + response);
        } catch (Throwable t) {
            Log.e(TAG, t.getMessage());
        }
    }

    private void _notifyFile(JSONObject clientResult, final String[] types) {
        try {
            String notifyUrl = clientResult.getString("notify_url");
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

            JSONObject result = new JSONObject(response);

            listener.notifyFileDone(this, result);

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

        int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

        while (bytesRead > 0) {
            dos.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
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
            for (Map.Entry<String, Object> entry: query.entrySet()) {
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
}
