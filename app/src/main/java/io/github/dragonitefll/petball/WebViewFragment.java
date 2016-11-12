package io.github.dragonitefll.petball;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

/**
 * Created by anli5005 on 11/11/16.
 */

public class WebViewFragment extends Fragment {
    WebView webView = null;

    static String defaultUrl = "about:blank";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        webView = new WebView(getActivity().getApplicationContext());
        webView.setX(0);
        webView.setY(0);
        webView.setBackgroundColor(0xFFFFFF);
        webView.setMinimumWidth(container.getWidth());
        webView.setMinimumHeight(container.getHeight());

        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new PetBallJavascriptInterface(), "petball");

        loadDefault();

        return webView;
    }

    public void loadUrl(String url) {
        final String u = url;
        if (webView != null) {
            new Handler(getActivity().getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    webView.loadUrl(u);
                }
            });
        }
        ArduinoConnection.getInstance().stopMotors(3);
    }

    public void loadDefault() {
        loadUrl(defaultUrl);
    }

    private class PetBallJavascriptInterface {
        ArduinoConnection arduinoConnection;

        public PetBallJavascriptInterface(ArduinoConnection connection) {
            arduinoConnection = connection;
        }

        public PetBallJavascriptInterface() {
            this(ArduinoConnection.getInstance());
        }

        @JavascriptInterface
        public String version() {
            return "0.1";
        }

        @JavascriptInterface
        public void startMotor(String motor, int speed) {
            if (motor == "a") {
                arduinoConnection.setMotorSpeed(0, speed);
            } else if (motor == "b") {
                arduinoConnection.setMotorSpeed(1, speed);
            }
        }

        @JavascriptInterface
        public void startBothMotors(int a, int b) {
            arduinoConnection.setMotorSpeeds(a, b);
        }

        @JavascriptInterface
        public void stopMotors(boolean a, boolean b) {
            if (a && b) {
                arduinoConnection.stopMotors(3);
            } else if (a) {
                arduinoConnection.stopMotors(1);
            } else if (b) {
                arduinoConnection.stopMotors(2);
            }
        }

        private boolean laserState = false;

        @JavascriptInterface
        public boolean laser() {
            return laserState;
        }

        @JavascriptInterface
        public void setLaser(boolean state) {
            laserState = state;
            if (laserState) {
                arduinoConnection.turnLaserOn();
            } else {
                arduinoConnection.turnLaserOff();
            }
        }

        @JavascriptInterface
        public void moveLaser(int pos) {
            arduinoConnection.moveLaser(pos);
        }

        @JavascriptInterface
        public void dispenseTreat() {
            arduinoConnection.dispenseTreat();
        }
    }
}
