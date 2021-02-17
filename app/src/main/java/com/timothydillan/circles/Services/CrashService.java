package com.timothydillan.circles.Services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

import com.google.android.gms.tasks.Task;
import com.timothydillan.circles.CrashConfirmationActivity;
import com.timothydillan.circles.MainActivity;
import com.timothydillan.circles.R;
import com.timothydillan.circles.Receivers.ActivityTransitionsReceiver;
import com.timothydillan.circles.Utils.PermissionUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.app.PendingIntent.FLAG_ONE_SHOT;

// https://developer.android.com/codelabs/activity-recognition-transition#0
public class CrashService extends Services implements CrashListener.OnCrashListener, ActivityTransitionsReceiver.ActivityChangeListener {
    private static final String CHANNEL_ID = "circlesCrashService";
    private static final String TAG = "CrashService";
    private static final String ACTIVITY_KEY = "ACTIVITY_KEY";
    private static final long VIBRATION_DURATION = 1000;

    private int currentActivity;
    private int activityMode;
    private ActivityTransitionsReceiver activityTransitionsReceiver;
    private List<ActivityTransition> activityTransitionList;
    private PendingIntent activityTransitionsPendingIntent;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private PermissionUtil permissionUtil;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        permissionUtil = new PermissionUtil(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (permissionUtil.hasFitPermissions()) {
            initializeSensors();
            initializeActivityTransitionListener();
            startCrashService();
            registerReceiver(activityTransitionsReceiver, new IntentFilter(ActivityTransitionsReceiver.TRANSITIONS_RECEIVER_ACTION));
            registerActivityListener();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onCrash() {
        if (currentActivity != DetectedActivity.IN_VEHICLE || activityMode != ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
            Log.d(TAG, "User is possibly in a crash accident!");
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(VIBRATION_DURATION);
            }
            startActivity(new Intent(this, CrashConfirmationActivity.class));
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called, removing receiver and stopping service");
        removeTransitionListener();
        unregisterReceiver(activityTransitionsReceiver);
        super.onDestroy();
    }

    @Override
    protected void initializeNotificationChannel() {
        notificationManager = ContextCompat.getSystemService(this, NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "circles", NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (CrashService.class.getName().equals(service.service.getClassName())) {
                return service.foreground;
            }
        }
        return false;
    }

    void initializeSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        new CrashListener(sensorManager, accelerometerSensor, this);
    }

    void initializeActivityTransitionListener() {
        activityTransitionList = new ArrayList<>();
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
        Intent activityIntent = new Intent(ActivityTransitionsReceiver.TRANSITIONS_RECEIVER_ACTION);
        activityTransitionsPendingIntent =
                PendingIntent.getBroadcast(this, 0, activityIntent, 0);
        activityTransitionsReceiver = new ActivityTransitionsReceiver(this);
    }

    void startCrashService() {
        Log.d(TAG, "Crash service started.");
        Intent safetyIntent = new Intent(this, MainActivity.class);
        safetyIntent.putExtra(ACTIVITY_KEY, "safety");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, safetyIntent, FLAG_ONE_SHOT);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("crash notif")
                .setContentText("hey there. currently checking if you got into a crash 🚗.")
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(2, notification);
    }

    void registerActivityListener() {
        ActivityTransitionRequest request = new ActivityTransitionRequest(activityTransitionList);

        // Register for Transitions Updates.
        Task<Void> task =
                ActivityRecognition.getClient(this)
                        .requestActivityTransitionUpdates(request, activityTransitionsPendingIntent);

        task.addOnSuccessListener(
                result -> {
                    Log.d(TAG, "Transitions Api was successfully registered.");
                });
        task.addOnFailureListener(
                e -> Log.e(TAG, "Transitions Api could NOT be registered: " + e));
    }

    void removeTransitionListener() {
        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(activityTransitionsPendingIntent)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Transitions successfully unregistered."))
                .addOnFailureListener(e -> Log.e(TAG,"Transitions could not be unregistered: " + e));
    }

    @Override
    public void onActivityChange(ActivityTransitionResult result) {
        for (ActivityTransitionEvent event : result.getTransitionEvents()) {
            String info = "Transition: " + toActivityString(event.getActivityType()) +
                    " (" + toTransitionType(event.getTransitionType()) + ")" + "   " +
                    new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
            Log.d(TAG, info);
            currentActivity = event.getActivityType();
            activityMode = event.getTransitionType();
        }
    }

    private String toActivityString(int activity) {
        switch (activity) {
            case DetectedActivity.IN_VEHICLE:
                return "VEHICLE";
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.WALKING:
                return "WALKING";
            case DetectedActivity.RUNNING:
                return "RUNNING";
            default:
                return "UNKNOWN";
        }
    }

    private String toTransitionType(int transitionType) {
        switch (transitionType) {
            case ActivityTransition.ACTIVITY_TRANSITION_ENTER:
                return "ENTER";
            case ActivityTransition.ACTIVITY_TRANSITION_EXIT:
                return "EXIT";
            default:
                return "UNKNOWN";
        }
    }
}
