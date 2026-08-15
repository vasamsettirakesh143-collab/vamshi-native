package com.vamshi.ai;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public class AppLauncherUtil {

    public static boolean launch(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
            return true;
        }

        return false;
    }
}
