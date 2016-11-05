package io.github.dragonitefll.petball;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.SSLContext;

public class MainActivity extends AppCompatActivity {
    private static VideoChatWebSocketClient webSocketClient = null;
    private String token;

    public static VideoChatWebSocketClient getWebSocketClient() {
        return webSocketClient;
    }

    private VideoChatFragment videoChatFragment;
    private boolean inVideoChat = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoChatFragment = new VideoChatFragment();

        token = this.getIntent().getStringExtra("io.github.dragonitefll.AuthToken");

        videoChatFragment.authToken = token;

        URI uri = null;
        try {
            uri = new URI("wss://petball.ward.li:3000");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        webSocketClient = new VideoChatWebSocketClient(uri);
        webSocketClient.observer = new VideoChatWebSocketObserver() {
            @Override
            void onRemoteDescription(JSONObject sdp) {
                final JSONObject SDP = sdp;
                Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!inVideoChat) {
                            inVideoChat = true;
                            FragmentTransaction transaction = getFragmentManager().beginTransaction();
                            transaction.replace(android.R.id.content, videoChatFragment);
                            transaction.commit();
                            getFragmentManager().executePendingTransactions();
                            videoChatFragment.createPeerConnection();
                        }
                        videoChatFragment.onRemoteDescription(SDP);
                    }
                };
                mainHandler.post(runnable);
            }

            @Override
            void onIceCandidate(JSONObject candidate) {
                videoChatFragment.addIceCandidate(candidate);
            }
        };
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

        /*Button button = (Button) findViewById(R.id.button2);
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
                            connection.setMotorSpeeds(128, 128);
                            Thread.sleep(1000);
                            connection.setMotorSpeeds(-128, -128);
                            Thread.sleep(1000);
                            connection.setMotorSpeeds(128, -128);
                            Thread.sleep(1000);
                            connection.setMotorSpeeds(-128, 128);
                            Thread.sleep(1000);
                            connection.stopMotors(3);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });*/
    }
}