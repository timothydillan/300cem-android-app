package com.timothydillan.circles.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtil {
    public static final String CRASH_KEY = "CRASH_KEY";
    public static final String BIOMETRICS_KEY = "BIOMETRICS_KEY";
    public static final String COVID_KEY = "COVID_KEY";
    public static final String ACTIVITY_APP_KEY = "ACTIVITY_APP_KEY";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;
    private Context ctx;

    public SharedPreferencesUtil(Context context) {
        ctx = context;
        sharedPreferences = ctx.getSharedPreferences("circles", Context.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();
    }

    public boolean isCovidDetectionEnabled() {
        return sharedPreferences.getBoolean(COVID_KEY, false);
    }

    public boolean isCrashDetectionEnabled() {
        return sharedPreferences.getBoolean(CRASH_KEY, false);
    }

    public boolean isBiometricsSecurityEnabled() {
        return sharedPreferences.getBoolean(BIOMETRICS_KEY, false);
    }

    public boolean wasAppInForeground() {
        return sharedPreferences.getBoolean(ACTIVITY_APP_KEY, false);
    }

    public boolean writeBoolean(String key, boolean bool) {
        sharedPreferencesEditor.putBoolean(key, bool);
        return sharedPreferencesEditor.commit();
    }

}
