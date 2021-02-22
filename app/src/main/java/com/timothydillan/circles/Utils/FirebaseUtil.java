package com.timothydillan.circles.Utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

// A facade class made to make security authentication easier.
public class FirebaseUtil {
    public static final String FCM_KEY = "AAAAT4PeuDA:APA91bEB-PPv1_vQefd-KOFun5df3HZvzjhSv-cok8NrTLhY--OJW5xS1aFgu0t19Z9m8c-0k1qz2uslz9DylyyuAP4Dp9u32p0agsBuzX37r7Q_tBjMQjkZLywZUYdbPlWUfEtAq3Wm";
    private static StorageReference firebaseStorageReference = null;
    private static DatabaseReference firebaseDatabaseReference = null;
    private static FirebaseAuth firebaseAuth = null;
    private static FirebaseUser firebaseCurrentUser = null;
    private static String firebaseMessagingToken = null;
    private static String currentCircleName = null;

    public static void initializeFirebaseDbAuthStorage() {
        /* Initializes the Database, Auth, and Storage reference, if they're null */
        if (firebaseDatabaseReference != null && firebaseAuth != null && firebaseStorageReference != null) {
            return;
        }
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        firebaseStorageReference = FirebaseStorage.getInstance().getReference();
    }

    public static void initializeCurrentFirebaseUser() {
        /* Initializes the current firebase user if they're null. This function also updates the user's current firebase messaing token, as it changes frequently */
        if (firebaseCurrentUser != null) {
            return;
        }
        firebaseCurrentUser = firebaseAuth.getCurrentUser();
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(s -> {
            Log.d("TOKEN:",  s);
            if (firebaseCurrentUser != null) {
                firebaseDatabaseReference.child("Users").child(firebaseCurrentUser.getUid()).child("token").setValue(s);
                firebaseMessagingToken = s;
            }
        });
    }

    public static void initializeCircleName() {
        /* Initializes the name of the circle that the user is in. */
        firebaseDatabaseReference.child("Circles")
            .child(String.valueOf(UserUtil.getInstance().getCurrentUser().getCurrentCircleSession()))
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    currentCircleName = snapshot.child("name").getValue(String.class);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
    }

    public static String getCurrentCircleName() {
        return currentCircleName;
    }

    public static String getFirebaseMessagingToken() { return firebaseMessagingToken; }

    public static DatabaseReference getDbReference() {
        return firebaseDatabaseReference;
    }

    public static String getUid() {
        if (firebaseCurrentUser == null) {
            return null;
        }
        return firebaseCurrentUser.getUid();
    }

    public static FirebaseUser getCurrentUser() {
        return firebaseCurrentUser;
    }

    public static FirebaseAuth getFirebaseAuth() {
        return firebaseAuth;
    }

    public static StorageReference getStorageReference() { return firebaseStorageReference; }

    public static void signOut() {
        firebaseCurrentUser = null;
        firebaseAuth.signOut();
    }

    public static void deleteAccount() {
        firebaseCurrentUser.delete();
        signOut();
    }

}
