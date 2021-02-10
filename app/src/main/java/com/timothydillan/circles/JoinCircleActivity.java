package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.timothydillan.circles.Models.Circle;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.UserUtil;

import java.util.ArrayList;

public class JoinCircleActivity extends ActivityInterface {
    private static final String TAG = "JoinCircleActivity";
    private TextInputLayout circleInput;
    private CircleUtil circleUtil = new CircleUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_circle);

        Toolbar toolbar = findViewById(R.id.joinToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        circleInput = findViewById(R.id.circleCodeInputLayout);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }

    public void onJoinButtonClick(View v) {

        String circleCode = circleInput.getEditText().getText().toString();

        if (circleCode.length() != 6) {
            circleInput.setErrorEnabled(true);
            circleInput.setError("Error: A code has 6 digits.");
            circleInput.requestFocus();
            return;
        } else {
            circleInput.setErrorEnabled(false);
        }

        circleUtil.joinCircle(circleCode);

        circleUtil.addEventListener(new CircleUtil.CircleUtilListener() {
            @Override
            public void onCircleReady(ArrayList<User> members) { }
            @Override
            public void onCircleChange() { }
            @Override
            public void onJoinCircle(boolean success) {
                if (success) {
                    Log.d(TAG, "Successfully joined the circle.");
                    showSnackbar("Successfully joined the circle.");
                } else {
                    Log.d(TAG, "Failed to join the circle.");
                    showSnackbar("Failed to join the circle.");
                }
            }
        });
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }
}