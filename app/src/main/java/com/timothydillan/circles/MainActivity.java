package com.timothydillan.circles;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        //bottomNavigationView.getMenu().getItem(2).setEnabled(false);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new MapsFragment()).commit();
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                Fragment currentFragment = new MapsFragment();
                int itemId = item.getItemId();
                // Using if else instead since it is recommended to use it this way as resource IDs won't be fixed.
                if (itemId == R.id.location) {
                    currentFragment = new MapsFragment();
                } else if (itemId == R.id.mood) {
                    currentFragment = new MoodsFragment();
                } else if (itemId == R.id.safety) {
                    currentFragment = new SafetyFragment();
                } else if (itemId == R.id.settings) {
                    currentFragment = new SettingsFragment();
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment, currentFragment).commit();
                return true;
            };

    public void onSignOutClick(View v) {
        FirebaseAuth.getInstance().signOut();
        Intent signUpActivity = new Intent(this, SignUpActivity.class);
        startActivity(signUpActivity);
        finish();
    }

    public void onJoinCircleClick(View v) {
        Intent joinCircleActivity = new Intent(this, JoinCircleActivity.class);
        startActivity(joinCircleActivity);
        finish();
    }
}