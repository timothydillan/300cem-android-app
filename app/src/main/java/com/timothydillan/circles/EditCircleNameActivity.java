package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.timothydillan.circles.Utils.FirebaseUtil;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;
import com.timothydillan.circles.Utils.UserUtil;

public class EditCircleNameActivity extends AppCompatActivity {

    private SharedPreferencesUtil sharedPreferences;
    private TextInputEditText circleNameTextEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_circle_name);
        sharedPreferences = new SharedPreferencesUtil(this);
        sharedPreferences.writeBoolean(SharedPreferencesUtil.ACTIVITY_APP_KEY, true);
        FirebaseUtil.initializeCircleName();
        circleNameTextEdit = findViewById(R.id.circleNameTextEdit);
        circleNameTextEdit.setText(FirebaseUtil.getCurrentCircleName());
    }

    public void updateCircleName(View v) {
        FirebaseUtil.getDbReference().child("Circles")
                .child(String.valueOf(UserUtil.getCurrentUser().getCurrentCircleSession()))
                .child("Members")
                .child(UserUtil.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.child("memberRole").getValue(String.class).equals("Admin")) {
                            FirebaseUtil.getDbReference()
                                    .child("Circles")
                                    .child(String.valueOf(UserUtil.getCurrentUser().getCurrentCircleSession()))
                                    .child("name")
                                    .setValue(circleNameTextEdit.getText().toString())
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(EditCircleNameActivity.this, "Successfully updated your circle name.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(EditCircleNameActivity.this, "Failed to update your circle name.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(EditCircleNameActivity.this, "You are not an admin, unfortunately!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

}