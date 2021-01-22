package com.timothydillan.circles.Utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.Marker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.timothydillan.circles.Models.User;

import java.util.ArrayList;
import java.util.Map;

public class CircleUtil {
    private CircleUtilListener listener;

    // Make these static as they need to be shared for all instances using it.
    private static final String FIREBASE_TAG = "circleUtility";
    private static final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private static final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private static final ArrayList<User> circleMembers = new ArrayList<>();
    private static User currentMember;

    public CircleUtil() {
        this.listener = null;
        circleInitialization();
    }

    public void addCircleListener(CircleUtilListener listener) {
        this.listener = listener;
    }

    public CircleUtil(CircleUtilListener listener) {
        // If a class provides the listener immediately on construct,
        // set the circle listener to the new listener instance created,
        this.listener = listener;
        // and then check whether the currentMember and circleMembers are not null.
        if (currentMember != null && !circleMembers.isEmpty()) {
            // If they're not, first remove all duplicate members from the circlemembers array list
            // this is a weird bug that I haven't found a fix for yet.
            // TODO: find why circlemembers keep on adding even though the retrieveCircleMembers func is not called.
            removeDuplicateMembers();
            // then trigger the onCircleReady event
            this.listener.onCircleReady(circleMembers);
        } else {
            // if they're null, then initialize the circle first.
            circleInitialization();
        }
    }

    private void circleInitialization() {
        // To initialize the circle, we must first get the current user's details.
        databaseReference.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // After successfully getting the current user's details,
                currentMember = snapshot.child(currentUser.getUid()).getValue(User.class);
                circleMembers.add(currentMember);
                Log.d(FIREBASE_TAG, "Successfully retrieved current user, getting every member ID");
                // we need to get the UID of each member in the circle that the user is currently in.
                retrieveCircleMemberUid();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(FIREBASE_TAG, "circleInitialization:failure", error.toException());
            }
        });
    }

    private void retrieveCircleMemberUid() {
        /* Retrieves all UIDs listed in a circle. TODO: DO THIS STYLE OF COMMENTING FOR OTHER FUNCTIONS.
        * Args:
        *   none
        * Returns:
        *   none
        * Description:
        *   Calling this function will trigger an event listener that listens/gets data
        *   one time from the Circles table/node, specifically the user's current circle node.
        *   The data retrieved is the UID of each member registered inside the circle.
        *   After retrieving the data, the retrieveCircleMembers function will be called. */

        // Declare an array that will store every member's UID.
        final ArrayList<String> circleMemberUidList = new ArrayList<>();

        // Create a single value event listener that returns the member list of the current user's circle
        databaseReference.child("Circles").child(String.valueOf(currentMember.getCurrentCircleSession()))
                .child("Members")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // If we received the data
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            // we'll skip the user's UID because we've added him/her to the list.
                            if (ds.getKey().equals(currentMember.getUid()))
                                continue;
                            Log.d(FIREBASE_TAG, "Adding member UID into list.");
                            // and we'll add every other member into the uid list array.
                            circleMemberUidList.add(ds.getKey());
                        }
                        // Once we're done, call the retrieveCircleMembers function and pass the circleMemberUidList.
                        Log.d(FIREBASE_TAG, "Finished adding every member's uid into list. Getting member details.");
                        retrieveCircleMembers(circleMemberUidList);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w(FIREBASE_TAG, "getCircleMemberUid:failure", error.toException());
                    }
                });
    }

    private void retrieveCircleMembers(ArrayList<String> circleMemberUidList) {
        // First check whether the uid list retrieved is empty or not.
        if (circleMemberUidList.isEmpty())
            return;

        // If it's not, read the Users node once
        databaseReference.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // get the children (aka the UID listed in the users node)
                for (DataSnapshot ds : snapshot.getChildren()) {
                    // If the uid retrieved is the same in the current uid being iterated
                    for (String uid : circleMemberUidList) {
                        if (ds.getKey().equals(uid)) {
                            // Get the user details and store it into a variable
                            User circleMember = ds.getValue(User.class);
                            assert circleMember != null;
                            circleMembers.add(circleMember);
                            Log.d(FIREBASE_TAG, "Retrieved " + circleMember.getFirstName() + "'s details.");
                        }
                    }
                }
                // Once we're done with this, run a continuous event listener that updates each member's details.
                assert listener != null;
                listener.onCircleReady(circleMembers);
                Log.d(FIREBASE_TAG, "Circle initialization done. Running update circle function.");
                updateCircle();
                Log.d(FIREBASE_TAG, "Running update users function.");
                updateUsers();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(FIREBASE_TAG, "getCircleMembers:failure", error.toException());
            }
        });
    }

    private void updateCircle() {
        Log.d(FIREBASE_TAG, "Update circle function called. Listening for events..");
        // Check if the circle that the user is currently in is having any changes.
        databaseReference.child("Circles").child(String.valueOf(currentMember
                .getCurrentCircleSession())).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {  }
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // if it does,
                Log.d(FIREBASE_TAG, "Something has changed within the circle with the code: " +
                        currentMember.getCurrentCircleSession() + ".");
                Log.d(FIREBASE_TAG, "Calling onCircleChange, resetting circle, and re-initializing.");
                assert listener != null;
                // trigger the onCircleChange event.
                listener.onCircleChange();
                // reset the circle (reset the data)
                resetCircle();
                // and re-initialize the circle.
                circleInitialization();
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {   }
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) { }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(FIREBASE_TAG, "updateCircle:failure", error.toException());
            }
        });
    }

    private void updateUsers() {
        Log.d(FIREBASE_TAG, "Update users function called. Listening for events..");
        // Check if any data in the users node changed.
        databaseReference.child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(FIREBASE_TAG, "Something has changed within the users group.");
                assert listener != null;
                // If it did, trigger the onUsersChange event.
                listener.onUsersChange(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(FIREBASE_TAG, "updateUsers:failure", error.toException());
            }
        });
    }

    private void removeDuplicateMembers() {
        ArrayList<User> noDuplicates = new ArrayList<>();
        // Linear scan to check whether there are any duplicates
        for (User member : circleMembers) {
            if (noDuplicates.contains(member))
                continue;
            noDuplicates.add(member);
        }
        // Clear the array list
        circleMembers.clear();
        // and add the new list without duplicates.
        circleMembers.addAll(noDuplicates);
    }

    private void resetCircle() {
        circleMembers.clear();
        currentMember = null;
    }

    private ArrayList<User> getCircleMembers() {
        return circleMembers;
    }

    private User getCurrentMember() {
        return currentMember;
    }

    public interface CircleUtilListener {
        void onCircleReady(ArrayList<User> members);
        void onCircleChange();
        void onUsersChange(@NonNull DataSnapshot snapshot);
    }

}
