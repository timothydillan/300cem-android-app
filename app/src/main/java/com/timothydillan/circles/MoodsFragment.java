package com.timothydillan.circles;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.timothydillan.circles.Utils.PermissionUtil;

import java.util.List;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;

public class MoodsFragment extends Fragment {

    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1;
    private static final String TAG = "MoodsFragment";
    private PermissionUtil permissionUtil;

    GoogleSignInOptionsExtension fitnessOptions =
            FitnessOptions.builder()
                    .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                    .build();

    GoogleSignInAccount googleSignInAccount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionUtil = new PermissionUtil(requireContext());
        permissionUtil.requestFitPermissions();

        googleSignInAccount = GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_moods, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!GoogleSignIn.hasPermissions(googleSignInAccount, fitnessOptions)) {
            GoogleSignIn.requestPermissions(Objects.requireNonNull(getActivity()),
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    googleSignInAccount,
                    fitnessOptions);
        } else {
            accessGoogleFit();
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
                    permissionUtil.showPermissionsDialog(permissions[i], false);
                }
            }
            assert getFragmentManager() != null;
            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE && resultCode == RESULT_OK) {
            accessGoogleFit();
        }
    }

    private void accessGoogleFit() {
        Fitness.getSensorsClient(requireContext(), GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions))
                .findDataSources(
                        new DataSourcesRequest.Builder()
                                .setDataTypes(DataType.TYPE_HEART_RATE_BPM)
                                .setDataSourceTypes(DataSource.TYPE_RAW)
                                .build())
                .addOnSuccessListener(new OnSuccessListener<List<DataSource>>() {
                    @Override
                    public void onSuccess(List<DataSource> dataSources) {
                        for (DataSource i : dataSources) {
                            Log.d(TAG, i.getStreamIdentifier());
                            Log.d(TAG, i.getDataType().getName());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Find data sources request failed", e);

                    }
                });
    }
}