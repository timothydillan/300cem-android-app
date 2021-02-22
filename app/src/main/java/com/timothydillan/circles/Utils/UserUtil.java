package com.timothydillan.circles.Utils;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.timothydillan.circles.Models.Circle;
import com.timothydillan.circles.Models.Item;
import com.timothydillan.circles.Models.User;

import java.util.ArrayList;
import java.util.Random;

// A combination of a Singleton (single instance, global point of access), facade, and an observer/listener class used for user-related operations.
public class UserUtil {

    // Since a lot of our data is shared and since it shouldn't depend on Fragments, this class was made
    // to keep variables even though the lifecycle of the fragment holding it is destroyed.
    private static final String TAG = "UserUtil";
    private static final DatabaseReference databaseReference = FirebaseUtil.getDbReference();
    private static final StorageReference storageReference = FirebaseUtil.getStorageReference();
    private static final FirebaseAuth firebaseAuth = FirebaseUtil.getFirebaseAuth();

    /* Since this is a singleton class, we'll create a static instance that will be initialized once,
     * and retrieved every other time (seems good to me since we don't have to create so many instances when
     * moving between fragments).*/
    private static UserUtil instance;
    private static ArrayList<String> registeredCirclesCodes = new ArrayList<>();
    private static ArrayList<String> registeredCircleTokens = new ArrayList<>();
    private static ArrayList<String> currentCircleTokens = new ArrayList<>();
    private static Item registeredCircles = new Item();
    private static String currentRole = null;
    private static User currentUser = null;
    private String USER_UID = FirebaseUtil.getUid();

    // Since most of the fragments and activities will have different implementation when one of the listener's event is triggered,
    // we'll just create a list of listeners, and the singleton class loop through all the current listeners and trigger the events.
    private ArrayList<UsersListener> listeners = new ArrayList<>();

    // The default constructor should be private to prevent direct constructor calls and only allow
    // the constructor to be called from the getInstance() method.
    private UserUtil() {
        addUsersChangeListener();
    }

    public static synchronized UserUtil getInstance() {
        // If the instance is null,
        if (instance == null) {
            // we'll create a new instance of the user util.
            instance = new UserUtil();
        }
        // Whether its null or not, we'll keep on returning the instance.
        return instance;
    }

    public void registerListener(UsersListener listener) {
        // To prevent duplicate listeners, we'll first check whether the listener being registered
        // has been added to the list.
        if (!listeners.contains(listener)) {
            // if it hasn't, then we'll add the listener.
            listeners.add(listener);
        }
    }

    public void unregisterListener(UsersListener listener) {
        // If the listener that wants to be removed isn't null
        if (listener != null) {
            // we'll just remove the listener. We don't need to check whether the listener
            // that wants to be removed is in the list, since the remove function does that for us.
            listeners.remove(listener);
        }
    }

