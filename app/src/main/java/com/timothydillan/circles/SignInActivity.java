package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.timothydillan.circles.UI.ProgressButton;

public class SignInActivity extends ActivityInterface {

    private final String TAG = "FIREBASE_AUTH";
    private TextInputLayout emailInput, passwordInput;
    private String email, password;
    private FirebaseAuth firebaseAuth;
    private View signInButton;
    private ProgressButton progressButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Assign inputs to corresponding XML ids
        emailInput = findViewById(R.id.emailInputLayout);
        passwordInput = findViewById(R.id.passwordInputLayout);

        signInButton = findViewById(R.id.signInButton);
        progressButton = new ProgressButton("Sign In", getResources(), signInButton, R.color.dark_blue);

        signInButton.setOnClickListener(v -> signIn());
    }

    public void signIn() {
        if (!formValidation())
            return;

        progressButton.onLoading();

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnFailureListener(this, e -> progressButton.onFailed())
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithEmail:success");
                        progressButton.onFinished();
                        new Handler().postDelayed(() -> goToMainActivity(), 1500);
                    } else {
                        // If sign in fails
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Snackbar.make(findViewById(android.R.id.content), "Your password or e-mail may be invalid.", Snackbar.LENGTH_LONG).show();
                        progressButton.onFailed();
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

    public void onSignUpClick(View v) {
        Intent signUpActivity = new Intent(SignInActivity.this, SignUpActivity.class);
        startActivity(signUpActivity);
        finish();
    }

}