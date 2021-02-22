package com.timothydillan.circles;

import androidx.annotation.Nullable;

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
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.UI.ProgressButton;
import com.timothydillan.circles.Utils.UserUtil;

import java.util.HashMap;

public class SignUpActivity extends ActivityInterface implements UserUtil.UsersListener {

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
    private UserUtil userUtil = UserUtil.getInstance();
    private HashMap<TextInputLayout, String> textInputs = new HashMap<>();
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

        /* If the user has entered an email and a password from the sign in activity, and redirects here,
         * get the email and password from the sign up activity and set the email and password fields accordingly.
         * An extra feature that prevents repetition. */
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String savedEmail = extras.getString("EMAIL_KEY");
            String savedPassword = extras.getString("PASSWORD_KEY");
            if (!savedEmail.isEmpty() && !savedPassword.isEmpty()) {
                emailInput.getEditText().setText(savedEmail);
                passwordInput.getEditText().setText(savedPassword);
            } else if (!savedEmail.isEmpty()) {
                emailInput.getEditText().setText(savedEmail);
            } else if (!savedPassword.isEmpty()) {
                passwordInput.getEditText().setText(savedPassword);
            }
        }

        signUpButton.setOnClickListener(v -> onSignUpButtonClick());
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // We'll register a listener to detect whether the sign up was successful.
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

    public void googleSignIn(View v) {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN_CODE);
    }

    private void onSignUpButtonClick() {
        // Before registering the user, we'll first check if the user has filled out the fields properly.
        if (!formValidation())
            return;

        // If they did, we'll create a new user for them.
        progressButton.onLoading();
        User newUser = new User(firstName, lastName, email, phone);
        userUtil.createAccount(email, password, newUser);
    }

    public boolean formValidation() {
        /* This function validates whether the fields is not empty.
         * If one of the fields is empty, an error will be shown on the field, and the function will return false.
         * Likewise, if the fields are OK, the function will return true. */
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
        /* When the user wants to sign in, get the email and password fields
        if it isn't empty and place it in the extras of the intent and start the sign in actvity.*/
        Intent signInActivity = new Intent(SignUpActivity.this, SignInActivity.class);
        String email = emailInput.getEditText().getText().toString();
        String password = passwordInput.getEditText().getText().toString();
        if (!email.isEmpty() && !password.isEmpty()) {
            signInActivity.putExtra("EMAIL_KEY", email);
            signInActivity.putExtra("PASSWORD_KEY", password);
        } else if (!email.isEmpty()) {
            signInActivity.putExtra("EMAIL_KEY", email);
        } else if (!password.isEmpty()) {
            signInActivity.putExtra("PASSWORD_KEY", password);
        }
        startActivity(signInActivity);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
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

    @Override
    public void onAuthenticationSuccessful() {
        /* When the sign up operation is successful, we'll wait for 1.5 seconds before going to the main activity. */
        Toast.makeText(SignUpActivity.this, "Successfully created an account.",
                Toast.LENGTH_SHORT).show();
        progressButton.onFinished();
        new Handler().postDelayed(() -> {
            goToMainActivity();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 1500);
    }

    @Override
    public void onAuthenticationFailed() {
        /* When the sign up operation fails we'll just show a message */
        progressButton.onFailed();
        Toast.makeText(SignUpActivity.this, "Failed to create an account.",
                Toast.LENGTH_SHORT).show();
    }
}