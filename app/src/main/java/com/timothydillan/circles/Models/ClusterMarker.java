package com.timothydillan.circles.Models;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterItem;

public class ClusterMarker implements ClusterItem {

    private LatLng position;
    private String title;
    private String snippet;
    private String iconPicture;

    public ClusterMarker(User user) {
        this.position = new LatLng(user.getLatitude(), user.getLongitude());
        this.title = user.getFirstName();
        this.snippet = user.getLastSharingTime();
        this.iconPicture = user.getProfilePicUrl();
    }

    public void updateInfo(User user) {
        this.position = new LatLng(user.getLatitude(), user.getLongitude());
        this.title = user.getFirstName();
        this.snippet = user.getLastSharingTime();
        this.iconPicture = user.getProfilePicUrl();
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    public void setPosition(LatLng memberPosition) {
        this.position = memberPosition;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getIconPicture() {
        return iconPicture;
    }

    public void setIconPicture(String iconPicture) {
        this.iconPicture = iconPicture;
    }

}
