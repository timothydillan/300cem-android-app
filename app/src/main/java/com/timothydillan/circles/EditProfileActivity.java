package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.Utils.CircleUtil;
import com.timothydillan.circles.Utils.FirebaseUtil;
import com.timothydillan.circles.Utils.UserUtil;

import java.util.ArrayList;

public class EditProfileActivity extends AppCompatActivity {

    // Constants
    private static final String DATE_PICKER_TAG = "DATE_PICKER";
    private static final String FIREBASE_TAG = "firebaseRelated";
    private static final int GALLERY_REQUEST_CODE = 102;
    private static final DatabaseReference databaseReference = FirebaseUtil.getDbReference();

    // Views
    private ImageView profileImageView;
    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private AutoCompleteTextView genderDropdown;
    private TextView birthdayTextView;
    private TextView emailTextView;
    private TextView phoneTextView;
    private LinearLayout genderLayout, birthdayLayout;
    private Chip addInfoChip, saveChip;
    private final MaterialDatePicker.Builder<Long> dateBuilder = MaterialDatePicker.Builder.datePicker();
    private MaterialDatePicker<Long> datePicker;
    private Uri imageUri = null;

    // Misc
    private CircleUtil circleUtil = new CircleUtil();
    private UserUtil userUtil = new UserUtil();
    private User currentMember = UserUtil.getCurrentUser();
    private String firstName;
    private String lastName;
    private String gender;
    private String birthDate;
    private int TEN_DP;

    // Storage
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        storageReference = FirebaseStorage.getInstance().getReference();

        float dpRatio = getResources().getDisplayMetrics().density;
        TEN_DP = (int) (10 * dpRatio);

        // TODO: use sharedpreferences instead.
        // TODO: profile pic

        circleUtil.addEventListener(new CircleUtil.CircleUtilListener() {
            @Override
            public void onCircleReady(ArrayList<User> members) { }
            @Override
            public void onCircleChange() { }
        });

        userUtil.addEventListener(new UserUtil.UsersListener() {
            @Override
            public void onUserReady() { }

            @Override
            public void onUsersChange(@NonNull DataSnapshot snapshot) {
                updateUserProfile(snapshot);
            }
        });


        Toolbar toolbar = findViewById(R.id.editDetailsToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dateBuilder.setTitleText("SELECT YOUR BIRTH DATE");
        datePicker = dateBuilder.build();

        profileImageView = findViewById(R.id.profileImageView);
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        genderDropdown = findViewById(R.id.genderDropdown);
        birthdayTextView = findViewById(R.id.birthdayTextView);
        emailTextView = findViewById(R.id.emailTextView);
        phoneTextView = findViewById(R.id.phoneTextView);
        genderLayout = findViewById(R.id.genderLayout);
        birthdayLayout = findViewById(R.id.birthdayLayout);
        addInfoChip = findViewById(R.id.addInfoChip);
        saveChip = findViewById(R.id.saveInfoChip);

        final String[] genders = getResources().getStringArray(R.array.gender_options);
        final ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                genders);

        genderDropdown.setAdapter(genderAdapter);

