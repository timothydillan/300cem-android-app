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

public class SignInActivity extends ActivityInterface {

    private final String TAG = "FIREBASE_AUTH";
    private TextInputLayout emailInput, passwordInput;
    private String email, password;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Assign inputs to corresponding XML ids
        emailInput = findViewById(R.id.emailInputLayout);
        passwordInput = findViewById(R.id.passwordInputLayout);
    }

    public void onSignInButtonClick(View v) {
        if (!formValidation())
            return;

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            Toast.makeText(SignInActivity.this, "Authentication OK.",
                                    Toast.LENGTH_SHORT).show();
                            goToMainActivity();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(SignInActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            //updateUI(null);
                            // ...
                        }

                        // ...
                    }
                });
    }

    public boolean formValidation() {
        email = emailInput.getEditText().getText().toString();
        password = passwordInput.getEditText().getText().toString();

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
        Intent signUpActivity = new Intent(SignInActivity.this, SignUpActivity.class);
        startActivity(signUpActivity);
        finish();
    }

}