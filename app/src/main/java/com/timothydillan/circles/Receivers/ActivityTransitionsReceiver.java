package com.timothydillan.circles.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.gms.location.ActivityTransitionResult;
import com.timothydillan.circles.BuildConfig;

//https://developer.android.com/codelabs/activity-recognition-transition#0
public class ActivityTransitionsReceiver extends BroadcastReceiver {
    // This class is a BroadcastReceiver class that listens to activity changes.

    public static final String TRANSITIONS_RECEIVER_ACTION = "TRANSITIONS_RECEIVER_ACTION";
    private static final String TAG = "TransitionsReceiver";
    private ActivityChangeListener activityChangeListener;

    public ActivityTransitionsReceiver() { }

    public ActivityTransitionsReceiver(ActivityChangeListener listener) {
        activityChangeListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // When we receive an activity change,

        Log.d(TAG, "onReceive(): " + intent);

        if (!TextUtils.equals(TRANSITIONS_RECEIVER_ACTION, intent.getAction())) {
            return;
        }

        // and if the intent received contains an activitytransitionresult
        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            // We'll get the result, and if it's not null,
            if (activityChangeListener != null) {
                // we'll trigger the onActivityChange() event, and pass in the result.
                activityChangeListener.onActivityChange(result);
            }
        }
    }

    public interface ActivityChangeListener {
        void onActivityChange(ActivityTransitionResult result);
    }
}
