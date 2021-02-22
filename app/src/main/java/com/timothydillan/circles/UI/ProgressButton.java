package com.timothydillan.circles.UI;

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

import com.timothydillan.circles.R;

// A modified version of: https://www.youtube.com/watch?v=zv9R5EcRKHM
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

        // Initially, we'll make the progress bar and the success/failure indicator invisible, and set
        // the button's text view to the argument passed.
        layout.setBackgroundColor(oldResColor);
        textView.setText(text);
        textView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        indicatorImageView.setVisibility(View.GONE);
    }

    public void onLoading() {
        // When the onLoading() function is triggered, the progress bar will be visible, and the text view will change to
        // Please wait instead.
        progressBar.setVisibility(View.VISIBLE);
        textView.setText("Please wait...");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onFinished() {
        // When the onFinish() function is triggered, the progress bar will be gone and the text view will be gone
        progressBar.setVisibility(View.GONE);
        textView.setVisibility(View.GONE);
        // and the background color of the button would be green, indicating a successfull operation.
        layout.setBackgroundColor(button.getResources().getColor(R.color.green));
        // We'll then set the indicator image view's drawable to the tick drawable
        Drawable drawable = ResourcesCompat.getDrawable(resources, R.drawable.animated_done, null);
        indicatorImageView.setImageDrawable(drawable);
        indicatorImageView.setVisibility(View.VISIBLE);
        // and start the animation.
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
        // Similar to the previous function, when this function is triggered, the progress bar will be gone and the text view will be gone
        progressBar.setVisibility(View.GONE);
        textView.setVisibility(View.GONE);
        // the background color of the button would be red, indicating that the operation failed.
        layout.setBackgroundColor(button.getResources().getColor(R.color.red));
        // We'll then set the indicator image view's drawable to the X drawable
        Drawable drawable = ResourcesCompat.getDrawable(resources, R.drawable.animated_wrong, null);
        indicatorImageView.setImageDrawable(drawable);
        indicatorImageView.setVisibility(View.VISIBLE);
        // and start the animation.
        if (drawable instanceof AnimatedVectorDrawableCompat) {
            AnimatedVectorDrawableCompat avdCompat = (AnimatedVectorDrawableCompat) drawable;
            avdCompat.start();
        } else if (drawable instanceof AnimatedVectorDrawable) {
            AnimatedVectorDrawable avd = (AnimatedVectorDrawable) drawable;
            avd.start();
        }
        // and after 1.5 seconds, the button would be reset.
        new Handler().postDelayed(this::resetButton, 1500);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onFinished(@ColorRes int successColor) {
        // This function is exactly the same as the other onFinished() function, but it allows
        // a custom color to be used.
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
        // When the reset button is triggered, the indicator image view would be invisible,
        // the background color of the button would revert to the old color, and the text would also revert to the old text.
        indicatorImageView.setVisibility(View.GONE);
        layout.setBackgroundColor(oldResColor);
        textView.setText(text);
        textView.setVisibility(View.VISIBLE);
    }
}
