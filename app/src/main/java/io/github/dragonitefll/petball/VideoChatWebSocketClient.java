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

    public VideoChatWebSocketObserver observer = null;
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
}

abstract class VideoChatWebSocketObserver {
    abstract void onRemoteDescription(JSONObject sdp);
    abstract void onIceCandidate(JSONObject candidate);
    abstract void onCallEnd();
}