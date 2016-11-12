package io.github.dragonitefll.petball;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

/**
 * Created by anli5005 on 11/2/2016.
 */

public class VideoChatWebSocketClient extends WebSocketClient {

    public VideoChatWebSocketClient(URI serverURI) {
        super(serverURI);
    }

    public Observer observer = null;
    public String token = null;

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        JSONObject handshake = new JSONObject();
        try {
            handshake.put("hello", "pet");
            handshake.put("token", token);
            Log.e("MainActivity", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.send(handshake.toString());
    }

    @Override
    public void onMessage(String message) {
        if (observer != null) {
            try {
                JSONObject data = new JSONObject(message);

                if (data.has("sdp")) {
                    observer.onRemoteDescription(data.getJSONObject("sdp"));
                } else if (data.has("candidate")) {
                    observer.onIceCandidate(data.getJSONObject("candidate"));
                } else if (data.has("ended")) {
                    observer.onCallEnd();
                } else if (data.has("url")) {
                    observer.onReceiveUrl(data.getString("url"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    @Override
    public void onError(Exception ex) {

    }

    public interface Observer {
        void onRemoteDescription(JSONObject sdp);
        void onIceCandidate(JSONObject candidate);
        void onCallEnd();
        void onReceiveUrl(String url);
    }
}