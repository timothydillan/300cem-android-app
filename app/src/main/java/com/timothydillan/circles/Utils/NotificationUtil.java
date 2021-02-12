package com.timothydillan.circles.Utils;

import android.util.Log;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NotificationUtil {
    // https://stackoverflow.com/questions/42767249/android-post-request-with-json
    private static final String TAG = "NotificationUtil";

    public static void sendNotification(String title, String message, String token) {
        Thread thread = new Thread(() -> {
            try {
                URL url = new URL("https://fcm.googleapis.com/fcm/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization","key="+ FirebaseUtil.FCM_KEY);
                conn.setDoOutput(true);
                conn.setDoInput(true);

                JSONObject json = new JSONObject();
                JSONObject dataJson = new JSONObject();
                dataJson.put("title", title);
                dataJson.put("message", message);
                json.put("data", dataJson);
                json.put("to", token);

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(json.toString());

                os.flush();
                os.close();

                Log.i("Return Code: ", String.valueOf(conn.getResponseCode()));
                Log.i("Message: " , conn.getResponseMessage());
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

}
