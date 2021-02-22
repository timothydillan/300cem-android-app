package com.timothydillan.circles.UI;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

// Code taken from: https://developer.android.com/training/volley/requestqueue
public class VolleyImageRequest {
    private static VolleyImageRequest instance;
    private RequestQueue requestQueue;
    private ImageLoader imageLoader;
    private static Context ctx;

    private VolleyImageRequest(Context context) {
        ctx = context;
        requestQueue = getRequestQueue();
        imageLoader = new ImageLoader(requestQueue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap>
                            cache = new LruCache<>(20);
                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }
                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });
    }

    public static synchronized VolleyImageRequest getInstance(Context context) {
        if (instance == null) {
            instance = new VolleyImageRequest(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return requestQueue;
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }
}
