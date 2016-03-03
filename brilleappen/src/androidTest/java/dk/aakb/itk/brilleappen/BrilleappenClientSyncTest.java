package dk.aakb.itk.brilleappen;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class BrilleappenClientSyncTest extends TestCase {
    public void testCreateEventSync() throws Exception {
        BrilleappenClient client = createClient(serviceBaseUrl + "/brilleappen/event/create");
        String url = client.createEventSync(new Date().toString(), "test");
        assertNotNull(url);

        _testGetEventSync(url);
    }

    public void _testGetEventSync(String url) throws Exception {
        BrilleappenClient client = createClient(url);
        Event event = client.getEventSync();

        assertNotNull(event);
        assertNotNull(event.title);
        assertNotNull(event.addFileUrl);

        _testSendFileSync(event);
    }

    public void _testSendFileSync(Event event) throws Exception {
        BrilleappenClient client = createClient(event.addFileUrl);

        File file = getImageFile();

        boolean share = false;

        Media media = client.sendFileSync(file, share);
        assertNotNull(media);
        assertNotNull(media.id);
        assertNotNull(media.notifyUrl);

        _testNotifyFileSync(media);
    }

    public void _testNotifyFileSync(Media media) throws Exception {
        BrilleappenClient client = createClient(media.notifyUrl);
        boolean success = client.notifyFileSync(media);
        assertTrue(success);
    }

    private final String serviceBaseUrl = "http://brilleappen.hulk.aakb.dk";

    private BrilleappenClient createClient() {
        return createClient(serviceBaseUrl + "/brilleappen/event/21d59ce2-fa69-426a-bd06-6e6c9edee086/file");
    }

    private BrilleappenClient createClient(String url) {
        String username = "rest";
        String password = "rest";

        return new BrilleappenClient(url, username, password);
    }

    private File getImageFile() {
        try {
            File file = File.createTempFile("test", ".png");

            if (true) {
                FileOutputStream fos = new FileOutputStream(file);

                byte[] buffer = new byte[1024 * 1024];
                fos.write(buffer, 0, buffer.length);
                fos.close();

                return file;
            } else {
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
            }
        } catch (Exception ex) {
            fail(ex.getMessage() + "; Cannot write data to file");
        }
        return null;
    }
}