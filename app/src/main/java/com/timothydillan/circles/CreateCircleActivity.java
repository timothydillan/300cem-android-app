package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;

public class CreateCircleActivity extends AppCompatActivity implements CircleUtil.CircleUtilListener {

    private TextInputLayout circleNameInput;
    private CircleUtil circleUtil = CircleUtil.getInstance();
    private SharedPreferencesUtil sharedPreferences;
    private static final long DELAY_DURATION = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_circle);
        sharedPreferences = new SharedPreferencesUtil(this);

        Toolbar toolbar = findViewById(R.id.createCircleToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        circleNameInput = findViewById(R.id.circleNameInput);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register a circle listener to check whether the user has successfully created a circle.
        circleUtil.registerListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the circle when the user leaves the join circle activity.
        circleUtil.unregisterListener(this);
        // Make sure that the foreground state is set to true, so that the authentication process
        // runs only when the user completely leaves the app and goes back.
        sharedPreferences.writeBoolean(SharedPreferencesUtil.FOREGROUND_KEY, true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onCreateCircle(boolean success) {
        // If the user successfully created a new circle,
        if (success) {
            // Show a message, and redirect back to MainActivity.
            Toast.makeText(CreateCircleActivity.this, "Successfully created a new circle!", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(this::finish, DELAY_DURATION);
        // else, if the user failed to create a new circle
        } else {
            // Show a message too.
            Toast.makeText(CreateCircleActivity.this, "Failed to create a new circle", Toast.LENGTH_SHORT).show();
        }
    }

    public void onCreateCircleButtonClick(View v) {

        // If the user clicked the create button, get the circle name,
        String circleName = circleNameInput.getEditText().getText().toString();
        // and if the circle name is empty
        if (circleName.isEmpty()) {
            // Show an error
            circleNameInput.setErrorEnabled(true);
            circleNameInput.requestFocus();
            circleNameInput.setError("Please enter a name.");
            // and do nothing.
            return;
        }

        circleNameInput.setErrorEnabled(false);

        // Else, create an alert dialog that asks for the user's confirmation whether they will create a new circle.
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Are you sure?")
                .setMessage("Are you sure you want to create a new circle with the name: " + circleName + "?")
                .setCancelable(true)
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    // If the user confirms, create the circle.
                    circleUtil.createCircle(circleName);
                });
        builder.show();
    }
}