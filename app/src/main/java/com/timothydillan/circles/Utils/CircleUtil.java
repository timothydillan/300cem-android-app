package com.timothydillan.circles.Utils;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.timothydillan.circles.Models.Circle;
import com.timothydillan.circles.Models.User;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class CircleUtil {
    private static CircleUtil instance;
    private static final String TAG = "CircleUtil";
    private static final ArrayList<User> circleMembers = new ArrayList<>();
    private static final DatabaseReference databaseReference = FirebaseUtil.getDbReference();

    private UserUtil userUtil = UserUtil.getInstance();
    private String currentCircleCode = String.valueOf(userUtil.getCurrentUser().getCurrentCircleSession());
    private String USER_UID = FirebaseUtil.getUid();
    private ArrayList<CircleUtilListener> listeners = new ArrayList<>();

    private long THREE_SECONDS = TimeUnit.SECONDS.toMillis(3);

    private CircleUtil() {
        addCircleChangeListener();
    }

    public static synchronized CircleUtil getInstance() {
        if (instance == null) {
            instance = new CircleUtil();
        }
        return instance;
    }

    public void registerListener(CircleUtilListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            retrieveCircleMemberUid();
        }
    }

    public void unregisterListener(CircleUtilListener listener){
        if (listener != null && listeners.contains(listener)){
            listeners.remove(listener);
        }
    }

    public void retrieveCircleMemberUid() {
        /* Retrieves all member UIDs listed in a circle.
         * Args:
         *   none
         * Returns:
         *   none
         * Description:
         *   Calling this function will trigger an event listener that listens/gets data
         *   once from the Circles table/node, specifically the user's current circle node.
         *   After each member's UID is retrieved and stored in the array list,
         *   the retrieveCircleMembers function will be called. */

        if (circleMembers != null && !circleMembers.isEmpty()) {
            for (CircleUtilListener listener : listeners){
                listener.onCircleReady(circleMembers);
            }
            return;
        }

        Log.d(TAG, "Circle not ready, initializing.");
        currentCircleCode = String.valueOf(userUtil.getCurrentUser().getCurrentCircleSession());

        final ArrayList<String> circleMemberUidList = new ArrayList<>();

        // Get each member's UID in the circle that the user is currently in.
        databaseReference.child("Circles")
                .child(currentCircleCode)
                .child("Members")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            // No need to get the user's UID because he/she has been added
                            // into the list.
                            Log.d(TAG, "Adding member UID into list.");
                            // we'll add every other member into the uid list array.
                            if (!ds.getKey().equals(USER_UID)) {
                                circleMemberUidList.add(ds.getKey());
                            }
                        }
                        // Once all the data has been retrieved, each member's detail will now be
                        // retrieved.
                        Log.d(TAG,
                                "Finished adding every member's uid into list. Getting member details.");
                        retrieveCircleMembers(circleMemberUidList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w(TAG, "getCircleMemberUid:failure", error.toException());
                    }
                });
    }

    private void retrieveCircleMembers(ArrayList<String> circleMemberUidList) {
        // Read the Users node once
        databaseReference.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Clear the circle member list so the old data does not overlap with new one.
                resetCircle();
                circleMembers.add(userUtil.getCurrentUser());
                // get every member based on the UID retrieved
                for (String uid : circleMemberUidList) {
                    User circleMember =  snapshot.child(uid).getValue(User.class);
                    Log.d(TAG, "Retrieved " + circleMember.getFirstName() + "'s details.");
                    circleMembers.add(circleMember);
                }
                // Once we're done with this, run a continuous event listener that updates each member's details.
                for (CircleUtilListener listener : listeners){
                    listener.onCircleReady(circleMembers);
                }
                Log.d(TAG, "Circle initialization done.");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "getCircleMembers:failure", error.toException());
            }
        });
    }

    private void addCircleChangeListener() {
        Log.d(TAG, "Update circle function called. Listening for events..");
        // Check if the circle that the user is currently in is having any changes.
        databaseReference.child("Circles").child(currentCircleCode).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot,
                                       @Nullable String previousChildName) {
                // if it does,
                Log.d(TAG, "Something has changed within the circle with the code: " +
                        currentCircleCode + ".");
                Log.d(TAG, "Calling onCircleChange, resetting and re-initializing circle.");
                // reset the circle (reset the data)
                resetCircle();
                // and re-initialize the circle.
                retrieveCircleMemberUid();
                // trigger the onCircleChange event.
                userUtil.initializeCurrentUserRole();

                // while we're reintializing the circle, wait for 3 seconds, and then trigger the oncirclechange method
                new Handler().postDelayed(() -> {
                    for (CircleUtilListener listener : listeners){
                        listener.onCircleChange();
                    }
                }, THREE_SECONDS);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "updateCircle:failure", error.toException());
            }
        });
    }

    public void joinCircle(String circleCode) {
        // Check if the circle exists.
        databaseReference.child("Circles").child(circleCode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // If the circle does not exist,
                if (!snapshot.exists()) {
                    Log.d(TAG, "Circle doesn't exist.");
                    // trigger the event listener and return.
                    for (CircleUtilListener listener : listeners){
                        listener.onJoinCircle(false);
                    }
                    return;
                }
                // If it does exist, check if the member that's trying to go in is already in.
                databaseReference.child("Circles").child(circleCode).child("Members").child(USER_UID).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // If he/she is already in,
                        if (snapshot.exists()) {
                            Log.d(TAG, "Member has already joined this circle.");
                            // trigger the event listener and do nothing.
                            for (CircleUtilListener listener : listeners){
                                listener.onJoinCircle(false);
                            }
                            return;
                        }
                        // Else, add the member's UID into the Members node of the circle
                        Circle newCircle = new Circle("Member");
                        databaseReference.child("Circles")
                                .child(circleCode)
                                .child("Members")
                                .child(USER_UID)
                                .setValue(newCircle)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Successfully joined the circle.");
                                        switchCircle(circleCode);
                                        // and trigger the listener.
                                        for (CircleUtilListener listener : listeners){
                                            listener.onJoinCircle(true);
                                        }
                                    }
                                });
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // if the operation is cancelled, return false.
                        for (CircleUtilListener listener : listeners){
                            listener.onJoinCircle(false);
                        }
                    }
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                for (CircleUtilListener listener : listeners){
                    listener.onJoinCircle(false);
                }
            }
        });
    }

    public void switchCircle(String circleCode) {
        userUtil.updateDbUserCurrentCircle(Integer.parseInt(circleCode));
        userUtil.getCurrentUser().setCurrentCircleSession(Integer.parseInt(circleCode));
        userUtil.reinitializeRegisteredCircles();
        userUtil.initializeCurrentUserRole();
        FirebaseUtil.initializeCircleName();
        resetCircle();
    }

    public void createCircle(String circleName) {
        // Generate a random 6-digit code
        int circleCode = new Random().nextInt(999999);

        // Create new circle with user's uid and him/her being an admin
        Circle newCircle = new Circle("Admin");

        databaseReference.child("Circles").child(String.valueOf(circleCode))
                .child("name").setValue(circleName)
                .addOnCompleteListener(
                task ->
                    databaseReference.child("Circles")
                            .child(String.valueOf(circleCode))
                            .child("Members")
                            .child(USER_UID)
                            .setValue(newCircle)
                            .addOnCompleteListener(
                            task1 -> {
                                switchCircle(String.valueOf(circleCode));
                                for (CircleUtilListener listener : listeners){
                                    listener.onCreateCircle(true);
                                }
                            })
                            .addOnFailureListener(e -> {
                                for (CircleUtilListener listener : listeners){
                                    listener.onCreateCircle(false);
                                }
                            })
                            .addOnCanceledListener(() -> {
                                for (CircleUtilListener listener : listeners){
                                    listener.onCreateCircle(false);
                                }
                            }))
                .addOnFailureListener(e -> {
                    for (CircleUtilListener listener : listeners){
                        listener.onCreateCircle(false);
                    }
                })
                .addOnCanceledListener(() -> {
                    for (CircleUtilListener listener : listeners){
                        listener.onCreateCircle(false);
                    }
                });

    }

    public void editCircleName(String circleName) {
        databaseReference
                .child("Circles")
                .child(String.valueOf(UserUtil.getInstance().getCurrentUser().getCurrentCircleSession()))
                .child("name")
                .setValue(circleName)
                .addOnCompleteListener(task -> {
                    for (CircleUtilListener listener : listeners) {
                        listener.onEditCircleName(task.isSuccessful());
                    }
                });
    }

    public void removeMember(String userUid) {
        databaseReference.child("Circles").child(currentCircleCode).child("Members").child(userUid).removeValue();
    }

    public void editMemberRole(String userUid, String role) {
        databaseReference.child("Circles").child(currentCircleCode).child("Members").child(userUid).child("memberRole").setValue(role);
    }

    public static void resetCircle() {
        circleMembers.clear();
    }

    public void reset() {
        circleMembers.clear();
        listeners.clear();
    }

    public ArrayList<User> getCircleMembers() {
        return circleMembers;
    }

    public interface CircleUtilListener {
        default void onCircleReady(ArrayList<User> members) {}
        default void onCircleChange() {}
        default void onJoinCircle(boolean success) {}
        default void onCreateCircle(boolean success) {}
        default void onEditCircleName(boolean success) {}
    }

}
