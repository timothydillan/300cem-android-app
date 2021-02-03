package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.timothydillan.circles.Models.Circle;
import com.timothydillan.circles.Utils.CircleUtil;

public class JoinCircleActivity extends ActivityInterface {
    private static final String FIREBASE_TAG = "firebaseRelated";
    private DatabaseReference databaseReference;
    private TextInputLayout circleInput;
    private final String USER_UID = CircleUtil.getCurrentMember().getUid();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_circle);

        Toolbar toolbar = findViewById(R.id.joinToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        circleInput = findViewById(R.id.circleCodeInputLayout);
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }

    public void onJoinButtonClick(View v) {

        String circleCode = circleInput.getEditText().getText().toString();

        if (circleCode.length() != 6) {
            circleInput.setErrorEnabled(true);
            circleInput.setError("Error: A code has 6 digits.");
            circleInput.requestFocus();
            return;
        } else {
            circleInput.setErrorEnabled(false);
        }

        databaseReference.child("Circles").child(circleCode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.d(FIREBASE_TAG, "Circle doesn't exist.");
                    Snackbar.make(v, "Circle doesn't exist.", Snackbar.LENGTH_LONG).show();
                    return;
                }
                Circle newCircle = new Circle("Member");
                databaseReference.child("Circles")
                        .child(circleCode)
                        .child("Members")
                        .child(USER_UID)
                        .setValue(newCircle).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(FIREBASE_TAG, "Successfully joined the circle.");
                            Snackbar.make(v, "Successfully joined the circle.", Snackbar.LENGTH_LONG).show();
                            databaseReference.child("Users")
                                    .child(USER_UID)
                                    .child("currentCircleSession")
                                    .setValue(Double.parseDouble(circleCode));
                            goToMainActivity();
                        } else {
                            Snackbar.make(v, "Failed to join the circle.", Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(FIREBASE_TAG, "Failed to get circle information.");
            }
        });

    }
}