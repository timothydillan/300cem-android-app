package com.timothydillan.circles;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.timothydillan.circles.UI.ProgressButton;
import com.timothydillan.circles.Utils.FirebaseUtil;

public class SignInActivity extends ActivityInterface {

    private final String TAG = "FIREBASE_AUTH";
    private TextInputLayout emailInput;
    private TextInputLayout passwordInput;
    private String email;
    private String password;
    private FirebaseAuth firebaseAuth = FirebaseUtil.getFirebaseAuth();
    private View signInButton;
    private ProgressButton progressButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

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