package com.timothydillan.circles.Utils;

import android.location.Location;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.timothydillan.circles.Models.User;

import java.text.SimpleDateFormat;
import java.util.Date;

public class UserUtil {
    private UsersListener listener = null;
    private static final String TAG = "UserUtil";
    private static final String USER_UID = FirebaseUtil.getCurrentUser().getUid();
    private static final DatabaseReference databaseReference = FirebaseUtil.getDbReference();
    private static final StorageReference storageReference = FirebaseUtil.getStorageReference();
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
                || !oldUser.getProfilePicUrl().equals(newUser.getProfilePicUrl())
                || oldUser.getCurrentCircleSession() != newUser.getCurrentCircleSession();
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

    public void updateDbUserFirstName(String firstName) {
        databaseReference.child("Users").child(USER_UID).child("firstName").setValue(firstName);
    }

    public void updateDbUserLastName(String lastName) {
        databaseReference.child("Users").child(USER_UID).child("lastName").setValue(lastName);
    }

    public void updateDbUserBirthDate(String birthDate) {
        databaseReference.child("Users").child(USER_UID).child("birthDate").setValue(birthDate);
    }

    public void updateDbUserGender(String gender) {
        databaseReference.child("Users").child(USER_UID).child("gender").setValue(gender);
    }

    public void updateDbUserLastSharingTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm EEEE");
        String currentDateAndTime = dateFormat.format(new Date());
        databaseReference.child("Users").child(USER_UID).child("lastSharingTime").setValue(currentDateAndTime);
    }

    public void updateDbUserLocation(Location location) {
        databaseReference.child("Users").child(USER_UID).child("latitude").setValue(location.getLatitude());
        databaseReference.child("Users").child(USER_UID).child("longitude").setValue(location.getLongitude());
    }

    public void updateDbUserProfilePic(String url) {
        databaseReference.child("Users").child(USER_UID).child("profilePicUrl").setValue(url);
    }

    public void updateDbUser(User user) {
        databaseReference.child("Users").child(USER_UID).setValue(user);
    }

    public void updateDbUserCurrentCircle(int circleCode) {
        databaseReference.child("Users").child(USER_UID).child("currentCircleSession")
                .setValue(circleCode);
    }

    public void setNewProfilePicture(Uri imageUri) {
        StorageReference userStorageReference = storageReference.child("images/profileImages/" + USER_UID);
        userStorageReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        userStorageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                                    .setPhotoUri(uri)
                                    .build();
                            FirebaseUtil.getCurrentUser().updateProfile(request).addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Successfully updated profile image.");
                                String profilePicUrl = String.valueOf(FirebaseUtil.getCurrentUser().getPhotoUrl());
                                updateDbUserProfilePic(profilePicUrl);
                    });
                }))
                .addOnFailureListener(e -> Log.d(TAG, "Failed to upload image."));
    }

    public static void resetUser() {
        currentUser = null;
    }

    public interface UsersListener {
        void onUserReady();
        void onUsersChange(@NonNull DataSnapshot snapshot);
    }
}
