package dk.aakb.itk.brilleappen;

import junit.framework.TestCase;

public class MediaTest extends TestCase {
    public void testConstruct() {
        String json = "{ \"status\": \"OK\", \"message\": \"Media added to event \\\"The first event\\\"\", \"media_id\": \"some-media-id\", \"notify_url\": \"http://example.com/media/notify\", \"shareMessages\": { \"twitter\": \"OK\", \"email\": \"OK\" } }";

        Media media = new Media(json);
        assertNotNull(media);
        assertEquals("some-media-id", media.id);
        assertEquals("http://example.com/media/notify", media.notifyUrl);
    }
}