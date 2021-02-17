package com.timothydillan.circles.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;
import com.timothydillan.circles.MainActivity;
import com.timothydillan.circles.R;
import com.timothydillan.circles.Utils.HealthUtil;

import static android.app.PendingIntent.FLAG_ONE_SHOT;

public class WearableService extends WearableListenerService {
    private static final String CHANNEL_ID = "wearableListener";
    private static final String TAG = "WearableListener";
    private static final String HEART_RATE_KEY = "HEART_RATE_KEY";
    private static final String STEP_COUNT_KEY = "STEP_COUNT_KEY";
    private static final String HEART_RATE_PATH = "/heartRate";
    private static final String STEP_COUNT_PATH = "/stepCount";
    private static final String ACTIVITY_KEY = "ACTIVITY_KEY";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent moodIntent = new Intent(this, MainActivity.class);
        moodIntent.putExtra(ACTIVITY_KEY, "health");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, moodIntent, FLAG_ONE_SHOT);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("health notif")
                .setContentText("hey there. we're checking your heart rate and step count.")
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(3, notification);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initializeNotificationChannel();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onDataChanged: " + dataEvents);
        }

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo(HEART_RATE_PATH) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    int newHeartRate = dataMap.getInt(HEART_RATE_KEY);
                    Log.d(TAG, "User heart rate: " + newHeartRate);
                    HealthUtil.updateDbCurrentUserHeartRate(newHeartRate);
                }
                if (item.getUri().getPath().compareTo(STEP_COUNT_PATH) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    int newStepCount = dataMap.getInt(STEP_COUNT_KEY);
                    Log.d(TAG, "User step count: " + newStepCount);
                    HealthUtil.updateDbCurrentStepCount(newStepCount);
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    private void initializeNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "circles", NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager notificationManager = ContextCompat.getSystemService(this, NotificationManager.class);
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }
}
