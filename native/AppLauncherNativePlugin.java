package com.vamshi.ai;

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

        boolean opened = AppLauncherUtil.launch(getContext(), packageName);

        if (opened) {
            call.resolve();
        } else {
            call.reject("App not installed: " + packageName);
        }
    }
}
