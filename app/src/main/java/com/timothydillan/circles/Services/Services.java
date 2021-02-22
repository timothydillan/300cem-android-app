package com.timothydillan.circles.Services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

// An abstract superclass that overrides some methods that are frequently repeated in the services implemented.
public abstract class Services extends Service {

    protected static final String STOP_SERVICE = "STOP_SERVICE";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onCreate() {
        initializeNotificationChannel();
    }

    protected abstract void initializeNotificationChannel();
}
