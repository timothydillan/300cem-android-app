package com.timothydillan.circles.Utils;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.timothydillan.circles.R;

public class PermissionUtil {
    private static final boolean isDeviceQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;
    public static final int LOCATION_REQUEST_CODE = 100;
    public static final int FIT_REQUEST_CODE = 101;
    public static final int FINGERPRINT_REQUEST_CODE = 102;
    private Context context;

    public PermissionUtil(Context context) {
        this.context = context;
    }

    public boolean hasLocationPermissions() {
        if (!isDeviceQOrLater) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public boolean hasFitPermissions() {
        if (!isDeviceQOrLater) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, "com.google.android.gms.permission.ACTIVITY_RECOGNITION")
                    == PackageManager.PERMISSION_GRANTED;

        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS)
                            == PackageManager.PERMISSION_GRANTED;
        }
    }

    public void showPermissionsDialog(String permission, int requestedPermission) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.AlertDialogStyle);
        builder.setTitle("Circles Permissions")
                .setMessage("This app needs access to " + permission + " for it to function properly.")
                .setNegativeButton("CANCEL", (dialog, which) -> Toast.makeText(context,
                        "Well.. that's a shame.", Toast.LENGTH_LONG).show())
                .setPositiveButton("OK", (dialog, which) -> {
                    switch (requestedPermission) {
                        // if it's location
                        case 0:
                            requestLocationPermissions();
                            break;
                        case 1:
                            requestFitPermissions();
                            break;
                        case 2:
                            requestBiometricPermissions();
                            break;
                    }
                });
        builder.create().show();
    }



    public void requestLocationPermissions() {
        if (hasLocationPermissions()) {
            return;
        }

        if (!isDeviceQOrLater) {
            ActivityCompat.requestPermissions((Activity)context, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions((Activity)context, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public void requestFitPermissions() {
        if (hasFitPermissions()) {
            return;
        }

        if (!isDeviceQOrLater) {
            ActivityCompat.requestPermissions((Activity)context, new String[]
                    {Manifest.permission.BODY_SENSORS, "com.google.android.gms.permission.ACTIVITY_RECOGNITION"}, FIT_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions((Activity)context, new String[]
                    {Manifest.permission.BODY_SENSORS,
                            Manifest.permission.ACTIVITY_RECOGNITION}, FIT_REQUEST_CODE);
        }
    }



    public boolean hasBiometricPermissions() {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        if (!keyguardManager.isKeyguardSecure()) {
            return false;
        }

        if (BiometricManager.from(context).canAuthenticate() != BiometricManager.BIOMETRIC_SUCCESS) {
            return false;
        }

        return ContextCompat.checkSelfPermission(context, Manifest.permission.USE_BIOMETRIC)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean hasWatchApp() {
        try {
            context.getPackageManager().getPackageInfo("com.google.android.wearable.app", PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            //android wear app is not installed
            return false;
        }
    }

    public void requestBiometricPermissions() {
        if (hasBiometricPermissions()) {
            return;
        }
        ActivityCompat.requestPermissions((Activity)context, new String[]
                {Manifest.permission.USE_BIOMETRIC}, FINGERPRINT_REQUEST_CODE);
    }
}
