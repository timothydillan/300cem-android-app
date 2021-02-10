package com.timothydillan.circles.Utils;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.timothydillan.circles.Services.LocationService;

public class PermissionUtil {
    private static final boolean isDeviceQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;
    private static final int LOCATION_REQUEST_CODE = 100;
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

    public void showPermissionsDialog(String permission, boolean isLocation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Circles Permissions")
                .setMessage("This app needs access to " + permission + " for it to function properly.")
                .setNegativeButton("CANCEL", (dialog, which) -> Toast.makeText(context,
                        "Well.. that's a shame.", Toast.LENGTH_LONG).show())
                .setPositiveButton("OK", (dialog, which) -> {
                    if (isLocation) {
                        requestLocationPermissions();
                    } else {
                        requestFitPermissions();
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
                    {Manifest.permission.BODY_SENSORS, "com.google.android.gms.permission.ACTIVITY_RECOGNITION"}, LOCATION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions((Activity)context, new String[]
                    {Manifest.permission.BODY_SENSORS,
                            Manifest.permission.ACTIVITY_RECOGNITION}, LOCATION_REQUEST_CODE);
        }
    }


}
