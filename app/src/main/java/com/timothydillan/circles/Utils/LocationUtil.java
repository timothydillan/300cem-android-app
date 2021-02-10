package com.timothydillan.circles.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.database.DataSnapshot;
import com.google.maps.android.clustering.ClusterManager;
import com.timothydillan.circles.Models.ClusterMarker;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.R;
import com.timothydillan.circles.Services.LocationService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LocationUtil {
    private static final String TAG = "LocationUtil";
    private static final LatLng SINGAPORE_COORDINATES = new LatLng(1.290270, 103.851959);
    private Context ctx;
    private static GoogleMap map = null;
    private static HashMap<User, ClusterMarker> membersLocation = new HashMap<>();
    private static CameraPosition lastCameraPosition = null;
    private static Geocoder geocoder;
    private ClusterManager<ClusterMarker> clusterManager;
    private ClusterMarkerManager clusterRenderer;

    public LocationUtil(Context context) {
        ctx = context;
        geocoder = new Geocoder(ctx);
    }

    public void setMap(GoogleMap map) {
        LocationUtil.map = map;
    }

    public void setMapAppearanceMode() {
        // If the device is night/dark mode,
        if ((ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES) {
            // set the map appearance to dark mode
            changeMapAppearance(R.raw.darkmap);
        } else {
            // else, set it to the normal light mode.
            changeMapAppearance(R.raw.lightmap);
        }
    }

    private void changeMapAppearance(int resourceId) {
        // https://developers.google.com/maps/documentation/android-sdk/styling
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = map.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            ctx, resourceId));
            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
    }

    public void initializeMarkers(ArrayList<User> circleMembers) {

        // Clear the membersLocation hash map so that old data does not overlap with new ones.
        membersLocation.clear();

        // If we haven't initialized the foreground service,
        if (!LocationService.isServiceRunning(ctx)) {
            // start the foreground service.
            Intent locationForegroundService = new Intent(ctx, LocationService.class);
            ContextCompat.startForegroundService(ctx, locationForegroundService);
        }

        initializeClusterManagers();

        // Then for each member from the retrieved arraylist,
        for (User circleMember : circleMembers) {
            // Get their location
            LatLng memberLocation = new LatLng(circleMember.getLatitude(), circleMember.getLongitude());
            ClusterMarker clusterMarker = new ClusterMarker(circleMember);
            clusterManager.addItem(clusterMarker);
            // And create a marker with their location
            // Marker memberMarker = map.addMarker(new MarkerOptions().position(memberLocation).title(circleMember.getFirstName() + "'s Location"));
            // Animate/move the camera to the current user with a zoom of 15
            if (circleMember.getUid().equals(UserUtil.getCurrentUser().getUid()))
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(memberLocation, 15.0f));
            // and put it into the hashmap.
            membersLocation.put(circleMember, clusterMarker);
        }

        clusterManager.cluster();
    }

    public ArrayList<User> getUpdatedInformation(@NonNull DataSnapshot snapshot) {
        // Create an array list to store newly updated information
        ArrayList<User> newMemberInformation = new ArrayList<>();
        // For each of the data received,
        for (DataSnapshot ds : snapshot.getChildren()) {
            // and for every member in the circle
            for (Map.Entry<User, ClusterMarker> circleMembers : membersLocation.entrySet()) {
                // Get their current marker and user details
                ClusterMarker currentMemberMarker = circleMembers.getValue();
                User currentCircleMember = circleMembers.getKey();
                if (ds.getKey().equals(currentCircleMember.getUid())) {
                    User newMember = ds.getValue(User.class);
                    if (UserUtil.didUserChange(currentCircleMember, newMember)) {
                        UserUtil.updateCurrentUser(currentCircleMember, newMember);
                        LatLng newPosition = new LatLng(newMember.getLatitude(), newMember.getLongitude());
                        clusterRenderer.getMarker(currentMemberMarker).setPosition(newPosition);
                        currentMemberMarker.setPosition(newPosition);
                    }
                    // add the new information to the list.
                    newMemberInformation.add(currentCircleMember);
                }
            }
        }
        // return the new updated information.
        return newMemberInformation;
    }

    public void resetClusterManagers() {
        clusterRenderer = null;
        clusterManager = null;
    }

    public void initializeClusterManagers() {
        clusterManager = new ClusterManager<>(ctx, map);
        clusterRenderer = new ClusterMarkerManager(ctx, map, clusterManager);
        clusterManager.setRenderer(clusterRenderer);
    }

    public static void resetMap() {
        // Removes all markers, overlays, and polyline from the map.
        map.clear();
        // clear the hashmap
        membersLocation.clear();
        // clear last camera position
        lastCameraPosition = null;
    }

    public void storeLastCameraPosition(CameraPosition cameraPosition) {
        lastCameraPosition = cameraPosition;
    }

    public void moveToLastCameraPosition() {
        if (lastCameraPosition == null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(SINGAPORE_COORDINATES, 12.f));
        } else {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(lastCameraPosition));
        }
    }

    public HashMap<User, ClusterMarker> getMembers() {
        return membersLocation;
    }

    public String getAddress(double latitude, double longitude) {
        StringBuilder locationName = new StringBuilder("Getting user location..");
        try {
            // Retrieve the address which the user is currently at
            // by reverse geocoding the user's latitude and longitude.
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            try {
                // Try and retrieve the best possible address (index 0)
                Address address = addresses.get(0);
                locationName = new StringBuilder("Near ");
                // and get the full address.
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    locationName.append(address.getAddressLine(i));
                }
            // if the address list is somehow null,
            } catch (IndexOutOfBoundsException e) {
                // leave the location name to Getting user location instead to indicate that
                // the geocoder can't get the address from the user's latitude and longitude.
                locationName = new StringBuilder("Getting user location..");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return locationName.toString();
    }

    public void setMapType(boolean isNormal) {
        map.setMapType(isNormal ? GoogleMap.MAP_TYPE_NORMAL : GoogleMap.MAP_TYPE_HYBRID);
    }

}