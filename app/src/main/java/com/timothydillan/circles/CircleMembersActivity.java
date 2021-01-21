package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CircleMembersActivity extends AppCompatActivity {

    private TextView inviteCodeText;
    private String circleCode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circle_members);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        inviteCodeText = findViewById(R.id.circleInviteCode);
        DatabaseReference dbReference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseAuth.getCurrentUser().getUid());
        dbReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (ds.getKey().equals("currentCircleSession"))
                        circleCode = String.valueOf(ds.getValue());
                    inviteCodeText.setText(circleCode);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    public void onShareButtonClick(View v) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Hey there! Join my circle on the circles. app! Here's my code: " + circleCode);
        sendIntent.putExtra(Intent.EXTRA_TITLE, "Send circle code");
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Send your circle code using: "));
    }

}