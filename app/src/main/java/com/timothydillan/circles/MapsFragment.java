package com.timothydillan.circles;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
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
import com.timothydillan.circles.Models.Item;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.LocationUtil;
import com.timothydillan.circles.Utils.PermissionUtil;
import com.timothydillan.circles.Utils.UserUtil;
import com.timothydillan.circles.Utils.WearableUtil;
import java.util.ArrayList;

public class MapsFragment extends Fragment implements GoogleMap.OnInfoWindowClickListener, UserUtil.UsersListener, CircleUtil.CircleUtilListener {

    /* Global Vars */
    public static final String TAG = "MapsFragment";
    public static boolean mapTypeBool = false;
    private GoogleMap mMap;

    /* Utils */
    private LocationUtil locationUtil;
    private CircleUtil circleUtil = CircleUtil.getInstance();
    private UserUtil userUtil = UserUtil.getInstance();
    private WearableUtil wearableUtil;
    private PermissionUtil permissionUtil;

    /* Views */
    private Spinner circleSpinner;
    private Button sosButton;
    private CardView mapTypeButton;
    private RecyclerView circleMemberView;
    private LocationRecyclerAdapter.RecyclerViewClickListener circleMemberListener;
    private LocationRecyclerAdapter adapter;

    private final OnMapReadyCallback callback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            googleMap.setOnInfoWindowClickListener(MapsFragment.this);
            mMap = googleMap;

            /* When the map is ready, initialize the location util
             * by setting the map, the appearance (light mode/dark mode),
             * map type (satellite/normal). */
            locationUtil.setMap(googleMap);
            locationUtil.setMapAppearanceMode();
            locationUtil.setMapType(mapTypeBool);
            locationUtil.moveToLastCameraPosition();

            // Then check whether location permissions has been given by the user.
            if (!permissionUtil.hasLocationPermissions()) {
                // If the permissions are not yet given, return and do nothing.
                return;
            }

            /* Else, register a listener to check whether the circle which the user is currently in
             * has been fully initialized and whether there has been any changes to the circle
             * that the user is currently in. */
            circleUtil.registerListener(MapsFragment.this);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionUtil = new PermissionUtil(requireContext());
        locationUtil = new LocationUtil(requireContext());
        wearableUtil = new WearableUtil(requireContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        /* If the user goes out of the maps fragment and goes back in again, request for location permissions (if not granted)
         * and re-register the circle listener (since we're unregistered the listener onPause()). */
        permissionUtil.locationPermissions(requireActivity());
        if (mMap != null) {
            circleUtil.registerListener(MapsFragment.this);
        }
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

        circleSpinner = view.findViewById(R.id.circleSpinner);
        mapTypeButton = view.findViewById(R.id.mapTypeButton);
        sosButton = view.findViewById(R.id.sosButton);
        circleMemberView = view.findViewById(R.id.circleMembersView);

        circleMemberListener = (v, position) -> {
            /* If any of the items inside the location recycler view is clicked,
            *  get the current member being clicked and move the map */
            User member = adapter.getListOfMembers().get(position);
            // get their position,
            LatLng memberPosition = new LatLng(member.getLatitude(), member.getLongitude());
            // move the map to the position of the member that is being highlighted,
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(memberPosition, 16.0f));
            // and show the member's info window marker (allows users to get
            // directions to the member being highlighted)
            LocationUtil.getMember().get(member).showInfoWindow();
        };

