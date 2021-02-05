package com.timothydillan.circles.Utils;

import android.os.Handler;
import android.util.Log;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.timothydillan.circles.UI.ProgressButton;

public class FirebaseUtil {
    private static DatabaseReference firebaseDatabaseReference = null;
    private static FirebaseAuth firebaseAuth = null;
    private static FirebaseUser firebaseCurrentUser = null;

    public static void initializeFirebaseDbAndAuth() {
        if (firebaseDatabaseReference != null && firebaseAuth != null) {
            return;
        }
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
    }

    public static void initializeCurrentFirebaseUser() {
        if (firebaseCurrentUser != null) {
            return;
        }
        firebaseCurrentUser = firebaseAuth.getCurrentUser();
    }

    public static DatabaseReference getDbReference() {
        return firebaseDatabaseReference;
    }

    public static FirebaseUser getCurrentUser() {
        return firebaseCurrentUser;
    }

    public static FirebaseAuth getFirebaseAuth() {
        return firebaseAuth;
    }

    public static void signOut() {
        firebaseCurrentUser = null;
        firebaseAuth.signOut();
    }

}
