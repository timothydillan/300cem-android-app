package com.timothydillan.circles;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.timothydillan.circles.UI.ProgressButton;
import com.timothydillan.circles.Utils.NotificationUtil;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;
import com.timothydillan.circles.Utils.UserUtil;

import java.util.concurrent.TimeUnit;

// Note: An exact copy of Crash Confirmation.

public class SOSConfirmationActivity extends AppCompatActivity {

    private SharedPreferencesUtil sharedPreferences;
    private View userOkView;
    private View userHelpView;
    private ProgressButton userOkButton;
    private ProgressButton userHelpButton;
    private ProgressBar timerBar;
    private TextView timeTextView;
    private CountDownTimer countDownTimer;
    private ObjectAnimator timerBarAnimator;
    private static final String notificationMessage = UserUtil.getInstance().getCurrentUser().getFirstName() + " is in danger! Check them out right now!";
    private static final int MAX_PROGRESS_VALUE = 600;
    private static final long TIMER_DURATION = TimeUnit.SECONDS.toMillis(10);
    private static final long ONE_SECOND = TimeUnit.SECONDS.toMillis(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_s_o_s_confirmation);
        sharedPreferences = new SharedPreferencesUtil(this);

        userOkView = findViewById(R.id.userOkButton);
        userHelpView = findViewById(R.id.userHelpButton);
        userOkButton = new ProgressButton("Nevermind 👌", getResources(), userOkView, R.color.dark_blue);
        userHelpButton = new ProgressButton("I need help 🆘", getResources(), userHelpView, R.color.red);
        timerBar = findViewById(R.id.timerBar);
        timeTextView = findViewById(R.id.timeTextView);

        // Once the activity is triggered, a 10 second timer will be started.
        startCountdownTimer();
        startTimerBarAnimation();

        // If the user clicked on the OK button,
        userOkView.setOnClickListener(v -> {
            // Show a toast message,
            Toast.makeText(this, "Good to know.", Toast.LENGTH_SHORT).show();
            // Stop the count down timer,
            stopCountdownTimer();
            // and go back to the MainActivity.
            super.onBackPressed();
        });

        // If the user clicked on the "I need help" button,
        userHelpView.setOnClickListener(v -> {
            // send a notification to all the people in the circle that the user is registered in.
            sendHelpNotification();
            // stop the countdown timer
            stopTimerBarAnimation();
            stopCountdownTimer();
            // and show a toast message.
            Toast.makeText(this, "Alerting other members.", Toast.LENGTH_SHORT).show();
            userHelpButton.onFinished(R.color.red);
            new Handler().postDelayed(this::finish, TimeUnit.SECONDS.toMillis(5));
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Make sure that the foreground state is set to true, so that the authentication process
        // runs only when the user completely leaves the app and goes back.
        sharedPreferences.writeBoolean(SharedPreferencesUtil.FOREGROUND_KEY, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // If the user leaves the activity, we should stop the countdown timer.
        stopCountdownTimer();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void startTimerBarAnimation() {
        // We'll animate the timer bar (aka Progressbar) towards that maximum value in 10 seconds.
        timerBarAnimator = ObjectAnimator.ofInt(timerBar, "progress", 0, MAX_PROGRESS_VALUE);
        timerBarAnimator.setDuration(TIMER_DURATION);
        timerBarAnimator.setInterpolator(new DecelerateInterpolator());
        timerBarAnimator.start();
    }

    private void stopTimerBarAnimation() {
        timerBarAnimator.cancel();
    }

    private void startCountdownTimer() {
        // Create a countdown timer that lasts for 10 seconds with a 1 second interval that triggers onTick.
        countDownTimer = new CountDownTimer(TIMER_DURATION, ONE_SECOND) {
            @Override
            public void onTick(long millisUntilFinished) {
                // every time the countdown timer ticks, set the time text view to the current seconds.
                timeTextView.setText(String.valueOf((int) TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)));
            }
            @Override
            public void onFinish() {
                // if the user didn't do anything until the countdown timer is done,
                // send a notification to all the people in the circle that the user is registered in.
                sendHelpNotification();
                Toast.makeText(SOSConfirmationActivity.this, "Alerting other members.", Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(SOSConfirmationActivity.this::finish, TimeUnit.SECONDS.toMillis(5));
            }
        };
        countDownTimer.start();
    }

    private void stopCountdownTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void sendHelpNotification() {
        // For every user token in every circle that the user is in
        for (String token : UserUtil.getAllTokens()) {
            // send a help notification to each of them.
            if (!token.equals(UserUtil.getInstance().getCurrentUser().getToken())) {
                NotificationUtil.sendNotification(getResources().getString(R.string.sos_title), notificationMessage, token);
            }
        }
    }
}