    public void initializeRegisteredCircles() {
        // This function finds the circles that the user is currently in, and puts them into a list.

        // If the array lists are already filled, there's no reason why we should initialize all the arrays again.
        if (!registeredCirclesCodes.isEmpty() && !registeredCircles.getMap().isEmpty() && !registeredCircleTokens.isEmpty()) {
            return;
        }

        // Get the values in the circles node once,
        databaseReference.child("Circles").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // for each of the value retrieved, loop through all the children (the children at this level should be the list of circleDetails (members and name))
                for (DataSnapshot circle : snapshot.getChildren()) {
                    // for each circle details being iterated right now,
                    for (DataSnapshot circleDetails : circle.getChildren()) {
                        // we'll loop through all the members in each circle
                        for (DataSnapshot member : circleDetails.getChildren()) {
                            // and check whether the current member uid being iterated is the same as the user's uid.
                            if (!member.getKey().equals(USER_UID)) {
                                continue;
                            }
                            Log.d(TAG, "FOUND MATCH!");
                            // if it's the same, we'll add the circle code to the registeredCircleCodes list
                            registeredCirclesCodes.add(circle.getKey());
                            // and add both the name of the circle and the circle code to the registeredCircles list.
                            registeredCircles.addItem(circle.child("name").getValue(String.class), circle.getKey());
                        }
                    }
                }

                // Once we're done with the for loops, we'll initialize the tokens of each user in the registered circles.
                initializeAllTokens(registeredCirclesCodes);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    public void reinitializeRegisteredCircles() {
        /* This function does the exact same thing as initializeRegisteredCircles(), but it resets the arrays before hand */
        resetArrays();
        databaseReference.child("Circles").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot circle : snapshot.getChildren()) {
                    for (DataSnapshot id : circle.getChildren()) {
                        for (DataSnapshot member : id.getChildren()) {
                            if (member.getKey().equals(USER_UID)) {
                                Log.d(TAG, "FOUND MATCH!");
                                registeredCirclesCodes.add(circle.getKey());
                                registeredCircles.addItem(circle.child("name").getValue(String.class), circle.getKey());
                            }
                        }
                    }
                }
                initializeAllTokens(registeredCirclesCodes);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    public void initializeAllTokens(ArrayList<String> circleCodes) {
        // For all the circle codes that the user is currently in,
        for (String circleCode : circleCodes) {
            // We'll get each member's token in each circle
            databaseReference.child("Circles")
                .child(circleCode)
                .child("Members")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // For every member in each circle,
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            // We'll go to the users node and find their token
                            databaseReference.child("Users")
                                .child(ds.getKey())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        // Once we found their token,
                                        String token = snapshot.child("token").getValue(String.class);
                                        // if the token isn't empty
                                        if (!token.isEmpty()) {
                                            // if the registeredCircleTokens list does not contain the same token,
                                            if (!registeredCircleTokens.contains(token)) {
                                                // add the token.
                                                registeredCircleTokens.add(token);
                                            }
                                            // if the current circle code being iterated is the same as the current user's circle code, and if the currentcircletokens list does not contain the same token,
                                            if (circleCode.equals(String.valueOf(currentUser.getCurrentCircleSession())) && !currentCircleTokens.contains(token)) {
                                                // add the tokens.
                                                currentCircleTokens.add(token);
                                            }
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) { }
                                });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w(TAG, "getCircleMemberUid:failure", error.toException());
                    }
                });
        }
    }

    public void initializeCurrentUserRole() {
        /* This functions gets the user's role in the current circle */

        // We'll go into the circles node, then go into the current user's circle code node
        // and then go into the members node, and at the member's UID node
        databaseReference.child("Circles")
            .child(String.valueOf(currentUser.getCurrentCircleSession()))
            .child("Members")
            .child(USER_UID)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // we'll get the user's role from the memberRole node and assign it to a variable.
                    currentRole = snapshot.child("memberRole").getValue(String.class);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w(TAG, "initializeCurrentUserRole:failure", error.toException());
                }
            });
    }

    public void initializeCurrentUser() {
        /* This function initializes the current user */

        // If the current user isn't null,
        if (currentUser != null) {
            // For every listener registered,
            for (UsersListener listener : listeners){
                // trigger the onUserReady()
                listener.onUserReady();
            }
            return;
        }

        // To initialize the circle, we must first get the current user's details. We'll go into the users node in the DB
        databaseReference.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // get the user details from the user's UID node inside the users node
                currentUser = snapshot.child(USER_UID).getValue(User.class);
                for (UsersListener listener : listeners){
                    // then trigger the onUserReady() event for every listener registered.
                    listener.onUserReady();
                }
                Log.d(TAG, "Successfully retrieved current user.");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "initializeCurrentUser:failure", error.toException());
            }
        });
    }

    public static void updateCurrentUser(User user) {
        /* This function updates the current user, if the current user does not match the user received from the parameter */

        // If no profile data changed,
        if (!didUserProfileChange(user)) {
            // we'll return and do nothing.
            return;
        }

        // Else, we'll set the profile information to the new profile information received.
        currentUser.setFirstName(user.getFirstName());
        currentUser.setLastName(user.getLastName());
        currentUser.setBirthDate(user.getBirthDate());
        currentUser.setEmail(user.getEmail());
        currentUser.setPhone(user.getPhone());
        currentUser.setProfilePicUrl(user.getProfilePicUrl());
        currentUser.setBirthDate(user.getBirthDate());
        currentUser.setType(user.getType());
    }

    public static void updateCurrentUser(User oldUser, User newUser) {
        /* This function updates the oldUser data with the newUser data */
        oldUser.setFirstName(newUser.getFirstName());
        oldUser.setLastName(newUser.getLastName());
        oldUser.setBirthDate(newUser.getBirthDate());
        oldUser.setEmail(newUser.getEmail());
        oldUser.setPhone(newUser.getPhone());
        oldUser.setLatitude(newUser.getLatitude());
        oldUser.setLongitude(newUser.getLongitude());
        oldUser.updateLastSharingTime();
        oldUser.setProfilePicUrl(newUser.getProfilePicUrl());
        oldUser.setHeartRate(newUser.getHeartRate());
        oldUser.setStepCount(newUser.getStepCount());
        oldUser.setWalkActivity(newUser.getWalkActivity());
        oldUser.setRunningActivity(newUser.getRunningActivity());
        oldUser.setCyclingActivity(newUser.getCyclingActivity());
        oldUser.setMood(newUser.getMood());
        oldUser.setType(newUser.getType());
        oldUser.setToken(newUser.getToken());
    }

    public static boolean didUserProfileChange(User newUser) {
        /* This function checks whether the profile of the current user does not match the profile of the new user information
        *  Returns true if something changed, and it returns false if nothing changed. */
        return !currentUser.getFirstName().equals(newUser.getFirstName())
                || !currentUser.getLastName().equals(newUser.getLastName())
                || !currentUser.getBirthDate().equals(newUser.getBirthDate())
                || !currentUser.getEmail().equals(newUser.getEmail())
                || !currentUser.getPhone().equals(newUser.getPhone())
                || !currentUser.getProfilePicUrl().equals(newUser.getProfilePicUrl())
                || !currentUser.getType().equals(newUser.getType());
    }


    public static boolean didUserLocationChange(User oldUser, User newUser) {
        /* This function checks whether the location information of the current user does not match the location info of the new user
         *  Returns true if something changed, and it returns false if nothing changed. */
        return oldUser.getLatitude() != newUser.getLatitude()
                || oldUser.getLongitude() != newUser.getLongitude()
                || oldUser.getCurrentCircleSession() != newUser.getCurrentCircleSession()
                || !oldUser.getToken().equals(newUser.getToken());
    }

    public static boolean didUserHealthChange(User oldUser, User newUser) {
        /* This function checks whether the health information of the current user does not match the health info of the new user
         *  Returns true if something changed, and it returns false if nothing changed. */
        return oldUser.getHeartRate() != null
                && newUser.getHeartRate() != null && !oldUser.getHeartRate().equals(newUser.getHeartRate())
                || (oldUser.getStepCount() != null && newUser.getStepCount() != null
                && !oldUser.getStepCount().equals(newUser.getStepCount()))
                || (oldUser.getCyclingActivity() != null && newUser.getCyclingActivity() != null
                && !oldUser.getCyclingActivity().equals(newUser.getCyclingActivity()))
                || (oldUser.getRunningActivity() != null && newUser.getRunningActivity() != null
                && !oldUser.getRunningActivity().equals(newUser.getRunningActivity()))
                || (oldUser.getWalkActivity() != null && newUser.getWalkActivity() != null
                && !oldUser.getWalkActivity().equals(newUser.getWalkActivity()))
                || (oldUser.getMood() != null && newUser.getMood() != null
                && !oldUser.getMood().equals(newUser.getMood()))
                || !oldUser.getToken().equals(newUser.getToken())
                || !oldUser.getType().equals(newUser.getType());
    }


    public static boolean didUserMoodChange(User oldUser, User newUser) {
        /* This function checks whether the health information of the current user does not match the health info of the new user
         *  Returns true if something changed, and it returns false if nothing changed. */
        return oldUser.getHeartRate() != null
                && newUser.getHeartRate() != null && !oldUser.getHeartRate().equals(newUser.getHeartRate())
                || (oldUser.getMood() != null && newUser.getMood() != null
                && !oldUser.getMood().equals(newUser.getMood()))
                || !oldUser.getToken().equals(newUser.getToken())
                || !oldUser.getType().equals(newUser.getType());
    }

    private void addUsersChangeListener() {
        // Check if any data in the users node changed.
        databaseReference.child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Something has changed within the users group.");
                // If it did, we'll trigger the onUsersChange event on every listener registered.
                for (UsersListener listener : listeners){
                    listener.onUsersChange(snapshot);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "updateUsers:failure", error.toException());
            }
        });
    }

    public void updateDbUserFirstName(String firstName) {
        /* This function updates the current user's first name on the DB */
        databaseReference.child("Users").child(USER_UID).child("firstName").setValue(firstName);
    }

    public void updateDbUserLastName(String lastName) {
        /* This function updates the current user's last name on the DB */
        databaseReference.child("Users").child(USER_UID).child("lastName").setValue(lastName);
    }

    public void updateDbUserBirthDate(String birthDate) {
        /* This function updates the current user's birth date on the DB */
        databaseReference.child("Users").child(USER_UID).child("birthDate").setValue(birthDate);
    }

    public void updateDbUserType(String type) {
        /* This function updates the current user's type on the DB */
        databaseReference.child("Users").child(USER_UID).child("type").setValue(type);
    }

    public void updateDbUserToken(String token) {
        /* This function updates the current user's token on the DB */
        databaseReference.child("Users").child(USER_UID).child("token").setValue(token);
    }

    public void updateDbUserProfilePic(String url) {
        /* This function updates the current user's profile picture url on the DB */
        databaseReference.child("Users").child(USER_UID).child("profilePicUrl").setValue(url);
    }

    public void updateDbUser(User user) {
        /* This function updates the current user's with a new user instance on the DB */
        databaseReference.child("Users").child(USER_UID).setValue(user);
    }

    public void updateDbUserCurrentCircle(int circleCode) {
        /* This function updates the current user's current circle session on the DB */
        databaseReference.child("Users").child(USER_UID).child("currentCircleSession")
                .setValue(circleCode);
    }

    public void setNewProfilePicture(Uri imageUri) {
        /* This function updates the current user's profile picture on both the DB and Firebase Auth */
        // We first get a storage reference pointing specifically to the "images/profileImages/USER_UID" node
        StorageReference userStorageReference = storageReference.child("images/profileImages/" + USER_UID);
        // and we'll upload the selected image into the storage path specified above
        userStorageReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        // and once the upload is successful, get the download url to the image
                        userStorageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                                    .setPhotoUri(uri)
                                    .build();
                            // update the user's firebase auth pic url
                            FirebaseUtil.getCurrentUser().updateProfile(request).addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Successfully updated profile image.");
                                // and store the url on the DB.
                                String profilePicUrl = String.valueOf(FirebaseUtil.getCurrentUser().getPhotoUrl());
                                updateDbUserProfilePic(profilePicUrl);
                            });
                }))
                .addOnFailureListener(e -> Log.d(TAG, "Failed to upload image."));
    }

    public void createAccount(String email, String password, User newUser) {
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Sign in success, update UI with the signed-in user's information
                Log.d(TAG, "createUserWithEmail:success");

                FirebaseUtil.initializeCurrentFirebaseUser();

                // Get the current user
                String USER_UID = FirebaseUtil.getUid();

                // Generate a random 6-digit code
                int circleCode = new Random().nextInt(999999);

                newUser.setUid(USER_UID);
                newUser.setToken(FirebaseUtil.getFirebaseMessagingToken());
                newUser.setCurrentCircleSession(circleCode);
                newUser.setMyCircle(circleCode);

                // Create new circle with user's uid and him/her being an admin
                Circle newCircle = new Circle("Admin");

                databaseReference.child("Circles").child(String.valueOf(circleCode))
                        .child("name").setValue(newUser.getFirstName() + "'s Circle");

                databaseReference.child("Circles").child(String.valueOf(circleCode))
                        .child("Members").child(USER_UID).setValue(newCircle);

                // Add user details to the database
                databaseReference.child("Users").child(USER_UID)
                        .setValue(newUser).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Log.d(TAG, "addDataToDatabase:success");
                        for (UsersListener listener : listeners){
                            // If the sign up is successful, trigger the onAuthenticationSuccessful() event for every listener registered.
                            listener.onAuthenticationSuccessful();
                        }
                    } else {
                        Log.w(TAG, "addDataToDatabase:failure", task1.getException());
                        for (UsersListener listener : listeners){
                            // If the sign up failed, then trigger the onAuthenticationFailed() event for every listener registered.
                            listener.onAuthenticationFailed();
                        }
                    }
                }).addOnFailureListener(e -> {
                    for (UsersListener listener : listeners){
                        // If the sign up failed, then trigger the onAuthenticationFailed() event for every listener registered.
                        listener.onAuthenticationFailed();
                    }
                });
            } else {
                // If sign in fails, display a message to the user.
                for (UsersListener listener : listeners){
                    // If the sign up failed, then trigger the onAuthenticationFailed() event for every listener registered.
                    listener.onAuthenticationFailed();
                }
                Log.w(TAG, "createUserWithEmail:failure", task.getException());
            }
        });

    }

    public void authenticateAccountWithCredentials(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success");
                    FirebaseUtil.initializeCurrentFirebaseUser();

                    // Get the current user
                    FirebaseUser user = FirebaseUtil.getCurrentUser();

                    // Generate a random 6-digit code
                    int circleCode = new Random().nextInt(999999);

                    // Create new circle with user's uid and him/her being an admin
                    Circle newCircle = new Circle("Admin");

                    String[] fullName = user.getDisplayName().split(" ");

                    // Create a new user with the current session being on the current circle
                    User newUser = new User(user.getUid(), fullName[0], fullName[1], user.getEmail(), user.getPhoneNumber(), circleCode, FirebaseUtil.getFirebaseMessagingToken());

                    databaseReference.child("Circles").child(String.valueOf(circleCode))
                            .child("name").setValue(fullName[0] + "'s Circle");

                    databaseReference.child("Circles").child(String.valueOf(circleCode))
                            .child("Members").child(user.getUid()).setValue(newCircle);

                    // Add user details to the database
                    databaseReference.child("Users").child(user.getUid())
                            .setValue(newUser).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            Log.d(TAG, "addDataToDatabase:success");
                            for (UsersListener listener : listeners){
                                // If the sign up was successful, then trigger the onAuthenticationFailed() event for every listener registered.
                                listener.onAuthenticationSuccessful();
                            }

                        } else {
                            Log.w(TAG, "addDataToDatabase:failure", task1.getException());
                            for (UsersListener listener : listeners){
                                // If the sign up failed, then trigger the onAuthenticationFailed() event for every listener registered.
                                listener.onAuthenticationFailed();
                            }
                        }
                    });
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "authenticateWithCredential:failure", task.getException());
                    for (UsersListener listener : listeners){
                        // If the sign up failed, then trigger the onAuthenticationFailed() event for every listener registered.
                        listener.onAuthenticationFailed();
                    }
                }
            });
    }

    public void signIn(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnFailureListener(e -> {
                    for (UsersListener listener : listeners){
                        // If the sign in failed, then trigger the onAuthenticationFailed() event for every listener registered.
                        listener.onAuthenticationFailed();
                    }
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // If the sign in was a success, we'll initialize the current firebase user
                        FirebaseUtil.initializeCurrentFirebaseUser();
                        Log.d(TAG, "signInWithEmail:success");
                        for (UsersListener listener : listeners){
                            // and then trigger the onAuthenticationSuccessful() event for every listener registered.
                            listener.onAuthenticationSuccessful();
                        }
                    } else {
                        // If sign in fails
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        for (UsersListener listener : listeners){
                            // trigger the onAuthenticationFailed() event for every listener registered.
                            listener.onAuthenticationFailed();
                        }
                    }
                });
    }

    public static String getUserRole() {
        return currentRole;
    }

    public static ArrayList<String> getAllTokens() {
        return registeredCircleTokens;
    }

    public static ArrayList<String> getCurrentCircleTokens() {
        return currentCircleTokens;
    }

    public static Item getRegisteredCircles() {
        return registeredCircles;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public static ArrayList<String> getRegisteredCirclesCodes() {
        return registeredCirclesCodes;
    }

    public void reset() {
        currentUser = null;
        registeredCircles = null;
        registeredCircles = new Item();
        registeredCirclesCodes.clear();
        registeredCircleTokens.clear();
        currentCircleTokens.clear();
        listeners.clear();
    }

    public void resetArrays() {
        registeredCircles = null;
        registeredCircles = new Item();
        registeredCirclesCodes.clear();
        registeredCircleTokens.clear();
        currentCircleTokens.clear();
    }

    public void deleteAccount() {
        // To delete the users account, we'll loop through all the circles that the user is currently in,
        for (String circleCode : UserUtil.getRegisteredCirclesCodes()) {
            // remove their UID from each circle
            databaseReference.child("Circles").child(circleCode).child("Members").child(currentUser.getUid()).removeValue();
        }
        // remove their details from the Users node
        databaseReference.child("Users").child(currentUser.getUid()).removeValue();
        // and we'll reset all stored variables that is related to the user
        reset();
        CircleUtil.getInstance().reset();
        LocationUtil.resetMap();
        FirebaseUtil.deleteAccount();
    }

    public interface UsersListener {
        default void onUserReady() {
            Log.d(TAG, "onUserReady() fired.");
        }
        default void onUsersChange(@NonNull DataSnapshot snapshot) {
            Log.d(TAG, "onUsersChange() fired.");
        }
        default void onAuthenticationSuccessful() {
            Log.d(TAG, "onRegistrationSuccessful() fired.");
        }
        default void onAuthenticationFailed() {
            Log.d(TAG, "onRegistrationFailed() fired.");
        }
    }
}
