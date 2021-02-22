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
    private static final String HEART_RATE_PATH = "/heartRate";
    private static final String STEP_COUNT_PATH = "/stepCount";
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
    private SensorManager sensorManager;
    private Sensor heartRateSensor;
    private Sensor stepCounterSensor;
    private int currentStepCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        welcomeTextView = findViewById(R.id.textView);
        heartRateTextView = findViewById(R.id.heartRateTextView);
        stepCountTextView = findViewById(R.id.stepsTextView);
        walkingTextView = findViewById(R.id.walkingTextView);
        runningTextView = findViewById(R.id.runningTextView);
        cyclingTextView = findViewById(R.id.cyclingTextView);
        heartEmoji = findViewById(R.id.heartEmojiTextView);
        moodTextView = findViewById(R.id.moodTextView);

        ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(heartEmoji,
                PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                PropertyValuesHolder.ofFloat("scaleY", 1.2f));
        scaleDown.setDuration(300);

        scaleDown.setRepeatCount(ObjectAnimator.INFINITE);
        scaleDown.setRepeatMode(ObjectAnimator.REVERSE);
        scaleDown.start();

        requestFitPermissions();

        if (!hasFitPermissions()) {
            return;
        }

        if (!SensorService.isServiceRunning(this)) {
            Intent sensorService = new Intent(this, SensorService.class);
            ContextCompat.startForegroundService(this, sensorService);
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getIntExtra(HEART_RATE_KEY, 0) > 0) {
                int newHeartRate = intent.getIntExtra(HEART_RATE_KEY, 0);
                setHeartRateText(newHeartRate);
            }
            if (intent.getIntExtra(STEP_COUNT_KEY, 0) > 0) {
                currentStepCount = intent.getIntExtra(STEP_COUNT_KEY, 0);
                setStepCountText();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("broadcast_action"));
        Wearable.getDataClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getDataClient(this).removeListener(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo(PREV_STEP_COUNT_PATH) == 0) {
                    long prevStepCount = Long.parseLong(getData(item, PREV_STEP_COUNT_KEY));
                    receivedPrevStepCount = true;
                    Log.d(TAG, "Yesterday step count: " + prevStepCount);
                    setStepCountText(prevStepCount);
                }
                if (item.getUri().getPath().compareTo(WALK_TIME_PATH) == 0) {
                    String walkTime = getData(item, WALK_TIME_KEY);
                    Log.d(TAG, "User walk time: " + walkTime);
                    setWalkTimeText(walkTime);
                }
                if (item.getUri().getPath().compareTo(RUN_TIME_PATH) == 0) {
                    String runTime = getData(item, RUN_TIME_KEY);
                    Log.d(TAG, "User running time: " + runTime);
                    setRunTimeText(runTime);
                }
                if (item.getUri().getPath().compareTo(CYCLING_TIME_PATH) == 0) {
                    String cyclingTime = getData(item, CYCLING_TIME_KEY);
                    Log.d(TAG, "User cycling time: " + cyclingTime);
                    setCyclingTimeText(cyclingTime);
                }
                if (item.getUri().getPath().compareTo(MOOD_PATH) == 0) {
                    String mood = getData(item, MOOD_KEY);
                    Log.d(TAG, "User mood: " + mood);
                    setMoodText(mood);
                }
                if (item.getUri().getPath().compareTo(NAME_PATH) == 0) {
                    String name = getData(item, NAME_KEY);
                    Log.d(TAG, "Username: " + name);
                    setWelcomeTextView(name);
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    private String getData(DataItem item, String key) {
        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
        return dataMap.getString(key);
    }

    public void setHeartRateText(int heartRate) {
        heartRateTextView.setText(heartRate + " bpm");
    }

    public void setStepCountText(long prevStepCount) {
        String actualStepCount = String.valueOf(currentStepCount - prevStepCount) + " steps";
        stepCountTextView.setText(actualStepCount);
    }

    public void setStepCountText() {
        if (!receivedPrevStepCount && stepCountTextView != null) {
            stepCountTextView.setText(String.valueOf(currentStepCount) + " steps");
        }
    }

    public void setWalkTimeText(String walkTime) {
        walkingTextView.setText(walkTime);
    }

    public void setRunTimeText(String runTime) {
        runningTextView.setText(runTime);
    }

    public void setCyclingTimeText(String cyclingTime) {
        cyclingTextView.setText(cyclingTime);
    }

    public void setMoodText(String mood) {
        moodTextView.setTextSize(30);
        moodTextView.setText(mood);
    }

    public void setWelcomeTextView(String name) {
        welcomeTextView.setTypeface(ResourcesCompat.getFont(this, R.font.dmsans_bold));
        welcomeTextView.setText("Welcome Back, " + name + ".");
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public void requestFitPermissions() {
        if (hasFitPermissions()) {
            return;
        }
        ActivityCompat.requestPermissions((Activity)this, new String[]
                    {Manifest.permission.BODY_SENSORS}, FIT_REQUEST_CODE);
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
                    showPermissionsDialog(permissions[i]);
                }
            }
            // If everything goes well, reset the map, and "restart" the fragment.
            recreate();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public boolean hasFitPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED;
    }

    public void showPermissionsDialog(String permission) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.Theme_AppCompat_DayNight_Dialog);
        builder.setTitle("Circles Permissions")
                .setMessage("This app needs access to " + permission + " for it to function properly.")
                .setNegativeButton("CANCEL", (dialog, which) -> Toast.makeText(this,
                        "Well.. that's a shame.", Toast.LENGTH_LONG).show())
                .setPositiveButton("OK", (dialog, which) -> {
                    requestFitPermissions();
                });
        builder.create().show();
    }

}