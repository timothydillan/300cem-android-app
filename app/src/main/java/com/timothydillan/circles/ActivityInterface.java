package com.timothydillan.circles;

import android.content.Intent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

abstract class ActivityInterface extends AppCompatActivity {
    abstract void onTextClick(View v);
    void goToMainActivity() {
        Intent activity = new Intent();
        activity.setAction("com.timothydillan.circles.MainActivity");
        startActivity(activity);
    }
}