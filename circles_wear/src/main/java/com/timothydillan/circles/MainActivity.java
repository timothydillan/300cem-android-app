package com.timothydillan.circles;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.List;


public class MainActivity extends Activity implements SensorEventListener {

    private static final String TAG = "WatchOS";
    private static final String HEART_RATE_KEY = "HEART_RATE_KEY";
    private static final String STEP_COUNT_KEY = "STEP_COUNT_KEY";
    private static final String HEART_RATE_PATH = "/heartRate";
    private static final String STEP_COUNT_PATH = "/stepCount";

    private TextView heartRateTextView;
    private SensorManager sensorManager;
    private Sensor heartRateSensor;
    private Sensor stepCounterSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        heartRateTextView = findViewById(R.id.heartRateTextView);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        Log.d(TAG, "---- SENSORS ----");
        /*D/WatchOS: ---- SENSORS ----
    Type: 1
D/WatchOS: Name: Cywee Accelerometer Sensor
    Type: 2
    Name: Cywee Magnetic field Sensor
    Type: 4
    Name: Cywee Gyroscope Sensor
D/WatchOS: Type: 21
    Name: Cywee Heart Rate
    Type: 33171030
    Name: Cywee Heart Rate Touch
    Type: 3
D/WatchOS: Name: Cywee Orientation Sensor
    Type: 9
    Name: Cywee Gravity
D/WatchOS: Type: 10
    Name: Cywee Linear Acceleration
    Type: 11
    Name: Cywee Rotation Vector
D/WatchOS: Type: 14
    Name: Magnetic Uncalibrated
    Type: 16
    Name: Gyroscope Uncalibrated
D/WatchOS: Type: 15
    Name: Game Rotation Vector
    Type: 20
D/WatchOS: Name: Geomagnetic Rotation Vector
    Type: 18
    Name: Cywee Step Detector
    Type: 19
    Name: Cywee Step Counter
D/WatchOS: Type: 17
    Name: Significant Motion
    Type: 22
    Name: Cywee Tilt detector
D/WatchOS: Type: 33170990
    Name: Cywee Shake
D/WatchOS: Type: 33170991
    Name: Cywee Tap
    Type: 33171001
    Name: Cywee Reserve Sensor A
    Type: 33171002
D/WatchOS: Name: Cywee Context awareness
    Type: 33171003
    Name: Cywee Static Detector
    Type: 65536
D/WatchOS: Name: Cywee Watch_Hand_Up_Down
    Type: 33171019
    Name: Cywee Custom Algo1
    Type: 33171020
D/WatchOS: Name: Cywee Custom Algo2
    Type: 33171025
    Name: Cywee Customized Pedometer
D/WatchOS: Type: 26
    Name: Cywee Wrist Tilt
D/WatchOS: Type: 21
D/WatchOS: Name: Cywee Heart Rate (WAKE_UP)
    Type: 3
    Name: Orientation Sensor (WAKE_UP)
D/WatchOS: Type: 9
    Name: Gravity (WAKE_UP)
    Type: 10
    Name: Linear Acceleration (WAKE_UP)
    Type: 11
D/WatchOS: Name: Rotation Vector (WAKE_UP)
    Type: 14
    Name: Magnetic Uncalibrated (WAKE_UP)
D/WatchOS: Type: 16
    Name: Gyroscope Uncalibrated (WAKE_UP)
    Type: 15
    Name: Game Rotation Vector (WAKE_UP)
    Type: 20
D/WatchOS: Name: Geomagnetic Rotation Vector (WAKE_UP)
    Type: 18
    Name: Step Detector (WAKE_UP)
    Type: 19
D/WatchOS: Name: Step Counter (WAKE_UP)
    Type: 33171001
    Name: Cywee Reserve Sensor A (WAKE_UP)
D/WatchOS: Type: 33171025
    Name: Cywee Customized Pedometer (WAKE_UP)
D/WatchOS: Type: 33171098
    Name: Cywee Info*/
        for (Sensor sensor : deviceSensors) {
            Log.d(TAG, "Type: " + String.valueOf(sensor.getType()));
            Log.d(TAG, "Name: " + sensor.getName());
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null) {
            heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_GAME);
        } else {
            Log.d(TAG, "heartRateSensor null!");
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_GAME);
        } else {
            Log.d(TAG, "stepCountSensor null!");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            int heartRate = Math.round(sensorEvent.values[0]);
            if (heartRate > 0) {
                sendDataToCompanionDevice(HEART_RATE_PATH, HEART_RATE_KEY, heartRate);
                heartRateTextView.setText(String.valueOf(heartRate + " bpm"));
            }
            Log.d(TAG, "Heart rate: " + heartRate);
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int stepCount = Math.round(sensorEvent.values[0]);
            if (stepCount > 0) {
                sendDataToCompanionDevice(STEP_COUNT_PATH, STEP_COUNT_KEY, stepCount);
            }
            Log.d(TAG, "Step count: " + stepCount);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    private void sendDataToCompanionDevice(String path, String key, int data) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(path);
        putDataMapReq.getDataMap().putInt(key, data);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        Task<DataItem> putDataTask = Wearable.getDataClient(getApplicationContext()).putDataItem(putDataReq);
        putDataTask.addOnSuccessListener(dataItem ->
                Log.d(TAG, "Successfully sent " + key + ": " + data));
        putDataTask.addOnFailureListener(e ->
                Log.d(TAG, "Failed to send new data."));
    }
}