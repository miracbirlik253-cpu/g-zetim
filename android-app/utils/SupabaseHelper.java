package com.gozetim.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.gozetim.app.network.SupabaseService;
import com.gozetim.app.network.models.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseHelper {

    private static final String TAG = "SupabaseHelper";
    private static final String PREFS_NAME = "GozetimPrefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    // CONFIGURATION
    private static final String SUPABASE_URL = "https://hsqttindsdvoappumvlx.supabase.co/";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhzcXR0aW5kc2R2b2FwcHVtdmx4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjczNjM2NTQsImV4cCI6MjA4MjkzOTY1NH0.gWXRQa_65LJRae2c1yBAnlixPyYwQl-af53_myzEimo";

    private Context context;
    private SupabaseService service;
    private SharedPreferences prefs;
    private String deviceId;

    public SupabaseHelper(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.deviceId = getDeviceId();

        // Initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.service = retrofit.create(SupabaseService.class);
    }

    // AUTHENTICATION
    public void login(String email, String password, final AuthCallback callback) {
        Call<AuthResponse> call = service.login(SUPABASE_KEY, new AuthRequest(email, password));

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().accessToken;
                    saveToken(token);
                    callback.onSuccess(response.body().user);

                    // Cihazı kaydet
                    registerDevice(response.body().user.id);
                } else {
                    callback.onError("Giriş başarısız: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onError("Ağ hatası: " + t.getMessage());
            }
        });
    }

    public void logout() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    private void saveToken(String token) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply();
    }

    private String getToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    private String getAuthHeader() {
        return "Bearer " + getToken();
    }

    // DATA OPERATIONS

    public void registerDevice(String userId) {
        if (!isLoggedIn())
            return;

        Device device = new Device();
        device.deviceId = deviceId;
        device.userId = userId;
        device.name = Build.MODEL;
        device.model = Build.MODEL;
        device.manufacturer = Build.MANUFACTURER;
        device.androidVersion = Build.VERSION.RELEASE;
        device.isOnline = true;

        Call<Void> call = service.registerDevice(getAuthHeader(), SUPABASE_KEY, device);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Cihaz kaydedildi");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Cihaz kayıt hatası", t);
            }
        });
    }

    public void updateDeviceInfo(int batteryLevel) {
        if (!isLoggedIn())
            return;

        Device device = new Device();
        device.batteryLevel = batteryLevel;
        device.isOnline = true;

        Call<Void> call = service.updateDevice("Bearer " + getToken(), SUPABASE_KEY, "eq." + deviceId, device);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // Silent update
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Cihaz güncelleme hatası", t);
            }
        });
    }

    public void updateAppUsage(Map<String, Map<String, Object>> usageData) {
        if (!isLoggedIn())
            return;

        List<InstalledApp> appsList = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : usageData.entrySet()) {
            Map<String, Object> data = entry.getValue();

            InstalledApp app = new InstalledApp();
            app.deviceId = deviceId;
            app.packageName = entry.getKey();
            app.appName = (String) data.get("name");
            app.usageTodayMinutes = ((Long) data.get("usageToday")).intValue();
            app.usageWeekMinutes = ((Long) data.get("usageWeek")).intValue();

            // Mevcut status ve limit'i kaybetmemek için sunucudan okuma yapılabilir
            // ancak upsert işleminde sadece değişenleri güncellemek daha zordur.
            // Supabase upsert varsayılan olarak tüm satırı ezer.
            // Bu basit implementasyonda sadece usage gönderiyoruz, status null giderse
            // sunucu tarafında varsayılan değerler kullanılabilir veya stored procedure
            // gerekebilir.
            // Şimdilik status göndermiyoruz (null), Supabase tarafında ignore edilebilir.

            appsList.add(app);
        }

        if (appsList.isEmpty())
            return;

        Call<Void> call = service.upsertApps(getAuthHeader(), SUPABASE_KEY, appsList);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "App usage updated");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "App usage update error", t);
            }
        });
    }

    public void updateLocation(double lat, double lng, String address, float accuracy) {
        if (!isLoggedIn())
            return;

        LocationHistory loc = new LocationHistory();
        loc.deviceId = deviceId;
        loc.latitude = lat;
        loc.longitude = lng;
        loc.address = address;
        loc.accuracy = accuracy;

        Call<Void> call = service.addLocation(getAuthHeader(), SUPABASE_KEY, loc);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // Silent
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Location update error", t);
            }
        });
    }

    // Polling for blocked apps (Realtime replacement)
    public void checkBlockedApps(final OnBlockedAppsListener listener) {
        if (!isLoggedIn())
            return;

        Call<List<InstalledApp>> call = service.getApps(getAuthHeader(), SUPABASE_KEY, "eq." + deviceId,
                "package_name,status,daily_limit_minutes");

        call.enqueue(new Callback<List<InstalledApp>>() {
            @Override
            public void onResponse(Call<List<InstalledApp>> call, Response<List<InstalledApp>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listener.onBlockedAppsUpdated(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<InstalledApp>> call, Throwable t) {
                // Error handling
            }
        });
    }

    private String getDeviceId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public interface AuthCallback {
        void onSuccess(AuthResponse.User user);

        void onError(String message);
    }

    public interface OnBlockedAppsListener {
        void onBlockedAppsUpdated(List<InstalledApp> apps);
    }
}
