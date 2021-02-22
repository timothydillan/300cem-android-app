package com.timothydillan.circles.Services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
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
import com.timothydillan.circles.Utils.FirebaseUtil;
import com.timothydillan.circles.Utils.HealthUtil;
import com.timothydillan.circles.Utils.WearableUtil;

import static android.app.PendingIntent.FLAG_ONE_SHOT;

public class WearableService extends WearableListenerService {
    private static final String CHANNEL_ID = "wearableListener";
    private static final String TAG = "WearableListener";
    private static final String ACTIVITY_KEY = "ACTIVITY_KEY";
    private static final String STOP_SERVICE = "STOP_SERVICE";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If the stop button was clicked,
        if (intent.getAction() != null) {
            if (intent.getAction().equals(STOP_SERVICE)) {
                // we'll remove the notification and stop the service.
                stopForeground(true);
                stopSelf();
            }
        }
        /* We'll create two intents, one allowing the user to navigate to the Health fragment, and one stopping the service. */
        Intent healthIntent = new Intent(this, MainActivity.class);
        healthIntent.putExtra(ACTIVITY_KEY, "health");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, healthIntent, FLAG_ONE_SHOT);

        Intent stopIntent = new Intent(this, WearableService.class);
        stopIntent.setAction(STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("health notif")
                .setContentText("hey there. we're checking your mood and health.")
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.logo, "Stop", stopPendingIntent);

        // And then we'll start the foreground service.
        startForeground(3, notification.build());
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initializeNotificationChannel();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        // If the firebase db reference is still null, we shouldn't receive data, since
        // the code below uses the database reference to update user data.
        if (FirebaseUtil.getDbReference() == null || FirebaseUtil.getCurrentUser() == null) {
            return;
        }
        // When we receive data,
        for (DataEvent event : dataEvents) {
            // check if a data item was sent and a new value was sent
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                // and if the data sent was the heart rate,
                if (item.getUri().getPath().compareTo(WearableUtil.HEART_RATE_PATH) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    // we'll get the value from the data map stored inside the data item,
                    int newHeartRate = dataMap.getInt(WearableUtil.HEART_RATE_KEY);
                    Log.d(TAG, "User heart rate: " + newHeartRate);
                    // and update the data in the database
                    HealthUtil.getInstance().updateDbCurrentUserHeartRate(newHeartRate);
                }
                if (item.getUri().getPath().compareTo(WearableUtil.STEP_COUNT_PATH) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    // similarly, if we receive step count data, we'll get the data and update the step count data on the DB.
                    int newStepCount = dataMap.getInt(WearableUtil.STEP_COUNT_KEY);
                    Log.d(TAG, "User step count: " + newStepCount);
                    HealthUtil.getInstance().updateDbCurrentStepCount(newStepCount);
                }
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

    public static boolean isServiceRunning(Context context) {
        /* This function checks whether the service is running */
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (WearableService.class.getName().equals(service.service.getClassName())) {
                return service.foreground;
            }
        }
        return false;
    }
}
