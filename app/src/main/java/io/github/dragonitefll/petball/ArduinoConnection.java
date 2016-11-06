package io.github.dragonitefll.petball;

import android.app.Application;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crash.FirebaseCrash;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by anli5005 on 10/28/2016.
 */

public class ArduinoConnection {
    public static boolean bitRead(int x, int n) {
        return ((x >> n) & 1) > 0;
    }

    public static int bitWrite(int x, int n, boolean bit) {
        if (bit) {
            return (int) (x + (bitRead(x, n) ? 0 : Math.pow(2, n)));
        } else {
            return (int) (x - (bitRead(x, n) ? Math.pow(2, n) : 0));
        }
    }

    private static ArduinoConnection arduinoConnection = null;

    public static ArduinoConnection getInstance() {
        if (arduinoConnection == null) {
            arduinoConnection = new ArduinoConnection();
        }
        return arduinoConnection;
    }

    public UsbSerialDevice serialDevice = null;
    public boolean isReady = true;

    public void setMotorSpeed(int motor, int speed) {
        boolean isRightMotor = motor > 0;
        int mode = bitWrite(0, 0, true);
        mode = bitWrite(mode, 3, true);

        if (isRightMotor) {
            mode = bitWrite(mode, 0, true);
            mode = bitWrite(mode, 1, true);
            mode = bitWrite(mode, 2, speed < 0);
        } else {
            mode = bitWrite(mode, 3, true);
            mode = bitWrite(mode, 4, true);
            mode = bitWrite(mode, 5, speed < 0);
        }

        int[] toSend = {1, mode, isRightMotor ? 0 : Math.abs(speed), isRightMotor ? Math.abs(speed) : 0};
        sendInts(toSend);
    }

    public void setMotorSpeeds(int a, int b) {
        int mode = bitWrite(0, 0, true);
        mode = bitWrite(mode, 3, true);

        mode = bitWrite(mode, 1, true);
        mode = bitWrite(mode, 4, true);

        mode = bitWrite(mode, 5, a < 0);
        mode = bitWrite(mode, 2, b < 0);

        Log.e("ArduinoConnection", String.valueOf(mode));

        int[] toSend = {1, mode, Math.abs(a), Math.abs(b)};
        sendInts(toSend);
    }

    public void stopMotors(int motors) {
        int mode = 0;

        if (!bitRead(motors, 0)) {
            mode = bitWrite(mode, 0, true);
        }

        if (!bitRead(motors, 1)) {
            mode = bitWrite(mode, 3, true);
        }

        int[] toSend = {1, mode, 0, 0};
        sendInts(toSend);
    }

    public void sendInts(int[] intArray) {
        byte[] bytes = new byte[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            bytes[i] = (byte) intArray[i];
        }
        if (this.serialDevice != null) {
            this.serialDevice.write(bytes);
        }
    }

    public static UsbManager usbManager = null;
    private ArduinoConnection() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        if (deviceIterator.hasNext()) {
            try {
                UsbDevice device = deviceIterator.next();
                // Your code here!}


                UsbDeviceConnection connection = usbManager.openDevice(device);
                UsbSerialDevice serial = UsbSerialDevice.createUsbSerialDevice(device, connection);

                serial.open();
                serial.setBaudRate(9600);
                serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serial.setParity(UsbSerialInterface.PARITY_NONE);
                serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

                this.serialDevice = serial;
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
