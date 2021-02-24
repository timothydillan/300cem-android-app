package com.timothydillan.circles.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.timothydillan.circles.Models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HealthUtil {
    private static HealthUtil instance;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    private static final DatabaseReference databaseReference = FirebaseUtil.getDbReference();
    private static final int WEEKLY_MODE = 0;
    private static final int MONTHLY_MODE = 1;
    private static String USER_UID = FirebaseUtil.getUid();
    private User currentUser = UserUtil.getInstance().getCurrentUser();
    private long cyclingStart;
    private long runningStart;
    private long walkingStart;
    private WearableUtil wearableUtil;

    public static synchronized HealthUtil getInstance() {
        if (instance == null) {
            USER_UID = FirebaseUtil.getUid();
            instance = new HealthUtil();
        }
        return instance;
    }

    public static void removeInstance() {
        if (instance != null) {
            instance = null;
        }
    }

    public void initializeContext(Context ctx) {
        wearableUtil = new WearableUtil(ctx);
    }

    public void getActivityElapsedTime(int currentActivity, int activityMode) {
        /* This function updates each of the physical activities elapsed time in the database, and sends them to the wearable as well */
        if (currentUser == null) {
            return;
        }
        getCyclingElapsedTime(currentActivity, activityMode);
        getRunningElapsedTime(currentActivity, activityMode);
        getWalkingElapsedTime(currentActivity, activityMode);
    }

    private void getCyclingElapsedTime(int currentActivity, int activityMode) {
        // This function specifically updates the amount of cycling done by the current user.
        // If the user's activity was detected as ON_BICYCLE, and if the user just entered the activity
        if (currentActivity == DetectedActivity.ON_BICYCLE && activityMode == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
            // we'll store the current time.
            cyclingStart = System.currentTimeMillis();
        }
        // and if the user has exited the activity,
        if (currentActivity == DetectedActivity.ON_BICYCLE && activityMode == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
            // we'll get the current time and subtract it with the starting time to get the elapsed time
            long cyclingElapsed = System.currentTimeMillis() - cyclingStart;

            long previousCyclingElapsed = Long.parseLong(getActivityCount(currentUser.getStepCount(), new Date()));

            // and we'll check if the user has cycled previously at the current date,
            if (previousCyclingElapsed > 0) {
                // we'll add the new cycling time retrieved with the previous cycling time
                cyclingElapsed += previousCyclingElapsed;
            }

            // we'll then update the database with the new cycling elapsed time, and send the data to the wearable.
            databaseReference.child("Users").child(USER_UID).child("cyclingActivity").child(getDate(new Date())).setValue(String.valueOf(cyclingElapsed));
            wearableUtil.sendDataToWearable(WearableUtil.CYCLING_TIME_KEY, WearableUtil.CYCLING_TIME_PATH, millisToReadableFormat(cyclingElapsed));
        }
    }

    private void getRunningElapsedTime(int currentActivity, int activityMode) {
        // This function specifically updates the user's elapsed running time. This function works similarly
        // like the function above, with the difference that this function is concerned with the running activity of the user.
        if (currentActivity == DetectedActivity.RUNNING && activityMode == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
            runningStart = System.currentTimeMillis();
        }
        if (currentActivity == DetectedActivity.RUNNING && activityMode == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
            long runningElapsed = System.currentTimeMillis() - runningStart;
            long previousRunningElapsed = Long.parseLong(getActivityCount(currentUser.getRunningActivity(), new Date()));

            if (previousRunningElapsed > 0) {
                runningElapsed += previousRunningElapsed;
            }

            databaseReference.child("Users").child(USER_UID).child("runningActivity").child(getDate(new Date())).setValue(String.valueOf(runningElapsed));
            wearableUtil.sendDataToWearable(WearableUtil.RUN_TIME_KEY, WearableUtil.RUN_TIME_PATH, millisToReadableFormat(runningElapsed));
        }
    }

    private void getWalkingElapsedTime(int currentActivity, int activityMode) {
        // This function specifically updates the user's elapsed walking time. This function works similarly
        // like the function above, with the difference that this function is concerned with the walking activity of the user.
        if (currentActivity == DetectedActivity.WALKING && activityMode == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
            walkingStart = System.currentTimeMillis();
        }

        if (currentActivity == DetectedActivity.WALKING && activityMode == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
            long walkingElapsed = System.currentTimeMillis() - walkingStart;
            long previousWalkingElapsed = Long.parseLong(getActivityCount(currentUser.getWalkActivity(), new Date()));

            if (previousWalkingElapsed > 0) {
                walkingElapsed += previousWalkingElapsed;
            }

            databaseReference.child("Users").child(USER_UID).child("walkActivity").child(getDate(new Date())).setValue(String.valueOf(walkingElapsed));
            wearableUtil.sendDataToWearable(WearableUtil.WALK_TIME_KEY, WearableUtil.WALK_TIME_PATH, millisToReadableFormat(walkingElapsed));
        }
    }

    public ArrayList<User> getMemberHealthInformation(DataSnapshot snapshot) {
        // Create an array list to store newly updated information
        ArrayList<User> newMemberInformation = new ArrayList<>();
        // For every user retrieved,
        for (DataSnapshot ds : snapshot.getChildren()) {
            // and for every member in the user's current circle
            for (User circleMembers : CircleUtil.getInstance().getCircleMembers()) {
                // if the current user being iterated is the same as the current member being iterated
                if (ds.getKey().equals(circleMembers.getUid())) {
                    // we'll get new information about the member from the database
                    User newMember = ds.getValue(User.class);
                    // and check whether the information changed.
                    if (UserUtil.didUserHealthChange(circleMembers, newMember)) {
                        // if it did, then we'll update the information.
                        UserUtil.updateCurrentUser(circleMembers, newMember);
                    }
                    // add the information to the array list.
                    newMemberInformation.add(circleMembers);
                }
            }
        }
        // then we'll return the new updated information.
        return newMemberInformation;
    }

    public ArrayList<ArrayList<String>> getCircleHealthInformation(DataSnapshot snapshot) {
        /* This functions returns an array list of array string lists that contains the summary of the circle's health information */
        ArrayList<ArrayList<String>> newHealthInformation = new ArrayList<>();

        ArrayList<String> newStepInformation = new ArrayList<>();
        ArrayList<String> newRunningInformation = new ArrayList<>();
        ArrayList<String> newWalkingInformation = new ArrayList<>();
        ArrayList<String> newCyclingInformation = new ArrayList<>();

        long[] stepInformation = new long[3];
        long[] runningInformation = new long[3];
        long[] walkingInformation = new long[3];
        long[] cyclingInformation = new long[3];

        // For every circle member,
        for (User circleMembers : CircleUtil.getInstance().getCircleMembers()) {
            // if the data snapshot retrieved isn't null,
            if (snapshot != null) {
                // we'll get the UID of the current member being iterated
                User newMember = snapshot.child(circleMembers.getUid()).getValue(User.class);
                // and check whether their health information has changed
                if (UserUtil.didUserHealthChange(circleMembers, newMember)) {
                    // if it did change, the information will be updated
                    UserUtil.updateCurrentUser(circleMembers, newMember);
                }
            }
            // We'll then get the long arrays above and update each array with the total health information retrieved every time the loop iterates.
            getActivityInformation(stepInformation, circleMembers.getStepCount());
            getActivityInformation(runningInformation, circleMembers.getRunningActivity());
            getActivityInformation(walkingInformation, circleMembers.getWalkActivity());
            getActivityInformation(cyclingInformation, circleMembers.getCyclingActivity());
        }

        // after the loop is done, we'll loop through the long arrays, and add their information unto the string array lists.
        for (long steps : stepInformation) {
            newStepInformation.add(stepsToReadableFormat(steps));
        }

        for (long walk : walkingInformation) {
            newWalkingInformation.add(millisToReadableFormat(walk));
        }

        for (long run : runningInformation) {
            newRunningInformation.add(millisToReadableFormat(run));
        }

        for (long cycle : cyclingInformation) {
            newCyclingInformation.add(millisToReadableFormat(cycle));
        }

        // We'll then add the string arraylists to the newHealthInformation arraylist
        newHealthInformation.add(newStepInformation);
        newHealthInformation.add(newWalkingInformation);
        newHealthInformation.add(newRunningInformation);
        newHealthInformation.add(newCyclingInformation);

        // and return it.
        return newHealthInformation;
    }

    private void getActivityInformation(long[] activityList,  HashMap<String, String> activity) {
        /* This function modifies a long array by incrementing the first element with the total
         * activity count received for today, the second element with the total activity count received since last week,
         * and the third element of the array with total activity count received since last month.
        */
        if (activity == null) {
            return;
        }
        if (getActivityCount(activity, new Date()) != null) {
            activityList[0] += Long.parseLong(getActivityCount(activity, new Date()));
        }

        if (getActivityRange(activity, WEEKLY_MODE) != null) {
            activityList[1] += Long.parseLong(getActivityRange(activity, WEEKLY_MODE));
        }

        if (getActivityRange(activity, MONTHLY_MODE) != null) {
            activityList[2] += Long.parseLong(getActivityRange(activity, MONTHLY_MODE));
        }
    }

    public void updateDbCurrentUserHeartRate(int newHeartRate) {
        /* This functions updates the user's heart rate in the database */
        databaseReference.child("Users").child(USER_UID).child("heartRate").setValue(String.valueOf(newHeartRate));
    }

    public void updateDbCurrentStepCount(int newStepCount) {
        /* This function updates the user's step count in the database */

        String steps = String.valueOf(newStepCount);
        HashMap<String, String> stepCount = UserUtil.getInstance().getCurrentUser().getStepCount();
        // First, we'll check whether the user has any step count previously.
        if (stepCount != null) {
            String previousSteps = stepCount.get(getYesterdayDate());
            String currentSteps = stepCount.get(getDate(new Date()));
            // We'll then check whether there are any stepcount data for the user since yesterday.
            if (previousSteps != null && currentSteps != null) {
                if (!previousSteps.isEmpty() && !currentSteps.isEmpty()) {
                    int previousStepCount = Integer.parseInt(previousSteps);
                    // if the raw step count data from the wearable is more than the previous step count (needed because stepcount data doesn't reset until the user reboots their wearable, so data from yesterday is still kept till today).
                    if (newStepCount > previousStepCount) {
                        // then we'll subtract the user's current step count with the previous step count
                        steps = String.valueOf(newStepCount - previousStepCount);
                        // and send the data to the wearable
                        wearableUtil.sendDataToWearable(WearableUtil.PREV_STEP_COUNT_KEY, WearableUtil.PREV_STEP_COUNT_PATH, String.valueOf(previousStepCount));
                    }
                }
            }
        }
        // then we'll update the step count in the database.
        databaseReference.child("Users").child(USER_UID).child("stepCount").child(getDate(new Date())).setValue(steps);
    }

    public String getActivityCount(HashMap<String, String> activity, Date date) {
        /* This functions returns the activity count of an activity */
        if (activity == null) {
            return null;
        }
        if (activity.isEmpty()) {
            return null;
        }
        return activity.get(getDate(date));
    }

    public String getActivityRange(HashMap<String, String> activity, int range) {
        /* This functions returns the activity count of an activity in a week's range or a month's range*/

        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        start.setTime(new Date());

        switch (range) {
            // If weekly
            case 0:
                end.add(Calendar.DATE, -7);
                break;
            // If monthly
            case 1:
                end.add(Calendar.DATE, -30);
                break;
        }

        long currentActivityTime = 0;

        // While the starting date is still after the end date,
        while (start.after(end)) {
            // get the activity count for this specific date
            if (getActivityCount(activity, start.getTime()) != null) {
                // and increment the accumulator.
                currentActivityTime += Integer.parseInt(getActivityCount(activity, start.getTime()));
            }
            // Then we'll decrease one day and repeat the loop until the start date matches the end date.
            start.add(Calendar.DAY_OF_MONTH, -1);
        }
        return String.valueOf(currentActivityTime);
    }

    public String getYesterdayDate() {
        /* This function returns yesterday's date in a dd-MM-yyyy format */
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return DATE_FORMAT.format(cal.getTime());
    }

    public String getDate(Date date) {
        /* This function returns a date into the dd-MM-yyyy format */
        return DATE_FORMAT.format(date);
    }

    @SuppressLint("DefaultLocale")
    public String millisToReadableFormat(long millis) {
        /* This function converts a millisecond into several other time units,
        * such as hours, minutes, and seconds, and decides which time unit should be shown.
        * By default the seconds time unit will be returned. */
        if (TimeUnit.MILLISECONDS.toHours(millis) > 0) {
            return String.format("%d hr", TimeUnit.MILLISECONDS.toHours(millis));
        } else if (TimeUnit.MILLISECONDS.toMinutes(millis) > 0) {
            return String.format("%d min", TimeUnit.MILLISECONDS.toMinutes(millis));
        } else if (TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)) > 0) {
            return String.format("%d sec", TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
        }
        return String.format("%d sec", TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    @SuppressLint("DefaultLocale")
    public String stepsToReadableFormat(long steps) {
        /* This function returns steps into a readable format. e.g: 3 steps */
        return String.format("%d steps", steps);
    }

}