        updateProfileViews();
        initializeChangeListeners();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
            changeChipColor();
        }
    }

    private void updateProfileViews() {
        firstNameEditText.setText(currentMember.getFirstName());
        lastNameEditText.setText(currentMember.getLastName());
        emailTextView.setText(currentMember.getEmail());
        phoneTextView.setText(currentMember.getPhone());
        birthdayTextView.setText(currentMember.getBirthDate());
        genderDropdown.setText(currentMember.getGender());

        birthdayLayout.setVisibility(currentMember.getBirthDate().isEmpty() ? View.GONE : View.VISIBLE);
        genderLayout.setVisibility(currentMember.getGender().isEmpty() ? View.GONE : View.VISIBLE);
        addInfoChip.setVisibility(currentMember.getBirthDate().isEmpty()
                || currentMember.getGender().isEmpty() ? View.VISIBLE : View.GONE);
        addInfoChip.setVisibility(currentMember.getBirthDate().isEmpty()
                || currentMember.getGender().isEmpty() ? View.VISIBLE : View.GONE);

        if (!currentMember.getProfilePicUrl().isEmpty() && imageUri == null) {
            Glide.with(this).load(currentMember.getProfilePicUrl()).into(profileImageView);
        }

        ((ViewGroup.MarginLayoutParams) saveChip.getLayoutParams()).setMarginStart(
                addInfoChip.getVisibility() == View.VISIBLE ? TEN_DP : 0);
    }

    public void onSaveButtonClick(View v) {
        if (!didAnyFieldChange())
            return;
        final User oldUser = currentMember;
        final Uri oldImage = Uri.parse(currentMember.getProfilePicUrl());
        Log.d(FIREBASE_TAG, String.valueOf(oldUser));
        databaseReference.child("Users").child(currentMember.getUid()).child("firstName").setValue(firstName);
        databaseReference.child("Users").child(currentMember.getUid()).child("lastName").setValue(lastName);
        databaseReference.child("Users").child(currentMember.getUid()).child("gender").setValue(gender);
        databaseReference.child("Users").child(currentMember.getUid()).child("birthDate").setValue(birthDate);
        if (imageUri != null) {
            uploadProfilePicture(imageUri);
            imageUri = null;
        }
        Snackbar successSnackbar = Snackbar.make(findViewById(android.R.id.content),
                "Successfully updated your profile.", Snackbar.LENGTH_LONG);
        successSnackbar.setAction("Revert", v1 -> {
            databaseReference.child("Users").child(currentMember.getUid()).setValue(oldUser);
            if (oldImage != null)
                uploadProfilePicture(oldImage);
        });
        successSnackbar.show();
    }

    private void updateUserProfile(DataSnapshot snapshot) {
        User user = snapshot.child(currentMember.getUid()).getValue(User.class);
        UserUtil.updateCurrentUser(user);
        updateProfileViews();
    }

    private void initializeChangeListeners() {
        firstNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                changeChipColor();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        lastNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                changeChipColor();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        genderDropdown.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                changeChipColor();
            }
        });

        birthdayTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                changeChipColor();
            }
        });

    }

    private boolean didAnyFieldChange() {
        firstName = firstNameEditText.getText().toString();
        lastName = lastNameEditText.getText().toString();
        gender = genderDropdown.getText().toString();
        birthDate = birthdayTextView.getText().toString();
        return !currentMember.getFirstName().contentEquals(firstName) && !firstName.equals("") ||
                !currentMember.getLastName().contentEquals(lastName) && !lastName.equals("") ||
                !currentMember.getGender().contentEquals(gender) && !gender.equals("") ||
                !currentMember.getBirthDate().contentEquals(birthDate) && !birthDate.equals("")
                || imageUri != null;
    }

    public void onBirthdayEditClick(View v) {
        datePicker.show(getSupportFragmentManager(), DATE_PICKER_TAG);
        datePicker.addOnPositiveButtonClickListener(selection ->
                birthdayTextView.setText(datePicker.getHeaderText()));
    }

    private void changeChipColor() {
        saveChip.setChipBackgroundColor(didAnyFieldChange() ? AppCompatResources.getColorStateList(getApplicationContext(), R.color.chip_bg_color)
                : AppCompatResources.getColorStateList(getApplicationContext(), R.color.chip_disabled_color));
        saveChip.setTextColor(didAnyFieldChange() ? getResources().getColor(R.color.chip_text_color)
                : getResources().getColor(R.color.chip_disabled_text_color));
    }

    public void onEditImageClick(View v) {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
    }

    private void uploadProfilePicture(Uri imageUri) {
        StorageReference userStorageReference = storageReference.child("images/profileImages/" + currentMember.getUid());
        userStorageReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> setProfilePicture(userStorageReference))
                .addOnFailureListener(exception -> Log.d(FIREBASE_TAG, "Failed to upload image."));
    }

    private void setProfilePicture(StorageReference reference) {
        reference.getDownloadUrl().addOnSuccessListener(uri -> {
            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(uri)
                    .build();
            FirebaseUtil.getCurrentUser().updateProfile(request).addOnSuccessListener(aVoid -> {
                Log.d(FIREBASE_TAG, "Successfully updated profile image.");
                String profilePicUrl = String.valueOf(FirebaseUtil.getCurrentUser().getPhotoUrl());
                databaseReference.child("Users").child(currentMember.getUid())
                        .child("profilePicUrl").setValue(profilePicUrl);
            });
        });
    }

    public void toast(View v) {
        Toast.makeText(this, "TOAST!", Toast.LENGTH_SHORT).show();
    }
}