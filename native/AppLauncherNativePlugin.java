package com.vamshi.ai;

import android.content.Intent;
import android.net.Uri;

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

    /**
     * Opens WhatsApp in a specific contact's chat with the
     * message pre-filled, using the official SENDTO intent.
     * WhatsApp requires the user to tap send - a deliberate
     * safety feature on their side.
     */
    @PluginMethod
    public void sendWhatsApp(PluginCall call) {
        String contactName = call.getString("contactName");
        String message = call.getString("message");

        if (contactName == null || contactName.trim().isEmpty()) {
            call.reject("contactName is required");
            return;
        }

        ContactLookupUtil.Contact contact =
            ContactLookupUtil.findBestMatch(getContext(), contactName.trim());

        if (contact == null) {
            call.reject("No contact matched: " + contactName);
            return;
        }

        // Strip spaces, dashes and other characters WhatsApp does not want.
        String number = contact.number.replaceAll("[^0-9+]", "");

        if (number.isEmpty()) {
            call.reject("Contact has no usable phone number: " + contact.name);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + Uri.encode(number)));
        intent.setPackage("com.whatsapp");
        intent.putExtra("sms_body", message == null ? "" : message);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            getContext().startActivity(intent);

            JSObject result = new JSObject();
            result.put("success", true);
            result.put("contact", contact.name);
            result.put("number", number);
            call.resolve(result);
        } catch (Exception error) {
            call.reject("Could not open WhatsApp: " + error.getMessage());
        }
    }
}
