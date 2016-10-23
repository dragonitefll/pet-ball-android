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
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    private UsbManager mUsbManager;
    private static WebSocketClient webSocketClient = null;
    private String token;

    public static WebSocketClient getWebSocketClient() {
        return webSocketClient;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        token = this.getIntent().getStringExtra("io.github.dragonitefll.AuthToken");

        URI uri = null;
        try {
            uri = new URI("ws://petball.ward.li:3000");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        final MainActivity mainActivity = this;
        webSocketClient = new WebSocketClient(uri) {
            VideoChatActivity videoChatActivity = null;

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
        };

        webSocketClient.connect();

        Log.e("MainActivity", token);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        setContentView(R.layout.activity_main);
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        if (deviceIterator.hasNext()) {
            try {
                UsbDevice device = deviceIterator.next();
                // Your code here!}


                UsbDeviceConnection connection = mUsbManager.openDevice(device);
                UsbSerialDevice serial = UsbSerialDevice.createUsbSerialDevice(device, connection);

                serial.open();
                serial.setBaudRate(9600);
                serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serial.setParity(UsbSerialInterface.PARITY_NONE);
                serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                serial.write("test\n".getBytes());
            } catch(Exception e) {
                Toast.makeText(MainActivity.this, "Oops", Toast.LENGTH_SHORT);
            }
        }
    }

    private void switchToVideoChat(JSONObject sdp) {
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