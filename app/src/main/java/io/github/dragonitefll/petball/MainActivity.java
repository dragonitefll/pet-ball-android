package io.github.dragonitefll.petball;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;

import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;

import javax.net.ssl.SSLContext;

public class MainActivity extends AppCompatActivity {
    private static VideoChatWebSocketClient webSocketClient = null;
    private String token;

    public static VideoChatWebSocketClient getWebSocketClient() {
        return webSocketClient;
    }

    public boolean isMoving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        token = this.getIntent().getStringExtra("io.github.dragonitefll.AuthToken");

        URI uri = null;
        try {
            uri = new URI("wss://petball.ward.li:3000");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        webSocketClient = new VideoChatWebSocketClient(uri);
        webSocketClient.mainActivity = this;
        webSocketClient.token = token;

        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            webSocketClient.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));
        } catch (Exception e) {
            e.printStackTrace();
        }

        webSocketClient.connect();

        ArduinoConnection.usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        Button button = (Button) findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("MainActivity", "running demo");
                        try {
                            ArduinoConnection connection = ArduinoConnection.getInstance();
                            Thread.sleep(1000);
                            connection.setMotorSpeeds(255, 255);
                            Thread.sleep(1000);
                            connection.setMotorSpeeds(-255, -255);
                            Thread.sleep(1000);
                            connection.stopMotors(3);
                            Thread.sleep(1000);
                            connection.setMotorSpeed(0, 255);
                            Thread.sleep(2000);
                            connection.stopMotors(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }

    public void switchToVideoChat(JSONObject sdp) {
        Intent intent = new Intent(this, VideoChatActivity.class);
        intent.putExtra("io.github.dragonitefll.AuthToken", token);
        try {
            intent.putExtra("io.github.dragonitefll.sdp", sdp.getString("sdp"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        startActivity(intent);
    }
}

class VideoChatWebSocketClient extends WebSocketClient {

    public VideoChatWebSocketClient(URI serverURI) {
        super(serverURI);
    }

    public VideoChatActivity videoChatActivity = null;
    public MainActivity mainActivity = null;
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
        try {
            JSONObject data = new JSONObject(message);

            if (data.has("sdp")) {
                // Switch to VideoChatActivity
                mainActivity.switchToVideoChat(data.getJSONObject("sdp"));
            } else if (data.has("candidate")) {
                // Notify VideoChatActivity
                if (videoChatActivity != null) {
                    videoChatActivity.addIceCandidate(data.getJSONObject("candidate"));
                }
            } else if (data.has("motors"))  {
                if (videoChatActivity != null) {
                    JSONObject motors = data.getJSONObject("motors");
                    videoChatActivity.driveMotors(motors.getInt("a"), motors.getInt("b"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    @Override
    public void onError(Exception ex) {

    }
}