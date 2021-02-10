package com.timothydillan.circles.Services;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class CrashListener implements SensorEventListener {

    private static final String TAG = "CrashListener";
    private static final int SPEED_THRESHOLD = 20000;
    private static final int TIME_THRESHOLD = 70;
    private OnCrashListener crashListener;
    private float lastX = -1.0f;
    private float lastY = -1.0f;
    private float lastZ = -1.0f;
    private long lastTime;

    public CrashListener(SensorManager sensorManager, Sensor accelerometerSensor, OnCrashListener crashListener) {
        this.crashListener = crashListener;
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        long now = System.currentTimeMillis();

        // every 70 ms,
        if (now - lastTime > TIME_THRESHOLD) {
            // get the current time
            long currTime = now - lastTime;
            // event.values returns x, y, z acceleration values of the device in an array.
            // x, y, z values are in m/s^2
            // the difference between the current values, divided by the time * 10000 (1 second ^ 2) results in the speed
            float speed = Math.abs(event.values[0] + event.values[1] + event.values[2] - lastX - lastY - lastZ) / currTime * 10000;
            // if the speed is above the speed threshold
            if (speed > SPEED_THRESHOLD) {
                crashListener.onCrash();
            }

            lastTime = now;
            lastX = event.values[0];
            lastY = event.values[1];
            lastZ = event.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    public interface OnCrashListener {
        void onCrash();
    }
}
