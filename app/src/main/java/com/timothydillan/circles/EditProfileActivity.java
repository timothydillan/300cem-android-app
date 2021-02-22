package com.timothydillan.circles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.timothydillan.circles.Models.User;
import com.timothydillan.circles.UI.VolleyImageRequest;
import com.timothydillan.circles.Utils.SharedPreferencesUtil;
import com.timothydillan.circles.Utils.UserUtil;

import java.util.concurrent.atomic.AtomicReference;

public class EditProfileActivity extends AppCompatActivity implements UserUtil.UsersListener {

    private SharedPreferencesUtil sharedPreferences;

    // Constants
    private static final String DATE_PICKER_TAG = "DATE_PICKER";
    private static final int GALLERY_REQUEST_CODE = 102;
    private static ImageLoader imageLoader;
    private static ArrayAdapter<String> typeAdapter;

    // Views
    private ImageView profileImageView;
    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private AutoCompleteTextView typeDropdown;
    private TextView birthdayTextView;
    private LinearLayout birthdayLayout;
    private Chip addInfoChip, saveChip;
    private final MaterialDatePicker.Builder<Long> dateBuilder = MaterialDatePicker.Builder.datePicker();
    private MaterialDatePicker<Long> datePicker;
    private Uri imageUri = null;

    // Misc
    private UserUtil userUtil = UserUtil.getInstance();
    private User currentMember = userUtil.getCurrentUser();
    private String firstName;
    private String lastName;
    private String type;
    private String birthDate;
    private int TEN_DP;
    private int SIXTEEN_DP;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        sharedPreferences = new SharedPreferencesUtil(this);

        TEN_DP = (int) (10 * getResources().getDisplayMetrics().density);
        SIXTEEN_DP = (int) (16 * getResources().getDisplayMetrics().density);

        imageLoader = VolleyImageRequest.getInstance(this).getImageLoader();

