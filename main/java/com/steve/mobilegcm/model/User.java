package com.steve.mobilegcm.model;

import com.google.android.gms.maps.model.MarkerOptions;

public class User {

    private String name;
    private MarkerOptions marker;

    public User(String name) {
        this.name=name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setMarker(MarkerOptions marker) {
        this.marker=marker;
    }

    public MarkerOptions getMarker() {
        return marker;
    }
}
