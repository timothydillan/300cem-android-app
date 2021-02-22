package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.timothydillan.circles.UI.Message;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;

public class PasswordActivity extends AppCompatActivity {
    private SharedPreferencesUtil sharedPreferences;
    private TextInputLayout passwordInputLayout;
    private Button setPasswordButton;
    private TextView titleTextView;
    private TextView descriptionTextView;
    private Button disablePasswordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        Toolbar toolbar = findViewById(R.id.passwordToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sharedPreferences = new SharedPreferencesUtil(this);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        setPasswordButton = findViewById(R.id.setPasswordButton);
        disablePasswordButton = findViewById(R.id.disablePasswordButton);
        disablePasswordButton.setVisibility(View.GONE);
        titleTextView = findViewById(R.id.titleTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);

        disablePasswordButton.setOnClickListener(v -> {
            disablePasswordAuthentication();
        });

        setPasswordButton.setOnClickListener(v -> {
            enablePasswordAuthentication();
        });

        // If password authentication is enabled,
        if (sharedPreferences.isPasswordSecurityEnabled()) {
            // set the title and description text views to the appropriate texts
            titleTextView.setText(getString(R.string.password_title_enabled));
            descriptionTextView.setText(getString(R.string.password_description_enabled));
            // remove the password input and set password button
            setPasswordButton.setVisibility(View.GONE);
            passwordInputLayout.setVisibility(View.GONE);
            // and make the disable password button visible.
            disablePasswordButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Make sure that the foreground state is set to true, so that the authentication process
        // runs only when the user completely leaves the app and goes back.
        sharedPreferences.writeBoolean(SharedPreferencesUtil.FOREGROUND_KEY, true);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        return super.onOptionsItemSelected(item);
    }

    private void removeView(View view) {
        view.animate().alpha(0f).setDuration(800).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
            }
        });
    }

    private void showView(View view) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate().alpha(1f).setDuration(800).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.VISIBLE);
            }
        });
    }

    private void disablePasswordAuthentication() {
        /* If the disable password button is clicked, set the title and the description to the
         * default texts, remove the password from the app's sharedpreferences, and re-show the password input and
         * set password button. */
        titleTextView.setText(getString(R.string.password_default_title));
        descriptionTextView.setText(getString(R.string.password_default_description));
        if (!sharedPreferences.removeItem(SharedPreferencesUtil.PASSWORD_KEY)) {
            return;
        }
        removeView(disablePasswordButton);
        showView(passwordInputLayout);
        showView(setPasswordButton);
    }

    private void enablePasswordAuthentication() {
        // If the enable password button is clicked, get the password from the input edittext,
        String password = passwordInputLayout.getEditText().getText().toString();

        // and first check whether the input is empty.
        if (password.isEmpty()) {
            // if it is, show an error, and do nothing.
            passwordInputLayout.setErrorEnabled(true);
            passwordInputLayout.setError("Password can't be empty!");
            return;
        }

        // Then, write the password to the app's shared preferences.
        if (!sharedPreferences.writeString(SharedPreferencesUtil.PASSWORD_KEY, password)) {
            // If there's a failure in writing to the shared preferences, return and do nothing.
            return;
        }

        /* If all goes well, reset the password input layout, remove both the input view and set password button,
         * show the disable password button, and set the title and description to the enabled text. */
        passwordInputLayout.getEditText().setText("");
        removeView(passwordInputLayout);
        removeView(setPasswordButton);
        showView(disablePasswordButton);
        titleTextView.setText(getString(R.string.password_title_enabled));
        descriptionTextView.setText(getString(R.string.password_description_enabled));
    }

}