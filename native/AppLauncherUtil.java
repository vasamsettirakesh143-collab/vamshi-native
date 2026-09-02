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

    /*
     * UPGRADED MATCHER:
     *
     * Previously used length-difference scoring, which
     * could pick the wrong app for multi-word names like
     * "free fire max" or suffixed labels like
     * "PhonePe: Secure Payments".
     *
     * Now uses priority-based scoring:
     *   100 - exact match
     *    80 - label starts with the spoken name
     *    60 - label contains the spoken name
     *    40 - spoken name contains the label
     * (with closeness tie-breaking inside each tier)
     */
    public static AppEntry findBestMatch(Context context, String spokenName) {
        String query = spokenName.toLowerCase(Locale.US).trim();
        if (query.isEmpty()) {
            return null;
        }

        List<AppEntry> apps = getLaunchableApps(context);
        AppEntry bestMatch = null;
        int bestScore = Integer.MIN_VALUE;

        for (AppEntry app : apps) {
            String label = app.label.toLowerCase(Locale.US).trim();

            int score;
            if (label.equals(query)) {
                score = 100;                                    // exact match
            } else if (label.startsWith(query)) {
                score = 80 - (label.length() - query.length()); // starts with
            } else if (label.contains(query)) {
                score = 60 - (label.length() - query.length()); // contains
            } else if (query.contains(label) && label.length() > 2) {
                score = 40;                                     // spoken contains label
            } else {
                continue;
            }

            if (score > bestScore) {
                bestScore = score;
                bestMatch = app;
            }
        }

        return bestMatch;
    }
}
