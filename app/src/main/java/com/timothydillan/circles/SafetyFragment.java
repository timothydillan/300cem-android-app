package com.timothydillan.circles;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.timothydillan.circles.Services.CrashService;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;

public class SafetyFragment extends Fragment {

    private Button crashDetectButton;
    private ImageView enabledImageView;
    private SharedPreferencesUtil sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = new SharedPreferencesUtil(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_safety, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        crashDetectButton = view.findViewById(R.id.crashDetectButton);
        enabledImageView = view.findViewById(R.id.enabledImageView);
        enabledImageView.setVisibility(View.GONE);
        if (!sharedPreferences.isCrashDetectionEnabled()) {
            crashDetectButton.setVisibility(View.VISIBLE);
        } else {
            crashDetectButton.setVisibility(View.GONE);
            showEnabledAnimation();
        }
        crashDetectButton.setOnClickListener(v -> {
            // start the foreground service.
            Intent crashForegroundService = new Intent(requireContext(), CrashService.class);
            ContextCompat.startForegroundService(requireContext(), crashForegroundService);
            if (sharedPreferences.writeBoolean(SharedPreferencesUtil.CRASH_KEY, true)) {
                removeButton(crashDetectButton);
                showEnabledAnimation();
            }
        });
    }

    private void showEnabledAnimation() {
        Drawable drawable = ResourcesCompat.getDrawable(requireContext().getResources(), R.drawable.animated_done, null);
        enabledImageView.setImageDrawable(drawable);
        enabledImageView.setVisibility(View.VISIBLE);
        if (drawable instanceof AnimatedVectorDrawableCompat) {
            AnimatedVectorDrawableCompat avdCompat = (AnimatedVectorDrawableCompat) drawable;
            avdCompat.start();
        } else if (drawable instanceof AnimatedVectorDrawable) {
            AnimatedVectorDrawable avd = (AnimatedVectorDrawable) drawable;
            avd.start();
        }
    }

    private void removeButton(Button button) {

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        button.animate()
                .alpha(0f)
                .setDuration(800)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        button.setVisibility(View.GONE);
                    }
                });
    }

}