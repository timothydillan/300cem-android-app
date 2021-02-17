package com.timothydillan.circles.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.R;
import com.timothydillan.circles.Services.LocationService;
import com.timothydillan.circles.UI.VolleyImageRequest;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationUtil {
    private static ImageLoader imageLoader;
    private static final String TAG = "LocationUtil";
    private static final LatLng SINGAPORE_COORDINATES = new LatLng(1.290270, 103.851959);
    private static final String USER_UID = FirebaseUtil.getCurrentUser().getUid();
    private Context ctx;
    private static GoogleMap map = null;
    private static HashMap<User, Marker> membersLocation = new HashMap<>();
    private static final DatabaseReference databaseReference = FirebaseUtil.getDbReference();
    private static CameraPosition lastCameraPosition = null;
    private static Geocoder geocoder;

    public LocationUtil(Context context) {
        ctx = context;
        imageLoader = VolleyImageRequest.getInstance(ctx).getImageLoader();
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

        // Then for each member from the retrieved arraylist,
        for (User circleMember : circleMembers) {
            // Get their location
            LatLng memberLocation = new LatLng(circleMember.getLatitude(), circleMember.getLongitude());
            Bitmap imageBitmap = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.logo);

            Marker memberMarker = map.addMarker(new MarkerOptions()
                    .position(memberLocation)
                    .title(circleMember.getFirstName() + "'s Location")
                    .snippet(circleMember.getLastSharingTime())
                    .anchor(0.5f, 0.907f)
                    .icon(BitmapDescriptorFactory.fromBitmap(createUserBitmap(imageBitmap))));

            // Animate/move the camera to the current user with a zoom of 15
            if (circleMember.getUid().equals(UserUtil.getCurrentUser().getUid()))
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(memberLocation, 15.0f));
            // and put it into the hashmap.
            membersLocation.put(circleMember, memberMarker);
        }
        updateAvatars();
    }

    public void updateAvatars() {
        for (Map.Entry<User, Marker> marker : membersLocation.entrySet()) {
            imageLoader.get(marker.getKey().getProfilePicUrl(), new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    Bitmap imageBitmap = response.getBitmap() != null ? response.getBitmap() : BitmapFactory.decodeResource(ctx.getResources(), R.drawable.logo);
                    marker.getValue().setIcon(BitmapDescriptorFactory.fromBitmap(createUserBitmap(imageBitmap)));
                }
                @Override
                public void onErrorResponse(VolleyError error) { }
            });
        }
    }

    public ArrayList<User> getUpdatedInformation(@NonNull DataSnapshot snapshot) {
        if (membersLocation.isEmpty()) {
            return null;
        }
        // Create an array list to store newly updated information
        ArrayList<User> newMemberInformation = new ArrayList<>();
        // For each of the data received,
        for (DataSnapshot ds : snapshot.getChildren()) {
            // and for every member in the circle
            for (Map.Entry<User, Marker> circleMembers : membersLocation.entrySet()) {
                // Get their current marker and user details
                Marker currentMemberMarker = circleMembers.getValue();
                User currentCircleMember = circleMembers.getKey();
                if (ds.getKey().equals(currentCircleMember.getUid())) {
                    User newMember = ds.getValue(User.class);
                    if (UserUtil.didUserChange(currentCircleMember, newMember)) {
                        UserUtil.updateCurrentUser(currentCircleMember, newMember);
                        LatLng newPosition = new LatLng(newMember.getLatitude(), newMember.getLongitude());
                        currentMemberMarker.setSnippet(currentCircleMember.getLastSharingTime());
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

    private Bitmap createUserBitmap(Bitmap profilePic) {
        Bitmap result = null;
        try {
            result = Bitmap.createBitmap(dp(62), dp(76), Bitmap.Config.ARGB_8888);
            result.eraseColor(Color.TRANSPARENT);
            Canvas canvas = new Canvas(result);
            Drawable drawable = ResourcesCompat.getDrawable(ctx.getResources(), R.drawable.pin, null);
            drawable.setBounds(0, 0, dp(62), dp(76));
            drawable.draw(canvas);

            Paint roundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            RectF bitmapRect = new RectF();
            canvas.save();

            if (profilePic != null) {
                BitmapShader shader = new BitmapShader(profilePic, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                Matrix matrix = new Matrix();
                float scale = dp(52) / (float) profilePic.getWidth();
                matrix.postTranslate(dp(2), dp(2));
                matrix.postScale(scale, scale);
                roundPaint.setShader(shader);
                shader.setLocalMatrix(matrix);
                bitmapRect.set(dp(6), dp(6), dp(50 + 6), dp(50 + 6));
                canvas.drawRoundRect(bitmapRect, dp(25), dp(25), roundPaint);
            }
            canvas.restore();
            try {
                canvas.setBitmap(null);
            } catch (Exception e) {}

        } catch (Throwable t) {
            t.printStackTrace();
        }
        return result;
    }

    private int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(ctx.getResources().getDisplayMetrics().density * value);
    }

    public static void updateUserLocation(Location location) {
        // Get current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm EEEE");
        String currentDateAndTime = dateFormat.format(new Date());
        // Update latitude, longitude, and the last sharing time of the users.
        databaseReference.child("Users").child(USER_UID).child("latitude").setValue(location.getLatitude());
        databaseReference.child("Users").child(USER_UID).child("longitude").setValue(location.getLongitude());
        databaseReference.child("Users").child(USER_UID).child("lastSharingTime").setValue(currentDateAndTime);
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

    public HashMap<User, Marker> getMembers() {
        return membersLocation;
    }
    public static HashMap<User, Marker> getMember() {
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