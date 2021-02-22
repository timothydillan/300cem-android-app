package com.timothydillan.circles.Services;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.util.Log;


import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.timothydillan.circles.MainActivity;
import com.timothydillan.circles.R;
import com.timothydillan.circles.Utils.LocationUtil;
import com.timothydillan.circles.Utils.PermissionUtil;

import static android.app.PendingIntent.FLAG_ONE_SHOT;

public class LocationService extends Services {
    private static final String CHANNEL_ID = "circlesLocationChannel";
    private static final long REFRESH_LOC_TIME = 3000;
    private static final float MIN_LOC_DIST = 10F;

    private PermissionUtil permissionUtil;
    private LocationUtil locationUtil;
    private FusedLocationProviderClient locationProvider;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        permissionUtil = new PermissionUtil(this);
        locationUtil = new LocationUtil(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If the stop button was clicked,
        if (intent.getAction() != null) {
            if (intent.getAction().equals(STOP_SERVICE)) {
                // we'll remove the notification and stop the service.
                stopForeground(true);
                stopSelf();
            }
        }
        // We'll create a location provider that will be used to request location updates,
        locationProvider = LocationServices.getFusedLocationProviderClient(this);
        // and a location callback instance that listens to locatino changes
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult == null) {
                    return;
                }
                // and if a location change has been made,
                for (Location location : locationResult.getLocations()) {
                    Log.d("LOCATION SERVICE", "Location updated.");
                    // we'll update the user's location.
                    locationUtil.updateUserLocation(location);
                }
            }
        };

        getUserLocation();

        /* We'll create two intents, one allowing the user to navigate to the Map fragment, and one stopping the service. */
        Intent mapIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mapIntent, FLAG_ONE_SHOT);

        Intent stopIntent = new Intent(this, LocationService.class);
        stopIntent.setAction(STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("location notif")
                .setContentText("hey there. we're tracking your location 📍.")
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.logo, "Stop", stopPendingIntent);

        startForeground(1, notification.build());
        return START_NOT_STICKY;
    }

    @Override
    protected void initializeNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "circles", NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager notificationManager = ContextCompat.getSystemService(this, NotificationManager.class);
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        locationProvider.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }

    @SuppressLint("MissingPermission")
    private void getUserLocation() {
        // Initialize location request that updates the user's location if the user moves 10m from the initial position,
        // with an interval of 3 seconds to check if the user moves.
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setSmallestDisplacement(MIN_LOC_DIST);
        locationRequest.setFastestInterval(REFRESH_LOC_TIME);
        locationRequest.setInterval(REFRESH_LOC_TIME);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Sanity check so that requesting location updates doesn't crash.
        if (!permissionUtil.hasLocationPermissions()) {
            return;
        }

        // If the location permissions are granted, then we'll request location updates, enabling the location callback.
        locationProvider.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    //https://stackoverflow.com/a/47692206
    public static boolean isServiceRunning(Context context) {
        /* This function checks whether the service is running */
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationService.class.getName().equals(service.service.getClassName())) {
                return service.foreground;
            }
        }
        return false;
    }
}
