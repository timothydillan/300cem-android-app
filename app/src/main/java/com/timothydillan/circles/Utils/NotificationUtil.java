package com.timothydillan.circles.Utils;

import android.util.Log;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

// A facade class that helps send notifications using the FCM legacy HTTP API.
public class NotificationUtil {
    private static final String TAG = "NotificationUtil";

    // https://stackoverflow.com/questions/42767249/android-post-request-with-json
    // https://firebase.google.com/docs/cloud-messaging/send-message#java_3
    public static void sendNotification(String title, String message, String token) {
        // Firstly, we'll create a new background thread so that it doesn't interrupt the main UI thread to send notificaitons
        Thread thread = new Thread(() -> {
            try {
                // then, we'll create a connection to the FCM server and create a post request with our key as the header
                URL url = new URL("https://fcm.googleapis.com/fcm/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization","key="+ FirebaseUtil.FCM_KEY);
                conn.setDoOutput(true);
                conn.setDoInput(true);

                // We'll then create two JSON objects, one being the "main" one and the other being the nested json object.
                JSONObject json = new JSONObject();
                JSONObject dataJson = new JSONObject();
                // the nested json will include title and the message of the notification
                dataJson.put("title", title);
                dataJson.put("message", message);
                // then we'll put the nested json into the main json, and set them to argument to the token of the user we're sending to.
                json.put("data", dataJson);
                json.put("to", token);

                // then we'll write the json to the post request
                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(json.toString());

                // and once we're done, we'll flush and close the output stream, and close the connection made.
                os.flush();
                os.close();
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

}
