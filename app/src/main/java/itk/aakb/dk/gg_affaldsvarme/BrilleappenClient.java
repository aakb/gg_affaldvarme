package itk.aakb.dk.gg_affaldsvarme;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

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

public class BrilleappenClient extends AsyncTask<Object, Void, Boolean> {
    private static final String TAG = "bibliotek Brilleappen";

    private static final int EXECUTE_SENDFILE = 1;
    private static final int EXECUTE_NOTIFY = 2;
    private String url;
    private String username;
    private String password;
    private BrilleappenClientListener clientListener;

    public BrilleappenClient(BrilleappenClientListener clientListener, String url, String username, String password) {
        this.url = url.replaceFirst("/+$", "");
        this.username = username;
        this.password = password;
        this.clientListener = clientListener;
    }

    protected Boolean doInBackground(Object... args) {
        int action = (int)args[0];

        switch (action) {
            case EXECUTE_SENDFILE:
                File file = (File) args[1];
                boolean share = (boolean) args[2];
                _sendFile(file, share);
                break;
            case EXECUTE_NOTIFY:
                JSONObject result = (JSONObject)args[1];
                _notifyFile(result);
                break;
        }

        return true;
    }

    protected void onPostExecute(Boolean result) {
        // TODO: check this.exception
        // TODO: do something with the feed
    }

    private void _notifyFile(JSONObject clientResult) {
        try {
            String notifyUrl = clientResult.getString("notify_url");
            URL url = new URL(notifyUrl);

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

            clientListener.notifyFileDone(this, result);

            Log.i(TAG, serverResponseCode + ": " + response);
        } catch (Throwable t) {
            Log.e(TAG, t.getMessage());
        }
    }
    public void notifyFile(JSONObject result) {
        execute(EXECUTE_NOTIFY, result);
    }

    public void sendFile(File file, boolean share) {
        execute(EXECUTE_SENDFILE, file, share);
    }

    private void _sendFile(File file, boolean share) {
        try {
            String mimeType = URLConnection.guessContentTypeFromName(file.getName());

            URL url = new URL(this.url + "?type=" + mimeType + "&share=" + (share ?  "yes" : "no"));

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

            JSONObject result = new JSONObject(response);

            clientListener.sendFileDone(this, result);

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
        try {
            InputStream responseStream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream();

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
}
