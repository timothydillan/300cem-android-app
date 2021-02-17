package com.timothydillan.circles;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.timothydillan.circles.Adapters.LocationRecyclerAdapter;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.LocationUtil;
import com.timothydillan.circles.Utils.PermissionUtil;
import com.timothydillan.circles.Utils.UserUtil;
import java.util.ArrayList;
import java.util.Objects;

public class MapsFragment extends Fragment implements GoogleMap.OnInfoWindowClickListener {
    public static final String TAG = "MapsFragment";
    public static boolean mapTypeBool = true;

    // Utils
    private LocationUtil locationUtil;
    private CircleUtil circleUtil = new CircleUtil();
    private UserUtil userUtil = new UserUtil();
    private PermissionUtil permissionUtil;
    private GoogleMap mMap;

    // Views
    private CardView mapTypeButton;
    private RecyclerView circleMemberView;
    private LocationRecyclerAdapter.RecyclerViewClickListener circleMemberListener;
    private LocationRecyclerAdapter adapter;

    /* TODO:
    8. Saving data using ViewModel or onSaveInstanceState:
        https://medium.com/androiddevelopers/viewmodels-a-simple-example-ed5ac416317e
        https://medium.com/androiddevelopers/viewmodels-with-saved-state-jetpack-navigation-data-binding-and-coroutines-df476b78144e
        https://developer.android.com/topic/libraries/architecture/viewmodel-savedstate
        https://developer.android.com/guide/fragments/saving-state
    10. Test feature when new member added, need to move on fast to other features (TMW)
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
            googleMap.setOnInfoWindowClickListener(MapsFragment.this);
            mMap = googleMap;

            locationUtil.setMap(googleMap);
            locationUtil.setMapAppearanceMode();
            locationUtil.setMapType(mapTypeBool);
            locationUtil.moveToLastCameraPosition();

            if (!permissionUtil.hasLocationPermissions()) {
                return;
            }

            setUpMarkersAndList();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionUtil = new PermissionUtil(requireContext());
        locationUtil = new LocationUtil(requireContext());
    }

    @Override
    public void onStart() {
        super.onStart();
        // Request permissions if the user goes out of the app and goes back in again.
        permissionUtil.requestLocationPermissions();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Request permissions if the app goes to the onPause state and goes back to onResume.
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
        // Initialize and inflate layouts and views when the view has been fully created.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.setRetainInstance(true);
            mapFragment.getMapAsync(callback);
        }

        mapTypeButton = view.findViewById(R.id.mapTypeButton);
        setUpMapClickListener();
        circleMemberView = view.findViewById(R.id.circleMembersView);
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
            User member = circleUtil.getCircleMembers().get(position);
            LatLng memberPosition = Objects.requireNonNull(locationUtil.getMembers().get(member)).getPosition();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(memberPosition, 16.0f));
        };

        setUpRecyclerView(circleMemberListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtil.LOCATION_REQUEST_CODE && grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PermissionsRequest", permissions[i] + " granted.");
                } else {
                    // If somehow one of the permissions are denied, show a permission dialog.
                    Log.d("PermissionsRequest", permissions[i] + " denied.");
                    permissionUtil.showPermissionsDialog(permissions[i], 0);
                }
            }
            // If everything goes well, reset the map, and "restart" the fragment.
            LocationUtil.resetMap();
            resetFragment();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        locationUtil.storeLastCameraPosition(mMap.getCameraPosition());
    }

    private void setUpRecyclerView(LocationRecyclerAdapter.RecyclerViewClickListener listener) {
        adapter = new LocationRecyclerAdapter(requireContext(), listener, R.layout.circle_member_list);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(requireContext());
        circleMemberView.setLayoutManager(layoutManager);
        circleMemberView.setItemAnimator(new DefaultItemAnimator());
        circleMemberView.setAdapter(adapter);
    }

    public void setUpMapClickListener() {
        mapTypeButton.setOnClickListener(v -> {
            mapTypeBool = !mapTypeBool;
            locationUtil.setMapType(mapTypeBool);
        });
    }

    private void setUpMarkersAndList() {
        // When the map is ready, create a circle listener
        circleUtil.addEventListener(new CircleUtil.CircleUtilListener() {
            // and listen to each event.
            @Override
            public void onCircleReady(ArrayList<User> members) {
                // if the circle is ready (all data related to the members in the circle has been retrieved)
                // place the markers at the appropriate places
                locationUtil.initializeMarkers(members);
                userUtil.addEventListener(new UserUtil.UsersListener() {
                    @Override
                    public void onUserReady() { }

                    @Override
                    public void onUsersChange(@NonNull DataSnapshot snapshot) {
                        ArrayList<User> newMemberInformation = locationUtil.getUpdatedInformation(snapshot);
                        if (newMemberInformation != null && !newMemberInformation.isEmpty())
                            adapter.updateInformation(newMemberInformation);
                    }
                });
            }
            @Override
            public void onCircleChange() {
                LocationUtil.resetMap();
                resetFragment();
            }
            @Override
            public void onJoinCircle(boolean success) { }
        });
    }

    private void resetFragment() {
        getParentFragmentManager().beginTransaction().detach(this).attach(this).commit();
    }


    @Override
    public void onInfoWindowClick(Marker marker) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Are you sure?")
                .setMessage("Get directions to " + marker.getTitle() + "?")
                .setCancelable(true)
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    // https://developers.google.com/maps/documentation/urls/android-intents#:~:text=Android%20developer%20documentation.-,Intent%20requests,as%20a%20View%20action%20%E2%80%94%20ACTION_VIEW%20.
                    LatLng position = marker.getPosition();
                    Uri gmmIntentUri = Uri.parse("google.navigation:q="+position.latitude+","+position.longitude);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                });
        builder.show();
    }
}