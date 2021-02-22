package com.timothydillan.circles.Utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.location.DetectedActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.timothydillan.circles.Models.User;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

// A combination of a Singleton (single instance, global point of access), facade, and an observer/listener class used for mood-related operations.
public class MoodUtil {

    private static MoodUtil instance;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy");
    private static final SimpleDateFormat ageDateFormat = new SimpleDateFormat("yyyyMMdd");
    private static final DatabaseReference databaseReference = FirebaseUtil.getDbReference();
    private static int currentUserActivity = 0;
    private static User currentUser = UserUtil.getInstance().getCurrentUser();
    private String USER_UID = FirebaseUtil.getUid();
    private WearableUtil wearableUtil;

    public static synchronized MoodUtil getInstance() {
        if (instance == null) {
            instance = new MoodUtil();
        }
        return instance;
    }

    public void initializeContext(Context ctx) {
        wearableUtil = new WearableUtil(ctx);
    }
    
    private void updateCurrentUserMoodStatus(User member, User user) {

        // If the member being updated right now is not the current user
        if (!member.equals(currentUser)) {
            // return and do nothing.
            return;
        }

        // If the current user does not have any heart rate data, return and do nothing.
        if (currentUser.getHeartRate() == null || currentUser.getHeartRate().isEmpty()) {
            return;
        }

        // If the user does have heart rate data, get the heart rate
        int userHeartRate = Integer.parseInt(currentUser.getHeartRate());

        // and check the user's current physical activity. If the user is still, or is in a vehicle,
        if (currentUserActivity == DetectedActivity.STILL || currentUserActivity == DetectedActivity.IN_VEHICLE) {
            // and if the user's body type is set to Normal,
            if (currentUser.getType().equals("Normal")) {
                // and if the user's heart rate is above or equals to 50 and below or equal to 90 (normal resting heart rate for "normal" population)
                if (userHeartRate >= 50 && userHeartRate <= 90) {
                    // then they should be neutral.
                    databaseReference.child("Users").child(USER_UID).child("mood").setValue("Neutral");
                    user.setMood("Neutral");
                }
            // however, if they are athletic,
            } else if (currentUser.getType().equals("Athletic")) {
                // and if the user's heart rate is above or equals to 35 and below or equal to 90 (normal resting heart rate for "athletic" population)
                if (userHeartRate >= 35 && userHeartRate <= 90) {
                    // then they should be neutral.
                    databaseReference.child("Users").child(USER_UID).child("mood").setValue("Neutral");
                    user.setMood("Neutral");
                }
            }

            // if the user has a heart rate above 90, and the user's current mood isn't negative
            if (userHeartRate > 90 && !currentUser.getMood().equals("Negative")) {
                // then set the user's mood to negative.
                databaseReference.child("Users").child(USER_UID).child("mood").setValue("Negative");
                user.setMood("Negative");
                // and notify all of their circle that they should check them up
                notifyNegativeMood(user);
            }

            // if the user has a heart rate above 90, and the user's current mood isn't unwell
            else if (userHeartRate < 35 && !currentUser.getMood().equals("Unwell")) {
                // then set the user's mood to unwell.
                databaseReference.child("Users").child(USER_UID).child("mood").setValue("Unwell");
                user.setMood("Unwell");
                // and notify all of their circle that they should check them up
                notifyNegativeMood(user);
            }

        } else if (currentUserActivity == DetectedActivity.WALKING) {
            // Else, if the current physical activity that the user is doing is Brisk Walking/Walking,

            /* The heart rate should be as follows:
             * https://www.healthline.com/health/brisk-walking#steps-per-minute
             * Low-end target heart rate: 220 - age * 0.5
             * High-end target heart rate: 220 -  age * 0.85 */

            // So we'll first check whether the user has entered their birth date.
            if (currentUser.getBirthDate() == null || currentUser.getBirthDate().isEmpty()) {
                return;
            }

            Date birthDate = null;
            try {
                birthDate = dateFormat.parse(currentUser.getBirthDate());
            } catch (ParseException e) {
                e.printStackTrace();
            }

            // If they have, parse their birthdate from the database,
            int birth = Integer.parseInt(ageDateFormat.format(birthDate));
            // and get their current age
            int now = Integer.parseInt(ageDateFormat.format(new Date()));
            int age = (birth - now) / 10000;

            // Then we'll set the low end and high end ranges of their ideal heart rate.
            // with - 10 and + 10 for both fields respectively just for extra precautions.
            int lowEndHeartRate = (int) ((220 - age) * 0.5) - 10;
            int highEndHeartRate = (int) ((220 - age) * 0.85) + 10;

            // Then we'll check whether the user's heart rate is above the ideal low end heart rate,
            // and whether their heart rate is below the ideal high end heart rate.
            if (userHeartRate >= lowEndHeartRate && userHeartRate <= highEndHeartRate) {
                // set their mood to neutral if their heart rate is between the range
                databaseReference.child("Users").child(USER_UID).child("mood").setValue("Neutral");
                user.setMood("Neutral");
            // but if it's not between the range
            } else {
                // and if the current user's mood is not unwell
                if (!currentUser.getMood().equals("Unwell")) {
                    // set their mood to unwell
                    databaseReference.child("Users").child(USER_UID).child("mood").setValue("Unwell");
                    user.setMood("Unwell");
                    // and notify their circle.
                    notifyNegativeMood(user);
                }
            }
        }

        // after detecting their mood, we should send the mood data to the wearable paired
        wearableUtil.sendDataToWearable(WearableUtil.MOOD_KEY, WearableUtil.MOOD_PATH, getMoodEmoji(user.getMood()));
        // and update the current user's mood
        UserUtil.updateCurrentUser(member, user);
    }

