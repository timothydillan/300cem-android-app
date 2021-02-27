package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.NotificationUtil;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;
import com.timothydillan.circles.Utils.UserUtil;

public class JoinCircleActivity extends ActivityInterface implements CircleUtil.CircleUtilListener {
    private static final String TAG = "JoinCircleActivity";
    private UserUtil userUtil = UserUtil.getInstance();
    private final String notificationMessage = "Hey there! " + userUtil.getCurrentUser().getFirstName() + " joined your circle!";
    private CircleUtil circleUtil = CircleUtil.getInstance();
    private TextInputLayout circleInput;
    private SharedPreferencesUtil sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_circle);
        sharedPreferences = new SharedPreferencesUtil(this);

        Toolbar toolbar = findViewById(R.id.joinToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        circleInput = findViewById(R.id.circleCodeInputLayout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register a circle listener to check whether the user successfully joined the circle or not
        circleUtil.registerListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the circle when the user leaves the join circle activity.
        circleUtil.unregisterListener(this);
        // Make sure that the foreground state is set to true, so that the authentication process
        // runs only when the user completely leaves the app and goes back.
        sharedPreferences.writeBoolean(SharedPreferencesUtil.FOREGROUND_KEY, true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    public void onJoinButtonClick(View v) {
        // Hide the keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View windowToken = getCurrentFocus();
        if (windowToken != null) {
            imm.hideSoftInputFromWindow(windowToken.getWindowToken(), 0);
        }

        // If the user clicked the join button, get the circle code on the edit text,
        String circleCode = circleInput.getEditText().getText().toString();

        // and check whether the length of the code is 6 (each circle has a 6 digit code)
        if (circleCode.length() != 6) {
            // if the length isn't 6,
            circleInput.setErrorEnabled(true);
            // show an error.
            circleInput.setError(getResources().getString(R.string.circle_code_error));
            circleInput.requestFocus();
            // and do nothing.
            return;
        }
        // else, remove the error, and trigger the joinCircle function in the circle utility class.
        circleInput.setErrorEnabled(false);
        circleUtil.joinCircle(circleCode);
    }


    @Override
    public void onJoinCircle(boolean success) {
        // If the join process is successful,
        if (success) {
            // show a snackbar that shows a success message,
            Log.d(TAG, getResources().getString(R.string.success_join_message));
            showSnackbar(getResources().getString(R.string.success_join_message));
            // and send a notification to the new circle that a new user has joined their circle.
            sendJoinNotification();
        } else {
            // else, show a failed message.
            Log.d(TAG, getResources().getString(R.string.failure_join_message));
            showSnackbar(getResources().getString(R.string.failure_join_message));
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    private void sendJoinNotification() {
        // For every user token in the current circle,
        for (String token : UserUtil.getCurrentCircleTokens()) {
            // We don't want to send a notification to the current user that they joined the cirlce, so we'll skip the current user's token
            if (!token.equals(userUtil.getCurrentUser().getToken())) {
                // and send a notification to the other members in the circle.
                NotificationUtil.sendNotification(getResources().getString(R.string.join_notification_title), notificationMessage, token);
            }
        }
    }

}