        Toolbar toolbar = findViewById(R.id.editDetailsToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dateBuilder.setTitleText("SELECT YOUR BIRTH DATE");
        datePicker = dateBuilder.build();
        
        typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.type_options));

        profileImageView = findViewById(R.id.profileImageView);
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        typeDropdown = findViewById(R.id.typeDropdown);
        birthdayTextView = findViewById(R.id.birthdayTextView);
        birthdayLayout = findViewById(R.id.birthdayLayout);
        addInfoChip = findViewById(R.id.addInfoChip);
        saveChip = findViewById(R.id.saveInfoChip);
        typeDropdown.setAdapter(typeAdapter);

        // Since the autocompletetextview hides other options when a text is already entered,
        typeDropdown.setOnTouchListener((v, event) -> {
            // we're overriding the method and showing the drop down explicitly when the user touches the drop down.
            typeDropdown.showDropDown();
            return false;
        });
        // We're also overriding the onFocus method and showing the drop down explicitly when the view focuses on the drop down.
        typeDropdown.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                typeDropdown.showDropDown();
            }
        });

        typeDropdown.setOnClickListener(v -> typeDropdown.showDropDown());

        // We'll register a listener to detect changes in the user's information.
        userUtil.registerListener(this);
        updateProfileViews();
        initializeChangeListeners();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // If the user has selected an image, first check whether the request code for the started intent is the same
        // as the request code we use for requesting images. If it's the same, we'll check whether
        // the resultcode returns OK and if the data isn't null.
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // if the checks above pass, we'll set the imageUri to the data,
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
            changeSaveChipColor(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener when the user leaves the activity.
        userUtil.unregisterListener(this);
        // Make sure that the foreground state is set to true, so that the authentication process
        // runs only when the user completely leaves the app and goes back.
        sharedPreferences.writeBoolean(SharedPreferencesUtil.FOREGROUND_KEY, true);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }


    @Override
    public void onUsersChange(@NonNull DataSnapshot snapshot) {
        // If there are changes in the user node,
        // get the current user value from the database
        User user = snapshot.child(currentMember.getUid()).getValue(User.class);

        // If the user did not change,
        if (!UserUtil.didUserProfileChange(user)) {
            // return and do nothing.
            return;
        }

        // If the user did change, update the user stored in the device,
        UserUtil.updateCurrentUser(user);

        // change the save chip color
        changeSaveChipColor(false);

        // and update the views.
        updateProfileViews();
    }

    private void updateProfileViews() {

        // Update text views to the current user's profile.
        firstNameEditText.setText(currentMember.getFirstName());
        lastNameEditText.setText(currentMember.getLastName());
        birthdayTextView.setText(currentMember.getBirthDate());
        typeDropdown.setText(currentMember.getType());

        // If the birthday and type is empty,
        if (currentMember.getBirthDate().isEmpty()) {
            birthdayLayout.setVisibility(View.GONE);
            // set the add info chip to visible.
            showAddInfoChip();
        } else {
            birthdayLayout.setVisibility(View.VISIBLE);
            // if it's not empty, remove the add info chip.
            hideAddInfoChip();
        }

        String profilePicUrl = currentMember.getProfilePicUrl();
        // If the profile picture url isn't empty, and if the user hasn't selected any images,
        if (!profilePicUrl.isEmpty() && imageUri == null) {
            // load the image using volley
            imageLoader.get(profilePicUrl, new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    // and when we get a response, if the bitmap isn't null,
                    if (response.getBitmap() != null) {
                        // set the profile image view to the bitmap received
                        profileImageView.setImageBitmap(response.getBitmap());
                    }
                }
                @Override
                public void onErrorResponse(VolleyError error) { }
            });
        }

    }

    public void onSaveButtonClick(View v) {
        // if the save button was clicked, check if there were any field changes.
        if (!didAnyFieldChange()) {
            // if there wasn't any changes, then just return and do nothing.
            return;
        }

        changeSaveChipColor(true);

        // If a field changed, store the old user details (in case the user wants to revert the changes)
        final User oldUser = new User();
        UserUtil.updateCurrentUser(oldUser, currentMember);
        final Uri oldImage = Uri.parse(oldUser.getProfilePicUrl());

        // and save the new changes to the database
        saveNewUserInformation();

        // if the user selected a new image for their profile picture
        if (imageUri != null) {
            // set the user's profile picture to the new profile picture
            userUtil.setNewProfilePicture(imageUri);
            // and reset the selection.
            imageUri = null;
        }

        // Once done, a snackbar message is shown.
        Snackbar successSnackbar = Snackbar.make(findViewById(android.R.id.content),
                "Successfully updated your profile.", Snackbar.LENGTH_LONG);

        successSnackbar.setAction("Revert", v1 -> {
            // In case the user wants to revert their details,
            // update the current user to the old user's details,
            userUtil.updateDbUser(oldUser);
            // and if the oldImage isn't null,
            if (oldImage != null) {
                // set the profile picture to the previous image.
                userUtil.setNewProfilePicture(oldImage);
            }
        });

        successSnackbar.show();
    }

    private void saveNewUserInformation() {
        /* This function updates the first and last name of the user, as well as the
         * birth date and type of the user. */
        userUtil.updateDbUserFirstName(firstName);
        userUtil.updateDbUserLastName(lastName);
        userUtil.updateDbUserBirthDate(birthDate);
        userUtil.updateDbUserType(type);
    }

    private void initializeChangeListeners() {
        /* For every edit text, we'll set up a listener to check whether the user changed anything
         * if the user did change something, we'll call the changeChipColor function. */

        firstNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                changeSaveChipColor(false);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        lastNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                changeSaveChipColor(false);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        typeDropdown.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                changeSaveChipColor(false);
            }
        });

        birthdayTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                changeSaveChipColor(false);
            }
        });

    }

    private boolean didAnyFieldChange() {
        // Get each text from each inputs
        firstName = firstNameEditText.getText().toString();
        lastName = lastNameEditText.getText().toString();
        type = typeDropdown.getText().toString();
        birthDate = birthdayTextView.getText().toString();
        // and if any inputs doesn't match the current user profile, return true.
        // if nothing changed, then this function will return false.
        return !currentMember.getFirstName().equals(firstName) && !firstName.equals("") ||
                !currentMember.getLastName().equals(lastName) && !lastName.equals("") ||
                !currentMember.getType().equals(type) && !type.equals("") ||
                !currentMember.getBirthDate().equals(birthDate) && !birthDate.equals("")
                || imageUri != null;
    }

    public void onBirthdayEditClick(View v) {
        datePicker.show(getSupportFragmentManager(), DATE_PICKER_TAG);
        datePicker.addOnPositiveButtonClickListener(selection ->
                birthdayTextView.setText(datePicker.getHeaderText()));
    }

    private void changeSaveChipColor(boolean forceDisable) {
        // This function changes the color of the save chip to indicate whether the save chip can be used.
        // If any field changed,
        if (didAnyFieldChange() && !forceDisable) {
            // we'll set the background and text color to blue and white to show that the save chip can be used.
            saveChip.setChipBackgroundColor(AppCompatResources.getColorStateList(getApplicationContext(), R.color.chip_bg_color));
            saveChip.setTextColor(getResources().getColor(R.color.chip_text_color));
            // else
        } else {
            // we'll set the background and text color to grey to show that the save chip can't be used.
            saveChip.setChipBackgroundColor(AppCompatResources.getColorStateList(getApplicationContext(), R.color.chip_disabled_color));
            saveChip.setTextColor(getResources().getColor(R.color.chip_disabled_text_color));
        }
    }

    public void onEditImageClick(View v) {
        // If the user clicked on the edit image button,
        // An intent with the ACTION_GET_CONTENT action is created to allow users to select an image.
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        // The intent type would be image/* to indicate that the data type we want is an image.
        galleryIntent.setType("image/*");
        // We then start the intent.
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void addMoreInfo(View v) {
        /* If the user doesn't have birth date data and wants to add that data,
         * we'll set a view with the additional info layout as its layout for the alert dialog where
         * the user would input their birth date. */
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.additional_info_layout, null, false);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Add additional info");
        // We'll inflate the alert dialog view with the view we just set up.
        builder.setView(viewInflated);

        MaterialButton birthdayButton = viewInflated.findViewById(R.id.birthdayButton);

        /* Check whether the user already has already entered a birthdate.
         * If the user hasn't entered a birthdate, then we'll set the visibility of the birthdate input true,
         * and vice versa if the user has already entered a birthdate.*/
        birthdayButton.setVisibility(birthdayTextView.getText().toString().length() < 2 ? View.VISIBLE : View.GONE);

        AtomicReference<String> date = new AtomicReference<>("");
        birthdayButton.setOnClickListener(view -> {
            datePicker.show(getSupportFragmentManager(), DATE_PICKER_TAG);
            datePicker.addOnPositiveButtonClickListener(selection -> {
                // If the user clicked OK on the date dialog, set the date accordingly.
                date.set(datePicker.getHeaderText());
                birthdayButton.setText(datePicker.getHeaderText() + " - Edit?");
            });
            // Else, reset the date.
            datePicker.addOnNegativeButtonClickListener(view1 -> date.set(""));
        });

        // Set up the buttons
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            // If the user clicked OK on the alert dialog,
            // Check whether the date input is empty.
            if (!date.get().isEmpty()) {
                // If it isn't empty, set the birthday text view to the entered date,
                birthdayTextView.setText(date.get());
                // and make the birthday layout visible.
                birthdayLayout.setVisibility(View.VISIBLE);
                hideAddInfoChip();
            }
            // Then dismiss the dialog.
            dialog.dismiss();
        });

        // Cancel the dialog if the user clicked cancel.
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void hideAddInfoChip() {
        // if it's not empty, remove the add info chip.
        addInfoChip.setVisibility(View.GONE);
        // and set the save chip's margin start to 0 dp.
        ((ViewGroup.MarginLayoutParams) saveChip.getLayoutParams()).setMargins(0, SIXTEEN_DP, 0, SIXTEEN_DP);
    }

    private void showAddInfoChip() {
        // if it's not empty, remove the add info chip.
        addInfoChip.setVisibility(View.VISIBLE);
        // and set the save chip's margin start to 0 dp.
        ((ViewGroup.MarginLayoutParams) saveChip.getLayoutParams()).setMargins(TEN_DP, SIXTEEN_DP, 0, SIXTEEN_DP);
    }
}