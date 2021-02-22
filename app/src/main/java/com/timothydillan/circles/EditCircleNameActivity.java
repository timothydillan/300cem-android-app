package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.FirebaseUtil;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;
import com.timothydillan.circles.Utils.UserUtil;

public class EditCircleNameActivity extends AppCompatActivity implements CircleUtil.CircleUtilListener {

    private Button updateButton;
    private SharedPreferencesUtil sharedPreferences;
    private TextInputEditText circleNameTextEdit;
    private CircleUtil circleUtil = CircleUtil.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_circle_name);
        sharedPreferences = new SharedPreferencesUtil(this);

        Toolbar toolbar = findViewById(R.id.editCircleToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        circleNameTextEdit = findViewById(R.id.circleNameTextEdit);
        updateButton = findViewById(R.id.updateButton);

        // Set the edit text to the current circle name.
        circleNameTextEdit.setText(FirebaseUtil.getCurrentCircleName());

        updateButton.setOnClickListener(v -> editCircleName());
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
    protected void onPause() {
        super.onPause();
        // Unregister the circle when the user leaves the join circle activity.
        circleUtil.unregisterListener(this);
        // Make sure that the foreground state is set to true, so that the authentication process
        // runs only when the user completely leaves the app and goes back.
        sharedPreferences.writeBoolean(SharedPreferencesUtil.FOREGROUND_KEY, true);
    }

    @Override
    public void onEditCircleName(boolean success) {
        // If the circle name was successfully edited,
        if (success) {
            // show a successful toast message.
            Toast.makeText(this, "Successfully edited circle name.", Toast.LENGTH_SHORT).show();
        // Else,
        } else {
            // show a failure toast message.
            Toast.makeText(this, "Failed to edit circle name.", Toast.LENGTH_SHORT).show();
        }
    }

    private void editCircleName() {
        // If the edit button is clicked, get the edit text's text
        String circleName = circleNameTextEdit.getText().toString();

        // and first check whether it's empty or not.
        if (circleName.isEmpty()) {
            // if it is, then show an error and do nothing.
            circleNameTextEdit.setError("Circle name can't be empty!");
            return;
        }

        // if the circle name isn't empty, but the text is still the same as the current circle name,
        if (circleName.equals(FirebaseUtil.getCurrentCircleName())) {
            // also show an error and do nothing.
            circleNameTextEdit.setError("The circe name is the same!");
            return;
        }

        // If the sanity checks above are passed, then remove the error
        circleNameTextEdit.setError(null);
        // and edit the circle name.
        circleUtil.editCircleName(circleName);
    }
}