        setUpMapClickListener();
        setUpSosClickListener();
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
        /* When the user leaves the maps fragment, we don't need to update anything visually
        * So we should unregister the listener to prevent unnecessary updates and memory leaks. */
        circleUtil.unregisterListener(this);
        userUtil.unregisterListener(this);
        // We'll also store the last camera position so that when the user goes back, the last camera position will be shown.
        locationUtil.storeLastCameraPosition(mMap.getCameraPosition());
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        // https://developers.google.com/maps/documentation/urls/android-intents#:~:text=Android%20developer%20documentation.-,Intent%20requests,as%20a%20View%20action%20%E2%80%94%20ACTION_VIEW%20.
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Are you sure?")
                .setMessage("Get directions to " + marker.getTitle() + "?")
                .setCancelable(true)
                .setNegativeButton("No", ((dialog, which) -> dialog.dismiss()))
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    // If the info window of a marker is clicked, get the marker's position,
                    LatLng position = marker.getPosition();
                    // create an intent with the latitude and longitude of the marker's location,
                    // and specify that we want to get directions to the position using google.navigation
                    Uri gmmIntentUri = Uri.parse("google.navigation:q="+position.latitude+","+position.longitude);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    // ensure that the intent will be opened in the maps app,
                    mapIntent.setPackage("com.google.android.apps.maps");
                    // and start the intent.
                    startActivity(mapIntent);
                });
        builder.show();
    }

    @Override
    public void onCircleReady(ArrayList<User> members) {
        /* If the circle is ready (all data related to the members in the circle has been retrieved)
        *  place the markers at the appropriate places, and register the user util listener
        *  to listen for data changes in the users circle. */
        if (!isAdded()) {
            return;
        }

        locationUtil.initializeMarkers(members);
        userUtil.registerListener(MapsFragment.this);
        setUpCircleSpinner();
    }

    @Override
    public void onCircleChange() {
        // If something changed in the current circle that the user is in, reset the map
        LocationUtil.resetMap();
        // and restart the fragment.
        resetFragment();
    }

    @Override
    public void onUsersChange(@NonNull DataSnapshot snapshot) {
        // Every time something changes in the users node, get the new information
        ArrayList<User> newMemberInformation = locationUtil.getUpdatedInformation(snapshot);
        wearableUtil.sendDataToWearable(WearableUtil.NAME_KEY, WearableUtil.NAME_PATH, userUtil.getCurrentUser().getFirstName());
        // and if the new information isn't empty, send that information to the adapter.
        if (newMemberInformation != null && !newMemberInformation.isEmpty()) {
            adapter.updateInformation(newMemberInformation);
        }
    }

    private void setUpCircleSpinner() {
        Item getUserRegisteredCircles = UserUtil.getRegisteredCircles();

        int initialSpinnerPosition = 0;
        ArrayList<String> circleNames = new ArrayList<>();

        // For every item inside the registered circles hashmap,
        for (int i = 0; i < getUserRegisteredCircles.getMap().size(); i++) {
            // add the names of the circles into an arraylist (needed for spinner)
            circleNames.add(getUserRegisteredCircles.getItemName(i));
            // if the current circle being iterated equals to the user's current circle,
            if (getUserRegisteredCircles.getItemValue(i).equals(String.valueOf(userUtil.getCurrentUser().getCurrentCircleSession()))) {
                // set the initialSpinnerPosition to the position (needed for setSelection).
                initialSpinnerPosition = i;
            }
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(requireContext(), R.layout.custom_dropdown_layout, circleNames);
        circleSpinner.setAdapter(arrayAdapter);

        // To prevent onItemSelected from being called, we'll make a selection before the listener is called.
        // the selection will by default be the current user's circle.
        circleSpinner.setSelected(false);
        circleSpinner.setSelection(initialSpinnerPosition,false);

        if (!permissionUtil.hasLocationPermissions()) {
            return;
        }

        // If an item is selected from the circle spinner,
        circleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // pass the circle selected into the switch circle function.
                switchCircle(UserUtil.getRegisteredCircles().getItemName(position), (String) UserUtil.getRegisteredCircles().getItemValue(position));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void setUpRecyclerView(LocationRecyclerAdapter.RecyclerViewClickListener listener) {
        adapter = new LocationRecyclerAdapter(requireContext(), listener);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(requireContext());
        circleMemberView.setLayoutManager(layoutManager);
        circleMemberView.setItemAnimator(new DefaultItemAnimator());
        circleMemberView.setAdapter(adapter);
    }

    public void setUpMapClickListener() {
        mapTypeButton.setOnClickListener(v -> {
            // Switch the state of the map type boolean when the button is clicked,
            mapTypeBool = !mapTypeBool;
            // and set the map type accordingly.
            locationUtil.setMapType(mapTypeBool);
        });
    }

    private void setUpSosClickListener() {
        sosButton.setOnClickListener(v -> {
            Intent sosIntent = new Intent(requireContext(), SOSConfirmationActivity.class);
            startActivity(sosIntent);
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void resetFragment() {
        if (isAdded()) {
            getParentFragmentManager().beginTransaction().detach(this).attach(this).commitAllowingStateLoss();
        }
    }

    private void switchCircle(String circleName, String circleCode) {
        // If the circle code that the user wants to switch to is the same as the current circle that the user is in,
        if (circleCode.equals(String.valueOf(userUtil.getCurrentUser().getCurrentCircleSession()))) {
            // show a toast message and do nothing.
            Toast.makeText(requireContext(), "You're currently in this circle!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Else, create a confirmation dialog,
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Are you sure?")
                .setMessage("Are you sure you want to switch to: " + circleName + "?")
                .setCancelable(true)
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    // and if the user has confirmed that they want to switch, show a message
                    Toast.makeText(requireContext(), "Switching...", Toast.LENGTH_LONG).show();
                    // Switch the circle that the user is currently in
                    circleUtil.switchCircle(circleCode);
                    // and reset the fragment.
                    // We need to give a delay of two seconds to let the CircleUtil initialize the circle members again.
                    new Handler().postDelayed(() -> {
                        LocationUtil.resetMap();
                        resetFragment();
                    }, 2000);
                });
        builder.show();
    }

}