package com.vamshi.ai;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class VamshiAccessibilityService extends AccessibilityService {

    private static VamshiAccessibilityService instance;

    public static boolean isRunning() {
        return instance != null;
    }

    public static boolean launchApp(String packageName) {
        if (instance == null) {
            return false;
        }
        return AppLauncherUtil.launch(instance, packageName);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // We don't need to react to screen content — this service exists
        // purely so Android trusts it to open apps from the background.
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (instance == this) {
            instance = null;
        }
    }
}
