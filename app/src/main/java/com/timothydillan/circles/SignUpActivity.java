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
import com.timothydillan.circles.Models.Circle;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.Utils.FirebaseUtil;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Random;

public class SignUpActivity extends ActivityInterface {

    private final String TAG = "SignUpActivity";
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        FirebaseUtil.initializeCurrentFirebaseUser();

        // Assign inputs to corresponding XML ids
        firstNameInput = findViewById(R.id.firstNameInputLayout);
        lastNameInput = findViewById(R.id.lastNameInputLayout);
        phoneInput = findViewById(R.id.phoneInputLayout);
        emailInput = findViewById(R.id.emailInputLayout);
        passwordInput = findViewById(R.id.passwordInputLayout);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        if (FirebaseUtil.getCurrentUser() != null)
            goToMainActivity();
    }

    public void onSignUpButtonClick(View v) {

        if (!formValidation())
            return;

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success");

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
}