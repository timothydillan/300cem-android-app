package com.timothydillan.circles;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onSignOutClick(View v) {
        FirebaseAuth.getInstance().signOut();
        Intent signUpActivity = new Intent(MainActivity.this, SignUpActivity.class);
        startActivity(signUpActivity);
        finish();
    }
}