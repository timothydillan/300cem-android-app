package com.timothydillan.circles.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.timothydillan.circles.MainActivity;
import com.timothydillan.circles.R;
import com.timothydillan.circles.Utils.UserUtil;

import java.util.Random;

public class NotificationService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "notificationService";
    private static final String TAG = "NotificationService";
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = ContextCompat.getSystemService(this, NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "circles", NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, remoteMessage.getNotification().getTitle());
            Log.d(TAG, remoteMessage.getNotification().getBody());
        }
        Intent intent = new Intent(this, MainActivity.class);
        int randomNotificationId = new Random().nextInt();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        Uri notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(remoteMessage.getData().get("title"))
                .setContentText(remoteMessage.getData().get("message"))
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .setSound(notificationUri)
                .build();
        notificationManager.notify(randomNotificationId, notification);
        Log.d(TAG, "Received a message");
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        UserUtil userUtil = new UserUtil();
        userUtil.updateDbUserToken(s);
    }
}
