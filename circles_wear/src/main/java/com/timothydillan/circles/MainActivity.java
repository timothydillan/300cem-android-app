package com.timothydillan.circles;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;


public class MainActivity extends Activity implements DataClient.OnDataChangedListener {

    private static final String TAG = "WatchOS";
    private static final String HEART_RATE_KEY = "HEART_RATE_KEY";
    private static final String STEP_COUNT_KEY = "STEP_COUNT_KEY";
    private static final String PREV_STEP_COUNT_KEY = "PREV_STEP_COUNT_KEY";
    private static final String PREV_STEP_COUNT_PATH = "/prevStepCount";
    private static final String WALK_TIME_KEY = "WALK_TIME_KEY";
    private static final String WALK_TIME_PATH = "/walkTime";
    private static final String RUN_TIME_KEY = "RUN_TIME_KEY";
    private static final String RUN_TIME_PATH = "/runTime";
    private static final String CYCLING_TIME_KEY = "CYCLING_TIME_KEY";
    private static final String CYCLING_TIME_PATH = "/cyclingTime";
    private static final String MOOD_KEY = "MOOD_KEY";
    private static final String NAME_KEY = "NAME_KEY";
    private static final String NAME_PATH = "/namePath";
    private static final String MOOD_PATH = "/moodPath";
    private static final String HANDHELD_DATA_KEY = "HANDHELD_DATA_KEY";
    public static final int FIT_REQUEST_CODE = 101;

    private boolean receivedPrevStepCount = false;
    private TextView heartEmoji;
    private TextView moodTextView;
    private TextView welcomeTextView;
    private TextView heartRateTextView;
    private TextView stepCountTextView;
    private TextView walkingTextView;
    private TextView runningTextView;
    private TextView cyclingTextView;
    private int currentStepCount;
    private long prevStepCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Assign views to their corresponding IDs
        welcomeTextView = findViewById(R.id.textView);
        heartRateTextView = findViewById(R.id.heartRateTextView);
        stepCountTextView = findViewById(R.id.stepsTextView);
        walkingTextView = findViewById(R.id.walkingTextView);
        runningTextView = findViewById(R.id.runningTextView);
        cyclingTextView = findViewById(R.id.cyclingTextView);
        heartEmoji = findViewById(R.id.heartEmojiTextView);
        moodTextView = findViewById(R.id.moodTextView);

        // We'll create an animation that increases the scale of a view (0.2 more) and then goes back to their normal size
        // this creates a pulsating effect.
        ObjectAnimator pulsatingAnim = ObjectAnimator.ofPropertyValuesHolder(heartEmoji,
                PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                PropertyValuesHolder.ofFloat("scaleY", 1.2f));
        pulsatingAnim.setDuration(300);
        pulsatingAnim.setRepeatCount(ObjectAnimator.INFINITE);
        pulsatingAnim.setRepeatMode(ObjectAnimator.REVERSE);
        pulsatingAnim.start();

        // In-case the user hasn't granted us permissions, request for it
        requestFitPermissions();

        // If the fit permissions have not been given yet, we'll return and do nothing.
        if (!hasFitPermissions()) {
            return;
        }

