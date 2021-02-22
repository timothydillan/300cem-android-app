package com.timothydillan.circles.Utils;

import android.content.Context;
import android.content.SharedPreferences;

// A facade class made to use sharedpreferences easily.
public class SharedPreferencesUtil {
    public static final String CRASH_KEY = "CRASH_KEY";
    public static final String BIOMETRICS_KEY = "BIOMETRICS_KEY";
    public static final String FOREGROUND_KEY = "FOREGROUND_KEY";
    public static final String PASSWORD_KEY = "PASSWORD_KEY";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;
    private Context ctx;

    public SharedPreferencesUtil(Context context) {
        ctx = context;
        sharedPreferences = ctx.getSharedPreferences("circles", Context.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();
    }

    public boolean isCrashDetectionEnabled() {
        return sharedPreferences.getBoolean(CRASH_KEY, false);
    }

    public boolean isBiometricsSecurityEnabled() {
        return sharedPreferences.getBoolean(BIOMETRICS_KEY, false);
    }

    public boolean isPasswordSecurityEnabled() {
        return !sharedPreferences.getString(PASSWORD_KEY, "").isEmpty();
    }

    public String getPassword() {
        return sharedPreferences.getString(PASSWORD_KEY, "");
    }

    public boolean wasAppInForeground() {
        return sharedPreferences.getBoolean(FOREGROUND_KEY, false);
    }

    public boolean writeBoolean(String key, boolean value) {
        sharedPreferencesEditor.putBoolean(key, value);
        return sharedPreferencesEditor.commit();
    }

    public boolean writeString(String key, String value) {
        sharedPreferencesEditor.putString(key, value);
        return sharedPreferencesEditor.commit();
    }

    public boolean removeItem(String key) {
        sharedPreferencesEditor.remove(key);
        return sharedPreferencesEditor.commit();
    }

    public boolean removeAllItems() {
        sharedPreferencesEditor.remove(CRASH_KEY);
        sharedPreferencesEditor.remove(BIOMETRICS_KEY);
        sharedPreferencesEditor.remove(PASSWORD_KEY);
        sharedPreferencesEditor.remove(FOREGROUND_KEY);
        return sharedPreferencesEditor.commit();
    }

}
