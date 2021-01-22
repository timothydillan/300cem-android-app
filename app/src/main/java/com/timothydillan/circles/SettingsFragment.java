package com.timothydillan.circles;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.timothydillan.circles.Adapters.RecyclerAdapter;
import com.timothydillan.circles.Models.ItemModel;

public class SettingsFragment extends Fragment {

    /* TODO:
    2. Show invite code
    3. Modify circle name
    4. Circle management
    5. Other settings, account, privacy, blabla
    */
    private final ItemModel circleConfigItemList = new ItemModel();
    private final ItemModel accountConfigItemList = new ItemModel();
    private final ItemModel privacyConfigItemList = new ItemModel();

    private RecyclerView circleConfigView, accountConfigView, privacyConfigView;
    private RecyclerAdapter.RecyclerViewClickListener circleConfigListener, accountConfigListener, privacyConfigListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addCircleConfigurations();
        addAccountConfigurations();
        addPrivacyConfigurations();
    }

    private void setUpRecyclerView(RecyclerView recyclerView, ItemModel itemList, RecyclerAdapter.RecyclerViewClickListener listener) {
        RecyclerAdapter adapter = new RecyclerAdapter(itemList, listener, R.layout.settings_list_items);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
    }

    private void addCircleConfigurations() {
        circleConfigItemList.addItem("Edit Circle Name", JoinCircleActivity.class);
        circleConfigItemList.addItem("Remove Circle", JoinCircleActivity.class);
        circleConfigItemList.addItem("Circle Members", CircleMembersActivity.class);
        circleConfigItemList.addItem("Circle Places", JoinCircleActivity.class);
        circleConfigItemList.addItem("Join a Circle", JoinCircleActivity.class);
    }

    private void addAccountConfigurations() {
        accountConfigItemList.addItem("Edit Details", JoinCircleActivity.class);
        accountConfigItemList.addItem("Remove Account", JoinCircleActivity.class);
        accountConfigItemList.addItem("Notifications", JoinCircleActivity.class);
    }

    private void addPrivacyConfigurations() {
        privacyConfigItemList.addItem("Password", JoinCircleActivity.class);
        privacyConfigItemList.addItem("Fingerprint", JoinCircleActivity.class);
        privacyConfigItemList.addItem("Face ID", JoinCircleActivity.class);
    }

    private void setCircleOnClickListener() {
        circleConfigListener = (v, position) -> {
            Intent intent = new Intent(getContext(), circleConfigItemList.getItemActivity(position));
            startActivity(intent);
        };
    }

    private void setAccountOnClickListener() {
        accountConfigListener = (v, position) -> {
            Intent intent = new Intent(getContext(), accountConfigItemList.getItemActivity(position));
            startActivity(intent);
        };
    }

    private void setPrivacyOnClickListener() {
        privacyConfigListener = (v, position) -> {
            Intent intent = new Intent(getContext(), privacyConfigItemList.getItemActivity(position));
            startActivity(intent);
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        circleConfigView = rootView.findViewById(R.id.circleConfigurations);
        accountConfigView = rootView.findViewById(R.id.accountConfigurations);
        privacyConfigView = rootView.findViewById(R.id.privacyConfigurations);

        setCircleOnClickListener();
        setUpRecyclerView(circleConfigView, circleConfigItemList, circleConfigListener);

        setAccountOnClickListener();
        setUpRecyclerView(accountConfigView, accountConfigItemList, accountConfigListener);

        setPrivacyOnClickListener();
        setUpRecyclerView(privacyConfigView, privacyConfigItemList, privacyConfigListener);

        return rootView;
    }
}