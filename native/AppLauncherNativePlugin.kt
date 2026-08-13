package com.vamshi.ai

import android.content.Intent
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "AppLauncherNative")
class AppLauncherNativePlugin : Plugin() {

    @PluginMethod
    fun launch(call: PluginCall) {
        val packageName = call.getString("packageName")

        if (packageName == null) {
            call.reject("packageName is required")
            return
        }

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            call.resolve()
        } else {
            // Genuinely not installed — JS side falls back to the Play Store.
            call.reject("App not installed: $packageName")
        }
    }
}
