package com.timothydillan.circles.Utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.timothydillan.circles.Models.User;

public class UserUtil {
    private UsersListener listener = null;
    private static final String TAG = "UserUtil";
    private static final String USER_UID = FirebaseUtil.getCurrentUser().getUid();
    private static final DatabaseReference databaseReference = FirebaseUtil.getDbReference();
    public static User currentUser = null;

    public void addEventListener(UsersListener listener) {
        this.listener = listener;
        addUsersChangeListener();
    }

    public void initializeCurrentUser() {
        if (currentUser != null) {
            assert listener != null;
            listener.onUserReady();
            return;
        }
        // To initialize the circle, we must first get the current user's details.
        databaseReference.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // After successfully getting the current user's details,
                currentUser = snapshot.child(USER_UID).getValue(User.class);
                assert listener != null;
                listener.onUserReady();
                Log.d(TAG, "Successfully retrieved current user.");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "initializeCurrentUser:failure", error.toException());
            }
        });
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void updateCurrentUser(User user) {
        if (!didUserChange(user)) {
            return;
        }
        currentUser.setFirstName(user.getFirstName());
        currentUser.setLastName(user.getLastName());
        currentUser.setBirthDate(user.getBirthDate());
        currentUser.setEmail(user.getEmail());
        currentUser.setGender(user.getGender());
        currentUser.setPhone(user.getPhone());
        currentUser.setProfilePicUrl(user.getProfilePicUrl());
    }

    public static void updateCurrentUser(User oldUser, User newUser) {
        oldUser.setFirstName(newUser.getFirstName());
        oldUser.setLastName(newUser.getLastName());
        oldUser.setBirthDate(newUser.getBirthDate());
        oldUser.setEmail(newUser.getEmail());
        oldUser.setGender(newUser.getGender());
        oldUser.setPhone(newUser.getPhone());
        oldUser.setLatitude(newUser.getLatitude());
        oldUser.setLongitude(newUser.getLongitude());
        oldUser.updateLastSharingTime();
        oldUser.setProfilePicUrl(newUser.getProfilePicUrl());
    }

    public static boolean didUserChange(User newUser) {
        return !currentUser.getFirstName().equals(newUser.getFirstName())
                || !currentUser.getLastName().equals(newUser.getLastName())
                || !currentUser.getBirthDate().equals(newUser.getBirthDate())
                || !currentUser.getEmail().equals(newUser.getEmail())
                || !currentUser.getGender().equals(newUser.getGender())
                || !currentUser.getPhone().equals(newUser.getPhone())
                || !currentUser.getProfilePicUrl().equals(newUser.getProfilePicUrl());
    }

    public static boolean didUserChange(User oldUser, User newUser) {
        return !oldUser.getFirstName().equals(newUser.getFirstName())
                || !oldUser.getLastName().equals(newUser.getLastName())
                || !oldUser.getBirthDate().equals(newUser.getBirthDate())
                || !oldUser.getEmail().equals(newUser.getEmail())
                || !oldUser.getGender().equals(newUser.getGender())
                || !oldUser.getPhone().equals(newUser.getPhone())
                || oldUser.getLatitude() != newUser.getLatitude()
                || oldUser.getLongitude() != newUser.getLongitude()
                || !oldUser.getProfilePicUrl().equals(newUser.getProfilePicUrl());
    }

    private void addUsersChangeListener() {
        Log.d(TAG, "Update users function called. Listening for events..");
        // Check if any data in the users node changed.
        databaseReference.child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Something has changed within the users group.");
                // If it did, trigger the onUsersChange event.
                assert listener != null;
                listener.onUsersChange(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "updateUsers:failure", error.toException());
            }
        });
    }

    public interface UsersListener {
        void onUserReady();
        void onUsersChange(@NonNull DataSnapshot snapshot);
    }
}
