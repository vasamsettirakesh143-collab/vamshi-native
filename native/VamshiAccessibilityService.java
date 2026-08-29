package com.vamshi.ai;

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

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

    /**
     * Finds an element containing the given text and clicks it.
     * Returns true when a clickable element was found and clicked.
     */
    public static boolean clickText(String text) {
        if (instance == null || text == null || text.trim().isEmpty()) {
            return false;
        }

        AccessibilityNodeInfo root = instance.getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        try {
            List<AccessibilityNodeInfo> nodes =
                    root.findAccessibilityNodeInfosByText(text);

            if (nodes == null || nodes.isEmpty()) {
                return false;
            }

            for (AccessibilityNodeInfo node : nodes) {
                AccessibilityNodeInfo clickableNode = node;

                while (clickableNode != null) {
                    if (clickableNode.isClickable()) {
                        return clickableNode.performAction(
                                AccessibilityNodeInfo.ACTION_CLICK
                        );
                    }

                    clickableNode = clickableNode.getParent();
                }
            }

            return false;

        } finally {
            root.recycle();
        }
    }

    /**
     * Enters text into the currently focused input field.
     */
    public static boolean typeText(String text) {
        if (instance == null || text == null) {
            return false;
        }

        AccessibilityNodeInfo root = instance.getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        try {
            AccessibilityNodeInfo focusedNode =
                    root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);

            if (focusedNode == null) {
                return false;
            }

            Bundle arguments = new Bundle();
            arguments.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
            );

            return focusedNode.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    arguments
            );

        } finally {
            root.recycle();
        }
    }

    /**
     * Checks whether text is currently visible on the active screen.
     */
    public static boolean isTextVisible(String text) {
        if (instance == null || text == null || text.trim().isEmpty()) {
            return false;
        }

        AccessibilityNodeInfo root = instance.getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        try {
            List<AccessibilityNodeInfo> nodes =
                    root.findAccessibilityNodeInfosByText(text);

            return nodes != null && !nodes.isEmpty();

        } finally {
            root.recycle();
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // The service is now capable of reading the active window.
        // Command execution will be connected separately so existing
        // app-launching behavior remains unchanged for now.
    }

    @Override
    public void onInterrupt() {
        // Required AccessibilityService method.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (instance == this) {
            instance = null;
        }
    }
}
