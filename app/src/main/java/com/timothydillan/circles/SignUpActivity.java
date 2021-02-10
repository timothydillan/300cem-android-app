package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.timothydillan.circles.Models.Circle;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.UI.ProgressButton;
import com.timothydillan.circles.Utils.FirebaseUtil;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Random;

public class SignUpActivity extends ActivityInterface {

    private static final String TAG = "SignUpActivity";
    private static final int GOOGLE_SIGN_IN_CODE = 1;
    private TextInputLayout firstNameInput;
    private TextInputLayout lastNameInput;
    private TextInputLayout phoneInput;
    private TextInputLayout emailInput;
    private TextInputLayout passwordInput;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String password;
    private FirebaseAuth firebaseAuth = FirebaseUtil.getFirebaseAuth();
    private final HashMap<TextInputLayout, String> textInputs = new HashMap<>();
    private View signUpButton;
    private ProgressButton progressButton;

    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Assign inputs to corresponding XML ids
        firstNameInput = findViewById(R.id.firstNameInputLayout);
        lastNameInput = findViewById(R.id.lastNameInputLayout);
        phoneInput = findViewById(R.id.phoneInputLayout);
        emailInput = findViewById(R.id.emailInputLayout);
        passwordInput = findViewById(R.id.passwordInputLayout);

        signUpButton = findViewById(R.id.signUpButton);
        progressButton = new ProgressButton("GET STARTED", getResources(), signUpButton, R.color.dark_blue);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();


        signUpButton.setOnClickListener(v -> onSignUpButtonClick());

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    public void googleSignIn(View v) {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN_CODE);
    }

    private void onSignUpButtonClick() {

        if (!formValidation())
            return;

        progressButton.onLoading();
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success");

                        FirebaseUtil.initializeCurrentFirebaseUser();

                        // Get the current user
                        String USER_UID = FirebaseUtil.getCurrentUser().getUid();

                        // Generate a random 6-digit code
                        int circleCode = new Random().nextInt(999999);

                        // Create new circle with user's uid and him/her being an admin
                        Circle newCircle = new Circle("Admin");

                        // Create a new user with the current session being on the current circle
                        User newUser = new User(USER_UID, firstName, lastName, email, phone, circleCode);

                        FirebaseDatabase.getInstance().getReference("Circles").child(String.valueOf(circleCode))
                                .child("name").setValue(firstName + "'s Circle");

                        FirebaseDatabase.getInstance().getReference("Circles").child(String.valueOf(circleCode))
                                .child("Members").child(USER_UID).setValue(newCircle);

                        // Add user details to the database
                        FirebaseDatabase.getInstance().getReference("Users").child(USER_UID)
                                .setValue(newUser).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "addDataToDatabase:success");
                                    Toast.makeText(SignUpActivity.this, "Successfully created an account.",
                                            Toast.LENGTH_SHORT).show();
                                    progressButton.onFinished();
                                    new Handler().postDelayed(() -> goToMainActivity(), 1500);
                                } else {
                                    Log.w(TAG, "addDataToDatabase:failure", task.getException());
                                    Toast.makeText(SignUpActivity.this, "Failed creating an account.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressButton.onFailed();
                            }
                        });
                    } else {
                        // If sign in fails, display a message to the user.
                        progressButton.onFailed();
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(SignUpActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public boolean formValidation() {
        firstName = firstNameInput.getEditText().getText().toString();
        lastName = lastNameInput.getEditText().getText().toString();
        phone = phoneInput.getEditText().getText().toString();
        email = emailInput.getEditText().getText().toString();
        password = passwordInput.getEditText().getText().toString();

        textInputs.put(firstNameInput, firstName);
        textInputs.put(lastNameInput, lastName);
        textInputs.put(phoneInput, phone);
        textInputs.put(emailInput, email);
        textInputs.put(passwordInput, password);

        for (TextInputLayout input : textInputs.keySet()) {
            if (textInputs.get(input).isEmpty()) {
                input.setErrorEnabled(true);
                input.setError("This field shouldn't be empty.");
                input.requestFocus();
                return false;
            } else {
                input.setErrorEnabled(false);
            }
        }

        return true;
    }

    public void onSignInClick(View v) {
        // TODO: Can implement an extra feature here to send Email and Password data if filled already to the sign in activity class
        // making it easier for users
        Intent signInActivity = new Intent(SignUpActivity.this, SignInActivity.class);
        startActivity(signInActivity);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_SIGN_IN_CODE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            if (task.isSuccessful()) {
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    // Google Sign In failed, update UI appropriately
                    Log.w(TAG, "Google sign in failed", e);
                    // ...
                }
            } else {
                Log.e(TAG, task.getException().toString());
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
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
                            User newUser = new User(user.getUid(), fullName[0], fullName[1], user.getEmail(), circleCode);

                            FirebaseDatabase.getInstance().getReference("Circles").child(String.valueOf(circleCode))
                                    .child("name").setValue(firstName + "'s Circle");

                            FirebaseDatabase.getInstance().getReference("Circles").child(String.valueOf(circleCode))
                                    .child("Members").child(user.getUid()).setValue(newCircle);

                            // Add user details to the database
                            FirebaseDatabase.getInstance().getReference("Users").child(user.getUid())
                                    .setValue(newUser).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "addDataToDatabase:success");
                                        Toast.makeText(SignUpActivity.this, "Successfully created an account.",
                                                Toast.LENGTH_SHORT).show();
                                        goToMainActivity();
                                    } else {
                                        Log.w(TAG, "addDataToDatabase:failure", task.getException());
                                        Toast.makeText(SignUpActivity.this, "Failed creating an account.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                        }
                    }
                });
    }
}