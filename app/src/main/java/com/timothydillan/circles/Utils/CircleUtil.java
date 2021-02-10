package com.timothydillan.circles.Utils;

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

public class CircleUtil {
    private static final String TAG = "CircleUtil";
    private static final ArrayList<User> circleMembers = new ArrayList<>();
    private static final String USER_UID = FirebaseUtil.getCurrentUser().getUid();
    private static final DatabaseReference databaseReference = FirebaseUtil.getDbReference();

    private static String pastCircleCode = String.valueOf(UserUtil.getCurrentUser().getCurrentCircleSession());

    private final String currentCircleCode = String.valueOf(UserUtil.getCurrentUser().getCurrentCircleSession());
    private UserUtil userUtil = new UserUtil();
    private CircleUtilListener listener;

    public void addEventListener(CircleUtilListener listener) {
        // If a class provides the listener immediately on construct,
        // set the circle listener to the new listener instance created,
        this.listener = listener;
        // and then check whether the currentUser and circleMembers are not null.
        if (UserUtil.getCurrentUser() != null && !circleMembers.isEmpty() && pastCircleCode.equals(currentCircleCode)) {
            Log.d(TAG, "Circle already ready, firing onCircleReady listener.");
            // then trigger the onCircleReady event
            this.listener.onCircleReady(circleMembers);
            addCircleChangeListener();
        } else {
            Log.d(TAG, "Circle not ready, initializing.");
            pastCircleCode = currentCircleCode;
            retrieveCircleMemberUid();
        }
    }

    private void retrieveCircleMemberUid() {
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
                            circleMemberUidList.add(ds.getKey());
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
        // Clear the circle member list so the old data does not overlap with new one.
        resetCircle();
        // Read the Users node once
        databaseReference.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // get every member based on the UID retrieved
                for (String uid : circleMemberUidList) {
                    User circleMember =  snapshot.child(uid).getValue(User.class);
                    Log.d(TAG, "Retrieved " + circleMember.getFirstName() + "'s details.");
                    circleMembers.add(circleMember);
                }
                // Once we're done with this, run a continuous event listener that updates each member's details.
                listener.onCircleReady(circleMembers);
                Log.d(TAG, "Circle initialization done. Running update circle function.");
                addCircleChangeListener();
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
                // trigger the onCircleChange event.
                listener.onCircleChange();
                // reset the circle (reset the data)
                resetCircle();
                // and re-initialize the circle.
                retrieveCircleMemberUid();
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

    private void removeDuplicateMembers() {
        ArrayList<User> noDuplicates = new ArrayList<>();
        // For every member in the circleMembers array,
        for (User member : circleMembers) {
            // check if the member has already been added unto the noDuplicates list.
            // if the member has already been added, continue to the next iteration.
            if (noDuplicates.contains(member)) {
                continue;
            }
            // if not, add the member into the noDuplicates list.
            noDuplicates.add(member);
        }
        // Once the operation above is done, clear the circleMembers list
        circleMembers.clear();
        // and add the content from the list that has no duplicates unto the circleMembers list.
        circleMembers.addAll(noDuplicates);
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
                    listener.onJoinCircle(false);
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
                            listener.onJoinCircle(false);
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
                                        // and update the user's current circle session in the DB
                                        userUtil.updateDbUserCurrentCircle(Integer.parseInt(circleCode));
                                        // update the user's current circle session locally,
                                        UserUtil.getCurrentUser().setCurrentCircleSession(Integer.parseInt(circleCode));
                                        // and trigger the listener.
                                        listener.onJoinCircle(true);
                                    }
                                });
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // if the operation is cancelled, return false.
                        listener.onJoinCircle(false);
                    }
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onJoinCircle(false);
            }
        });
    }

    public static void resetCircle() {
        circleMembers.clear();
    }

    public ArrayList<User> getCircleMembers() {
        return circleMembers;
    }

    public interface CircleUtilListener {
        void onCircleReady(ArrayList<User> members);
        void onCircleChange();
        void onJoinCircle(boolean success);
    }

}
