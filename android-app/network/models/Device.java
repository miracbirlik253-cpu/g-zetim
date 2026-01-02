package com.gozetim.app.network.models;

import com.google.gson.annotations.SerializedName;

public class Device {
    @SerializedName("device_id")
    public String deviceId;

    @SerializedName("user_id")
    public String userId;

    public String name;
    public String model;
    public String manufacturer;

    @SerializedName("android_version")
    public String androidVersion;

    @SerializedName("battery_level")
    public int batteryLevel;

    @SerializedName("is_online")
    public boolean isOnline;

    @SerializedName("last_seen")
    public String lastSeen;
}
