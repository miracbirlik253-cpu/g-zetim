package com.gozetim.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.gozetim.app.services.MonitoringService;
import com.gozetim.app.utils.PermissionHelper;
import com.gozetim.app.utils.SupabaseHelper;
import com.gozetim.app.network.models.AuthResponse;

/**
 * Ana aktivite - Kullanıcı arayüzü ve izin yönetimi
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int USAGE_STATS_REQUEST_CODE = 101;
    private static final int OVERLAY_REQUEST_CODE = 102;

    private TextView statusText;
    private TextView permissionsText;
    private Button loginButton;
    private Button startMonitoringButton;
    private Button stopMonitoringButton;

    private SupabaseHelper supabaseHelper;
    private PermissionHelper permissionHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Helpers
        supabaseHelper = new SupabaseHelper(this);
        permissionHelper = new PermissionHelper(this);

        // Initialize UI
        initializeUI();

        // Check login status
        checkLoginStatus();

        // Check permissions
        updatePermissionStatus();
    }

    private void initializeUI() {
        statusText = findViewById(R.id.statusText);
        permissionsText = findViewById(R.id.permissionsText);
        loginButton = findViewById(R.id.loginButton);
        startMonitoringButton = findViewById(R.id.startMonitoringButton);
        stopMonitoringButton = findViewById(R.id.stopMonitoringButton);

        loginButton.setOnClickListener(v -> showLoginDialog());
        startMonitoringButton.setOnClickListener(v -> startMonitoring());
        stopMonitoringButton.setOnClickListener(v -> stopMonitoring());

        findViewById(R.id.requestPermissionsButton).setOnClickListener(v -> requestAllPermissions());
    }

    private void checkLoginStatus() {
        if (supabaseHelper.isLoggedIn()) {
            statusText.setText("Giriş yapıldı ✅");
            loginButton.setText("Çıkış Yap");
            loginButton.setOnClickListener(v -> logout());
            startMonitoringButton.setEnabled(true);
        } else {
            statusText.setText("Lütfen giriş yapın");
            loginButton.setText("Giriş Yap");
            loginButton.setOnClickListener(v -> showLoginDialog());
            startMonitoringButton.setEnabled(false);
        }
    }

    private void showLoginDialog() {
        // Demo için basit giriş (Gerçek uygulamada UI olmalı)
        String email = "demo@gozetim.com";
        String password = "demo123";

        Toast.makeText(this, "Giriş yapılıyor...", Toast.LENGTH_SHORT).show();

        supabaseHelper.login(email, password, new SupabaseHelper.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse.User user) {
                Toast.makeText(MainActivity.this, "Giriş başarılı!", Toast.LENGTH_SHORT).show();
                checkLoginStatus();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void logout() {
        supabaseHelper.logout();
        checkLoginStatus();
        stopMonitoring();
    }

    private void requestAllPermissions() {
        // 1. Location Permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    PERMISSION_REQUEST_CODE);
        }

        // 2. Usage Stats Permission
        if (!permissionHelper.hasUsageStatsPermission()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(intent, USAGE_STATS_REQUEST_CODE);
            Toast.makeText(this, "Lütfen 'Gözetim' uygulamasına izin verin", Toast.LENGTH_LONG).show();
        }

        // 3. Overlay Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_REQUEST_CODE);
            Toast.makeText(this, "Lütfen 'Diğer uygulamaların üzerinde göster' iznini verin",
                    Toast.LENGTH_LONG).show();
        }

        // 4. Battery Optimization
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void updatePermissionStatus() {
        StringBuilder status = new StringBuilder("İzin Durumu:\n\n");

        // Location
        boolean hasLocation = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        status.append("📍 Konum: ").append(hasLocation ? "✅ Verildi" : "❌ Verilmedi").append("\n");

        // Usage Stats
        boolean hasUsageStats = permissionHelper.hasUsageStatsPermission();
        status.append("📊 Kullanım İstatistikleri: ").append(hasUsageStats ? "✅ Verildi" : "❌ Verilmedi").append("\n");

        // Overlay
        boolean hasOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        status.append("🔝 Üstte Göster: ").append(hasOverlay ? "✅ Verildi" : "❌ Verilmedi").append("\n");

        permissionsText.setText(status.toString());

        // Enable start button only if all permissions granted and logged in
        boolean allPermissionsGranted = hasLocation && hasUsageStats && hasOverlay;
        startMonitoringButton.setEnabled(allPermissionsGranted && supabaseHelper.isLoggedIn());
    }

    private void startMonitoring() {
        if (!supabaseHelper.isLoggedIn()) {
            Toast.makeText(this, "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start monitoring service
        Intent serviceIntent = new Intent(this, MonitoringService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Toast.makeText(this, "İzleme başlatıldı", Toast.LENGTH_SHORT).show();
        statusText.setText("Durum: Aktif İzleniyor 🟢");
        startMonitoringButton.setEnabled(false);
        stopMonitoringButton.setEnabled(true);
    }

    private void stopMonitoring() {
        Intent serviceIntent = new Intent(this, MonitoringService.class);
        stopService(serviceIntent);

        Toast.makeText(this, "İzleme durduruldu", Toast.LENGTH_SHORT).show();
        statusText.setText("Durum: Durduruldu 🔴");
        startMonitoringButton.setEnabled(true);
        stopMonitoringButton.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            updatePermissionStatus();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == USAGE_STATS_REQUEST_CODE || requestCode == OVERLAY_REQUEST_CODE) {
            updatePermissionStatus();
        }
    }
}
