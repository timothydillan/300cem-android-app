package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;
import com.timothydillan.circles.Utils.UserUtil;

public class CircleMembersActivity extends AppCompatActivity {

    private SharedPreferencesUtil sharedPreferences;
    private final String CIRCLE_CODE = String.valueOf(UserUtil.getCurrentUser().getCurrentCircleSession());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circle_members);
        sharedPreferences = new SharedPreferencesUtil(this);
        Toolbar toolbar = findViewById(R.id.inviteCodeToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        Chip inviteCodeChip = findViewById(R.id.circleInviteCodeChip);
        inviteCodeChip.setText(CIRCLE_CODE);
        inviteCodeChip.setOnLongClickListener(v -> {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText(getResources().getString(R.string.app_name),
                    inviteCodeChip.getText().toString());
            clipboardManager.setPrimaryClip(clipData);
            Snackbar.make(findViewById(android.R.id.content),"Copied invite code to clipboard.", Snackbar.LENGTH_SHORT).show();
            return false;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.writeBoolean(SharedPreferencesUtil.ACTIVITY_APP_KEY, true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }

    public void onShareButtonClick(View v) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Hey there! Join my circle on the circles. app! Here's my code: " + CIRCLE_CODE);
        sendIntent.putExtra(Intent.EXTRA_TITLE, "Send circle code");
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Send your circle code using: "));
    }

}