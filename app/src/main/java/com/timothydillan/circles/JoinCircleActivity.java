package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.timothydillan.circles.Models.Circle;
import com.timothydillan.circles.Models.User;

public class JoinCircleActivity extends ActivityInterface {

    private DatabaseReference databaseReference;
    private TextInputLayout circleInput;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_circle);

        circleInput = findViewById(R.id.circleCodeInputLayout);
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference();
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
                    Toast.makeText(JoinCircleActivity.this, "Circle doesn't exist.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Circle newCircle = new Circle("Member");
                databaseReference.child("Circles").child(circleCode).child("Members").child(currentUser.getUid()).setValue(newCircle).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(JoinCircleActivity.this, "Successfully joined a circle.",
                                    Toast.LENGTH_SHORT).show();
                            databaseReference.child("Users").child(currentUser.getUid()).child("currentCircleSession").setValue(Double.parseDouble(circleCode));
                            goToMainActivity();
                        } else {
                            Toast.makeText(JoinCircleActivity.this, "Failed joining a circle.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("firebaseRelated", "failed.");
            }
        });

    }

    @Override
    void onTextClick(View v) {

    }
}