    public static String getMoodDescription(String mood) {
        /* This functions returns a description of each mood which is used for notifications
        * and for displaying purposes. */
        if (mood.equals("Neutral")) {
            return " is feeling okay!";
        } else if (mood.equals("Negative")) {
            return " may be harboring negative feelings. Cheer them up!";
        } else {
            return " may be feeling unwell. Check them out.";
        }
    }

    public static String getMoodEmoji(String mood) {
        /* This functions returns an emoji for each mood. */
        if (mood.equals("Neutral")) {
            return getEmojiByUnicode(0x1F60A);
        } else if (mood.equals("Negative")) {
            return getEmojiByUnicode(0x1F61E);
        } else {
            return getEmojiByUnicode(0x1F915);
        }
    }

    private static String getEmojiByUnicode(int unicode){
        return new String(Character.toChars(unicode));
    }

    public ArrayList<User> getMemberMoodInformation(@NonNull DataSnapshot snapshot) {
        // Create an array list to store newly updated information
        ArrayList<User> newMemberInformation = new ArrayList<>();
        // For every users in the database
        for (DataSnapshot ds : snapshot.getChildren()) {
            // and for every member in the user's current circle,
            for (User circleMembers : CircleUtil.getInstance().getCircleMembers()) {
                // check if the current user being iterated has the same uid to the current circle member being iterated
                if (ds.getKey().equals(circleMembers.getUid())) {
                    // and get the new information about the user from the database
                    User newMember = ds.getValue(User.class);
                    // if the new information about the user does not contain any heart rate data
                    if (newMember.getHeartRate() == null) {
                        // do nothing and continue to the next iteration
                        continue;
                    }
                    // but if the new information contains heart rate data, check if the heart rate or mood of the user changed
                    if (UserUtil.didUserMoodChange(circleMembers, newMember)) {
                        // and update the current user if it did change
                        UserUtil.updateCurrentUser(circleMembers, newMember);
                        // and update the current user's mood status
                        updateCurrentUserMoodStatus(circleMembers, newMember);
                    }
                    // add these new info to the list
                    newMemberInformation.add(circleMembers);
                }
            }
        }
        // and return the new updated information.
        return newMemberInformation;
    }

    public static void notifyNegativeMood(User user) {
        // For every user token in all circles that the user is registered in,
        for (String token : UserUtil.getAllTokens()) {
            // send a notification to all users, except the user.
            if (!token.equals(user.getToken())) {
                NotificationUtil.sendNotification("Circle Mood", "Hey there, it seems like " + user.getFirstName() + getMoodDescription(user.getMood()), token);
            }
        }
    }

    public static void setCurrentUserActivity(int currentActivity) {
        currentUserActivity = currentActivity;
    }

}
