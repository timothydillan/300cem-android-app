package com.timothydillan.circles.UI;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

public class Message {
    private Context ctx;
    private View v;

    public Message(Context context) {
        ctx = context;
    }

    public void showToastMessage(String message) {
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
    }

}
