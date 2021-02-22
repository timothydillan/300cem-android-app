package com.timothydillan.circles;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.timothydillan.circles.Adapters.MoodRecyclerAdapter;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.Services.WearableService;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.MoodUtil;
import com.timothydillan.circles.Utils.PermissionUtil;
import com.timothydillan.circles.Utils.UserUtil;
import java.util.ArrayList;

public class MoodsFragment extends Fragment implements UserUtil.UsersListener, CircleUtil.CircleUtilListener {

    private static final String TAG = "MoodsFragment";
    private RecyclerView circleMoodsRecyclerView;
    private MoodRecyclerAdapter moodAdapter;
    private PermissionUtil permissionUtil;
    private ProgressBar moodProgressBar;
    private MoodUtil moodUtil = MoodUtil.getInstance();
    private boolean isWatchPaired = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        moodUtil.initializeContext(requireContext());
        permissionUtil = new PermissionUtil(requireContext());
        permissionUtil.fitPermissions(requireActivity());

    }

    @Override
    public void onResume() {
        super.onResume();
        /* If the user goes out of the moods fragment and goes back in again,
         * request for fit permissions (if not granted) again. */
        permissionUtil.fitPermissions(requireActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_moods, container, false);
        // First check whether the user has already paired a wearable and has already granted fit permissions
        if (permissionUtil.hasWatchApp() && permissionUtil.hasFitPermissions() && !moodUtil.getMemberMoodInformation().isEmpty()) {
            // If the check above passes, check if the wearable service is running.
            if (!WearableService.isServiceRunning(requireContext())) {
                // if it isn't then start the service.
                Intent intent = new Intent(requireContext(), WearableService.class);
                ContextCompat.startForegroundService(requireContext(), intent);
            }
        } else {
            // If the checks returns false, set the view to the error layout.
            isWatchPaired = false;
            view = inflater.inflate(R.layout.error_page_layout, container, false);
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!isWatchPaired) {
            Button addMemberButton = view.findViewById(R.id.addMemberButton);
            addMemberButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), InviteCodeActivity.class)));
            return;
        }

        // If the view has been fully created, assign the views to their corresponding Resource IDs.
        moodProgressBar = view.findViewById(R.id.moodProgressBar);
        circleMoodsRecyclerView = view.findViewById(R.id.moodRecylcerView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(requireContext());
        circleMoodsRecyclerView.setLayoutManager(layoutManager);
        circleMoodsRecyclerView.setItemAnimator(new DefaultItemAnimator());
        // and set up the recycler view.
        setUpRecyclerView((v, position) -> {
            // If one of the items in the recycler view is clicked, check if the mood of the member being clicked is negative/unwell.
            if (moodAdapter.getMember(position).getMood().equals("Unwell") || moodAdapter.getMember(position).getMood().equals("Negative")) {
                // If the check above passes, open WhatsApp with a placeholder message that asks about the member's condition.
                openWhatsApp(moodAdapter.getMember(position).getPhone(), "Hey there, are you okay?");
            // else,
            } else {
                // open WhatsApp with a generic placeholder message.
                openWhatsApp(moodAdapter.getMember(position).getPhone(), "Yo!");
            }
        });
        // We'll register a user listener to listen for mood changes.
        UserUtil.getInstance().registerListener(this);
        // Then we'll also register a listener to listen for circle changes to make sure that the user gets updated circle data.
        CircleUtil.getInstance().registerListener(this);
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
            getParentFragmentManager().beginTransaction().detach(this).attach(this).commit();
        }
    }

    private void openWhatsApp(String phone, String message) {
        String url = "https://api.whatsapp.com/send?phone=" + phone + "&text=" + message;
        try {
            // First check if the device has WhatsApp. If the device does not have WhatsApp, the catch clause will handle the exception.
            PackageManager pm = requireContext().getPackageManager();
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
            // If the device does have WhatsApp, we'll create an intent and set the data to the
            // url above, with the phone number of the member and the message as arguments for the url
            Intent whatsAppIntent = new Intent(Intent.ACTION_VIEW);
            whatsAppIntent.setData(Uri.parse(url));
            // we'll then set the intent to be opened directly with WhatsApp.
            whatsAppIntent.setPackage("com.whatsapp");
            startActivity(whatsAppIntent);
        } catch (PackageManager.NameNotFoundException e) {
            // If the user does not have WhatsApp, show a toast message.
            Toast.makeText(requireContext(), "The WhatsApp app is not installed.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void setUpRecyclerView(MoodRecyclerAdapter.RecyclerViewClickListener listener) {
        moodAdapter = new MoodRecyclerAdapter(requireContext(), listener);
        circleMoodsRecyclerView.setAdapter(moodAdapter);
    }

    @Override
    public void onCircleChange() {
        resetFragment();
    }

    @Override
    public void onUsersChange(@NonNull DataSnapshot snapshot) {
        // If the user node changed, get new mood information from the database
        ArrayList<User> newMemberInformation = moodUtil.getMemberMoodInformation(snapshot);
        moodProgressBar.setVisibility(View.VISIBLE);
        if (newMemberInformation != null && !newMemberInformation.isEmpty()) {
            moodProgressBar.setVisibility(View.GONE);
            // and update the recylcer view with the new information.
            moodAdapter.updateInformation(newMemberInformation);
        }
    }

    private void resetFragment() {
        if (isAdded()) {
            getParentFragmentManager().beginTransaction().detach(this).attach(this).commit();
        }
    }
}