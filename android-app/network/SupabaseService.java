package com.gozetim.app.network;

import com.gozetim.app.network.models.*;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface SupabaseService {

    // Auth
    @POST("auth/v1/token?grant_type=password")
    Call<AuthResponse> login(
            @Header("apikey") String apiKey,
            @Body AuthRequest request);

    // Devices - Upsert (Insert or Update)
    @POST("rest/v1/devices")
    @Headers("Prefer: resolution=merge-duplicates")
    Call<Void> registerDevice(
            @Header("Authorization") String token,
            @Header("apikey") String apiKey,
            @Body Device device);

    // Update Device Info (PATCH)
    @PATCH("rest/v1/devices")
    Call<Void> updateDevice(
            @Header("Authorization") String token,
            @Header("apikey") String apiKey,
            @Query("device_id") String deviceId, // Filter by device_id
            @Body Device deviceUpdates);

    // Installed Apps - Upsert
    @POST("rest/v1/installed_apps")
    @Headers("Prefer: resolution=merge-duplicates")
    Call<Void> upsertApps(
            @Header("Authorization") String token,
            @Header("apikey") String apiKey,
            @Body List<InstalledApp> apps);

    // Get Installed Apps (for checking blocked status)
    @GET("rest/v1/installed_apps")
    Call<List<InstalledApp>> getApps(
            @Header("Authorization") String token,
            @Header("apikey") String apiKey,
            @Query("device_id") String deviceId,
            @Query("select") String select // e.g. "package_name,status,daily_limit_minutes"
    );

    // Location History - Insert
    @POST("rest/v1/location_history")
    Call<Void> addLocation(
            @Header("Authorization") String token,
            @Header("apikey") String apiKey,
            @Body LocationHistory location);

    // Alerts - Insert
    @POST("rest/v1/alerts")
    Call<Void> addAlert(
            @Header("Authorization") String token,
            @Header("apikey") String apiKey,
            @Body Object alert // Alert modeline gerek yok, map veya simple object
    );
}
