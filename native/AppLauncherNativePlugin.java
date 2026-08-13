package com.vamshi.ai;

import android.content.Intent;
import android.content.pm.PackageManager;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "AppLauncherNative")
public class AppLauncherNativePlugin extends Plugin {

    @PluginMethod
    public void launch(PluginCall call) {
        String packageName = call.getString("packageName");

        if (packageName == null) {
            call.reject("packageName is required");
            return;
        }

        PackageManager pm = getContext().getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(launchIntent);
            call.resolve();
        } else {
            call.reject("App not installed: " + packageName);
        }
    }
}
