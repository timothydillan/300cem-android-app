package com.timothydillan.circles.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.timothydillan.circles.Models.ClusterMarker;
import com.timothydillan.circles.R;
import com.timothydillan.circles.UI.VolleyImageRequest;


public class ClusterMarkerManager extends DefaultClusterRenderer<ClusterMarker> {
    private Context ctx;
    private static ImageLoader imageLoader;
    private final IconGenerator iconGenerator;
    private final ImageView avatarImageView;
    private final int markerWidth;
    private final int markerHeight;

    public ClusterMarkerManager(Context context, GoogleMap map, ClusterManager<ClusterMarker> clusterManager) {
        super(context, map, clusterManager);
        ctx = context;
        iconGenerator = new IconGenerator(context.getApplicationContext());
        avatarImageView = new ImageView(context.getApplicationContext());
        imageLoader = VolleyImageRequest.getInstance(context).getImageLoader();
        markerWidth = (int)context.getResources().getDimension(R.dimen.custom_marker);
        markerHeight = (int)context.getResources().getDimension(R.dimen.custom_marker);
        avatarImageView.setLayoutParams(new ViewGroup.LayoutParams(markerWidth, markerHeight));
        int padding = (int)context.getResources().getDimension(R.dimen.custom_marker_padding);
        avatarImageView.setPadding(padding, padding, padding, padding);
        iconGenerator.setContentView(avatarImageView);
    }

    @Override
    protected void onBeforeClusterItemRendered(@NonNull ClusterMarker item, @NonNull MarkerOptions markerOptions) {
        updateAvatar(item, markerOptions);
    }

    @Override
    protected void onClusterItemRendered(@NonNull ClusterMarker clusterItem, @NonNull Marker marker) {
        updateAvatar(clusterItem, marker);
    }

    void updateAvatar(@NonNull ClusterMarker item, MarkerOptions markerOptions) {
        String profilePicUrl = item.getIconPicture();
        if (profilePicUrl == null || profilePicUrl.isEmpty()) {
            avatarImageView.setImageResource(R.drawable.logo);
            Bitmap avatar = iconGenerator.makeIcon();
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(avatar)).title(item.getTitle()).snippet(item.getSnippet());
            return;
        }
        imageLoader.get(profilePicUrl, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                Bitmap imageBitmap = response.getBitmap() != null ? response.getBitmap() : BitmapFactory.decodeResource(ctx.getResources(), R.drawable.logo);
                avatarImageView.setImageBitmap(imageBitmap);
                Bitmap avatar = iconGenerator.makeIcon();
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(avatar)).title(item.getTitle()).snippet(item.getSnippet());
            }
            @Override
            public void onErrorResponse(VolleyError error) { }
        });
    }

    void updateAvatar(@NonNull ClusterMarker item, Marker marker) {
        String profilePicUrl = item.getIconPicture();
        if (profilePicUrl == null || profilePicUrl.isEmpty()) {
            avatarImageView.setImageResource(R.drawable.logo);
            Bitmap avatar = iconGenerator.makeIcon();
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(avatar));
            return;
        }
        imageLoader.get(profilePicUrl, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                Bitmap imageBitmap = response.getBitmap() != null ? response.getBitmap() : BitmapFactory.decodeResource(ctx.getResources(), R.drawable.logo);
                avatarImageView.setImageBitmap(imageBitmap);
                Bitmap avatar = iconGenerator.makeIcon();
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(avatar));
            }
            @Override
            public void onErrorResponse(VolleyError error) { }
        });
    }

    @Override
    protected boolean shouldRenderAsCluster(@NonNull Cluster<ClusterMarker> cluster) {
        return false;
    }
}
