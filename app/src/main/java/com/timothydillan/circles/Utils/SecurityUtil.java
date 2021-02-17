package com.timothydillan.circles.Utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

public class SecurityUtil {
    private Context ctx;
    private SecurityListener securityListener;

    public SecurityUtil(Context context) {
        ctx = context;
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
                    securityListener.onAuthenticationFailed();
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    securityListener.onAuthenticationSuccessful();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                }
            });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setTitle("Verify your identity")
            .setDescription("Please verify your identity to access this app.")
            .setNegativeButtonText("Cancel")
            .setConfirmationRequired(true)
            .build();

        biometricPrompt.authenticate(promptInfo);
    }

    public interface SecurityListener {
        void onAuthenticationSuccessful();
        void onAuthenticationFailed();
    }

}