        // If it has been granted, we'll run the sensor service.
        if (!SensorService.isServiceRunning(this)) {
            Intent sensorService = new Intent(this, SensorService.class);
            ContextCompat.startForegroundService(this, sensorService);
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Once we receive data from the service, we'll check whether the received data is more than 0
            if (intent.getIntExtra(HEART_RATE_KEY, 0) > 0) {
                // if it is, then we'll set the value of the UI elements accordingly
                int newHeartRate = intent.getIntExtra(HEART_RATE_KEY, 0);
                // in this case, we're setting the heart rate text to the newly received value.
                setHeartRateText(newHeartRate);
            }
            // We're doing the same thing for the pedometer data
            if (intent.getIntExtra(STEP_COUNT_KEY, 0) > 0) {
                currentStepCount = intent.getIntExtra(STEP_COUNT_KEY, 0);
                setStepCountText();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // In-case the user goes out of the app and goes back in, we'll ask for permissions again
        requestFitPermissions();
        // and we'll also re-register the broadcast receiver, as well as the onDataChanged listener (from the data layer API) so that we're able
        // to receive data from the handheld device.
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(HANDHELD_DATA_KEY));
        Wearable.getDataClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // When the user leaves the app, remove all listeners and receivers since the user does not need to receive any data from the companion device
        Wearable.getDataClient(this).removeListener(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        // When we receive data,
        for (DataEvent event : dataEventBuffer) {
            // check if a data item was sent and a new value was sent
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                // and if the data sent was the previous step count of the user
                if (item.getUri().getPath().compareTo(PREV_STEP_COUNT_PATH) == 0) {
                    // Get the data,
                    prevStepCount = Long.parseLong(getData(item, PREV_STEP_COUNT_KEY));
                    // and set receivedPrevStepCount to true so that we deduct the current step count of the user
                    // with the previous step count (since the step count data doesn't reset until the device is rebooted)
                    receivedPrevStepCount = true;
                    Log.d(TAG, "Yesterday step count: " + prevStepCount);
                }
                // If the walk time data was received,
                if (item.getUri().getPath().compareTo(WALK_TIME_PATH) == 0) {
                    // get the data, and update the corresponding text
                    String walkTime = getData(item, WALK_TIME_KEY);
                    Log.d(TAG, "User walk time: " + walkTime);
                    setWalkTimeText(walkTime);
                }
                // If the run time data was received
                if (item.getUri().getPath().compareTo(RUN_TIME_PATH) == 0) {
                    // get the data, and update the corresponding text
                    String runTime = getData(item, RUN_TIME_KEY);
                    Log.d(TAG, "User running time: " + runTime);
                    setRunTimeText(runTime);
                }
                // If the cycling time data was received
                if (item.getUri().getPath().compareTo(CYCLING_TIME_PATH) == 0) {
                    // get the data, and update the corresponding text
                    String cyclingTime = getData(item, CYCLING_TIME_KEY);
                    Log.d(TAG, "User cycling time: " + cyclingTime);
                    setCyclingTimeText(cyclingTime);
                }
                // If the mood data was received
                if (item.getUri().getPath().compareTo(MOOD_PATH) == 0) {
                    // get the data, and update the corresponding text
                    String mood = getData(item, MOOD_KEY);
                    Log.d(TAG, "User mood: " + mood);
                    setMoodText(mood);
                }
                // If the user's name data was received
                if (item.getUri().getPath().compareTo(NAME_PATH) == 0) {
                    // get the data, and update the corresponding text
                    String name = getData(item, NAME_KEY);
                    Log.d(TAG, "Username: " + name);
                    setWelcomeTextView(name);
                }
            }
        }
    }

    private String getData(DataItem item, String key) {
        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
        return dataMap.getString(key);
    }

    private void setHeartRateText(int heartRate) {
        heartRateTextView.setText(heartRate + " bpm");
    }

    private void setStepCountText() {
        if (!receivedPrevStepCount && stepCountTextView != null) {
            stepCountTextView.setText(currentStepCount + " steps");
        } else if (stepCountTextView != null) {
            String actualStepCount = (currentStepCount - prevStepCount) + " steps";
            stepCountTextView.setText(actualStepCount);
        }
    }

    private void setWalkTimeText(String walkTime) {
        walkingTextView.setText(walkTime);
    }

    private void setRunTimeText(String runTime) {
        runningTextView.setText(runTime);
    }

    private void setCyclingTimeText(String cyclingTime) {
        cyclingTextView.setText(cyclingTime);
    }

    private void setMoodText(String mood) {
        moodTextView.setTextSize(30);
        moodTextView.setText(mood);
    }

    private void setWelcomeTextView(String name) {
        welcomeTextView.setTypeface(ResourcesCompat.getFont(this, R.font.dmsans_bold));
        welcomeTextView.setText("Welcome Back, " + name + ".");
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    private void requestFitPermissions() {
        if (hasFitPermissions()) {
            return;
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BODY_SENSORS)) {
            showPermissionsDialog();
        } else {
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.BODY_SENSORS}, FIT_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FIT_REQUEST_CODE && grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PermissionsRequest", permissions[i] + " granted.");
                } else {
                    // If somehow one of the permissions are denied, show a permission dialog.
                    Log.d("PermissionsRequest", permissions[i] + " denied.");
                }
            }
            // Reset the activity nonetheless
            recreate();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    private boolean hasFitPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED;
    }

    private void showPermissionsDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Circles Permissions")
                .setMessage("This app needs access to read your sensor data for it to function properly.")
                .setNegativeButton("CANCEL", (dialog, which) -> Toast.makeText(this,
                        "Well.. that's a shame.", Toast.LENGTH_LONG).show())
                .setPositiveButton("OK", (dialog, which) -> {
                    requestFitPermissions();
                });
        builder.create().show();
    }

}