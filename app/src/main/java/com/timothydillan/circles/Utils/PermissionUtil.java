package com.timothydillan.circles.Utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionUtil {
    private static final int LOCATION_REQUEST_CODE = 100;
    private Context context;

    public PermissionUtil(Context context) {
        this.context = context;
    }

    public boolean hasLocationPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
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

    public void showPermissionsDialog(String permission) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Location Permission")
                .setMessage("This app needs access to " + permission + " for it to function properly.")
                .setNegativeButton("CANCEL", (dialog, which) -> Toast.makeText(context,
                        "Well.. that's a shame.", Toast.LENGTH_LONG).show())
                .setPositiveButton("OK", (dialog, which) -> requestLocationPermissions());
        builder.create().show();
    }

    public void requestLocationPermissions() {
        if (hasLocationPermissions()) {
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
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
}
