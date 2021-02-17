package com.timothydillan.circles.Utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.Marker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.timothydillan.circles.Models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class HealthUtil {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    private static final String USER_UID = FirebaseUtil.getCurrentUser().getUid();
    private static final DatabaseReference databaseReference = FirebaseUtil.getDbReference();
    private static final HashMap<String, String> stepCount = UserUtil.getCurrentUser().getStepCount();
    private static final int WEEKLY_MODE = 0;
    private static final int MONTHLY_MODE = 1;

    public static ArrayList<User> getMemberHealthInformation(@NonNull DataSnapshot snapshot) {
        // Create an array list to store newly updated information
        ArrayList<User> newMemberInformation = new ArrayList<>();

        for (DataSnapshot ds : snapshot.getChildren()) {
            for (User circleMembers : LocationUtil.getMember().keySet()) {
                if (ds.getKey().equals(circleMembers.getUid())) {
                    User newMember = ds.getValue(User.class);
                    if (newMember.getHeartRate() != null && newMember.getStepCount() != null) {
                        if (UserUtil.didUserChange(circleMembers, newMember)) {
                            UserUtil.updateCurrentUser(circleMembers, newMember);
                        }
                        newMemberInformation.add(circleMembers);
                    }
                }
            }
        }
        // return the new updated information.
        return newMemberInformation;
    }

    public static ArrayList<String> getCircleHealthInformation(@NonNull DataSnapshot snapshot) {
        // Create an array list to store newly updated information
        ArrayList<String> newStepInformation = new ArrayList<>();

        int dailyStep = 0;
        int weeklyStep = 0;
        int monthlyStep = 0;

        for (User circleMembers : LocationUtil.getMember().keySet()) {
            User newMember = snapshot.child(circleMembers.getUid()).getValue(User.class);
            if (newMember.getStepCount() != null) {
                if (UserUtil.didUserChange(circleMembers, newMember)) {
                    UserUtil.updateCurrentUser(circleMembers, newMember);
                }
                dailyStep = Integer.parseInt(getSteps(circleMembers.getStepCount(), new Date()));
                weeklyStep = Integer.parseInt(getStepsRange(circleMembers.getStepCount(), WEEKLY_MODE));
                monthlyStep = Integer.parseInt(getStepsRange(circleMembers.getStepCount(), MONTHLY_MODE));
                newStepInformation.add(String.valueOf(dailyStep));
                newStepInformation.add(String.valueOf(weeklyStep));
                newStepInformation.add(String.valueOf(monthlyStep));
            }
        }

        // return the new updated information.
        return newStepInformation;
    }

    public static void updateDbCurrentUserHeartRate(int newHeartRate) {
        databaseReference.child("Users").child(USER_UID).child("heartRate").setValue(String.valueOf(newHeartRate));
    }

    public static void updateDbCurrentStepCount(int newStepCount) {
        String steps = String.valueOf(newStepCount);
        String previousSteps = stepCount.get(getYesterdayDate());
        String currentSteps = stepCount.get(getDate(new Date()));
        if (previousSteps != null && currentSteps != null) {
            if (!previousSteps.isEmpty() && !currentSteps.isEmpty()) {
                int previousStepCount = Integer.parseInt(previousSteps);
                if (newStepCount > previousStepCount) {
                    steps = String.valueOf(newStepCount - previousStepCount);
                }
            }
        }
        databaseReference.child("Users").child(USER_UID).child("stepCount").child(getDate(new Date())).setValue(steps);
    }

    public static String getSteps(HashMap<String, String> stepCount, Date date) {
        if (stepCount == null) {
            return null;
        }
        if (stepCount.isEmpty()) {
            return null;
        }
        return stepCount.get(getDate(date));
    }

    public static String getStepsRange(HashMap<String, String> stepCount, int range) {
        Calendar start = Calendar.getInstance();
        start.setTime(new Date());

        Calendar end = Calendar.getInstance();

        switch (range) {
            // if weekly
            case 0:
                end.add(Calendar.DATE, -7);
                break;
            // if monthly
            case 1:
                end.add(Calendar.DATE, -30);
                break;
        }

        Log.d("END", String.valueOf(end.getTime()));
        Log.d("START", String.valueOf(start.getTime()));

        int currentStepCount = 0;

        while (start.after(end)) {
            if (getSteps(stepCount, start.getTime()) != null) {
                currentStepCount += Integer.parseInt(getSteps(stepCount, start.getTime()));
            }
            //add one day to date
            start.add(Calendar.DAY_OF_MONTH, -1);
        }

        return String.valueOf(currentStepCount);
    }

    public static String getYesterdayDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return DATE_FORMAT.format(cal.getTime());
    }

    public static String getDate(Date date) {
        return DATE_FORMAT.format(date);
    }

}
