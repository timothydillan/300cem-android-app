package com.timothydillan.circles;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

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
import com.google.firebase.database.DataSnapshot;
import com.timothydillan.circles.Adapters.CMemberRecyclerAdapter;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.Services.LocationService;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.PermissionUtil;
import com.timothydillan.circles.Utils.UserUtil;

import java.security.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MapsFragment extends Fragment {
    private static final String LOG_TAG = "MapsFragment";
    private static final LatLng SINGAPORE_COORDINATES = new LatLng(1.290270, 103.851959);

    private CircleUtil circleUtil = new CircleUtil();
    private UserUtil userUtil = new UserUtil();
    private PermissionUtil permissionUtil;
    private GoogleMap mMap;

    private BottomSheetBehavior mBottomSheetBehavior;
    private RecyclerView circleMemberView;
    private CMemberRecyclerAdapter.RecyclerViewClickListener circleMemberListener;
    private CMemberRecyclerAdapter adapter;
    private static HashMap<User, Marker> membersLocation = new HashMap<>();

    /* TODO:
    3. Custom marker
    4. Get location in name (GeoCoder API)
    8. Saving data using ViewModel or onSaveInstanceState:
        https://medium.com/androiddevelopers/viewmodels-a-simple-example-ed5ac416317e
        https://medium.com/androiddevelopers/viewmodels-with-saved-state-jetpack-navigation-data-binding-and-coroutines-df476b78144e
        https://developer.android.com/topic/libraries/architecture/viewmodel-savedstate
        https://developer.android.com/guide/fragments/saving-state
    10. Test feature when new member added, need to move on fast to other features
    */

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
            mMap = googleMap;
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(SINGAPORE_COORDINATES, 12.f));

            // When the map is ready, create a circle listener
            circleUtil.addEventListener(new CircleUtil.CircleUtilListener() {
                // and listen to each event.
                @Override
                public void onCircleReady(ArrayList<User> members) {
                    // if the circle is ready (all data related to the members in the circle has been retrieved)
                    // place the markers at the appropriate places
                    initializeMarkers(members);
                    // set up the recycler view for the bottom sheet
                    setUpRecyclerView(members, circleMemberListener);
                    // show the bottom sheet
                    mBottomSheetBehavior.setHideable(false);
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    Intent locationForegroundService = new Intent(requireContext(), LocationService.class);
                    ContextCompat.startForegroundService(requireContext(), locationForegroundService);
                }
                @Override
                public void onCircleChange() {
                    resetMap();
                }
            });

            userUtil.addEventListener(new UserUtil.UsersListener() {
                @Override
                public void onUserReady() { }
                @Override
                public void onUsersChange(@NonNull DataSnapshot snapshot) {
                    updateLocations(snapshot);
                }
            });
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionUtil = new PermissionUtil(requireContext());
        permissionUtil.requestLocationPermissions();
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

        circleMemberView = view.findViewById(R.id.circleMembersView);
        LinearLayout mBottomSheet = view.findViewById(R.id.circleMemberBottomSheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheet);
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
            // Only show the member
            setUpRecyclerView(circleMemberView, memberList, circleMemberListener);
            */
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            User member = circleUtil.getCircleMembers().get(position);
            LatLng memberPosition = Objects.requireNonNull(membersLocation.get(member)).getPosition();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(memberPosition, 16.0f));
        };
        mBottomSheetBehavior.setHideable(true);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PermissionsRequest", permissions[i] + " granted.");
                } else {
                    // If somehow one of the permissions are denied, show a permission dialog.
                    Log.d("PermissionsRequest", permissions[i] + " denied.");
                    permissionUtil.showPermissionsDialog(permissions[i]);
                }
            }
            // If everything goes well, reset the map, and "restart" the fragment.
            resetMap();
            assert getFragmentManager() != null;
            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
        }
    }

    private void setUpRecyclerView(ArrayList<User> memberList, CMemberRecyclerAdapter.RecyclerViewClickListener listener) {
        adapter = new CMemberRecyclerAdapter(requireContext(), memberList, listener, R.layout.circle_member_list);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(requireContext());
        circleMemberView.setLayoutManager(layoutManager);
        circleMemberView.setItemAnimator(new DefaultItemAnimator());
        circleMemberView.setAdapter(adapter);
    }

    private void initializeMarkers(ArrayList<User> circleMembers) {
        // For each member
        for (User circleMember : circleMembers) {
            // Get their location
            LatLng memberLocation = new LatLng(circleMember.getLatitude(), circleMember.getLongitude());
            // And create a marker with their location
            Marker memberMarker = mMap.addMarker(new MarkerOptions().position(memberLocation).title(circleMember.getFirstName() + "'s Location"));
            // Animate/move the camera to the current member being iterated with a zoom of 15 (if its the user)
            if (circleMember.getUid().equals(UserUtil.getCurrentUser().getUid()))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(memberLocation, 15.0f));
            // and put it into the hashmap.
            membersLocation.put(circleMember, memberMarker);
        }
    }

    private void updateLocations(@NonNull DataSnapshot snapshot) {
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
                        currentMemberMarker.setPosition(newMember.getPosition());
                    }
                    // add the new information to the list.
                    newMemberInformation.add(currentCircleMember);
                }
            }
        }
        // update the RecyclerView to match the latest information.
        if (!newMemberInformation.isEmpty())
            adapter.updateInformation(newMemberInformation);
    }

    private void resetMap() {
        // Removes all markers, overlays, and polyline from the map.
        mMap.clear();
        // clear the hashmap
        membersLocation.clear();
    }
}