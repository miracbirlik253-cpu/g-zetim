package com.gozetim.app.utils;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Build;
import android.os.Process;

/**
 * İzin kontrolü için yardımcı sınıf
 */
public class PermissionHelper {

    private Context context;

    public PermissionHelper(Context context) {
        this.context = context;
    }

    /**
     * Usage Stats izninin verilip verilmediğini kontrol et
     */
    public boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        if (appOps == null) {
            return false;
        }

        int mode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mode = appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.getPackageName());
        } else {
            mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.getPackageName());
        }

        return mode == AppOpsManager.MODE_ALLOWED;
    }
}
