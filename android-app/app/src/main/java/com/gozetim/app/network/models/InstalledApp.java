package com.gozetim.app.network.models;

import com.google.gson.annotations.SerializedName;

public class InstalledApp {
    public Long id;

    @SerializedName("device_id")
    public String deviceId;

    @SerializedName("package_name")
    public String packageName;

    @SerializedName("app_name")
    public String appName;

    @SerializedName("icon_char")
    public String iconChar;

    public String status; // allowed, limited, blocked

    @SerializedName("daily_limit_minutes")
    public Integer dailyLimitMinutes;

    @SerializedName("usage_today_minutes")
    public int usageTodayMinutes;

    @SerializedName("usage_week_minutes")
    public int usageWeekMinutes;
}
