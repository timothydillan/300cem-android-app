package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;

import static android.content.Context.LOCATION_SERVICE;

public class MapsFragment extends Fragment {

    private Marker marker;
    private LatLng currentUserLocation = new LatLng(-5, 5);
    private LocationManager locationManager;
    private final long REFRESH_LOC_TIME = 3000;
    private final long MIN_LOC_DIST = 1;

    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;

    /* TODO:
    1. Get Circles Node, Get Members inner-node, for every member, get each Lat and Long and add marker to map
    2. On Firebase data change, remove all markers and add it again.
    3. For every circle, look for member or admin node, if member/admin == user.id -> update user latitude and longitude every onLocationChanged
    4. Marker with member PFP.
    5. Full card-view of each member.
    */

    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        @Override
        public void onMapReady(GoogleMap googleMap) {
            if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                return;

            marker = googleMap.addMarker(new MarkerOptions().position(currentUserLocation));
            LocationListener locationListener = location -> {
                marker.remove();
                currentUserLocation = new LatLng(location.getLatitude(), location.getLongitude());
                marker = googleMap.addMarker(new MarkerOptions().position(currentUserLocation).title("Your Location!"));
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation, 15.0f));
            };

            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, REFRESH_LOC_TIME, MIN_LOC_DIST, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, REFRESH_LOC_TIME, MIN_LOC_DIST, locationListener);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissions(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PackageManager.PERMISSION_GRANTED);

        locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }
}