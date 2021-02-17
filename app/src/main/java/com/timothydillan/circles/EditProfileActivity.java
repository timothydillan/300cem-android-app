package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.UI.VolleyImageRequest;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;
import com.timothydillan.circles.Utils.UserUtil;

import java.util.concurrent.atomic.AtomicReference;

public class EditProfileActivity extends AppCompatActivity {

    private SharedPreferencesUtil sharedPreferences;
    // Constants
    private static final String DATE_PICKER_TAG = "DATE_PICKER";
    private static final String FIREBASE_TAG = "firebaseRelated";
    private static final int GALLERY_REQUEST_CODE = 102;
    private static ImageLoader imageLoader;
    private static String[] genders;
    private static ArrayAdapter<String> genderAdapter;

    // Views
    //private ImageView profileImageView;
    private ImageView profileImageView;
    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private AutoCompleteTextView genderDropdown;
    private TextView birthdayTextView;
    private TextView emailTextView;
    private TextView phoneTextView;
    private LinearLayout genderLayout;
    private LinearLayout birthdayLayout;
    private Chip addInfoChip, saveChip;
    private final MaterialDatePicker.Builder<Long> dateBuilder = MaterialDatePicker.Builder.datePicker();
    private MaterialDatePicker<Long> datePicker;
    private Uri imageUri = null;

    // Misc
    private UserUtil userUtil = new UserUtil();
    private User currentMember = UserUtil.getCurrentUser();
    private String firstName;
    private String lastName;
    private String gender;
    private String birthDate;
    private int TEN_DP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        sharedPreferences = new SharedPreferencesUtil(this);

        float dpRatio = getResources().getDisplayMetrics().density;
        TEN_DP = (int) (10 * dpRatio);

        imageLoader = VolleyImageRequest.getInstance(this).getImageLoader();

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

        genders = getResources().getStringArray(R.array.gender_options);
        genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                genders);

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

    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.writeBoolean(SharedPreferencesUtil.ACTIVITY_APP_KEY, true);
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
        if (currentMember.getBirthDate().isEmpty()
                || currentMember.getGender().isEmpty()) {
            addInfoChip.setVisibility(View.VISIBLE);
        } else {
            addInfoChip.setVisibility(View.GONE);
        }

        String profilePicUrl = currentMember.getProfilePicUrl();
        if (!profilePicUrl.isEmpty() && imageUri == null) {
            imageLoader.get(profilePicUrl, new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    Bitmap imageBitmap = response.getBitmap() != null ? response.getBitmap() :
                            BitmapFactory.decodeResource(getResources(), R.drawable.logo);
                    profileImageView.setImageBitmap(imageBitmap);
                }
                @Override
                public void onErrorResponse(VolleyError error) { }
            });
        }

        if (addInfoChip.getVisibility() == View.VISIBLE) {
            ((ViewGroup.MarginLayoutParams) saveChip.getLayoutParams()).setMarginStart(TEN_DP);
        } else {
            ((ViewGroup.MarginLayoutParams) saveChip.getLayoutParams()).setMarginStart(0);
        }

    }

    public void onSaveButtonClick(View v) {
        if (!didAnyFieldChange())
            return;
        final User oldUser = currentMember;
        final Uri oldImage = Uri.parse(currentMember.getProfilePicUrl());
        Log.d(FIREBASE_TAG, String.valueOf(oldUser));
        saveNewUserInformation();
        if (imageUri != null) {
            userUtil.setNewProfilePicture(imageUri);
            imageUri = null;
        }
        Snackbar successSnackbar = Snackbar.make(findViewById(android.R.id.content),
                "Successfully updated your profile.", Snackbar.LENGTH_LONG);
        successSnackbar.setAction("Revert", v1 -> {
            userUtil.updateDbUser(oldUser);
            if (oldImage != null) {
                userUtil.setNewProfilePicture(oldImage);
            }
        });
        successSnackbar.show();
    }

    private void saveNewUserInformation() {
        userUtil.updateDbUserFirstName(firstName);
        userUtil.updateDbUserLastName(lastName);
        userUtil.updateDbUserBirthDate(birthDate);
        userUtil.updateDbUserGender(gender);
    }

    private void updateUserProfile(DataSnapshot snapshot) {
        User user = snapshot.child(currentMember.getUid()).getValue(User.class);
        if (UserUtil.didUserChange(user)) {
            changeChipColor();
            UserUtil.updateCurrentUser(user);
            updateProfileViews();
        }

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
        saveChip.setChipBackgroundColor(didAnyFieldChange() ?
                AppCompatResources.getColorStateList(getApplicationContext(), R.color.chip_bg_color)
                : AppCompatResources.getColorStateList(getApplicationContext(), R.color.chip_disabled_color));
        saveChip.setTextColor(didAnyFieldChange() ? getResources().getColor(R.color.chip_text_color)
                : getResources().getColor(R.color.chip_disabled_text_color));
    }

    public void onEditImageClick(View v) {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
    }

    public void toast(View v) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogStyle);
        builder.setTitle("Add additional info");
        // I'm using fragment here so I'm using getView() to provide ViewGroup
        // but you can provide here any other instance of ViewGroup from your Fragment / Activity
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.additional_info_layout, (ViewGroup) findViewById(android.R.id.content), false);
        // Set up the input

        TextInputLayout genderView = viewInflated.findViewById(R.id.genderLayout);
        MaterialButton birthdayButton = viewInflated.findViewById(R.id.birthdayButton);
        AutoCompleteTextView genderInput = viewInflated.findViewById(R.id.genderInput);

        genderView.setVisibility(genderDropdown.getText().toString().length() < 2 ? View.VISIBLE : View.GONE);
        birthdayButton.setVisibility(birthdayTextView.getText().toString().length() < 2 ? View.VISIBLE : View.GONE);

        genderInput.setOnFocusChangeListener((view, b) -> {
            if (b) { genderInput.showDropDown(); }
        });

        genderInput.setOnTouchListener((view, motionEvent) -> {
            genderInput.showDropDown();
            return false;
        });

        genderInput.setAdapter(genderAdapter);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        builder.setView(viewInflated);

        AtomicReference<String> date = new AtomicReference<>("");

        birthdayButton.setOnClickListener(view -> {
            datePicker.show(getSupportFragmentManager(), DATE_PICKER_TAG);
            datePicker.addOnPositiveButtonClickListener(selection -> {
                date.set(datePicker.getHeaderText());
                birthdayButton.setText(datePicker.getHeaderText() + " - Edit?");
            });
            datePicker.addOnNegativeButtonClickListener(view1 -> date.set(""));
        });

        // Set up the buttons
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            dialog.dismiss();
            String gender = genderInput.getText().toString();
            if (!gender.isEmpty()) {
                genderDropdown.setText(gender);
                genderLayout.setVisibility(View.VISIBLE);
            }

            if (!date.get().isEmpty()) {
                birthdayTextView.setText(date.get());
                birthdayLayout.setVisibility(View.VISIBLE);
            }
        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }
}