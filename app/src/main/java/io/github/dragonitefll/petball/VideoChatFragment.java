package io.github.dragonitefll.petball;

import android.opengl.GLSurfaceView;
import android.support.annotation.NonNull;
import android.app.Fragment;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.videoengine.VideoCaptureAndroid;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class VideoChatFragment extends Fragment {

    VideoRenderer renderer = null;

    GLSurfaceView videoView = null;

    PeerConnection peerConnection = null;
    PeerConnectionFactory peerConnectionFactory;

    MediaConstraints mediaConstraints = new MediaConstraints();
    MediaStream stream;

    String authToken;

    public Observer observer;

    private int motorSpeedA = 0;
    private int motorSpeedB = 0;

    private boolean connected = false;

    static PeerConnection.IceServer[] iceServers = {
            new PeerConnection.IceServer("stun:stun.l.google.com:19302")
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (peerConnectionFactory == null) {
            PeerConnectionFactory.initializeAndroidGlobals(getActivity().getApplicationContext(), true, true, true, null);
            peerConnectionFactory = new PeerConnectionFactory();

            VideoCapturer capture = VideoCapturerAndroid.create(VideoCapturerAndroid.getNameOfFrontFacingDevice());
            VideoSource videoSource = peerConnectionFactory.createVideoSource(capture, mediaConstraints);
            VideoTrack localVideoTrack = peerConnectionFactory.createVideoTrack("petVideoTrack", videoSource);

            AudioSource audioSource = peerConnectionFactory.createAudioSource(mediaConstraints);
            AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack("petAudioTrack", audioSource);

            stream = peerConnectionFactory.createLocalMediaStream("petMediaStream");
            stream.addTrack(localVideoTrack);
            stream.addTrack(localAudioTrack);
        }
    }


    public void createPeerConnection() {
        peerConnection = peerConnectionFactory.createPeerConnection(Arrays.asList(iceServers), mediaConstraints, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {

            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                    connected = true;
                } else if (connected && observer != null) {
                    connected = false;
                    peerConnection = null;
                    observer.onCallEnd();
                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {

            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                if (iceCandidate != null) {
                    JSONObject candidate = new JSONObject();
                    try {
                        candidate.put("candidate", iceCandidate.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        candidate.put("token", authToken);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    MainActivity.getWebSocketClient().send(candidate.toString());
                }
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                try {
                    renderer = VideoRendererGui.createGui(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
                    mediaStream.videoTracks.getFirst().addRenderer(renderer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {

            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                dataChannel.registerObserver(new DataChannel.Observer() {
                    @Override
                    public void onBufferedAmountChange(long l) {

                    }

                    @Override
                    public void onStateChange() {

                    }

                    @Override
                    public void onMessage(DataChannel.Buffer buffer) {
                        byte[] bytes = new byte[buffer.data.remaining()];
                        buffer.data.get(bytes);
                        String data = new String(bytes, Charset.forName("UTF-8"));
                        try {
                            JSONObject json = new JSONObject(data);
                            if (json.has("motors")) {
                                JSONObject motors = json.getJSONObject("motors");
                                driveMotors(motors.getInt("a"), motors.getInt("b"));
                            } else if (json.has("laser")) {
                                if (json.getBoolean("laser")) {
                                    Log.i("VideoChatFragment", "Laser on");
                                    ArduinoConnection.getInstance().turnLaserOn();
                                } else {
                                    Log.i("VideoChatFragment", "Laser off");
                                    ArduinoConnection.getInstance().turnLaserOff();
                                }
                            } else if (json.has("treat")) {
                                ArduinoConnection.getInstance().dispenseTreat();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onRenegotiationNeeded() {

            }
        });

        peerConnection.addStream(stream);
    }

    public void close() {
        if (peerConnection != null) {
            peerConnection.close();
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        videoView = new GLSurfaceView(this.getActivity().getApplicationContext());
        videoView.setMinimumWidth(container.getWidth());
        videoView.setMinimumHeight(container.getHeight());
        videoView.setX(0);
        videoView.setY(0);
        videoView.setBackgroundColor(0x000000);


        try {
            VideoRendererGui.setView(videoView, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return videoView;
    }

    public void onRemoteDescription(JSONObject sdp) {
        Log.d("VideoChatFragment", "Received remote description");
        try {
            Log.d("VideoChatFragment", "Setting remote description");
            peerConnection.setRemoteDescription(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {

                }

                @Override
                public void onSetSuccess() {
                    Log.d("VideoChatFragment", "Creating answer");
                    peerConnection.createAnswer(new SdpObserver() {
                        @Override
                        public void onCreateSuccess(final SessionDescription sessionDescription) {
                            Log.d("VideoChatFragment", "Setting local description");
                            peerConnection.setLocalDescription(new SdpObserver() {
                                @Override
                                public void onCreateSuccess(SessionDescription sessionDescription) {

                                }

                                @Override
                                public void onSetSuccess() {
                                    Log.d("VideoChatFragment", "Sending answer");
                                    JSONObject answer = new JSONObject();
                                    JSONObject sdp = new JSONObject();
                                    try {
                                        sdp.put("sdp", peerConnection.getLocalDescription().description);
                                        sdp.put("type", peerConnection.getLocalDescription().type.canonicalForm());
                                        answer.put("sdp", sdp);
                                        answer.put("token", authToken);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    MainActivity.getWebSocketClient().send(answer.toString());
                                }

                                @Override
                                public void onCreateFailure(String s) {

                                }

                                @Override
                                public void onSetFailure(String s) {

                                }
                            }, sessionDescription);
                        }

                        @Override
                        public void onSetSuccess() {

                        }

                        @Override
                        public void onCreateFailure(String s) {
                        }

                        @Override
                        public void onSetFailure(String s) {

                        }
                    }, mediaConstraints);
                }

                @Override
                public void onCreateFailure(String s) {

                }

                @Override
                public void onSetFailure(String s) {
                    Log.e("VideoChatFragment", "Could not set remote description");
                    Log.e("VideoChatFragment", s);
                }
            }, new SessionDescription(SessionDescription.Type.OFFER, sdp.getString("sdp")));
            Log.d("VideoChatFragment", "hi");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addIceCandidate(JSONObject candidate) {
        try {
            peerConnection.addIceCandidate(new IceCandidate(candidate.getString("sdpMid"), candidate.getInt("sdpMLineIndex"), candidate.getString("candidate")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void driveMotors(int a, int b) {
        if (motorSpeedA != a || motorSpeedB != b) {
            ArduinoConnection connection = ArduinoConnection.getInstance();
            if (a == 0 & b == 0) {
                connection.stopMotors(3);
            } else {
                connection.setMotorSpeeds(a, b);
            }
            motorSpeedA = a;
            motorSpeedB = b;
        }
    }

    public interface Observer {
        void onCallEnd();
    }
}
