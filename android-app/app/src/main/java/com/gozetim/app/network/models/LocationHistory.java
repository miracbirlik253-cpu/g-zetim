package com.gozetim.app.network.models;

import com.google.gson.annotations.SerializedName;

public class LocationHistory {
    public Long id;

    @SerializedName("device_id")
    public String deviceId;

    public double latitude;
    public double longitude;
    public String address;
    public float accuracy;
    public String timestamp;
}
