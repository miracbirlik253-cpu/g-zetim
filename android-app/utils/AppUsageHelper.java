package com.gozetim.app.utils;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Uygulama kullanım istatistiklerini toplayan yardımcı sınıf
 */
public class AppUsageHelper {

    private Context context;
    private UsageStatsManager usageStatsManager;
    private PackageManager packageManager;

    public AppUsageHelper(Context context) {
        this.context = context;
        this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        this.packageManager = context.getPackageManager();
    }

    /**
     * Tüm uygulamaların kullanım istatistiklerini al
     * 
     * @return Map<PackageName, AppData>
     */
    public Map<String, Map<String, Object>> getAppUsageStats() {
        Map<String, Map<String, Object>> result = new HashMap<>();

        // Bugün için kullanım
        Map<String, Long> todayUsage = getUsageForPeriod(getTodayStart(), System.currentTimeMillis());

        // Bu hafta için kullanım
        Map<String, Long> weekUsage = getUsageForPeriod(getWeekStart(), System.currentTimeMillis());

        // Bu ay için kullanım
        Map<String, Long> monthUsage = getUsageForPeriod(getMonthStart(), System.currentTimeMillis());

        // Tüm paketleri birleştir
        for (String packageName : todayUsage.keySet()) {
            Map<String, Object> appData = new HashMap<>();

            // Uygulama adını al
            String appName = getAppName(packageName);
            if (appName == null)
                continue;

            appData.put("name", appName);
            appData.put("package", packageName);
            appData.put("usageToday", todayUsage.getOrDefault(packageName, 0L) / 60000); // Dakikaya çevir
            appData.put("usageWeek", weekUsage.getOrDefault(packageName, 0L) / 60000);
            appData.put("usageMonth", monthUsage.getOrDefault(packageName, 0L) / 60000);
            appData.put("lastUpdated", System.currentTimeMillis());

            result.put(packageName, appData);
        }

        return result;
    }

    /**
     * Belirli bir zaman aralığı için kullanım verilerini al
     */
    private Map<String, Long> getUsageForPeriod(long startTime, long endTime) {
        Map<String, Long> usageMap = new HashMap<>();

        if (usageStatsManager == null)
            return usageMap;

        List<UsageStats> statsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime);

        if (statsList != null) {
            for (UsageStats stats : statsList) {
                String packageName = stats.getPackageName();
                long totalTime = stats.getTotalTimeInForeground();

                // Sadece kullanıcı uygulamalarını dahil et
                if (isUserApp(packageName) && totalTime > 0) {
                    usageMap.put(packageName, usageMap.getOrDefault(packageName, 0L) + totalTime);
                }
            }
        }

        return usageMap;
    }

    /**
     * Şu anda ön planda olan uygulamayı al
     */
    public String getForegroundApp() {
        long currentTime = System.currentTimeMillis();

        List<UsageStats> statsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - 1000 * 60, // Son 1 dakika
                currentTime);

        if (statsList != null && !statsList.isEmpty()) {
            // En son kullanılan uygulamayı bul
            UsageStats mostRecent = null;
            for (UsageStats stats : statsList) {
                if (mostRecent == null || stats.getLastTimeUsed() > mostRecent.getLastTimeUsed()) {
                    mostRecent = stats;
                }
            }

            if (mostRecent != null) {
                return mostRecent.getPackageName();
            }
        }

        return null;
    }

    /**
     * Uygulama adını al
     */
    private String getAppName(String packageName) {
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return packageManager.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * Kullanıcı uygulaması mı kontrol et (sistem uygulaması değil)
     */
    private boolean isUserApp(String packageName) {
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Bugünün başlangıç zamanını al (00:00)
     */
    private long getTodayStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * Bu haftanın başlangıç zamanını al (Pazartesi 00:00)
     */
    private long getWeekStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * Bu ayın başlangıç zamanını al (1. gün 00:00)
     */
    private long getMonthStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
