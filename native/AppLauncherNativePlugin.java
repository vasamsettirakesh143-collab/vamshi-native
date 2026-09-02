package com.vamshi.ai;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "AppLauncherNative")
public class AppLauncherNativePlugin extends Plugin {

    @PluginMethod
    public void launch(PluginCall call) {
        String packageName = call.getString("packageName");

        if (packageName == null || packageName.trim().isEmpty()) {
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

    @PluginMethod
    public void findAndLaunch(PluginCall call) {
        String spokenName = call.getString("name", "");

        if (spokenName == null || spokenName.trim().isEmpty()) {
            call.reject("name is required");
            return;
        }

        AppLauncherUtil.AppEntry match =
            AppLauncherUtil.findBestMatch(getContext(), spokenName);

        if (match == null) {
            call.reject("No installed app matched: " + spokenName);
            return;
        }

        boolean opened = AppLauncherUtil.launch(getContext(), match.packageName);

        if (!opened) {
            call.reject("Could not open: " + match.label);
            return;
        }

        JSObject result = new JSObject();
        result.put("label", match.label);
        result.put("packageName", match.packageName);
        call.resolve(result);
    }

    /**
     * Opens YouTube and performs a search using the
     * accessibility service automation.
     */
    @PluginMethod
    public void searchYouTube(PluginCall call) {

        String query = call.getString("query");

        if (query == null || query.trim().isEmpty()) {
            call.reject("query is required");
            return;
        }

        boolean ok = VamshiAccessibilityService.searchYouTube(query.trim());

        JSObject result = new JSObject();
        result.put("success", ok);
        call.resolve(result);
    }
}
