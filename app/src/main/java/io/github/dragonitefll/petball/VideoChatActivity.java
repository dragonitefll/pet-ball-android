package io.github.dragonitefll.petball;

import android.opengl.GLSurfaceView;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class VideoChatActivity extends AppCompatActivity {

    VideoRenderer renderer = null;
    VideoTrack localVideoTrack = null;

    PeerConnection peerConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);
        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true, null);
        PeerConnectionFactory factory = new PeerConnectionFactory();

        final MediaConstraints constraints = new MediaConstraints();

        VideoCapturer capture = VideoCapturerAndroid.create(VideoCapturerAndroid.getNameOfFrontFacingDevice());
        VideoSource videoSource = factory.createVideoSource(capture, constraints);
        localVideoTrack = factory.createVideoTrack("petVideoTrack", videoSource);

        AudioSource audioSource = factory.createAudioSource(constraints);
        AudioTrack localAudioTrack = factory.createAudioTrack("petAudioTrack", audioSource);

        MediaStream stream = factory.createLocalMediaStream("petMediaStream");
        stream.addTrack(localVideoTrack);
        stream.addTrack(localAudioTrack);

        GLSurfaceView videoView = new GLSurfaceView(this);
        ViewGroup contentView = (ViewGroup)findViewById(android.R.id.content);
        contentView.addView(videoView);
        videoView.setX(0);
        videoView.setY(0);
        videoView.setMinimumWidth(contentView.getWidth());
        videoView.setMinimumHeight(contentView.getHeight());
        videoView.setBackgroundColor(0x000000);

        VideoRendererGui.setView(videoView, null);
        try {
            renderer = VideoRendererGui.createGui(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FIT, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        peerConnection = factory.createPeerConnection(Collections.<PeerConnection.IceServer>emptyList(), constraints, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {

            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

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
                        candidate.put("token", getIntent().getStringExtra("io.github.dragonitefll.AuthToken"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    MainActivity.getWebSocketClient().send(candidate.toString());
                }
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                localVideoTrack.removeRenderer(renderer);
                mediaStream.videoTracks.getFirst().addRenderer(renderer);
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                finish();
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {

            }

            @Override
            public void onRenegotiationNeeded() {

            }
        });

        peerConnection.addStream(stream);

        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

            }

            @Override
            public void onSetSuccess() {
                peerConnection.createAnswer(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(final SessionDescription sessionDescription) {
                        peerConnection.setLocalDescription(new SdpObserver() {
                            @Override
                            public void onCreateSuccess(SessionDescription sessionDescription) {

                            }

                            @Override
                            public void onSetSuccess() {
                                JSONObject answer = new JSONObject();
                                JSONObject sdp = new JSONObject();
                                try {
                                    sdp.put("sdp", peerConnection.getLocalDescription().description);
                                    sdp.put("type", peerConnection.getLocalDescription().type.canonicalForm());
                                    answer.put("sdp", sdp);
                                    answer.put("token", getIntent().getStringExtra("io.github.dragonitefll.AuthToken"));
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
                }, constraints);
            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        }, new SessionDescription(SessionDescription.Type.OFFER, getIntent().getStringExtra("io.github.dragonitefll.sdp")));
    }

    public void addIceCandidate(JSONObject candidate) {
        try {
            peerConnection.addIceCandidate(new IceCandidate(candidate.getString("sdpMid"), candidate.getInt("sdpMLineIndex"), candidate.getString("candidate")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void driveMotors(int a, int b) {
        ArduinoConnection connection = ArduinoConnection.getInstance();
        Log.e("VideoChatActivity", "hi");
        if (a == 0 & b == 0) {
            connection.stopMotors(3);
        } else {
            connection.setMotorSpeeds(a, b);
        }
    }

    public void onResume() {
        super.onResume();
        MainActivity.getWebSocketClient().videoChatActivity = this;
    }

    public void onPause() {
        super.onPause();
        MainActivity.getWebSocketClient().videoChatActivity = null;
    }
}
