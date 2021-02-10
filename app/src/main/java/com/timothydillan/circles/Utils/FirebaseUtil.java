package com.timothydillan.circles.Utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseUtil {
    private static StorageReference firebaseStorageReference;
    private static DatabaseReference firebaseDatabaseReference = null;
    private static FirebaseAuth firebaseAuth = null;
    private static FirebaseUser firebaseCurrentUser = null;

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

    public static StorageReference getStorageReference() { return firebaseStorageReference; }

    public static void signOut() {
        firebaseCurrentUser = null;
        firebaseAuth.signOut();
    }
}
