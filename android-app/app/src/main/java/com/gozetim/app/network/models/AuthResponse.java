package com.gozetim.app.network.models;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("access_token")
    public String accessToken;

    @SerializedName("refresh_token")
    public String refreshToken;

    public User user;

    public static class User {
        public String id;
        public String email;
    }
}
