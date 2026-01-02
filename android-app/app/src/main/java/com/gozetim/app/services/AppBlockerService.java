package com.gozetim.app.services;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.gozetim.app.R;
import com.gozetim.app.utils.AppUsageHelper;
import com.gozetim.app.utils.SupabaseHelper;
import com.gozetim.app.network.models.InstalledApp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Engellenen uygulamaları kontrol eden ve engelleyen servis
 */
public class AppBlockerService {

    private Context context;
    private SupabaseHelper supabaseHelper;
    private AppUsageHelper appUsageHelper;
    private WindowManager windowManager;
    private View blockOverlay;

    private Map<String, String> blockedApps = new HashMap<>();
    private Map<String, Integer> limitedApps = new HashMap<>();
    private Map<String, Long> appUsageToday = new HashMap<>();

    private String currentBlockedApp = null;

    public AppBlockerService(Context context, SupabaseHelper supabaseHelper) {
        this.context = context;
        this.supabaseHelper = supabaseHelper;
        this.appUsageHelper = new AppUsageHelper(context);
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * Kuralları sunucudan güncelle
     */
    public void refreshRules() {
        supabaseHelper.checkBlockedApps(new SupabaseHelper.OnBlockedAppsListener() {
            @Override
            public void onBlockedAppsUpdated(List<InstalledApp> apps) {
                blockedApps.clear();
                limitedApps.clear();

                for (InstalledApp app : apps) {
                    if ("blocked".equals(app.status)) {
                        blockedApps.put(app.packageName, app.appName);
                    } else if ("limited".equals(app.status)) {
                        limitedApps.put(app.packageName, app.dailyLimitMinutes != null ? app.dailyLimitMinutes : 0);
                    }
                }
            }
        });
    }

    /**
     * Ön plandaki uygulamayı kontrol et ve gerekirse engelle
     */
    public void checkAndBlockApps() {
        String foregroundApp = appUsageHelper.getForegroundApp();

        if (foregroundApp == null || foregroundApp.equals(context.getPackageName())) {
            // Kendi uygulamamız veya null ise overlay'i kaldır
            removeBlockOverlay();
            return;
        }

        // Engellenen uygulama mı?
        if (blockedApps.containsKey(foregroundApp)) {
            if (!foregroundApp.equals(currentBlockedApp)) {
                showBlockOverlay(foregroundApp, "Bu uygulama engellenmiştir");
                currentBlockedApp = foregroundApp;
                goToHomeScreen();
            }
            return;
        }

        // Sınırlı uygulama mı ve limiti aştı mı?
        if (limitedApps.containsKey(foregroundApp)) {
            int limit = limitedApps.get(foregroundApp); // Dakika cinsinden
            long usage = getAppUsageToday(foregroundApp);

            if (usage >= limit) {
                if (!foregroundApp.equals(currentBlockedApp)) {
                    showBlockOverlay(foregroundApp,
                            "Günlük limit aşıldı\nLimit: " + limit + " dakika");
                    currentBlockedApp = foregroundApp;
                    goToHomeScreen();
                }
                return;
            }
        }

        // Engellenmeyen uygulama, overlay'i kaldır
        removeBlockOverlay();
        currentBlockedApp = null;
    }

    /**
     * Engelleme overlay'ini göster
     */
    private void showBlockOverlay(String packageName, String message) {
        if (!Settings.canDrawOverlays(context)) {
            return;
        }

        // Eğer zaten gösteriliyorsa tekrar ekleme
        if (blockOverlay != null) {
            return;
        }

        // Overlay view oluştur
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        blockOverlay = inflater.inflate(R.layout.block_overlay, null);

        // Mesajı ayarla
        TextView messageText = blockOverlay.findViewById(R.id.blockMessage);
        messageText.setText(message);

        // Window parametreleri
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.CENTER;

        // Overlay'i ekle
        try {
            windowManager.addView(blockOverlay, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Engelleme overlay'ini kaldır
     */
    private void removeBlockOverlay() {
        if (blockOverlay != null) {
            try {
                windowManager.removeView(blockOverlay);
            } catch (Exception e) {
                e.printStackTrace();
            }
            blockOverlay = null;
        }
    }

    public void removeBlockOverlayPublic() {
        removeBlockOverlay();
    }

    /**
     * Ana ekrana dön
     */
    private void goToHomeScreen() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(homeIntent);
    }

    /**
     * Uygulamanın bugünkü kullanım süresini al (dakika)
     */
    private long getAppUsageToday(String packageName) {
        // Cache'den kontrol et
        if (appUsageToday.containsKey(packageName)) {
            return appUsageToday.get(packageName);
        }

        // Yeni veri al
        Map<String, Map<String, Object>> usageData = appUsageHelper.getAppUsageStats();

        if (usageData.containsKey(packageName)) {
            Map<String, Object> appData = usageData.get(packageName);
            Object usageObj = appData.get("usageToday");

            if (usageObj instanceof Long) {
                long usage = (Long) usageObj;
                appUsageToday.put(packageName, usage);
                return usage;
            }
        }

        return 0;
    }

    /**
     * Temizlik
     */
    public void cleanup() {
        removeBlockOverlay();
    }
}
