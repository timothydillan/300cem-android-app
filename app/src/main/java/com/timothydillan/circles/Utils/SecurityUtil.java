package com.timothydillan.circles.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import com.google.android.material.textfield.TextInputLayout;
import com.timothydillan.circles.MainActivity;
import com.timothydillan.circles.R;

import java.util.concurrent.Executor;

// A facade class made to make security authentication easier.
public class SecurityUtil {
    private Context ctx;
    private SecurityListener securityListener;
    private SharedPreferencesUtil sharedPreferences;
    private byte attempts = 3;

    public SecurityUtil(Context context) {
        ctx = context;
        sharedPreferences = new SharedPreferencesUtil(ctx);
    }

    public void addListener(SecurityListener listener) {
        securityListener = listener;
    }

    public void authenticateUserBiometrics() {
        Executor mainExecutor = ContextCompat.getMainExecutor(ctx);

        BiometricPrompt biometricPrompt = new BiometricPrompt((FragmentActivity) ctx, mainExecutor,
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    // If the biometric authentication failed, we'll trigger the onAuthenticationFailed listener.
                    securityListener.onAuthenticationFailed();
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    // If the biometric authentication succeeded, we'll trigger the onAuthenticationSuccessful listener.
                    securityListener.onAuthenticationSuccessful();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                }
            });

        // We'll create a biometric prompt that allows Weak Biometric authenticators (problems in Samsung devices, the Samsung Face recognition is detected as a weak authenticator.)
        // https://stackoverflow.com/questions/60219450/android-10-biometric-manager-security
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setTitle("Verify your identity")
            .setDescription("Please verify your identity to access this app.")
            .setNegativeButtonText("Cancel")
            .setConfirmationRequired(true)
            .build();

        // and authenticate the user.
        biometricPrompt.authenticate(promptInfo);
    }

    public boolean isUserPasswordCorrect(String password) {
        return password.equals(sharedPreferences.getPassword());
    }

    public void authenticateUserPassword(Activity activity) {
        View passwordConfirmationView = LayoutInflater.from(ctx).inflate(R.layout.password_confirmation, null, false);
        TextInputLayout passwordLayout = passwordConfirmationView.findViewById(R.id.passwordInputLayout);

        AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(ctx, R.style.AlertDialogStyle))
                .setView(passwordConfirmationView)
                .setTitle("Enter your password")
                .setMessage("To access the app, you must enter the password you have set for yourself.")
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            // If the OK button was clicked,
            positiveButton.setOnClickListener(view -> {
                // We'll first get the password entered
                String password = passwordLayout.getEditText().getText().toString();
                // and check whether the password is correct.
                if (!isUserPasswordCorrect(password)) {
                    // if the password isn't correct, decrease the number of attempts allowed
                    if (--attempts > 0) {
                        // and show an error.
                        passwordLayout.setErrorEnabled(true);
                        passwordLayout.setError("Incorrect password.");
                        passwordLayout.requestFocus();
                        Toast.makeText(activity, "Incorrect password. " + attempts + " attempts remaining.", Toast.LENGTH_SHORT).show();
                    } else {
                        // If the user has no more attempts, call finish() / exit the app.
                        activity.finish();
                    }
                } else {
                    // If the authentication was successful, show a message and dismiss the authentication dialog.
                    Toast.makeText(activity, "Authentication successful. Welcome back.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
            // If the cancel button was clicked, exit the app.
            negativeButton.setOnClickListener(v1 -> activity.finish());
        });

        // Prevent users from cancelling the dialog by clicking the outside of the dialog.
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

    public interface SecurityListener {
        void onAuthenticationSuccessful();
        void onAuthenticationFailed();
    }

}
