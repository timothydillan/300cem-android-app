package com.timothydillan.circles.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationResult;
import com.google.firebase.database.DatabaseReference;
import com.timothydillan.circles.Utils.CircleUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LocationService extends BroadcastReceiver {
    public static final String TAG = "UPDATE_LOCATION";
    private static final String USER_UID = CircleUtil.getCurrentMember().getUid();
    private static final DatabaseReference databaseReference = CircleUtil.databaseReference;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        if (!intent.getAction().equals(TAG)) {
            return;
        }
        Location location = LocationResult.extractResult(intent).getLastLocation();
        if (location != null) {
            Log.d("LOCATION service", "Location service updated.");
            updateUserLocation(location);
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
}
