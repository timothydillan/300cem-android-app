package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.timothydillan.circles.Services.CrashService;
import com.timothydillan.circles.UI.Message;
import com.timothydillan.circles.Utils.FirebaseUtil;
import com.timothydillan.circles.Utils.SecurityUtil;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;
import com.timothydillan.circles.Utils.UserUtil;

public class MainActivity extends AppCompatActivity {

    private Message message;
    private SharedPreferencesUtil sharedPreferences;
    private SecurityUtil securityUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = new SharedPreferencesUtil(this);
        securityUtil = new SecurityUtil(this);
        message = new Message(this);
        initializeFragments();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!sharedPreferences.wasAppInForeground() && sharedPreferences.isBiometricsSecurityEnabled()) {
            securityUtil.addListener(new SecurityUtil.SecurityListener() {
                @Override
                public void onAuthenticationSuccessful() {
                    message.showToastMessage("Authentication successful!");
                }
                @Override
                public void onAuthenticationFailed() {
                    finish();
                    message.showToastMessage("Authentication failed.");
                }
            });
            securityUtil.authenticateUserBiometrics();
        }
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                Fragment currentFragment = new MapsFragment();
                int itemId = item.getItemId();
                if (itemId == R.id.location) {
                    currentFragment = new MapsFragment();
                } else if (itemId == R.id.mood) {
                    currentFragment = new MoodsFragment();
                } else if (itemId == R.id.health) {
                    currentFragment = HealthFragment.create(1);
                } else if (itemId == R.id.safety) {
                    currentFragment = new SafetyFragment();
                } else if (itemId == R.id.settings) {
                    currentFragment = new SettingsFragment();
                }
                showFragment(currentFragment);
                return true;
            };

    public void showFragment(Fragment fragment){
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .setReorderingAllowed(true)
                .replace(R.id.fragment, fragment)
                .commit();
    }

    private void initializeFragments() {
        if (sharedPreferences.isCrashDetectionEnabled()) {
            if (!CrashService.isServiceRunning(this)) {
                Intent crashForegroundService = new Intent(this, CrashService.class);
                ContextCompat.startForegroundService(this, crashForegroundService);
            }
        }

        FirebaseUtil.initializeCurrentFirebaseUser();
        UserUtil userUtil = new UserUtil();
        userUtil.initializeRegisteredCircles();
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        userUtil.addEventListener(new UserUtil.UsersListener() {
            @Override
            public void onUserReady() {
                FirebaseUtil.initializeCircleName();
                bottomNavigationView.setOnNavigationItemSelectedListener(navListener);
                showFragment(new MapsFragment());
            }

            @Override
            public void onUsersChange(@NonNull DataSnapshot snapshot) { }
        });
        userUtil.initializeCurrentUser();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPreferences.writeBoolean(SharedPreferencesUtil.ACTIVITY_APP_KEY, false);
    }
}