package com.timothydillan.circles;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.timothydillan.circles.UI.ProgressButton;
import com.timothydillan.circles.Utils.FirebaseUtil;
import com.timothydillan.circles.Utils.UserUtil;

public class SignInActivity extends ActivityInterface implements UserUtil.UsersListener {

    private static final String TAG = "SignInActivity";
    private static final int GOOGLE_SIGN_IN_CODE = 1;

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private String email;
    private String password;
    private UserUtil userUtil = UserUtil.getInstance();
    private View signInButton;
    private ProgressButton progressButton;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Assign inputs to corresponding resource ids
        emailLayout = findViewById(R.id.emailInputLayout);
        passwordLayout = findViewById(R.id.passwordInputLayout);
        signInButton = findViewById(R.id.signInButton);
        progressButton = new ProgressButton("SIGN IN", getResources(), signInButton, R.color.dark_blue);

        /* If the user has entered an email and a password from the sign up activity, and redirects here,
        * get the email and password from the sign up activity and set the email and password fields accordingly.
        * An extra feature that prevents repetition. */
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String savedEmail = extras.getString("EMAIL_KEY");
            String savedPassword = extras.getString("PASSWORD_KEY");
            if (!savedEmail.isEmpty() && !savedPassword.isEmpty()) {
                emailLayout.getEditText().setText(savedEmail);
                passwordLayout.getEditText().setText(savedPassword);
            } else if (!savedEmail.isEmpty()) {
                emailLayout.getEditText().setText(savedEmail);
            } else if (!savedPassword.isEmpty()) {
                passwordLayout.getEditText().setText(savedPassword);
            }
        }

        signInButton.setOnClickListener(v -> signIn());
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // We'll register a listener to detect whether the sign in was successful.
        userUtil.registerListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // We'll unregister the listener when the user leaves the activity since it's not needed anymore.
        userUtil.unregisterListener(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    // https://firebase.google.com/docs/auth/android/google-signin
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
                    userUtil.authenticateAccountWithCredentials(account.getIdToken());
                } catch (ApiException e) {
                    // Google Sign In failed, update UI appropriately
                    Log.w(TAG, "Google sign in failed", e);
                }
            } else {
                Log.e(TAG, task.getException().toString());
            }
        }
    }

    public void signIn() {
        // Before signing the user in, we'll first check if the user has filled out the fields properly.
        if (!formValidation())
            return;

        // If they did, we'll sign in the user.
        progressButton.onLoading();
        userUtil.signIn(email, password);
    }

    public void googleSignIn(View v) {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN_CODE);
    }

    public void facebookSignIn(View v) {
        Toast.makeText(this, "Coming soon.", Toast.LENGTH_SHORT).show();
    }

    public boolean formValidation() {
        /* This function validates whether the fields is not empty.
        * If one of the fields is empty, an error will be shown on the field, and the function will return false.
        * Likewise, if the fields are OK, the function will return true. */
        email = emailLayout.getEditText().getText().toString();
        password = passwordLayout.getEditText().getText().toString();

        if (email.isEmpty()) {
            emailLayout.setErrorEnabled(true);
            emailLayout.setError("Your email address shouldn't be empty.");
            emailLayout.requestFocus();
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
        /* When the user wants to sign up instead, get the email and password fields
        if it isn't empty and place it in the extras of the intent and start the sign up actvity.*/
        Intent signUpIntent = new Intent(SignInActivity.this, SignUpActivity.class);
        String email = emailLayout.getEditText().getText().toString();
        String password = passwordLayout.getEditText().getText().toString();
        if (!email.isEmpty() && !password.isEmpty()) {
            signUpIntent.putExtra("EMAIL_KEY", email);
            signUpIntent.putExtra("PASSWORD_KEY", password);
        } else if (!email.isEmpty()) {
            signUpIntent.putExtra("EMAIL_KEY", email);
        } else if (!password.isEmpty()) {
            signUpIntent.putExtra("PASSWORD_KEY", password);
        }
        startActivity(signUpIntent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    public void onAuthenticationSuccessful() {
        /* When the user successfully authenticated themselves, we'll wait for 1.5 seconds before going to the main activity. */
        Toast.makeText(this, "Successfully signed in.",
                Toast.LENGTH_SHORT).show();
        progressButton.onFinished();
        new Handler().postDelayed(() -> {
            goToMainActivity();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 1500);
    }

    @Override
    public void onAuthenticationFailed() {
        /* When the user successfully fails to authenticate themselves, we'll just show a message */
        progressButton.onFailed();
        Toast.makeText(this, "Your password or e-mail may be invalid.",
                Toast.LENGTH_SHORT).show();
    }

}