package com.timothydillan.circles;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.timothydillan.circles.UI.ProgressButton;
import com.timothydillan.circles.Utils.FirebaseUtil;

public class SignInActivity extends ActivityInterface {

    private final String TAG = "FIREBASE_AUTH";
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
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
        emailLayout = findViewById(R.id.emailInputLayout);
        passwordLayout = findViewById(R.id.passwordInputLayout);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);

        if (savedInstanceState == null) {
            String savedEmail;
            String savedPassword;
            Bundle extras = getIntent().getExtras();
            savedEmail = extras.getString("EMAIL_KEY");
            savedPassword = extras.getString("PASSWORD_KEY");
            if (!savedEmail.isEmpty() && !savedPassword.isEmpty()) {
                emailInput.setText(savedEmail);
                passwordInput.setText(savedPassword);
            } else if (!savedEmail.isEmpty()) {
                emailInput.setText(savedEmail);
            } else if (!savedPassword.isEmpty()) {
                passwordInput.setText(savedPassword);
            }
        }

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
        email = emailLayout.getEditText().getText().toString();
        password = passwordLayout.getEditText().getText().toString();

        if (email.isEmpty()) {
            emailLayout.setErrorEnabled(true);
            emailInput.setError("Your email address shouldn't be empty.");
            emailInput.requestFocus();
            return false;
        } else {
            emailLayout.setErrorEnabled(false);
        }

        if (password.length() < 8) {
            passwordLayout.setErrorEnabled(true);
            passwordLayout.setError("Your password needs to have at least 8 symbols.");
            passwordLayout.requestFocus();
            return false;
        } else {
            passwordLayout.setErrorEnabled(false);
            passwordLayout.setError(null);
        }

        return true;
    }

    public void onSignUpClick(View v) {
        Intent signUpActivity = new Intent(SignInActivity.this, SignUpActivity.class);
        startActivity(signUpActivity);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

}