package com.gozetim.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

import com.gozetim.app.MainActivity;
import com.gozetim.app.R;
import com.gozetim.app.utils.AppUsageHelper;
import com.gozetim.app.utils.SupabaseHelper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Arka planda sürekli çalışan izleme servisi
 * - Uygulama kullanımını izler
 * - Konum bilgisini gönderir
 * - Cihaz bilgilerini günceller
 * - Engellenen uygulamaları kontrol eder
 */
public class MonitoringService extends Service {

    private static final String CHANNEL_ID = "GozetimMonitoringChannel";
    private static final int NOTIFICATION_ID = 1;

    private ScheduledExecutorService scheduler;
    private SupabaseHelper supabaseHelper;
    private AppUsageHelper appUsageHelper;
    private LocationService locationService;
    private AppBlockerService appBlockerService;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize helpers
        supabaseHelper = new SupabaseHelper(this);
        appUsageHelper = new AppUsageHelper(this);
        locationService = new LocationService(this, supabaseHelper);
        appBlockerService = new AppBlockerService(this, supabaseHelper);

        // Create notification channel
        createNotificationChannel();

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification());

        // Start scheduled tasks
        startScheduledTasks();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Service will restart if killed
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't provide binding
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Gözetim İzleme Servisi",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Arka planda cihaz izleme servisi");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Gözetim Aktif")
                .setContentText("Cihaz izleniyor...")
                .setSmallIcon(R.drawable.ic_shield) // Kendi icon'unuzu ekleyin
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void startScheduledTasks() {
        scheduler = Executors.newScheduledThreadPool(4); // 4 threads now

        // Task 1: Update app usage every 5 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateAppUsage();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.MINUTES);

        // Task 2: Update location every 15 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                locationService.updateLocation();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 15, TimeUnit.MINUTES);

        // Task 3: Update device info every 10 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateDeviceInfo();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 10, TimeUnit.MINUTES);

        // Task 4: Check for blocked apps every 2 seconds
        // This is the blocking logic (fast loop)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                appBlockerService.checkAndBlockApps();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 2, TimeUnit.SECONDS);

        // Task 5: Refresh rules from Supabase every 1 minute
        // This is the usage limit update polling
        scheduler.scheduleAtFixedRate(() -> {
            try {
                appBlockerService.refreshRules();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    private void updateAppUsage() {
        // Get app usage data
        java.util.Map<String, java.util.Map<String, Object>> usageData = appUsageHelper.getAppUsageStats();

        // Send to Supabase
        supabaseHelper.updateAppUsage(usageData);
    }

    private void updateDeviceInfo() {
        // Get battery level
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
        int batteryPct = (int) (level * 100 / (float) scale);

        supabaseHelper.updateDeviceInfo(batteryPct);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Shutdown scheduler
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }

        // Restart service (anti-kill protection)
        Intent restartIntent = new Intent(getApplicationContext(), MonitoringService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent);
        } else {
            startService(restartIntent);
        }
    }
}
