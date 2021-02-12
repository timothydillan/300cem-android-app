package com.timothydillan.circles.Utils;

import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseUtil {

    public static final String FCM_KEY = "AAAAT4PeuDA:APA91bFTJa2tgHxH0-1vreezBGKnmne8SNZpkrsymZtMgNJnveu3OmzvA1n7Y8QDh_v2nF_eF5F_QaJOIJ9SP04FN0ejK-K0yEAi1PWow-oV7C2C550Xeq_HEUMsP3-7MpldgYbEpalj";
    private static StorageReference firebaseStorageReference = null;
    private static DatabaseReference firebaseDatabaseReference = null;
    private static FirebaseAuth firebaseAuth = null;
    private static FirebaseUser firebaseCurrentUser = null;
    private static String firebaseMessagingToken = null;

    public static void initializeFirebaseDbAuthStorage() {
        if (firebaseDatabaseReference != null && firebaseAuth != null && firebaseStorageReference != null) {
            return;
        }
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        firebaseStorageReference = FirebaseStorage.getInstance().getReference();
    }

    public static void initializeCurrentFirebaseUser() {
        if (firebaseCurrentUser != null) {
            return;
        }
        firebaseCurrentUser = firebaseAuth.getCurrentUser();
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(s -> {
            Log.d("TOKEN:",  s);
            firebaseMessagingToken = s;
        });
    }

    public static String getFirebaseMessagingToken() { return firebaseMessagingToken; }

    public static DatabaseReference getDbReference() {
        return firebaseDatabaseReference;
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

}
