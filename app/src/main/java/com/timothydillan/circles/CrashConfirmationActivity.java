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
import com.timothydillan.circles.Utils.UserUtil;
import java.util.concurrent.TimeUnit;

public class CrashConfirmationActivity extends AppCompatActivity {

    private View userOkView;
    private View userHelpView;
    private ProgressButton userOkButton;
    private ProgressButton userHelpButton;
    private ProgressBar timerBar;
    private TextView timeTextView;
    private CountDownTimer countDownTimer;
    private static final String notificationTitle = "SOS";
    private static final String notificationMessage = UserUtil.getCurrentUser().getFirstName() + " is in danger! Check them out right now!";
    private static final long TIMER_DURATION = 30000;
    private static final long ONE_SECOND = 1000;
    private static final String TAG = "CrashConfirmation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_confirmation);

        userOkView = findViewById(R.id.userOkButton);
        userHelpView = findViewById(R.id.userHelpButton);
        userOkButton = new ProgressButton("I'm OK 👌", getResources(), userOkView, R.color.dark_blue);
        userHelpButton = new ProgressButton("I need help 🆘", getResources(), userHelpView, R.color.red);
        timerBar = findViewById(R.id.timerBar);
        timeTextView = findViewById(R.id.timeTextView);
        countDownTimer = new CountDownTimer(TIMER_DURATION, ONE_SECOND) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeTextView.setText(String.valueOf((int) TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)));
            }
            @Override
            public void onFinish() {
                sendHelpNotification();
                Toast.makeText(CrashConfirmationActivity.this, "Alerting other members.", Toast.LENGTH_SHORT).show();
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        countDownTimer.cancel();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ObjectAnimator animation = ObjectAnimator.ofInt(timerBar, "progress", 0, 2000); // see this max value coming back here, we animate towards that value
        animation.setDuration(TIMER_DURATION); // in milliseconds
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
        countDownTimer.start();
        userOkView.setOnClickListener(v -> {
            Toast.makeText(this, "Good to know.", Toast.LENGTH_SHORT).show();
            countDownTimer.cancel();
            finish();
        });
        userHelpView.setOnClickListener(v -> {
            sendHelpNotification();
            animation.pause();
            countDownTimer.cancel();
            Toast.makeText(this, "Alerting other members.", Toast.LENGTH_SHORT).show();
            userHelpButton.onFinished(R.color.red);
            new Handler().postDelayed(this::finish, 5000);
        });
    }

    public void sendHelpNotification() {
        for (String token : UserUtil.getTokens()) {
            /*if (token.equals(TOKEN))
                continue;*/
            NotificationUtil.sendNotification(notificationTitle, notificationMessage, token);
        }
    }
}