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
    public static final String TRANSITIONS_RECEIVER_ACTION =
            BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION";
    private static final String TAG = "TransitionsReceiver";
    private ActivityChangeListener activityChangeListener;

    public ActivityTransitionsReceiver() { }

    public ActivityTransitionsReceiver(ActivityChangeListener listener) {
        activityChangeListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "onReceive(): " + intent);

        if (!TextUtils.equals(TRANSITIONS_RECEIVER_ACTION, intent.getAction())) {
            return;
        }

        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            if (activityChangeListener != null) {
                activityChangeListener.onActivityChange(result);
            }
        }
    }

    public interface ActivityChangeListener {
        void onActivityChange(ActivityTransitionResult result);
    }
}
