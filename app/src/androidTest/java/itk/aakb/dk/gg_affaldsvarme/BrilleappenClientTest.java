package itk.aakb.dk.gg_affaldsvarme;

import android.app.Application;
import android.test.ApplicationTestCase;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

public class BrilleappenClientTest extends ApplicationTestCase<Application> implements BrilleappenClientListener {
    public BrilleappenClientTest() {
        super(Application.class);
    }

    CountDownLatch signal;
    BrilleappenClient client;
    JSONObject clientResult;

    public void testSendFile() {
        File file = new File(getContext().getCacheDir(), "test.png");
        boolean share = false;

        try {
            // Get image data from lorempixel.com.
            URL imageUrl = new URL("http://lorempixel.com/600/400/");

            HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();

            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("GET");

            int serverResponseCode = connection.getResponseCode();

            assertEquals(200, serverResponseCode);
            InputStream responseStream = connection.getInputStream();

            // Write data to file.
            FileOutputStream fos = new FileOutputStream(file);

            byte[] buffer = new byte[1024 * 1024];
            int bytesRead;
            while ((bytesRead = responseStream.read(buffer)) > 0) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.close();
            responseStream.close();
        } catch (Exception ex) {
            fail(ex.getMessage() + "; Cannot write data to file: " + file.getAbsolutePath());
        }

        try {
            client = createClient();
            clientResult = null;

            signal = new CountDownLatch(1);

            client.sendFile(file, share);

            signal.await();

            signal = new CountDownLatch(1);

            client = createClient();
            client.notifyFile(clientResult);

            signal.await();

        } catch (Throwable t) {
            assertFalse(true);
        }
    }

    private BrilleappenClient createClient() {
        String url = "http://teknikogmiljoe.hulk.aakb.dk/brilleappen/event/a1cae76d-288c-41a5-9e00-d961f7f26c00/file";
        String username = "rest";
        String password = "rest";

        return new BrilleappenClient(this, url, username, password);
    }

    @Override
    public void sendFileDone(BrilleappenClient client, JSONObject result) {
        assertTrue(result.has("notify_url"));
        clientResult = result;
        signal.countDown();
    }

    @Override
    public void notifyFileDone(BrilleappenClient client, JSONObject result) {
        assertTrue(result.has("notifyMessages"));
        signal.countDown();
    }
}
