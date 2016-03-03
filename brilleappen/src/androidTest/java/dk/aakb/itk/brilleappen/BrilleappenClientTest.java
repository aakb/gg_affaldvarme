package dk.aakb.itk.brilleappen;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.Suppress;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class BrilleappenClientTest extends ApplicationTestCase<Application> implements BrilleappenClientListener {
    public BrilleappenClientTest() {
        super(Application.class);
    }

    CountDownLatch signal;
    BrilleappenClient client;
    Media media;

    // @ Suppress
    public void testCreateEvent() {
        try {
            client = createClient(serviceBaseUrl + "/brilleappen/event/create");
            media = null;

            signal = new CountDownLatch(1);

            client.createEvent(new Date().toString(), "test");

            signal.await();
        } catch (Throwable t) {
            assertFalse(true);
        }
    }

    @Override
    public void createEventDone(BrilleappenClient client, boolean success, String eventUrl) {
        assertTrue(success);
        assertNull(eventUrl);
        assertNotNull(eventUrl);
        //assertTrue(result.has("url"));
    }

    @Suppress
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
            media = null;

            signal = new CountDownLatch(1);

            client.sendFile(file, share);

            signal.await();

            signal = new CountDownLatch(1);

            client = createClient();

            client.notifyFile(media);

            signal.await();

        } catch (Throwable t) {
            fail(t.getMessage());
        }
    }

    @Suppress
    private void testNotifyFile(Media media) {
        try {
            client = createClient();

            signal = new CountDownLatch(1);

            client.notifyFile(media, null);

            signal.await();
        } catch (Throwable t) {
            fail(t.getMessage());
        }
    }

    private final String serviceBaseUrl = "http://brilleappen.hulk.aakb.dk";

    private BrilleappenClient createClient() {
        return createClient(serviceBaseUrl + "/brilleappen/event/21d59ce2-fa69-426a-bd06-6e6c9edee086/file");
    }

    private BrilleappenClient createClient(String url) {
        String username = "rest";
        String password = "rest";

        return new BrilleappenClient(this, url, username, password);
    }

    @Override
    public void sendFileDone(BrilleappenClient client, boolean success, File file, Media media) {
        assertNotNull(media);
        //clientResult = result;
        signal.countDown();

        //testNotifyFile(result);
    }

    @Override
    public void sendFileProgress(BrilleappenClient client, File file, int current, int total) {}

    @Override
    public void getEventDone(BrilleappenClient client, boolean success, Event event) {
        //assertTrue(result.has("event"));
        signal.countDown();
    }

    @Override
    public void notifyFileDone(BrilleappenClient client, boolean success, Media media) {
//        assertTrue(result.has("notifyMessages"));
        signal.countDown();
    }
}
