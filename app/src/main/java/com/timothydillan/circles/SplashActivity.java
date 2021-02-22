package com.timothydillan.circles;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.timothydillan.circles.Utils.FirebaseUtil;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        // On the first launch of the app, the DB, Auth, and Storage references will be initialized
        FirebaseUtil.initializeFirebaseDbAuthStorage();
        // The FirebaseUser will also be initialized so that we can redirect the user to the appropriate activity
        FirebaseUtil.initializeCurrentFirebaseUser();
        final int SPLASH_SCREEN_DURATION = 3000;

        TextView appTitleTextView = findViewById(R.id.appNameLabel);
        ImageView logoImageView = findViewById(R.id.logoImage);
        logoImageView.setAlpha(0f);
        Animation slideInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        slideInAnimation.setDuration(2000);
        logoImageView.animate().alpha(1f).setDuration(2000);
        appTitleTextView.startAnimation(slideInAnimation);

        // postDelayed(Runnable r, Object token, long delayMillis)
        // Causes the Runnable r to be added to the message queue, to be run after the specified amount of time elapses.
        // In here, we override the run method that belongs to the Runnable object and instead load the Main Activity
        // after 3 seconds.
        new Handler().postDelayed(() -> {
            Intent activity = new Intent();
            // If the user is logged in,
            if (FirebaseUtil.getCurrentUser() != null) {
                // we'll redirect them to the MainActivity
                activity.setClass(this, MainActivity.class);
            } else {
                // else, we'll redirect them to the sign up activity instead.
                activity.setClass(this, SignUpActivity.class);
            }
            startActivity(activity);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_SCREEN_DURATION);

    }
}