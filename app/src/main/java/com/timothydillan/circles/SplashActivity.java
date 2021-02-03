package com.timothydillan.circles;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.timothydillan.circles.Utils.CircleUtil;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // postDelayed(Runnable r, Object token, long delayMillis)
        // Causes the Runnable r to be added to the message queue, to be run after the specified amount of time elapses.
        // In here, we override the run method that belongs to the Runnable object and instead load the Main Activity
        // after 3 seconds.
        final int SPLASH_SCREEN_DURATION = 3000;
        new Handler().postDelayed(() -> {
            Intent mainActivity = new Intent(SplashActivity.this, SignUpActivity.class);
            startActivity(mainActivity);
            finish();
        }, SPLASH_SCREEN_DURATION);

    }
}