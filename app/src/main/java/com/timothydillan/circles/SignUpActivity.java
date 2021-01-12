package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity implements ActivityInterface {

    private final String TAG = "FIREBASE_AUTH";
    private TextInputLayout firstNameInput, lastNameInput, phoneInput, emailInput, passwordInput, circleIdInput;
    private String firstName, lastName, phone, email, password, circleId;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Assign inputs to corresponding XML ids
        firstNameInput = findViewById(R.id.firstNameInputLayout);
        lastNameInput = findViewById(R.id.lastNameInputLayout);
        phoneInput = findViewById(R.id.phoneInputLayout);
        emailInput = findViewById(R.id.emailInputLayout);
        passwordInput = findViewById(R.id.passwordInputLayout);
        circleIdInput = findViewById(R.id.circleInputLayout);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null)
            goToMainActivity();
    }

    public void onSignUpButtonClick(View v) {

        if (!formValidation())
            return;

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            User newUser = new User(firstName, lastName, email, phone, circleId);
                            FirebaseUser user = firebaseAuth.getCurrentUser();
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
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SignUpActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public boolean formValidation() {
        firstName = firstNameInput.getEditText().getText().toString();
        lastName = lastNameInput.getEditText().getText().toString();
        phone = phoneInput.getEditText().getText().toString();
        email = emailInput.getEditText().getText().toString();
        password = passwordInput.getEditText().getText().toString();
        circleId = circleIdInput.getEditText().getText().toString();

        if (firstName.isEmpty()) {
            firstNameInput.setErrorEnabled(true);
            firstNameInput.setError("Your first name shouldn't be empty.");
            firstNameInput.requestFocus();
            return false;
        } else {
            firstNameInput.setErrorEnabled(false);
        }

        if (lastName.isEmpty()) {
            lastNameInput.setErrorEnabled(true);
            lastNameInput.setError("Your last name shouldn't be empty.");
            lastNameInput.requestFocus();
            return false;
        } else {
            lastNameInput.setErrorEnabled(false);
        }

        if (phone.isEmpty()) {
            phoneInput.setErrorEnabled(true);
            phoneInput.setError("Your phone number shouldn't be empty.");
            phoneInput.requestFocus();
            return false;
        } else {
            phoneInput.setErrorEnabled(false);
        }

        if (email.isEmpty()) {
            emailInput.setErrorEnabled(true);
            emailInput.setError("Your email address shouldn't be empty.");
            emailInput.requestFocus();
            return false;
        } else {
            emailInput.setErrorEnabled(false);
        }

        if (password.length() < 8) {
            passwordInput.setErrorEnabled(true);
            passwordInput.setError("Your password needs to have at least 8 symbols.");
            passwordInput.requestFocus();
            return false;
        } else {
            passwordInput.setErrorEnabled(false);
            passwordInput.setError(null);
        }

        return true;
    }

    @Override
    public void onTextClick(View v) {
        Intent mainActivity = new Intent(SignUpActivity.this, SignInActivity.class);
        startActivity(mainActivity);
        finish();
    }

    @Override
    public void goToMainActivity() {
        Intent mainActivity = new Intent(SignUpActivity.this, MainActivity.class);
        startActivity(mainActivity);
        finish();
    }
}