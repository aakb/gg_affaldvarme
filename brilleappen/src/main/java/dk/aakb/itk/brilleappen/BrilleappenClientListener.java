package dk.aakb.itk.brilleappen;

import java.io.File;

public interface BrilleappenClientListener {
    void createEventDone(BrilleappenClient client, boolean success, String url);

    void getEventDone(BrilleappenClient client, boolean success, Event event);

    void sendFileDone(BrilleappenClient client, boolean success, File file, Media media);

    void sendFileProgress(BrilleappenClient client, File file, int progress, int max);

    void notifyFileDone(BrilleappenClient client, boolean success, Media media);
}