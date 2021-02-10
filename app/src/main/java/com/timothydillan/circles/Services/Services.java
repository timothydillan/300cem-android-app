package com.timothydillan.circles.Services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

// A superclass that overrides some methods that are repeated in services.
public abstract class Services extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopSelf();
    }

    @Override
    public void onCreate() {
        initializeNotificationChannel();
    }

    protected abstract void initializeNotificationChannel();
}
