package com.timothydillan.circles.Utils;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class WearableUtil {
    // Constants used to send and receive data from the wearable and the handheld device.
    // These constants are used to ensure that only data with the key and paths below will be received, kind of like a filter.
    public static final String PREV_STEP_COUNT_KEY = "PREV_STEP_COUNT_KEY";
    public static final String PREV_STEP_COUNT_PATH = "/prevStepCount";
    public static final String WALK_TIME_KEY = "WALK_TIME_KEY";
    public static final String WALK_TIME_PATH = "/walkTime";
    public static final String RUN_TIME_KEY = "RUN_TIME_KEY";
    public static final String RUN_TIME_PATH = "/runTime";
    public static final String CYCLING_TIME_KEY = "CYCLING_TIME_KEY";
    public static final String CYCLING_TIME_PATH = "/cyclingTime";
    public static final String MOOD_KEY = "MOOD_KEY";
    public static final String MOOD_PATH = "/moodPath";
    public static final String NAME_KEY = "NAME_KEY";
    public static final String NAME_PATH = "/namePath";
    public static final String HEART_RATE_KEY = "HEART_RATE_KEY";
    public static final String STEP_COUNT_KEY = "STEP_COUNT_KEY";
    public static final String HEART_RATE_PATH = "/heartRate";
    public static final String STEP_COUNT_PATH = "/stepCount";

    private Context context;

    public WearableUtil(Context ctx) {
        context = ctx;
    }

    /* Function used to send any data from the handheld device */
    public void sendDataToWearable(String key, String path, String data) {
        if (context == null) {
            return;
        }
        // To send data, we need to first create a data map and put data in it.
        PutDataMapRequest dataMap = PutDataMapRequest.create(path);
        // We'll put in the time value inside the data that we're going to send to make the data "unique".
        // Because putDataItem won't work if the data sent is the same, we need to do this.
        dataMap.getDataMap().putLong("Time", System.currentTimeMillis());
        dataMap.getDataMap().putString(key, data);
        // After we've created the data map, we should put it in a PutDataRequest object
        // so that we'll be able to send the data using putDataItem.
        PutDataRequest request = dataMap.asPutDataRequest();
        // We'll set the request as urgent to make the data sync almost immediately.
        request.setUrgent();
        Task<DataItem> sendDataTask = Wearable.getDataClient(context).putDataItem(request);
        sendDataTask.addOnSuccessListener(dataItem ->
                Log.d("APP -> WEARABLE", "Successfully sent " + key + ": " + data));
        sendDataTask.addOnFailureListener(e ->
                Log.d("APP -> WEARABLE", "Failed to send new data."));
    }

}
