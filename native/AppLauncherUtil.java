package com.vamshi.ai;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppLauncherUtil {

    public static boolean launch(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);

        if (launchIntent == null) {
            return false;
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launchIntent);
        return true;
    }

    public static class AppEntry {
        public final String label;
        public final String packageName;

        public AppEntry(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
    }

    public static List<AppEntry> getLaunchableApps(Context context) {
        List<AppEntry> result = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo info : apps) {
            String label = info.loadLabel(packageManager).toString();
            String packageName = info.activityInfo.packageName;
            result.add(new AppEntry(label, packageName));
        }

        return result;
    }

    public static AppEntry findBestMatch(Context context, String spokenName) {
        String query = spokenName.toLowerCase(Locale.US).trim();
        if (query.isEmpty()) {
            return null;
        }

        List<AppEntry> apps = getLaunchableApps(context);
        AppEntry bestMatch = null;
        int bestScore = Integer.MAX_VALUE;

        for (AppEntry app : apps) {
            String label = app.label.toLowerCase(Locale.US);

            if (label.equals(query)) {
                return app;
            }

            if (label.contains(query) || query.contains(label)) {
                int score = Math.abs(label.length() - query.length());

                if (score < bestScore) {
                    bestScore = score;
                    bestMatch = app;
                }
            }
        }

        return bestMatch;
    }
}
