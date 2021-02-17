package com.timothydillan.circles;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.timothydillan.circles.Adapters.RecyclerAdapter;
import com.timothydillan.circles.Models.ItemModel;
import com.timothydillan.circles.Services.CrashService;
import com.timothydillan.circles.Services.LocationService;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.FirebaseUtil;
import com.timothydillan.circles.Utils.LocationUtil;
import com.timothydillan.circles.Utils.PermissionUtil;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;
import com.timothydillan.circles.Utils.UserUtil;

public class SettingsFragment extends Fragment {

    /* TODO:
    1. Circle related configurations:
        1. Edit Circle Name
        2. Remove Circle
        3. Circle Members
        4. Circle Places (Geo-fencing, notifications)
    2. Account Configuration
        2. Remove Account -> delete circle and data in db
        3. Notifications
            1. Notification if a user joined circle
            2. Notification that we're currently tracking him/her
            3. Mood notifications
    3. Privacy & Security
        1. Password
    */

    private ItemModel circleConfigItemList = new ItemModel();
    private ItemModel accountConfigItemList = new ItemModel();
    private ConstraintLayout passwordButton;
    private SwitchMaterial biometricsSwitch;
    private SharedPreferencesUtil sharedPreferences;
    private PermissionUtil permissionUtil;

    private RecyclerAdapter.RecyclerViewClickListener circleConfigListener;
    private RecyclerAdapter.RecyclerViewClickListener accountConfigListener;

    private Button signOutButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = new SharedPreferencesUtil(requireContext());
        permissionUtil = new PermissionUtil(requireContext());
        addCircleConfigurations();
        addAccountConfigurations();
    }

    private void setUpRecyclerView(RecyclerView recyclerView, ItemModel itemList, RecyclerAdapter.RecyclerViewClickListener listener) {
        RecyclerAdapter adapter = new RecyclerAdapter(itemList, listener, R.layout.settings_list_items);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        DividerItemDecoration mDividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(mDividerItemDecoration);
        recyclerView.setAdapter(adapter);
    }

    private void addCircleConfigurations() {
        circleConfigItemList.addItem("Edit Circle Name", EditCircleNameActivity.class);
        circleConfigItemList.addItem("Remove Circle", JoinCircleActivity.class);
        circleConfigItemList.addItem("Circle Members", CircleMembersActivity.class);
        circleConfigItemList.addItem("Circle Places", JoinCircleActivity.class);
        circleConfigItemList.addItem("Join a Circle", JoinCircleActivity.class);
    }

    private void addAccountConfigurations() {
        accountConfigItemList.addItem("Edit Details", EditProfileActivity.class);
        accountConfigItemList.addItem("Remove Account", JoinCircleActivity.class);
        accountConfigItemList.addItem("Notifications", JoinCircleActivity.class);
    }

    private void setCircleOnClickListener() {
        circleConfigListener = (v, position) -> {
            startActivity(circleConfigItemList, position);
        };
    }

    private void setAccountOnClickListener() {
        accountConfigListener = (v, position) -> {
            startActivity(accountConfigItemList, position);
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    void startActivity(ItemModel itemList, int position) {
        Intent intent = new Intent(requireContext(), itemList.getItemActivity(position));
        startActivity(intent);
        requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView circleConfigView = view.findViewById(R.id.circleConfigurations);
        RecyclerView accountConfigView = view.findViewById(R.id.accountConfigurations);
        passwordButton = view.findViewById(R.id.passwordButton);
        signOutButton = view.findViewById(R.id.signOutButton);
        biometricsSwitch = view.findViewById(R.id.fingeprintSwitch);

        passwordButton.setOnClickListener(view1 -> {
            Intent intent = new Intent(requireContext(), PasswordActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        biometricsSwitch.setChecked(sharedPreferences.isBiometricsSecurityEnabled());

        biometricsSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            permissionUtil.requestBiometricPermissions();
            sharedPreferences.writeBoolean(SharedPreferencesUtil.BIOMETRICS_KEY, b);
            Toast.makeText(requireContext(), b ? "Biometric authentication enabled." : "Biometric authentication disabled.", Toast.LENGTH_SHORT).show();
        });

        signOutButton.setOnClickListener(v -> signOut());

        setCircleOnClickListener();
        setUpRecyclerView(circleConfigView, circleConfigItemList, circleConfigListener);

        setAccountOnClickListener();
        setUpRecyclerView(accountConfigView, accountConfigItemList, accountConfigListener);
    }

    public void signOut() {
        // Reset all variables.
        FirebaseUtil.signOut();
        UserUtil.resetUser();
        CircleUtil.resetCircle();
        LocationUtil.resetMap();
        // Stop all services.
        requireContext().stopService(new Intent(requireContext(), LocationService.class));
        requireContext().stopService(new Intent(requireContext(), CrashService.class));
        // Redirect back to Sign up activity.
        Intent signUpActivity = new Intent(requireContext(), SignUpActivity.class);
        startActivity(signUpActivity);
        requireActivity().finish();
    }
}