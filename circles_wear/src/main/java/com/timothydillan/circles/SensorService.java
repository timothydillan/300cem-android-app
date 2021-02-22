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

import static android.app.PendingIntent.FLAG_ONE_SHOT;

public class SensorService extends Service implements SensorEventListener {
    private NotificationManager notificationManager;
    private static final String CHANNEL_ID = "sensorService";
    private static final String TAG = "SensorService";
    private SensorManager sensorManager;
    private Sensor heartRateSensor;
    private Sensor stepCounterSensor;
    private static final String HEART_RATE_KEY = "HEART_RATE_KEY";
    private static final String STEP_COUNT_KEY = "STEP_COUNT_KEY";
    private static final String HEART_RATE_PATH = "/heartRate";
    private static final String STEP_COUNT_PATH = "/stepCount";
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
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("sensors notif")
                .setContentText("hey there. we are currently reading your sensor data in the bg.")
                .setSmallIcon(R.drawable.logo)
                .build();
        startForeground(999, notification);
        broadcastManager = LocalBroadcastManager.getInstance(this);
        initializeSensors();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
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
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            int heartRate = Math.round(event.values[0]);
            if (heartRate > 0) {
                sendDataToCompanionDevice(HEART_RATE_PATH, HEART_RATE_KEY, heartRate);
                sendDataToUi(HEART_RATE_KEY, heartRate);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int stepCount = Math.round(event.values[0]);
            if (stepCount > 0) {
                sendDataToCompanionDevice(STEP_COUNT_PATH, STEP_COUNT_KEY, stepCount);
                sendDataToUi(STEP_COUNT_KEY, stepCount);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

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

    private void sendDataToUi(String key, int data) {
        Intent intent = new Intent("broadcast_action");
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
