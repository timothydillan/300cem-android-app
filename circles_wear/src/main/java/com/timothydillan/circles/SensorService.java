package com.timothydillan.circles;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Random;

import static android.app.PendingIntent.FLAG_ONE_SHOT;

public class SensorService extends Service implements SensorEventListener {

    private static final String STOP_SERVICE = "STOP_SERVICE";
    private static final String CHANNEL_ID = "sensorService";
    private static final String TAG = "SensorService";
    private static final String HEART_RATE_KEY = "HEART_RATE_KEY";
    private static final String STEP_COUNT_KEY = "STEP_COUNT_KEY";
    private static final String HEART_RATE_PATH = "/heartRate";
    private static final String STEP_COUNT_PATH = "/stepCount";
    private static final String HANDHELD_DATA_KEY = "HANDHELD_DATA_KEY";
    private NotificationManager notificationManager;
    private SensorManager sensorManager;
    private Sensor heartRateSensor;
    private Sensor stepCounterSensor;
    private LocalBroadcastManager broadcastManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        initializeNotificationChannel();
        Log.d(TAG, "Sensor service started.");
        Intent stopIntent = new Intent(this, SensorService.class);
        stopIntent.setAction(STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("sensors notif")
                .setContentText("hey there. we are currently reading your sensor data in the bg.")
                .setSmallIcon(R.drawable.logo)
                .addAction(R.drawable.logo, "Stop", stopPendingIntent)
                .build();
        startForeground(new Random().nextInt(), notification);
        broadcastManager = LocalBroadcastManager.getInstance(this);
        initializeSensors();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If the stop button was clicked,
        if (TextUtils.equals(STOP_SERVICE, intent.getAction())) {
            // we'll remove the notification and stop the service.
            stopForeground(true);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(this);
    }

    private void initializeNotificationChannel() {
        notificationManager = ContextCompat.getSystemService(this, NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "circles", NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    void initializeSensors() {
        // Get the sensor manager of the device to access every sensor that the device has
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // Check if the heart rate sensor exists.
        if (sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null) {
            // If it does, assign the heartratesensor variable to the heart rate sensor
            heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            // and we'll register a listener that listens to data changes in the heart rate sensor
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_GAME);
        } else {
            Log.d(TAG, "The heart rate sensor does not exist!");
        }

        // also check whether the pedometer exists.
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            // if it does, we'll assign the pedometer/stepcountersensor to the sensor from the device
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            // similarly, we'll also register a listener that listens to data changes in the pedometer sensor.
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_GAME);
        } else {
            Log.d(TAG, "The pedometer sensor does not exist!");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // If any data from the device's sensors changed,
        // check if the received data was from the device's heart rate
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            // if it is, round the value (since the value given is a float)
            int heartRate = Math.round(event.values[0]);
            // and if it's above 0,
            if (heartRate > 0) {
                // we'll send the heart rate data to the companion (handheld device)
                sendDataToCompanionDevice(HEART_RATE_PATH, HEART_RATE_KEY, heartRate);
                // and we'll also send the data to the broadcast receiver so that we can display the data
                // on the wearable device.
                sendDataToUi(HEART_RATE_KEY, heartRate);
            }
        // else, if the data received was from the pedometer
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // we'll also round the values since the given value is a float
            int stepCount = Math.round(event.values[0]);
            // and then we'll check whether the step count data received is above 0
            if (stepCount > 0) {
                // we'll then send the data to the companion device, and send the data to the broadcast receiver.
                sendDataToCompanionDevice(STEP_COUNT_PATH, STEP_COUNT_KEY, stepCount);
                sendDataToUi(STEP_COUNT_KEY, stepCount);
            }
        }
    }

    // We don't really care whether the sensor's accuracy changed, so it'll just be here as a stub.
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void sendDataToCompanionDevice(String path, String key, int data) {
        // To send data, we need to first create a data map and put data in it.
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(path);
        // We'll put the data inside the datamap, along with its key.
        putDataMapReq.getDataMap().putInt(key, data);
        // After we've created the data map, we should put it in a PutDataRequest object
        // so that we'll be able to send the data using putDataItem.
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        // We'll set the request as urgent to make the data sync almost immediately.
        putDataReq.setUrgent();
        Task<DataItem> putDataTask = Wearable.getDataClient(getApplicationContext()).putDataItem(putDataReq);
        putDataTask.addOnSuccessListener(dataItem ->
                Log.d("WEARABLE -> APP", "Successfully sent " + key + ": " + data));
        putDataTask.addOnFailureListener(e ->
                Log.d("WEARABLE -> APP", "Failed to send new data."));
    }

    private void sendDataToUi(String key, int data) {
        // Sends data to the broadcast receiver registered, encapsulated inside an intent.
        Intent intent = new Intent(HANDHELD_DATA_KEY);
        intent.putExtra(key, data);
        broadcastManager.sendBroadcast(intent);
    }

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SensorService.class.getName().equals(service.service.getClassName())) {
                return service.foreground;
            }
        }
        return false;
    }
}
