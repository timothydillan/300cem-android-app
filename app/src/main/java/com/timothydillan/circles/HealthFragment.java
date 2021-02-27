package com.timothydillan.circles;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.database.DataSnapshot;
import com.timothydillan.circles.Adapters.CircleHealthRecyclerAdapter;
import com.timothydillan.circles.Adapters.HealthRecyclerAdapter;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.Services.WearableService;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.HealthUtil;
import com.timothydillan.circles.Utils.PermissionUtil;
import com.timothydillan.circles.Utils.UserUtil;

import java.util.ArrayList;

public class HealthFragment extends Fragment implements UserUtil.UsersListener, CircleUtil.CircleUtilListener {

    private static final String MODE_KEY = "MODE_KEY";

    private ProgressBar healthProgressBar;
    private RecyclerView circleMemberHealthView;
    private HealthRecyclerAdapter memberHealthAdapter;
    private CircleHealthRecyclerAdapter circleHealthAdapter;
    private PermissionUtil permissionUtil;
    private MaterialButtonToggleGroup healthViewSwitch;
    private HealthUtil healthUtil = HealthUtil.getInstance();
    private boolean switchFragmentFlag = false;
    private int healthMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        healthUtil.initializeContext(requireContext());
        permissionUtil = new PermissionUtil(requireContext());
        // Ensure that activity recognition permissions are given, because this fragment uses that permission.
        permissionUtil.fitPermissions(requireActivity());
        // Get the current mode that the fragment is currently in (0 -> Circle Summary, 1 -> Member Summary)
        // From Lab 8
        healthMode = getArguments().getInt(MODE_KEY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_health, container, false);
        // Inflate the layout for this fragment.
        // If the user has granted permissions
        if (permissionUtil.hasFitPermissions()) {
            // Check if the wearable service is running.
            if (!WearableService.isServiceRunning(requireContext())) {
                // If it isn't, run the wearable service.
                Intent intent = new Intent(requireContext(), WearableService.class);
                ContextCompat.startForegroundService(requireContext(), intent);
            }
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        /* If the user goes out of the health fragment and goes back in again,
         * request for fit permissions (if not granted) again.
         * We'll also re-register the listener here, so that the app works when the user
         * goes out and goes back in again.*/
        permissionUtil.fitPermissions(requireActivity());
        if (circleHealthAdapter != null || memberHealthAdapter != null) {
            UserUtil.getInstance().registerListener(this);
        }
        // We'll register a listener to listen for circle changes to make sure that the user gets updated data.
        CircleUtil.getInstance().registerListener(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Once the view has been fully created, assign the views to the appropriate resource IDs.
        healthProgressBar = view.findViewById(R.id.healthProgressBar);
        memberHealthAdapter = new HealthRecyclerAdapter(requireContext(), null);
        circleHealthAdapter = new CircleHealthRecyclerAdapter(requireContext(), null);

        setUpRecyclerView(view);
        setUpHealthSwitch(view);

        // We should then check the current health mode and set the appropriate adapters.
        switch (healthMode) {
            // If the current health mode is 0 (Circle)
            case 0:
                // Set the recycler view adapter to the circle health adapter.
                circleMemberHealthView.setAdapter(circleHealthAdapter);
                break;
            // If the current health mode is 1 (Member)
            case 1:
                // Set the recycler view adapter to the circle health adapter.
                circleMemberHealthView.setAdapter(memberHealthAdapter);
                break;
        }

        // Once the recycler view have been set up, register a listener to listen for data changes.
        UserUtil.getInstance().registerListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtil.FIT_REQUEST_CODE && grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PermissionsRequest", permissions[i] + " granted.");
                } else {
                    // If somehow one of the permissions are denied, show a permission dialog.
                    Log.d("PermissionsRequest", permissions[i] + " denied.");
                }
            }
            // We don't really care whether the user has allowed or denied the permissions since
            // we're always checking and requesting the permission, so just restart the fragment once we're done.
            getParentFragmentManager().beginTransaction().detach(this).attach(this).commit();
        }
    }

    private void setUpRecyclerView(@NonNull View view) {
        circleMemberHealthView = view.findViewById(R.id.healthMemberList);


        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(requireContext());
        circleMemberHealthView.setLayoutManager(layoutManager);
        circleMemberHealthView.setItemAnimator(new DefaultItemAnimator());

        // To make it less "jarring", we'll slowly reveal the recyclerview with a slide in animation.
        Animation slideInAnimation = AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_in_left);
        slideInAnimation.setDuration(700);
        circleMemberHealthView.startAnimation(slideInAnimation);
    }

    private void setUpHealthSwitch(@NonNull View view) {
        healthViewSwitch = view.findViewById(R.id.healthViewSwitch);
        // By default, the health fragment should be at the member's mode, so we'll check that
        healthViewSwitch.check(R.id.memberHealthButton);
        healthViewSwitch.setSelectionRequired(true);
        // If the health view switch is clicked,
        healthViewSwitch.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            // Check if the buttons inside the switch is checked or clicked
            if (isChecked) {
                // If they are, check whether the clicked button is the circle health button.
                if (checkedId == R.id.circleHealthButton) {
                    // If it's the circle health button, set the healthMode to 0 (Circle mode)
                    healthMode = 0;
                    // and if the switch fragment boolean flag is currently false,
                    if (!switchFragmentFlag) {
                        // restart the fragment
                        resetFragment();
                        // and set the flag to true (needed to prevent the line above from executing multiple times).
                        switchFragmentFlag = true;
                    }
                } else if (checkedId == R.id.memberHealthButton) {
                    // else, if the member health button is clicked, set the health mode to 1
                    healthMode = 1;
                    // and if the switch fragment boolean flag is currently true,
                    if (switchFragmentFlag) {
                        // restart the fragment
                        resetFragment();
                        // and set the flag to true (needed to prevent the line above from executing multiple times).
                        switchFragmentFlag = false;
                    }
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        // When the user leaves the fragment, we should unregister the listener to prevent memory leaks.
        UserUtil.getInstance().unregisterListener(this);
        CircleUtil.getInstance().unregisterListener(this);
    }


    public static HealthFragment create(int mode) {
        // A method that creates an instance of the healthfragment with the health mode included.
        // Reference to Lab 8.
        HealthFragment healthFragment = new HealthFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(MODE_KEY, mode);
        healthFragment.setArguments(bundle);
        return healthFragment;
    }

    @Override
    public void onUsersChange(@NonNull DataSnapshot snapshot) {
        // If there's a change in the user node, get the current health mode
        switch (healthMode) {
            // if the current health mode is 0,
            case 0:
                // get new circle health information from the getCircleHealthInformation method.
                ArrayList<ArrayList<String>> circleHealthInformation = healthUtil.getCircleHealthInformation(snapshot);
                healthProgressBar.setVisibility(View.VISIBLE);
                if (circleHealthInformation != null && !circleHealthInformation.isEmpty()) {
                    healthProgressBar.setVisibility(View.GONE);
                    // and update the adapter with the new information.
                    circleHealthAdapter.updateInformation(circleHealthInformation);
                }
                break;
            // else, if the current health mode is 1,
            case 1:
                // get new member health information from the getMemberHealthInformation method.
                ArrayList<User> memberHealthInformation = healthUtil.getMemberHealthInformation(snapshot);
                healthProgressBar.setVisibility(View.VISIBLE);
                if (memberHealthInformation != null && !memberHealthInformation.isEmpty()) {
                    healthProgressBar.setVisibility(View.GONE);
                    // and update the adapter with the new information.
                    memberHealthAdapter.updateInformation(memberHealthInformation);
                }
                break;
        }
    }

    @Override
    public void onCircleChange() {
        resetFragment();
    }

    private void resetFragment() {
        if (isAdded()) {
            getParentFragmentManager().beginTransaction().detach(this).attach(this).commitAllowingStateLoss();
        }
    }
}