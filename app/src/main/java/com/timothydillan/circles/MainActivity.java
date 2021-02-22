package com.timothydillan.circles;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.timothydillan.circles.Services.CrashService;
import com.timothydillan.circles.UI.Message;
import com.timothydillan.circles.Utils.FirebaseUtil;
import com.timothydillan.circles.Utils.SecurityUtil;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;
import com.timothydillan.circles.Utils.UserUtil;

public class MainActivity extends AppCompatActivity implements UserUtil.UsersListener {

    private Message message;
    private SharedPreferencesUtil sharedPreferences;
    private SecurityUtil securityUtil;
    private UserUtil userUtil = UserUtil.getInstance();
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = new SharedPreferencesUtil(this);
        securityUtil = new SecurityUtil(this);
        message = new Message(this);
        initializeServicesAndUtils();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* Every time the user leaves (onDestroy()) and then comes back to the app,
        authenticate the user (if the user enabled authentication on settings). */
        authenticateUser();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /* when the activity reaches the onDestroy cycle, we can safely assume that the user has left
        * the app completely, so we should set the wasAppInForeground boolean to false so that
        * the next time the user launches the app, the user would have to go through authentication. */
        sharedPreferences.writeBoolean(SharedPreferencesUtil.FOREGROUND_KEY, false);
    }

    @Override
    public void onUserReady() {
        /* When the current user has been properly initialized, we should initialize their role in the circle,
         * the current circle name (for the edit circle name activity), and
         * enable the navigation on the bottom nav. Also, by default, the Map fragment will be shown. */
        userUtil.initializeCurrentUserRole();
        FirebaseUtil.initializeCircleName();
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);
        showFragment(new MapsFragment());
        // Unregister the listener when the initialization is done, because we only need to initialize it once.
        userUtil.unregisterListener(this);
    }

    public void showFragment(Fragment fragment){
        // A really "ghetto" way to prevent the app from crashing (FragmentManager has been destroyed) on app launch.
        // While the fragment manager is still destroyed, just do nothing.
        while (getSupportFragmentManager().isDestroyed() && isFinishing()) {
            return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .setReorderingAllowed(true)
                .replace(R.id.fragment, fragment)
                .commitAllowingStateLoss();
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

    private void initializeServicesAndUtils() {
        // If the user turned on crash detection,
        if (sharedPreferences.isCrashDetectionEnabled()) {
            // first check whether the service is already running
            if (!CrashService.isServiceRunning(this)) {
                // if it's not running, run the crash detection service.
                Intent crashForegroundService = new Intent(this, CrashService.class);
                ContextCompat.startForegroundService(this, crashForegroundService);
            }
        }
        /* Initialize the Firebase user on the Firebase Util singleton (global access point).
         * We'll also register a listener to check whether the user is ready (has been initialized).
         * Afterwards, we initialize the circles that the user is currently in, and initialize the user.
         */
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        FirebaseUtil.initializeCurrentFirebaseUser();
        userUtil.registerListener(this);
        userUtil.initializeRegisteredCircles();
        userUtil.initializeCurrentUser();
    }

    private void authenticateUser() {
        // If the app was in foreground (or was still active and the user is accessing the app again)
        if (sharedPreferences.wasAppInForeground()) {
            // return and do nothing.
            return;
        }
        // Else, first check whether the biometrics and password authentication is both on.
        if (sharedPreferences.isBiometricsSecurityEnabled() && sharedPreferences.isPasswordSecurityEnabled()) {
            // If they're both on, add a security listener to check whether the user has successfully authenticated themselves.
            securityUtil.addListener(new SecurityUtil.SecurityListener() {
                @Override
                public void onAuthenticationSuccessful() {
                    // if the biometric authentication succeeded, show a toast message
                    message.showToastMessage("Biometric authentication successful. Proceeding to password authentication.");
                    // and proceed to password authentication.
                    securityUtil.authenticateUserPassword(MainActivity.this);
                }
                @Override
                public void onAuthenticationFailed() {
                    // If the authentication failed, show a toast message and exit the app.
                    message.showToastMessage("Authentication failed.");
                    finish();
                }
            });
            // Start the biometric authentication first.
            securityUtil.authenticateUserBiometrics();
        // If the password auth is not on, but the biometrics auth is,
        } else if (sharedPreferences.isBiometricsSecurityEnabled()) {
            // again, add a listener like we did before, and show a message accordingly.
            securityUtil.addListener(new SecurityUtil.SecurityListener() {
                @Override
                public void onAuthenticationSuccessful() {
                    message.showToastMessage("Authentication successful!");
                }
                @Override
                public void onAuthenticationFailed() {
                    message.showToastMessage("Authentication failed.");
                    finish();
                }
            });
            securityUtil.authenticateUserBiometrics();
        // If the password auth is on and the biometrics auth isn't,
        } else if (sharedPreferences.isPasswordSecurityEnabled()) {
            // start the password authentication.
            securityUtil.authenticateUserPassword(MainActivity.this);
        }
    }

}