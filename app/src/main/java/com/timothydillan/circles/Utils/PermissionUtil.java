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

// A facade class made to use request and check for permissions easily. However, permission results still needs to be handled on the Activity.
public class PermissionUtil {
    private static final boolean isDeviceQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;
    public static final int LOCATION_REQUEST_CODE = 100;
    public static final int FIT_REQUEST_CODE = 101;
    public static final int BIOMETRIC_REQUEST_CODE = 102;
    private Context context;

    public PermissionUtil(Context context) {
        this.context = context;
    }

    public boolean hasLocationPermissions() {
        // If the device isn't Q or Later,
        if (!isDeviceQOrLater) {
            // The only permissions that we need to ask is the Fine and Coarse location permission.
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;
        } else {
            // However, if the device is Q or later, we also need to request the Background location permission
            // so that we're able to track the user's location in the background.
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
        // If the device isn't Q or Later,
        if (!isDeviceQOrLater) {
            // we need to ask for, explicitly, the com.google.android.gms.permission.ACTIVITY_RECOGNITION permission.
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
        // If a permission was rejected, a rationale dialog will be shown to the user.
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("Circles Permissions")
                .setMessage("This app needs access to " + permission + " for it to function properly.")
                .setNegativeButton("CANCEL", (dialog, which) -> Toast.makeText(context,
                        "Well.. that's a shame.", Toast.LENGTH_LONG).show())
                .setPositiveButton("OK", (dialog, which) -> {
                    switch (requestedPermission) {
                        // If the permission that is rejected is 0, or location related
                        case 0:
                            // then we'll request location permissions
                            requestLocationPermissions();
                            break;
                        // 1 = fit/activity permissions
                        case 1:
                            requestFitPermissions();
                            break;
                        // 2 = biometric permissions
                        case 2:
                            requestBiometricPermissions();
                            break;
                    }
                });
        builder.create().show();
    }



    public void requestLocationPermissions() {
        // When requesting location permission, we should check whether the location permissions have been granted beforehand.
        // If the permissions are already granted beforehand, we'll just return and do nothing.
        if (hasLocationPermissions()) {
            return;
        }

        /* The code below will ask the necessary permissions for background location tracking */
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
        // When requesting fit permissions, we should check whether the fit permissions have been granted beforehand.
        // If the permissions are already granted beforehand, we'll just return and do nothing.
        if (hasFitPermissions()) {
            return;
        }

        /* The code below will ask the necessary permissions for background location tracking */
        if (!isDeviceQOrLater) {
            ActivityCompat.requestPermissions((Activity)context, new String[]
                    {Manifest.permission.BODY_SENSORS, "com.google.android.gms.permission.ACTIVITY_RECOGNITION"}, FIT_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions((Activity)context, new String[]
                    {Manifest.permission.BODY_SENSORS,
                            Manifest.permission.ACTIVITY_RECOGNITION}, FIT_REQUEST_CODE);
        }
    }

    public void locationPermissions(Activity activity) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            showPermissionsDialog(Manifest.permission.ACCESS_BACKGROUND_LOCATION, 0);
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            showPermissionsDialog(Manifest.permission.ACCESS_FINE_LOCATION, 0);
        } else {
            requestLocationPermissions();
        }
    }

    public void fitPermissions(Activity activity) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            showPermissionsDialog(Manifest.permission.BODY_SENSORS, 1);
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACTIVITY_RECOGNITION)) {
            showPermissionsDialog(Manifest.permission.ACTIVITY_RECOGNITION, 1);
        } else {
            requestFitPermissions();
        }
    }

    public void biometricPermissions(Activity activity) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.USE_BIOMETRIC)) {
            showPermissionsDialog(Manifest.permission.USE_BIOMETRIC, 2);
        } else {
            requestBiometricPermissions();
        }
    }

    public boolean hasBiometricPermissions() {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        // Next, we'll check whether the device already has a Fingeprint/Face ID/Any other biometric authentication applied.
        if (!keyguardManager.isKeyguardSecure()) {
            // If it doesn't then we'll return false.
            return false;
        }

        // If it does, we'll then check whether if a biometric sensor is available in the device.
        if (BiometricManager.from(context).canAuthenticate() != BiometricManager.BIOMETRIC_SUCCESS) {
            // If it doesn't then we'll return false.
            return false;
        }

        // If it does, we'll then check whether the user has granted permissions for us to use their biometrics to authenticate them.
        return ContextCompat.checkSelfPermission(context, Manifest.permission.USE_BIOMETRIC)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean hasWatchApp() {
        /* This function checks whether the user has paired a Wearable, specifically wearables using the WearOS (since other wearables use third-party APIs)
        * by checking whether the WearOS app is installed. (this is a really bad method but :P) */
        try {
            // If the app does exist, return true.
            context.getPackageManager().getPackageInfo("com.google.android.wearable.app", PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            // else return false.
            return false;
        }
    }

    public void requestBiometricPermissions() {
        // When requesting biometric permissions, we should check whether the biometric permissions have been granted beforehand.
        // If the permissions are already granted beforehand, we'll just return and do nothing.
        if (hasBiometricPermissions()) {
            return;
        }

        /* The code below will ask the necessary permissions for biometric usage */
        ActivityCompat.requestPermissions((Activity)context, new String[]
                {Manifest.permission.USE_BIOMETRIC}, BIOMETRIC_REQUEST_CODE);
    }
}
