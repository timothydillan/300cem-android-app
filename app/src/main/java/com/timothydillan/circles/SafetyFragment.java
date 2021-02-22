package com.timothydillan.circles;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.timothydillan.circles.Services.CrashService;
import com.timothydillan.circles.Utils.PermissionUtil;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;

public class SafetyFragment extends Fragment {

    private PermissionUtil permissionUtil;
    private Button enableCrashDetectButton;
    private Button disableCrashDetectButton;
    private Button sosButton;
    private SharedPreferencesUtil sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionUtil = new PermissionUtil(requireContext());
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

        // When the view has been fully created, assign the views to the corresponding resource IDs.
        enableCrashDetectButton = view.findViewById(R.id.crashDetectButton);
        disableCrashDetectButton = view.findViewById(R.id.disableCrashDetectButton);
        sosButton = view.findViewById(R.id.sosButton);

        // If crash detection isn't enabled,
        if (!sharedPreferences.isCrashDetectionEnabled()) {
            // show the enable crash detection button, and hide the disable crash detet button.
            enableCrashDetectButton.setVisibility(View.VISIBLE);
            disableCrashDetectButton.setVisibility(View.GONE);
        } else {
            // else, hide the crash detection button, and show the disable crash detect button.
            enableCrashDetectButton.setVisibility(View.GONE);
            disableCrashDetectButton.setVisibility(View.VISIBLE);
        }

        // If the SOS button is clicked,
        sosButton.setOnClickListener(v -> {
            // redirect the activity to the SOSConfirmationActvity.
            startActivity(new Intent(requireActivity(), SOSConfirmationActivity.class));
        });

        enableCrashDetectButton.setOnClickListener(v -> {
            enableCrashDetection();
        });

        disableCrashDetectButton.setOnClickListener(v -> {
            // If the disable crash detection button is clicked, stop the service, hide the
            // disable crash detection button, and show the enable crash detection button.
            requireActivity().stopService(new Intent(requireContext(), CrashService.class));
            removeButton(disableCrashDetectButton);
            showButton(enableCrashDetectButton);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Ask for fit permissions
        permissionUtil.fitPermissions(requireActivity());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtil.FIT_REQUEST_CODE && grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PermissionsRequest", permissions[i] + " granted.");
                } else {
                    // If somehow one of the permissions are denied, show a permission dialog.
                    Log.d("PermissionsRequest", permissions[i] + " denied.");
                }
            }
            enableCrashDetection();
        }
    }

    private void enableCrashDetection() {
        // If the enable crash detection button is clicked, check if fit permissions is granted
        if (!permissionUtil.hasFitPermissions()) {
            // If it isn't granted, request for it, and do nothing.
            permissionUtil.fitPermissions(requireActivity());
            return;
        }
        // If permissions are granted, turn on the crash detection service,
        Intent crashForegroundService = new Intent(requireContext(), CrashService.class);
        ContextCompat.startForegroundService(requireContext(), crashForegroundService);
        // and write to sharedpreferences that the user has enabled crash detection.
        if (!sharedPreferences.writeBoolean(SharedPreferencesUtil.CRASH_KEY, true)) {
            // if the write operation failed, return and do nothing.
            return;
        }
        // then remove the enable crash detection buton, and show the disable crash detection button.
        removeButton(enableCrashDetectButton);
        showButton(disableCrashDetectButton);
    }

    private void removeButton(Button button) {
        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE.
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

    private void showButton(Button button) {
        // Animate the loading view to 100% opacity. After the animation ends,
        // set its visibility to VISIBLE.
        button.setAlpha(0f);
        button.setVisibility(View.VISIBLE);
        button.animate()
                .alpha(1f)
                .setDuration(800)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        button.setVisibility(View.VISIBLE);
                    }
                });
    }

}