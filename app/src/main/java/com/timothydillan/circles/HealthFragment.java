package com.timothydillan.circles;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.LayoutRes;
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
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.database.DataSnapshot;
import com.timothydillan.circles.Adapters.CircleHealthRecyclerAdapter;
import com.timothydillan.circles.Adapters.HealthRecyclerAdapter;
import com.timothydillan.circles.Adapters.LocationRecyclerAdapter;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.Services.WearableService;
import com.timothydillan.circles.Utils.HealthUtil;
import com.timothydillan.circles.Utils.LocationUtil;
import com.timothydillan.circles.Utils.PermissionUtil;
import com.timothydillan.circles.Utils.UserUtil;

import java.util.ArrayList;
import java.util.Objects;

public class HealthFragment extends Fragment {

    private static final String MODE_KEY = "MODE_KEY";

    private RecyclerView circleMemberHealthView;
    private HealthRecyclerAdapter.RecyclerViewClickListener circleMemberHealthListener;
    private HealthRecyclerAdapter memberHealthAdapter;
    private CircleHealthRecyclerAdapter circleHealthAdapter;
    private UserUtil userUtil = new UserUtil();
    private PermissionUtil permissionUtil;
    private MaterialButtonToggleGroup healthViewSwitch;
    private boolean switchFragment = false;
    int healthMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionUtil = new PermissionUtil(requireContext());
        permissionUtil.requestFitPermissions();
        healthMode = getArguments().getInt(MODE_KEY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_health, container, false);
        // Inflate the layout for this fragment
        if (permissionUtil.hasWatchApp()) {
            Intent intent = new Intent(requireContext(), WearableService.class);
            ContextCompat.startForegroundService(requireContext(), intent);
        } else {
            // show Empty view.
            // heartRateTextView.setText("Your device is not paired to a Watch.");
            healthMode = 2;
            view = inflater.inflate(R.layout.error_page_layout, container, false);
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        switch (healthMode) {
            // If circle
            case 0:
                setUpHealthSwitch(view);
                setUpRecyclerView();
                setInformationChangeListener(0);
                break;
            // If member
            case 1:
                setUpHealthSwitch(view);
                circleMemberHealthListener = (v, position) -> {
                    //Toast.makeText(requireContext(), "CHECK YES", Toast.LENGTH_SHORT).show();
                };
                setUpRecyclerView(circleMemberHealthListener);
                setInformationChangeListener(1);
                break;
            // If error
            case 2:
                // add member button
                break;
        }
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
                    permissionUtil.showPermissionsDialog(permissions[i], 1);
                }
            }
            getParentFragmentManager().beginTransaction().detach(this).attach(this).commit();
        }
    }

    private void setUpHealthSwitch(@NonNull View view) {
        circleMemberHealthView = view.findViewById(R.id.healthMemberList);
        circleMemberHealthView.setAlpha(0f);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(requireContext());
        circleMemberHealthView.setLayoutManager(layoutManager);
        circleMemberHealthView.setItemAnimator(new DefaultItemAnimator());
        circleMemberHealthView.animate().alpha(1f).setDuration(1000);
        healthViewSwitch = view.findViewById(R.id.healthViewSwitch);
        healthViewSwitch.check(R.id.memberHealthButton);
        healthViewSwitch.setSelectionRequired(true);
        healthViewSwitch.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.circleHealthButton) {
                    healthMode = 0;
                    if (!switchFragment) {
                        getParentFragmentManager().beginTransaction().detach(this).attach(this).commit();
                        switchFragment = true;
                    }
                } else if (checkedId == R.id.memberHealthButton) {
                    healthMode = 1;
                    if (switchFragment) {
                        getParentFragmentManager().beginTransaction().detach(this).attach(this).commit();
                        switchFragment = false;
                    }
                }
            }
        });
    }

    private void setUpRecyclerView(HealthRecyclerAdapter.RecyclerViewClickListener listener) {
        memberHealthAdapter = new HealthRecyclerAdapter(requireContext(), listener, R.layout.member_health_list);
        circleMemberHealthView.setAdapter(memberHealthAdapter);
    }

    private void setUpRecyclerView() {
        circleHealthAdapter = new CircleHealthRecyclerAdapter(requireContext(), null, R.layout.circle_health_list);
        circleMemberHealthView.setAdapter(circleHealthAdapter);
    }

    private void setInformationChangeListener(int modeOf) {
        switch (modeOf) {
            case 0:
                userUtil.addEventListener(new UserUtil.UsersListener() {
                    @Override
                    public void onUserReady() { }

                    @Override
                    public void onUsersChange(@NonNull DataSnapshot snapshot) {
                        ArrayList<String> newMemberInformation = HealthUtil.getCircleHealthInformation(snapshot);
                        if (newMemberInformation != null && !newMemberInformation.isEmpty())
                            circleHealthAdapter.updateInformation(newMemberInformation);
                    }
                });
                break;
            case 1:
                userUtil.addEventListener(new UserUtil.UsersListener() {
                    @Override
                    public void onUserReady() { }

                    @Override
                    public void onUsersChange(@NonNull DataSnapshot snapshot) {
                        ArrayList<User> newMemberInformation = HealthUtil.getMemberHealthInformation(snapshot);
                        if (newMemberInformation != null && !newMemberInformation.isEmpty())
                            memberHealthAdapter.updateInformation(newMemberInformation);
                    }
                });
                break;
        }
    }

    public static HealthFragment create(int mode) {
        HealthFragment healthFragment = new HealthFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(MODE_KEY, mode);
        healthFragment.setArguments(bundle);
        return healthFragment;
    }
}