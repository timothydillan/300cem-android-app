package com.timothydillan.circles.UI;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.google.android.material.snackbar.Snackbar;
import com.timothydillan.circles.R;

public class ProgressButton {

    private String text;
    private Resources resources;
    private CardView button;
    private ConstraintLayout layout;
    private ProgressBar progressBar;
    private TextView textView;
    private ImageView indicatorImageView;
    private int oldResColor;

    public ProgressButton(String text, Resources resources, View view, @ColorRes int bgColor) {
        button = view.findViewById(R.id.progressButton);
        layout = view.findViewById(R.id.constraintLayout);
        progressBar = view.findViewById(R.id.progressBar);
        textView = view.findViewById(R.id.textView);
        indicatorImageView = view.findViewById(R.id.indicatorImageView);
        oldResColor = view.getResources().getColor(bgColor);
        this.resources = resources;
        this.text = text;

        layout.setBackgroundColor(oldResColor);
        textView.setText(text);
        textView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        indicatorImageView.setVisibility(View.GONE);
    }

    public void onLoading(String loadingText) {
        progressBar.setVisibility(View.VISIBLE);
        textView.setText(loadingText);
    }

    public void onLoading() {
        progressBar.setVisibility(View.VISIBLE);
        textView.setText("Please wait...");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onFinished() {
        progressBar.setVisibility(View.GONE);
        textView.setVisibility(View.GONE);
        layout.setBackgroundColor(button.getResources().getColor(R.color.green));
        Drawable drawable = ResourcesCompat.getDrawable(resources, R.drawable.animated_done, null);
        indicatorImageView.setImageDrawable(drawable);
        indicatorImageView.setVisibility(View.VISIBLE);
        if (drawable instanceof AnimatedVectorDrawableCompat) {
            AnimatedVectorDrawableCompat avdCompat = (AnimatedVectorDrawableCompat) drawable;
            avdCompat.start();
        } else if (drawable instanceof AnimatedVectorDrawable) {
            AnimatedVectorDrawable avd = (AnimatedVectorDrawable) drawable;
            avd.start();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onFailed() {
        progressBar.setVisibility(View.GONE);
        textView.setVisibility(View.GONE);
        layout.setBackgroundColor(button.getResources().getColor(R.color.red));
        Drawable drawable = ResourcesCompat.getDrawable(resources, R.drawable.animated_wrong, null);
        indicatorImageView.setImageDrawable(drawable);
        indicatorImageView.setVisibility(View.VISIBLE);
        if (drawable instanceof AnimatedVectorDrawableCompat) {
            AnimatedVectorDrawableCompat avdCompat = (AnimatedVectorDrawableCompat) drawable;
            avdCompat.start();
        } else if (drawable instanceof AnimatedVectorDrawable) {
            AnimatedVectorDrawable avd = (AnimatedVectorDrawable) drawable;
            avd.start();
        }
        new Handler().postDelayed(() -> resetButton(), 2000);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onFinished(@ColorRes int successColor) {
        progressBar.setVisibility(View.GONE);
        textView.setVisibility(View.GONE);
        layout.setBackgroundColor(button.getResources().getColor(successColor));
        Drawable drawable = indicatorImageView.getDrawable();
        indicatorImageView.setVisibility(View.VISIBLE);
        if (drawable instanceof AnimatedVectorDrawableCompat) {
            AnimatedVectorDrawableCompat avdCompat = (AnimatedVectorDrawableCompat) drawable;
            avdCompat.start();
        } else if (drawable instanceof AnimatedVectorDrawable) {
            AnimatedVectorDrawable avd = (AnimatedVectorDrawable) drawable;
            avd.start();
        }
    }

    public void resetButton() {
        indicatorImageView.setVisibility(View.GONE);
        layout.setBackgroundColor(oldResColor);
        textView.setText(text);
        textView.setVisibility(View.VISIBLE);
    }
}
