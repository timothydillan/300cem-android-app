package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.os.Bundle;

import com.timothydillan.circles.UI.Message;
import com.timothydillan.circles.Utils.PermissionUtil;
import com.timothydillan.circles.Utils.SecurityUtil;

public class PasswordActivity extends AppCompatActivity {
    Message message;
    SecurityUtil securityUtil;
    PermissionUtil permissionUtil;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);
        message = new Message(this);
        securityUtil = new SecurityUtil(this);
        permissionUtil = new PermissionUtil(this);
        permissionUtil.requestBiometricPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtil.FINGERPRINT_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                securityUtil.authenticateUserBiometrics();
            } else {
                permissionUtil.showPermissionsDialog(permissions[0], 2);
            }
        }
    }
}