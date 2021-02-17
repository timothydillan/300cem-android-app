package com.timothydillan.circles;

import androidx.annotation.NonNull;
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
    private static final String notificationTitle = "SOS";
    private static final String notificationMessage = UserUtil.getCurrentUser().getFirstName() + " is in danger! Check them out right now!";
    private static final int MAX_PROGRESS_VALUE = 500;
    private static final long TIMER_DURATION = TimeUnit.SECONDS.toMillis(10);
    private static final long ONE_SECOND = TimeUnit.SECONDS.toMillis(1);
    private static final String STORED_PROGRESS_VALUE = "STORED_PROGRESS_VALUE_KEY";
    private static final String STORED_TIMER_DURATION = "STORED_TIMER_DURATION_KEY";

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

        if (savedInstanceState != null) {
            startCountdownTimer(savedInstanceState.getLong(STORED_TIMER_DURATION));
            startTimerBarAnimation(savedInstanceState.getLong(STORED_TIMER_DURATION),
                    savedInstanceState.getInt(STORED_PROGRESS_VALUE));
        } else {
            startCountdownTimer(TIMER_DURATION);
            startTimerBarAnimation(TIMER_DURATION, 0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.writeBoolean(SharedPreferencesUtil.ACTIVITY_APP_KEY, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCountdownTimer();
    }

    @Override
    protected void onStart() {
        super.onStart();
        userOkView.setOnClickListener(v -> {
            Toast.makeText(this, "Alright.", Toast.LENGTH_SHORT).show();
            stopCountdownTimer();
            super.onBackPressed();
        });
        userHelpView.setOnClickListener(v -> {
            sendHelpNotification();
            stopTimerBarAnimation();
            stopCountdownTimer();
            Toast.makeText(this, "Alerting other members.", Toast.LENGTH_SHORT).show();
            userHelpButton.onFinished(R.color.red);
            new Handler().postDelayed(this::finish, TimeUnit.SECONDS.toMillis(5));
        });
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        long currentTime = Long.parseLong(timeTextView.getText().toString());
        outState.putInt(STORED_PROGRESS_VALUE, timerBar.getProgress());
        outState.putLong(STORED_TIMER_DURATION, TimeUnit.SECONDS.toMillis(currentTime));
    }

    private void startTimerBarAnimation(long duration, int start) {
        if (duration == 0) {
            timerBar.setProgress(MAX_PROGRESS_VALUE);
            return;
        }
        timerBarAnimator = ObjectAnimator.ofInt(timerBar, "progress", start, MAX_PROGRESS_VALUE); // see this max value coming back here, we animate towards that value
        timerBarAnimator.setDuration(duration); // in milliseconds
        timerBarAnimator.setInterpolator(new DecelerateInterpolator());
        timerBarAnimator.start();
    }

    private void stopTimerBarAnimation() {
        timerBarAnimator.cancel();
    }

    private void startCountdownTimer(long duration) {
        if (duration == 0) {
            timeTextView.setText("0");
            return;
        }
        countDownTimer = new CountDownTimer(duration, ONE_SECOND) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeTextView.setText(String.valueOf((int) TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)));
            }
            @Override
            public void onFinish() {
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
        for (String token : UserUtil.getAllTokens()) {
            /*if (token.equals(TOKEN))
                continue;*/
            NotificationUtil.sendNotification(notificationTitle, notificationMessage, token);
        }
    }
}