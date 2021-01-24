package com.timothydillan.circles;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.timothydillan.circles.Adapters.CMemberRecyclerAdapter;
import com.timothydillan.circles.Models.Circle;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.Utils.CircleUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.LOCATION_SERVICE;

public class MapsFragment extends Fragment {

    private static final LatLng singaporeCoordinates = new LatLng(1.290270, 103.851959);
    protected static final String KEY_UID = "keyUid";

    private LinearLayout mBottomSheet;
    private BottomSheetBehavior mBottomSheetBehavior;
    private RecyclerView circleMemberView;
    private CMemberRecyclerAdapter.RecyclerViewClickListener circleMemberListener;
    private CMemberRecyclerAdapter adapter;

    private CircleUtil circleUtil;
    private GoogleMap mMap;
    private LocationManager locationManager;

    private final long REFRESH_LOC_TIME = 3000;
    private final long MIN_LOC_DIST = 5;

    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;

    private static final HashMap<User, Marker> membersLocation = new HashMap<>();

    /* TODO:
    3. Marker with member PFP. (needs custom marker and usr img)
    4. Get location in name (GEOCODER API)
    5. If bottom sheet is clicked, get user ID, move camera to user with big zoom.
    6. Popup Dialog  if permissions are not allowed (maybe appropriate in MainActivity instead)
    7. Background location updates.
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
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(singaporeCoordinates, 12.f));

            circleUtil = new CircleUtil(new CircleUtil.CircleUtilListener() {
                @Override
                public void onCircleReady(ArrayList<User> members) {
                    initializeCircleMarkers(members);
                    setUpRecyclerView(circleMemberView, members, circleMemberListener);
                    mBottomSheetBehavior.setHideable(false);
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                @Override
                public void onCircleChange() {
                    resetCircleMap();
                }

                @Override
                public void onUsersChange(@NonNull DataSnapshot snapshot) {
                    updateCircleMemberLocations(snapshot);
                }
            });

            // Every time our location changes, update our latitude and longitude.
            LocationListener locationListener = location ->
                    updateUserLocation(location.getLatitude(), location.getLongitude());

            // Update our location every 3 second with a minimum of 5 meter accuracy.
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

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_maps, container, false);
        circleMemberView = rootView.findViewById(R.id.circleMembersView);
        mBottomSheet = rootView.findViewById(R.id.circleMemberBottomSheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheet);
        mBottomSheetBehavior.setHideable(true);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        circleMemberListener = (v, position) -> {
            /*Options:
            1. When member clicks on member, it shows a full profile of that member
            Intent intent = new Intent(getActivity(), CircleMembersActivity.class);
            intent.putExtra(KEY_UID, circleUtil.getCircleMembers().get(position).getUid());
            startActivity(intent);
            2. No.5
            User member = circleUtil.getCircleMembers().get(position);
            LatLng memberPosition = membersLocation.get(member).getPosition();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(memberPosition, 16.0f));
            3. No.5 and EXTRA
            ArrayList<User> memberList = new ArrayList<>();
            User member = circleUtil.getCircleMembers().get(position);
            LatLng memberPosition = membersLocation.get(member).getPosition();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(memberPosition, 16.0f));
            memberList.add(member)
            // Only show the memebr
            setUpRecyclerView(circleMemberView, memberList, circleMemberListener);
            */
            Intent intent = new Intent(getActivity(), CircleMembersActivity.class);
            startActivity(intent);
        };

        return rootView;
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

    private void setUpRecyclerView(RecyclerView recyclerView, ArrayList<User> memberList, CMemberRecyclerAdapter.RecyclerViewClickListener listener) {
        adapter = new CMemberRecyclerAdapter(memberList, listener, R.layout.circle_member_list);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
    }

    private void initializeCircleMarkers(ArrayList<User> circleMembers) {
        // For each member
        for (User circleMember : circleMembers) {
            // Get their location
            LatLng memberLocation = new LatLng(circleMember.getLatitude(), circleMember.getLongitude());
            // And create a marker with their location
            Marker memberMarker = mMap.addMarker(new MarkerOptions().position(memberLocation).title(circleMember.getFirstName() + "'s Location"));
            // Animate/move the camera to the current member being iterated with a zoom of 15
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(memberLocation, 15.0f));
            // and put it into the hashmap.
            membersLocation.put(circleMember, memberMarker);
        }
    }

    private void updateCircleMemberLocations(@NonNull DataSnapshot snapshot) {
        // Just a little sanity check :).
        if (membersLocation.isEmpty()) {
            System.out.println("Members locations has not been initialized yet.");
            return;
        }

        // Create an array list
        ArrayList<User> newMemberInformation = new ArrayList<>();
        // For each of the data received,
        for (DataSnapshot ds : snapshot.getChildren()) {
            // and for every member in the circle
            for (Map.Entry<User, Marker> circleMembers : membersLocation.entrySet()) {
                // Get their current marker and user details
                Marker currentMemberMarker = circleMembers.getValue();
                User currentCircleMember = circleMembers.getKey();
                // Get the user that's the same as the current member ID
                if (ds.getKey().equals(currentCircleMember.getUid())) {
                    LatLng dbLocation = new LatLng(ds.child("latitude").getValue(Double.class), ds.child("longitude").getValue(Double.class));
                    // If their location changed from the initial values,
                    if (currentCircleMember.getLatitude() != dbLocation.latitude ||
                            currentCircleMember.getLongitude() != dbLocation.longitude) {
                        // Update their location details
                        currentCircleMember.setLatitude(dbLocation.latitude);
                        currentCircleMember.setLongitude(dbLocation.longitude);
                        currentCircleMember.updateLastSharingTime();
                        currentMemberMarker.setPosition(dbLocation);
                    }
                    // add the new information to the list.
                    newMemberInformation.add(currentCircleMember);
                }
            }
        }
        // update the RecyclerView to match the latest information.
        adapter.updateInformation(newMemberInformation);
    }

    private void resetCircleMap() {
        // Removes all markers, overlays, and polylines from the map.
        mMap.clear();
        // clear the hashmap
        membersLocation.clear();
    }

    private void updateUserLocation(double latitude, double longitude) {
        // Get current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm EEEE");
        String currentDateAndTime = dateFormat.format(new Date());
        // Update latitude, longitude, and the last sharing time of the users.
        databaseReference.child("Users").child(currentUser.getUid()).child("latitude").setValue(latitude);
        databaseReference.child("Users").child(currentUser.getUid()).child("longitude").setValue(longitude);
        databaseReference.child("Users").child(currentUser.getUid()).child("lastSharingTime").setValue(currentDateAndTime);
    }

}