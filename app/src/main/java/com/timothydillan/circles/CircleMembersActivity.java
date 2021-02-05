package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.chip.Chip;
import com.timothydillan.circles.Utils.UserUtil;

public class CircleMembersActivity extends AppCompatActivity {

    private final String CIRCLE_CODE = String.valueOf(UserUtil.getCurrentUser().getCurrentCircleSession());
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circle_members);
        Toolbar toolbar = findViewById(R.id.inviteCodeToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        Chip inviteCodeChip = findViewById(R.id.circleInviteCodeChip);
        inviteCodeChip.setText(CIRCLE_CODE);
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