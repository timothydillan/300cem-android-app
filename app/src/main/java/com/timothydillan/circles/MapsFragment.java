package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.icu.lang.UScript;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.timothydillan.circles.Models.User;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.LOCATION_SERVICE;

public class MapsFragment extends Fragment {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private final long REFRESH_LOC_TIME = 3000;
    private final long MIN_LOC_DIST = 1;
    private static final String FIREBASE_TAG = "firebaseRelated";

    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    private ArrayList<String> circleMemberUidList = new ArrayList<>();
    private HashMap<Marker, User> membersLocation = new HashMap<>();
    private Query memberUpdates;
    private User currentMember;

    /* TODO:
    1. Get Circles Node, Get Members inner-node, for every member, get each Lat and Long and add marker to map
    2. On Firebase data change, remove all markers and add it again.
    3. Marker with member PFP.
    4. Full card-view of each member.
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

            mMap = googleMap;

            // Every time our location changes, update our latitude and longitude.
            LocationListener locationListener = location -> {
                LatLng currentUserLocation = new LatLng(location.getLatitude(), location.getLongitude());
                databaseReference.child("Users").child(currentUser.getUid()).child("latitude").setValue(currentUserLocation.latitude);
                databaseReference.child("Users").child(currentUser.getUid()).child("longitude").setValue(currentUserLocation.longitude);
            };

            // Update our location every 3 second with a minimum of 1 meter accuracy.
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, REFRESH_LOC_TIME, MIN_LOC_DIST, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, REFRESH_LOC_TIME, MIN_LOC_DIST, locationListener);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make sure these location permissions are enabled by the user.
        requestPermissions(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                PackageManager.PERMISSION_GRANTED);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize the Circle location, TODO: Should make a facade for this instead.
        locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
        memberUpdates = databaseReference.child("Users");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        circleInitialization();
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

    public void circleInitialization() {
        // To initialize the circle, we must first get the current user's details.
        databaseReference.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // After successfully getting the current user's details,
                currentMember = snapshot.child(currentUser.getUid()).getValue(User.class);
                Log.d(FIREBASE_TAG, "Successfully retrieved current user, getting every member ID");
                // we need to get the UID of each member in the circle that the user is currently in.
                getCircleMemberUid();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(FIREBASE_TAG, "circleInitialization:failure", error.toException());
            }
        });
    }

    public void getCircleMemberUid() {
        // Get the current user's circle, and look into the members nodes.
        databaseReference.child("Circles").child(String.valueOf(currentMember.getCurrentCircleSession()))
                .child("Members")
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // For every user UID in it
                for (DataSnapshot ds : snapshot.getChildren()) {
                    // add it to an array list.
                    Log.d(FIREBASE_TAG, "Adding member UID into list.");
                    circleMemberUidList.add(ds.getKey());
                }
                Log.d(FIREBASE_TAG, "Finished adding every member's uid into list. Getting member details.");
                // Then actually get the details for each user.
                getCircleMembers();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(FIREBASE_TAG, "getCircleMemberUid:failure", error.toException());
            }
        });
    }

    public void getCircleMembers() {
        // A sanity check.
        if (circleMemberUidList.isEmpty())
            return;

        // Read the Users node once
        databaseReference.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // get the children (aka the UID listed in the users node)
                for (DataSnapshot ds : snapshot.getChildren()) {
                    // If the uid retrieved is the same in the current uid being iterated
                    for (String uid : circleMemberUidList) {
                        if (ds.getKey().equals(uid)) {
                            // Get the user details and store it into a variable
                            User circleMember = ds.getValue(User.class);
                            // Get the user's location and show it in the map
                            assert circleMember != null;
                            LatLng memberLocation = new LatLng(circleMember.getLatitude(), circleMember.getLongitude());
                            Marker memberMarker = mMap.addMarker(new MarkerOptions().position(memberLocation).title(circleMember.getFirstName() + "'s Location"));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(memberLocation, 15.0f));
                            // And then put each member and their respective markers into a HashMap to be modified later.
                            Log.d(FIREBASE_TAG, "Retrieved " + circleMember.getFirstName() + "'s details.");
                            membersLocation.put(memberMarker, circleMember);
                        }
                    }
                }
                // Once we're done with this, run a continuous event listener that updates each member's location.
                Log.d(FIREBASE_TAG, "Circle initialization done. Running update function.");
                updateCircleMemberLocations();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(FIREBASE_TAG, "getCircleMembers:failure", error.toException());
            }
        });
    }

    ValueEventListener getLocationUpdates = new ValueEventListener() {
        // If there's a change,
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            // For every user
            for (DataSnapshot ds : snapshot.getChildren()) {
                // For every member in the circle
                for (Map.Entry<Marker, User> circleMembers : membersLocation.entrySet()) {
                    // Get their current marker and user details
                    Marker currentMemberMarker = circleMembers.getKey();
                    User currentCircleMember = circleMembers.getValue();
                    // Get the user that's the same as the current member ID
                    if (ds.getKey().equals(currentCircleMember.getUid())) {
                        // Update their location details
                        currentCircleMember.setLatitude(ds.child("latitude").getValue(Double.class));
                        currentCircleMember.setLongitude(ds.child("longitude").getValue(Double.class));
                        // Update their marker location
                        LatLng memberLocation = new LatLng(currentCircleMember.getLatitude(), currentCircleMember.getLongitude());
                        currentMemberMarker.setPosition(memberLocation);
                    }
                }
            }
        }
        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.w(FIREBASE_TAG, "updateCircleMemberLocations:failure", error.toException());
        }
    };

    public void updateCircleMemberLocations() {

        // Just a little sanity check :).
        if (membersLocation.isEmpty()) {
            System.out.println("Members locations has not been initialized yet.");
            return;
        }

        memberUpdates.addValueEventListener(getLocationUpdates);
        // TODO: Test this tomorrow.
        updateCircle();
        /*
        // Implement a value event listener that's supposed to listen for latitude/longitude changes.
        databaseReference.child("Users").addValueEventListener(new ValueEventListener() {
            // If there's a change,
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // For every user
                for (DataSnapshot ds : snapshot.getChildren()) {
                    // For every member in the circle
                    for (Map.Entry<Marker, User> circleMembers : membersLocation.entrySet()) {
                        // Get their current marker and user details
                        Marker currentMemberMarker = circleMembers.getKey();
                        User currentCircleMember = circleMembers.getValue();
                        // Get the user that's the same as the current member ID
                        if (ds.getKey().equals(currentCircleMember.getUid())) {
                            // Update their location details
                            currentCircleMember.setLatitude(ds.child("latitude").getValue(Double.class));
                            currentCircleMember.setLongitude(ds.child("longitude").getValue(Double.class));
                            // Update their marker location
                            LatLng memberLocation = new LatLng(currentCircleMember.getLatitude(), currentCircleMember.getLongitude());
                            currentMemberMarker.setPosition(memberLocation);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(FIREBASE_TAG, "updateCircleMemberLocations:failure", error.toException());
            }
        });*/
    }

    public void updateCircle() {
        // Implement a value event listener that's supposed to listen for latitude/longitude changes.
        databaseReference.child("Circles").child(String.valueOf(currentMember.getCurrentCircleSession())).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                memberUpdates.removeEventListener(getLocationUpdates);
                // Clear memberUidlist and memberLocations because a change has occurred (remove/addition).
                circleMemberUidList.clear();
                for (Map.Entry<Marker, User> circleMembers : membersLocation.entrySet()) {
                    circleMembers.getKey().remove();
                }
                membersLocation.clear();
                // Initialize our circle again to retrieve new data.
                circleInitialization();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}