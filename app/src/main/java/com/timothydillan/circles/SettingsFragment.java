package com.timothydillan.circles;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.timothydillan.circles.Adapters.SettingsRecyclerAdapter;
import com.timothydillan.circles.Models.Item;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.Services.CrashService;
import com.timothydillan.circles.Services.LocationService;
import com.timothydillan.circles.Services.WearableService;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.FirebaseUtil;
import com.timothydillan.circles.Utils.LocationUtil;
import com.timothydillan.circles.Utils.PermissionUtil;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;
import com.timothydillan.circles.Utils.UserUtil;

public class SettingsFragment extends Fragment implements CircleUtil.CircleUtilListener {

    private CircleUtil circleUtil = CircleUtil.getInstance();
    private UserUtil userUtil = UserUtil.getInstance();
    private User currentUser = userUtil.getCurrentUser();
    private Item circleConfigItemList = new Item();
    private Item accountConfigItemList = new Item();
    private SharedPreferencesUtil sharedPreferences;
    private PermissionUtil permissionUtil;

    private SettingsRecyclerAdapter.RecyclerViewClickListener circleConfigListener;
    private SettingsRecyclerAdapter.RecyclerViewClickListener accountConfigListener;
    private ConstraintLayout passwordButton;
    private SwitchMaterial biometricsSwitch;
    private Button signOutButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = new SharedPreferencesUtil(requireContext());
        permissionUtil = new PermissionUtil(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // When the view has been fully created, assign the views to the corresponding resource IDs.
        RecyclerView circleConfigView = view.findViewById(R.id.circleConfigurations);
        RecyclerView accountConfigView = view.findViewById(R.id.accountConfigurations);
        passwordButton = view.findViewById(R.id.passwordButton);
        signOutButton = view.findViewById(R.id.signOutButton);
        biometricsSwitch = view.findViewById(R.id.fingeprintSwitch);

        addCircleConfigurations();
        addAccountConfigurations();

        // If the user clicked on the password authentication button,
        passwordButton.setOnClickListener(view1 -> {
            // we'll redirect the user to that activity.
            Intent intent = new Intent(requireContext(), PasswordActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Set the biometrics switch value to the same value in sharedpreferences.
        // By default, isBiometricsSecurityEnabled returns false, so by default biometrics switch should also be false.
        biometricsSwitch.setChecked(sharedPreferences.isBiometricsSecurityEnabled());

        biometricsSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            // If the biometric switch is on, request biometric permissions.
            if (b) { permissionUtil.biometricPermissions(requireActivity()); }
            if (permissionUtil.hasBiometricPermissions()) {
                // write the new value of the switch to sharedpreferences
                sharedPreferences.writeBoolean(SharedPreferencesUtil.BIOMETRICS_KEY, b);
                // and show a toast message that corresponds to the boolean value of the switch.
                Toast.makeText(requireContext(), b ? "Biometric authentication enabled." : "Biometric authentication disabled.", Toast.LENGTH_SHORT).show();
            }
        });

        signOutButton.setOnClickListener(v -> signOut());

        setCircleOnClickListener();
        setUpRecyclerView(circleConfigView, circleConfigItemList, circleConfigListener);

        setAccountOnClickListener();
        setUpRecyclerView(accountConfigView, accountConfigItemList, accountConfigListener);
    }

    @Override
    public void onCircleChange() {
        // When there is a data change in the user's current circle, reset the fragment.
        resetFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        // We'll register a listener to listen for circle changes to make sure that the user gets updated data.
        CircleUtil.getInstance().registerListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // We'll unregister the listener when the user leaves the activity because we don't need to update the settings data when the user isn't in the activity.
        CircleUtil.getInstance().unregisterListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtil.BIOMETRIC_REQUEST_CODE && grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PermissionsRequest", permissions[i] + " granted.");
                } else {
                    // If somehow one of the permissions are denied, show a permission dialog.
                    Log.d("PermissionsRequest", permissions[i] + " denied.");
                }
            }
            getParentFragmentManager().beginTransaction().detach(this).attach(this).commit();
        }
    }

    private void setUpRecyclerView(RecyclerView recyclerView, Item itemList, SettingsRecyclerAdapter.RecyclerViewClickListener listener) {
        SettingsRecyclerAdapter adapter = new SettingsRecyclerAdapter(itemList, listener);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        DividerItemDecoration mDividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(mDividerItemDecoration);
        recyclerView.setAdapter(adapter);
    }

    private void addCircleConfigurations() {
        // Since the fragment may restart, we should clear all items in the item list,
        // and create a new instance to prevent the old settings from overlapping with the new settings lists.
        circleConfigItemList = null;
        circleConfigItemList = new Item();
        // Add a list of items with corresponding classes for the circle settings.
        if (UserUtil.getUserRole().equals("Admin")) {
            circleConfigItemList.addItem("Edit Circle Name", EditCircleNameActivity.class);
        }
        if (currentUser.getCurrentCircleSession() != currentUser.getMyCircle()) {
            circleConfigItemList.addItem("Leave Circle", null);
        }
        circleConfigItemList.addItem("Create a Circle", CreateCircleActivity.class);
        circleConfigItemList.addItem("Circle Members", CircleMembersActivity.class);
        circleConfigItemList.addItem("Join a Circle", JoinCircleActivity.class);
    }

    private void addAccountConfigurations() {
        // Since the fragment may restart, we should clear all items in the item list,
        // and create a new instance to prevent the old settings from overlapping with the new settings lists.
        accountConfigItemList = null;
        accountConfigItemList = new Item();
        // Add a list of items with corresponding classes for the account settings.
        accountConfigItemList.addItem("Edit Details", EditProfileActivity.class);
        accountConfigItemList.addItem("Remove Account", null);
    }

    private void setCircleOnClickListener() {
        circleConfigListener = (v, position) -> {
            // If one of the items in the circle settings is clicked,
            // check if the position = 0 (member) or if the position = 1 (admin) and if the current circle that the user is in isn't his/her circle.
            if ((UserUtil.getUserRole().equals("Member") && position == 0
                    || UserUtil.getUserRole().equals("Admin") && position == 1)
                    && currentUser.getCurrentCircleSession() != currentUser.getMyCircle()) {
                // If the check above passes, we'll create a alert dialog to confirm that the user actually wants to leave the circle.
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                builder.setTitle("Are you sure?")
                        .setMessage("Leaving this circle means that you won't see any information about each member in your current circle, and you'll stop sharing your information with them too.")
                        .setCancelable(true)
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Yes", (dialogInterface, i) -> {
                            // If the user confirms that they want to leave the circle, we'll stop the location service
                            requireContext().stopService(new Intent(requireContext(), LocationService.class));
                            // We'll remove the user from the circle,
                            circleUtil.removeMember(currentUser.getUid());
                            // and update the user's current circle session remotely and locally.
                            userUtil.updateDbUserCurrentCircle(currentUser.getMyCircle());
                            currentUser.setCurrentCircleSession(currentUser.getMyCircle());

                            CircleUtil.resetCircle();
                            // We'll then go back to MainActivity.
                            Intent mainIntent = new Intent(requireContext(), MainActivity.class);
                            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(mainIntent);
                            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            requireActivity().finish();
                        });
                builder.show();
            } else {
                // Else, we'll just start the activity that corresponds to the current item position.
                startActivity(circleConfigItemList, position);
            }
        };
    }

    private void setAccountOnClickListener() {
        accountConfigListener = (v, position) -> {
            // Similar to the code above, we'll check if the position of the item being clicked isn't 1 (Remove Account).
            if (position != 1) {
                // if it isn't 1, we'll just start the activity that corresponds to the current item position.
                startActivity(accountConfigItemList, position);
            } else {
                // Else, we'll create a alert dialog to confirm that the user actually wants to delete their account,
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                builder.setTitle("Are you sure?")
                        .setMessage("Removing your account will remove all your data in the app, including data stored in each circle you've joined.")
                        .setCancelable(true)
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Yes", (dialogInterface, i) -> {
                            // and we'll delete their account if they confirm.
                            deleteAccount();
                        });
                builder.show();
            }
        };
    }

    private void startActivity(Item itemList, int position) {
        Intent intent = new Intent(requireContext(), (Class<?>) itemList.getItemValue(position));
        startActivity(intent);
        requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void signOut() {
        // Stop all services.
        requireContext().stopService(new Intent(requireContext(), LocationService.class));
        requireContext().stopService(new Intent(requireContext(), CrashService.class));
        requireContext().stopService(new Intent(requireContext(), WearableService.class));

        // Reset all variables.
        FirebaseUtil.signOut();
        userUtil.reset();
        circleUtil.reset();
        LocationUtil.resetMap();

        // Redirect back to Sign up activity.
        goToSignUpActivity();
    }

    private void deleteAccount() {
        // Stop all services.
        requireContext().stopService(new Intent(requireContext(), LocationService.class));
        requireContext().stopService(new Intent(requireContext(), CrashService.class));
        requireContext().stopService(new Intent(requireContext(), WearableService.class));

        // Reset all variables and delete user from firebase auth.
        userUtil.deleteAccount();
        // And we should reset their configuration data as well.
        sharedPreferences.removeAllItems();

        // Redirect back to Sign up activity.
        goToSignUpActivity();
    }

    private void goToSignUpActivity() {
        Intent signUpIntent = new Intent(requireContext(), SignUpActivity.class);
        signUpIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(signUpIntent);
        requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        requireActivity().finish();
    }

    private void resetFragment() {
        if (isAdded()) {
            getParentFragmentManager().beginTransaction().detach(this).attach(this).commit();
        }
    }
}