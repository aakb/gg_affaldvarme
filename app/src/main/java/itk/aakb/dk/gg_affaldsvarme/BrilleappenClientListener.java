package itk.aakb.dk.gg_affaldsvarme;

import org.json.JSONObject;

/**
 * Created by jakobrindom on 09/02/16.
 */
public interface BrilleappenClientListener {
    void sendFileDone(BrilleappenClient client, JSONObject result);

    void notifyFileDone(BrilleappenClient client, JSONObject result);
}