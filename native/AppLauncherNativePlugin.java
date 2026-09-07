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
     * Opens WhatsApp in a contact's chat with the message
     * pre-filled. Tries three official WhatsApp entry
     * points in order until one works:
     *
     *   1. wa.me URL pinned to the WhatsApp package
     *   2. wa.me URL unpinned (system routes it)
     *   3. whatsapp://send deep link
     *
     * wa.me needs an international number, so 10-digit
     * local numbers get "91" (India) prepended.
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

        String digits = contact.number.replaceAll("[^0-9]", "");

        if (digits.isEmpty()) {
            call.reject("Contact has no usable phone number: " + contact.name);
            return;
        }

        if (digits.length() == 10) {
            digits = "91" + digits;
        }

        String text = message == null ? "" : message;
        String encodedText = Uri.encode(text);

        /*
         * Attempt 1: wa.me pinned directly to WhatsApp.
         */
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(
                "https://wa.me/" + digits + "?text=" + encodedText
            ));
            intent.setPackage("com.whatsapp");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);

            resolveSuccess(call, contact.name, digits);
            return;
        } catch (Exception ignored) {
            // Fall through to attempt 2.
        }

        /*
         * Attempt 2: wa.me without pinning the package.
         */
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(
                "https://wa.me/" + digits + "?text=" + encodedText
            ));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);

            resolveSuccess(call, contact.name, digits);
            return;
        } catch (Exception ignored) {
            // Fall through to attempt 3.
        }

        /*
         * Attempt 3: the whatsapp:// deep link scheme.
         */
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(
                "whatsapp://send?phone=" + digits + "&text=" + encodedText
            ));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);

            resolveSuccess(call, contact.name, digits);
            return;
        } catch (Exception error) {
            call.reject("Could not open WhatsApp: " + error.getMessage());
        }
    }

    private void resolveSuccess(PluginCall call, String name, String number) {
        JSObject result = new JSObject();
        result.put("success", true);
        result.put("contact", name);
        result.put("number", number);
        call.resolve(result);
    }
}
