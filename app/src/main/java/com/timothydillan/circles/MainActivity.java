package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.timothydillan.circles.Services.CrashService;
import com.timothydillan.circles.Services.LocationService;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.FirebaseUtil;
import com.timothydillan.circles.Utils.LocationUtil;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;
import com.timothydillan.circles.Utils.UserUtil;

public class MainActivity extends AppCompatActivity {
    private SharedPreferencesUtil sharedPreferences;
    private static final String ACTIVITY_KEY = "ACTIVITY_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = new SharedPreferencesUtil(this);

        if (sharedPreferences.isCrashDetectionEnabled()) {
            if (!CrashService.isServiceRunning(this)) {
                Intent crashForegroundService = new Intent(this, CrashService.class);
                ContextCompat.startForegroundService(this, crashForegroundService);
            }
        }

        FirebaseUtil.initializeCurrentFirebaseUser();
        UserUtil userUtil = new UserUtil();
        final BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        userUtil.addEventListener(new UserUtil.UsersListener() {
            @Override
            public void onUserReady() {
                bottomNavigationView.setOnNavigationItemSelectedListener(navListener);
                showFragment(new MapsFragment());
            }

            @Override
            public void onUsersChange(@NonNull DataSnapshot snapshot) { }
        });
        userUtil.initializeCurrentUser();
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                Fragment currentFragment = new MapsFragment();
                int itemId = item.getItemId();
                if (itemId == R.id.location) {
                    currentFragment = new MapsFragment();
                } else if (itemId == R.id.mood) {
                    currentFragment = new MoodsFragment();
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

}