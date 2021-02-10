package com.timothydillan.circles.Services;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.timothydillan.circles.MainActivity;
import com.timothydillan.circles.R;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.FirebaseUtil;
import com.timothydillan.circles.Utils.PermissionUtil;
import com.timothydillan.circles.Utils.UserUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LocationService extends Services {
    private static final String CHANNEL_ID = "circlesLocationChannel";
    private static final long REFRESH_LOC_TIME = 3000;
    private static final float MIN_LOC_DIST = 10F;
    private static final String USER_UID = UserUtil.getCurrentUser().getUid();
    private static final DatabaseReference databaseReference = FirebaseUtil.getDbReference();
    private PermissionUtil permissionUtil;

    private FusedLocationProviderClient locationProvider;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        permissionUtil = new PermissionUtil(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationProvider = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.d("LOCATION SERVICE", "Location updated.");
                    updateUserLocation(location);
                }
            }
        };
        getUserLocation();
        Intent mapActivityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mapActivityIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("location notif")
                .setContentText("hey there. we're tracking your location 🌝📍.")
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        return START_NOT_STICKY;
    }

    @Override
    protected void initializeNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "circles", NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    private void updateUserLocation(Location location) {
        // Get current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm EEEE");
        String currentDateAndTime = dateFormat.format(new Date());
        // Update latitude, longitude, and the last sharing time of the users.
        databaseReference.child("Users").child(USER_UID).child("latitude").setValue(location.getLatitude());
        databaseReference.child("Users").child(USER_UID).child("longitude").setValue(location.getLongitude());
        databaseReference.child("Users").child(USER_UID).child("lastSharingTime").setValue(currentDateAndTime);
    }

    @SuppressLint("MissingPermission")
    private void getUserLocation() {
        // Initialize Location Request
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setSmallestDisplacement(MIN_LOC_DIST);
        locationRequest.setFastestInterval(REFRESH_LOC_TIME);
        locationRequest.setInterval(REFRESH_LOC_TIME);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Sanity check so that requesting location updates doesn't crash.
        if (!permissionUtil.hasLocationPermissions()) {
            return;
        }

        locationProvider.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    //https://stackoverflow.com/a/47692206
    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationService.class.getName().equals(service.service.getClassName())) {
                return service.foreground;
            }
        }
        return false;
    }
}
