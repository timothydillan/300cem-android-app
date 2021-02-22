package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.timothydillan.circles.Adapters.MemberRecyclerAdapter;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;
import com.timothydillan.circles.Utils.UserUtil;

public class CircleMembersActivity extends AppCompatActivity implements CircleUtil.CircleUtilListener {

    private SharedPreferencesUtil sharedPreferences;
    private RecyclerView circleMemberList;
    private MemberRecyclerAdapter memberAdapter;
    private CircleUtil circleUtil = CircleUtil.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circle_members);
        sharedPreferences = new SharedPreferencesUtil(this);

        Toolbar toolbar = findViewById(R.id.inviteCodeToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        setUpRecyclerView(listener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // We'll register a listener to listen for circle changes to make sure that the user gets updated data.
        circleUtil.registerListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Make sure that the foreground state is set to true, so that the authentication process
        // runs only when the user completely leaves the app and goes back.
        sharedPreferences.writeBoolean(SharedPreferencesUtil.FOREGROUND_KEY, true);
        // We'll unregister the listener when the user leaves the activity because we don't need to update the data when the user isn't in the activity.
        circleUtil.unregisterListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.invitation_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else if (id == R.id.inviteButton) {
            startActivity(new Intent(this, InviteCodeActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void setUpRecyclerView(MemberRecyclerAdapter.RecyclerViewClickListener listener) {
        memberAdapter = new MemberRecyclerAdapter(this, listener);
        circleMemberList = findViewById(R.id.circleMemberList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        circleMemberList.setLayoutManager(layoutManager);
        circleMemberList.setItemAnimator(new DefaultItemAnimator());
        circleMemberList.setAdapter(memberAdapter);
    }

    private void showEditMemberDialog(int position) {
        // Create a view that uses the role layout for the alert dialog layout.
        View view = LayoutInflater.from(CircleMembersActivity.this).inflate(R.layout.role_layout,
                (ViewGroup) findViewById(android.R.id.content), false);
        RadioGroup roleGroup = view.findViewById(R.id.roleGroup);
        roleGroup.check(R.id.memberRoleButton);

        // Get the current member,
        User member = memberAdapter.getMember(position);

        // Create an alert dialog with the role selection layout as it's view.
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(view);
        builder.setTitle("Edit " + member.getFirstName() + "'s role")
                .setCancelable(true)
                .setNegativeButton("No", ((dialog, which) -> dialog.dismiss()))
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    // If the user clicked yes, get the radiobutton ID that's being clicked.
                    int id = roleGroup.getCheckedRadioButtonId();
                    // If the id is the member role button,
                    if (id == R.id.memberRoleButton) {
                        // set the member role as member
                        circleUtil.editMemberRole(member.getUid(), "Member");
                        Toast.makeText(CircleMembersActivity.this, "Changed " +
                                member.getFirstName() + "'s role to Member.", Toast.LENGTH_SHORT).show();
                    // else, if the id is the admin role button,
                    } else if (id == R.id.adminRoleButton) {
                        // set the member role as admin
                        circleUtil.editMemberRole(member.getUid(), "Admin");
                        Toast.makeText(CircleMembersActivity.this, "Changed " +
                                member.getFirstName() + "'s role to Admin.", Toast.LENGTH_SHORT).show();
                    }
                });
        builder.show();
    }

    private void showDeleteMemberDialog(int position) {
        // Get the current member,
        User member = memberAdapter.getMember(position);
        // Create an alert dialog with the role selection layout as it's view.
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Are you sure?")
                .setMessage("Are you sure you want to remove " + member.getFirstName() + " from your circle?")
                .setCancelable(true)
                .setNegativeButton("No", ((dialog, which) -> dialog.dismiss()))
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    // If the user confirms the operation, remove the member and show a message.
                    circleUtil.removeMember(member.getUid());
                    // and update the user's current circle session remotely and locally.
                    UserUtil.getInstance().updateDbUserCurrentCircle(member.getUid(), member.getMyCircle());

                    CircleUtil.resetCircle();

                    Toast.makeText(CircleMembersActivity.this, "Removed " +
                            member.getFirstName() + " from your circle.", Toast.LENGTH_SHORT).show();
                });
        builder.show();
    }

    private MemberRecyclerAdapter.RecyclerViewClickListener listener =
            new MemberRecyclerAdapter.RecyclerViewClickListener() {
                @Override
                public void onEditClick(int position) {
                    showEditMemberDialog(position);
                }
                @Override
                public void onDeleteClick(int position) {
                    showDeleteMemberDialog(position);
                }
            };

    @Override
    public void onCircleChange() {
        recreate();
    }

}