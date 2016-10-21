package io.github.dragonitefll.petball;

import android.opengl.GLSurfaceView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.videoengine.VideoCaptureAndroid;

public class VideoChatActivity extends AppCompatActivity {

    VideoRenderer renderer = null;
    VideoTrack localVideoTrack = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);
        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true, null);
        PeerConnectionFactory factory = new PeerConnectionFactory();

        MediaConstraints videoConstraints = new MediaConstraints();
        MediaConstraints audioConstraints = new MediaConstraints();

        VideoCapturer capture = VideoCapturerAndroid.create(VideoCapturerAndroid.getNameOfFrontFacingDevice());
        VideoSource videoSource = factory.createVideoSource(capture, videoConstraints);
        localVideoTrack = factory.createVideoTrack("dogVideoTrack", videoSource);

        AudioSource audioSource = factory.createAudioSource(audioConstraints);
        AudioTrack localAudioTrack = factory.createAudioTrack("dogAudioTrack", audioSource);

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
    }

    protected void onStart() {
        super.onStart();
        localVideoTrack.addRenderer(renderer);
    }
}
