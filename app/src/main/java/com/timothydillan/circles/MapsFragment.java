package com.timothydillan.circles;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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

    private CircleUtil circleUtil;
    private GoogleMap mMap;
    private LocationManager locationManager;

    private static final long REFRESH_LOC_TIME = 3000;
    private static final long MIN_LOC_DIST = 5;

    private BottomSheetBehavior mBottomSheetBehavior;
    private RecyclerView circleMemberView;
    private CMemberRecyclerAdapter.RecyclerViewClickListener circleMemberListener;
    private CMemberRecyclerAdapter adapter;

    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;

    private static final HashMap<User, Marker> membersLocation = new HashMap<>();

    /* TODO:
    3. Marker with member PFP. (needs custom marker and usr img)
    4. Get location in name (GeoCoder API)
    5. If bottom sheet is clicked, get user ID, move camera to user with big zoom.
    6. Popup Dialog if permissions are not allowed (maybe appropriate in MainActivity instead)
    7. Background location updates.
    8. Saving data using ViewModel or onSaveInstanceState:
        https://medium.com/androiddevelopers/viewmodels-a-simple-example-ed5ac416317e
        https://medium.com/androiddevelopers/viewmodels-with-saved-state-jetpack-navigation-data-binding-and-coroutines-df476b78144e
        https://developer.android.com/topic/libraries/architecture/viewmodel-savedstate
        https://developer.android.com/guide/fragments/saving-state
    9. Use FusedLocationProvider?
    10. Test feature when new member added, need to move on fast to other features
    */

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    /*private ActivityResultLauncher<String[]> requestPermissionLauncher =
            getActivity().registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                permissions.forEach((k, v) -> Log.d("DEBUG", k + " = " + v));
            });*/

    private final OnMapReadyCallback callback = new OnMapReadyCallback() {
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
            // Check whether location-related permissions are allowed. If they aren't, just return.
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //requestPermissionLauncher.launch(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
                return;
            }

            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                    || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Toast.makeText(getContext(), "The application requires location access to locate you and your circle", Toast.LENGTH_LONG).show();
            }

            mMap = googleMap;

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(singaporeCoordinates, 12.f));

            // When the map is ready, create a circle listener
            circleUtil = new CircleUtil(new CircleUtil.CircleUtilListener() {
                // and listen to each event.
                @Override
                public void onCircleReady(ArrayList<User> members) {
                    // if the circle is ready (all data related to the members in the circle has been retrieved)
                    // place the markers at the appropriate places
                    initializeCircleMarkers(members);
                    // set up the recycler view for the bottom sheet
                    setUpRecyclerView(circleMemberView, members, circleMemberListener);
                    // show the bottom sheet
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

            // Every time our location changes, update the user's latitude and longitude in the database.
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
        LinearLayout mBottomSheet = rootView.findViewById(R.id.circleMemberBottomSheet);
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
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            User member = circleUtil.getCircleMembers().get(position);
            LatLng memberPosition = membersLocation.get(member).getPosition();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(memberPosition, 16.0f));
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