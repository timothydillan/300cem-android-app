package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;
import com.timothydillan.circles.Utils.UserUtil;

public class InviteCodeActivity extends AppCompatActivity {

    private SharedPreferencesUtil sharedPreferences;
    private final String CURRENT_CIRCLE_CODE = String.valueOf(UserUtil.getInstance().getCurrentUser().getCurrentCircleSession());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_code);
        sharedPreferences = new SharedPreferencesUtil(this);

        Toolbar toolbar = findViewById(R.id.inviteCodeToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        Chip inviteCodeChip = findViewById(R.id.circleInviteCodeChip);

        // Set the chip text to the current user's circle code
        inviteCodeChip.setText(CURRENT_CIRCLE_CODE);
        // if the chip is being clicked,
        inviteCodeChip.setOnLongClickListener(v -> {
            // get the clipboard manager of the device,
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // create a new clip data with the current circle code used as the data
            ClipData clipData = ClipData.newPlainText(getResources().getString(R.string.app_name),
                    CURRENT_CIRCLE_CODE);
            // and send the new clip data to the clipboard manager.
            clipboardManager.setPrimaryClip(clipData);
            // After successfully copying the code, a message will be shown.
            Snackbar.make(findViewById(android.R.id.content),"Copied invite code to clipboard.", Snackbar.LENGTH_SHORT).show();
            return false;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Make sure that the foreground state is set to true, so that the authentication process
        // runs only when the user completely leaves the app and goes back.
        sharedPreferences.writeBoolean(SharedPreferencesUtil.FOREGROUND_KEY, true);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        return super.onOptionsItemSelected(item);
    }

    public void onShareButtonClick(View v) {
        // If the share button is clicked, create an intent with ACITON_SEND as the action of the intent
        // to specify that we're going to send data to someone else.
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        // For the data inside, we'll put a short message that contains the current circle code,
        sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share_message) + CURRENT_CIRCLE_CODE);
        sendIntent.putExtra(Intent.EXTRA_TITLE, getResources().getString(R.string.share_title));
        sendIntent.setType("text/plain");
        // and then start the intent with Intent.createChooser to allow the user to pick where they want to send the code to.
        startActivity(Intent.createChooser(sendIntent, "Send your circle code using: "));
    }

}