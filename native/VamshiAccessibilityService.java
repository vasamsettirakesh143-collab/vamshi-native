package com.vamshi.ai;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class VamshiAccessibilityService extends AccessibilityService {

    private static VamshiAccessibilityService instance;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private static String pendingYouTubeSearch = null;
    private static boolean youtubeSearchRunning = false;

    private static final String YOUTUBE_PACKAGE =
            "com.google.android.youtube";


    public static boolean isRunning() {
        return instance != null;
    }


    public static boolean launchApp(String packageName) {

        if (instance == null) {
            return false;
        }

        return AppLauncherUtil.launch(
                instance,
                packageName
        );
    }


    /*
     * Starts YouTube and remembers what Vamshi
     * should search for after YouTube opens.
     */
    public static boolean searchYouTube(String query) {

        if (instance == null
                || query == null
                || query.trim().isEmpty()) {

            return false;
        }

        pendingYouTubeSearch = query.trim();
        youtubeSearchRunning = true;

        boolean opened = AppLauncherUtil.launch(
                instance,
                YOUTUBE_PACKAGE
        );

        if (!opened) {

            pendingYouTubeSearch = null;
            youtubeSearchRunning = false;

            return false;
        }

        /*
         * Give YouTube some time to load.
         */
        instance.handler.postDelayed(
                instance::continueYouTubeSearch,
                1800
        );

        return true;
    }


    /*
     * Main YouTube automation sequence.
     */
    private void continueYouTubeSearch() {

        if (!youtubeSearchRunning
                || pendingYouTubeSearch == null) {

            return;
        }

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {

            retryYouTubeSearch();
            return;
        }


        /*
         * STEP 1:
         * Check if a text field is already open.
         */
        AccessibilityNodeInfo editable =
                findEditableNode(root);

        if (editable != null) {

            enterYouTubeSearch(editable);
            return;
        }


        /*
         * STEP 2:
         * Find YouTube's Search button.
         *
         * Depending on the YouTube version it may
         * appear as text or only as a content description.
         */
        AccessibilityNodeInfo searchButton =
                findNode(
                        root,
                        "search"
                );

        if (searchButton != null) {

            boolean clicked =
                    clickNodeOrParent(
                            searchButton
                    );

            if (clicked) {

                handler.postDelayed(
                        this::fillYouTubeSearchBox,
                        700
                );

                return;
            }
        }


        retryYouTubeSearch();
    }


    /*
     * Called after clicking YouTube's Search icon.
     */
    private void fillYouTubeSearchBox() {

        if (!youtubeSearchRunning
                || pendingYouTubeSearch == null) {

            return;
        }

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {

            retryYouTubeSearch();
            return;
        }

        AccessibilityNodeInfo editable =
                findEditableNode(root);

        if (editable == null) {

            /*
             * Sometimes YouTube exposes the search box
             * with accessible text rather than immediately
             * marking it editable.
             */
            editable =
                    findNode(
                            root,
                            "search youtube"
                    );
        }

        if (editable == null) {

            retryYouTubeSearch();
            return;
        }

        enterYouTubeSearch(editable);
    }


    /*
     * Types the requested search text.
     */
    private void enterYouTubeSearch(
            AccessibilityNodeInfo field
    ) {

        field.performAction(
                AccessibilityNodeInfo.ACTION_FOCUS
        );

        Bundle arguments = new Bundle();

        arguments.putCharSequence(
                AccessibilityNodeInfo
                        .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                pendingYouTubeSearch
        );

        boolean typed =
                field.performAction(
                        AccessibilityNodeInfo.ACTION_SET_TEXT,
                        arguments
                );

        if (!typed) {

            retryYouTubeSearch();
            return;
        }

        handler.postDelayed(
                this::submitYouTubeSearch,
                500
        );
    }


    /*
     * Submits the search.
     */
    private void submitYouTubeSearch() {

        AccessibilityNodeInfo root =
                getRootInActiveWindow();

        if (root == null) {

            retryYouTubeSearch();
            return;
        }

        AccessibilityNodeInfo editable =
                findEditableNode(root);

        /*
         * Android API 30+ exposes the keyboard's
         * IME Enter action directly.
         */
        if (editable != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            boolean submitted =
                    editable.performAction(
                            AccessibilityNodeInfo
                                    .AccessibilityAction
                                    .ACTION_IME_ENTER
                                    .getId()
                    );

            if (submitted) {

                finishYouTubeSearch();
                return;
            }
        }


        /*
         * Fallback:
         * look for a visible Search button/action.
         */
        AccessibilityNodeInfo search =
                findNode(
                        root,
                        "search"
                );

        if (search != null
                && clickNodeOrParent(search)) {

            finishYouTubeSearch();
            return;
        }


        /*
         * Another possible label used by some
         * keyboard / YouTube versions.
         */
        AccessibilityNodeInfo go =
                findNode(
                        root,
                        "go"
                );

        if (go != null
                && clickNodeOrParent(go)) {

            finishYouTubeSearch();
            return;
        }


        retryYouTubeSearch();
    }


    /*
     * Retry because YouTube may still be loading.
     */
    private void retryYouTubeSearch() {

        if (!youtubeSearchRunning) {
            return;
        }

        handler.postDelayed(
                this::continueYouTubeSearch,
                700
        );
    }


    private void finishYouTubeSearch() {

        youtubeSearchRunning = false;
        pendingYouTubeSearch = null;
    }


    /*
     * Searches the accessibility tree using
     * text OR content description.
     */
    private AccessibilityNodeInfo findNode(
            AccessibilityNodeInfo node,
            String target
    ) {

        if (node == null
                || target == null) {

            return null;
        }

        String targetLower =
                target.toLowerCase();

        CharSequence text =
                node.getText();

        if (text != null
                && text.toString()
                .toLowerCase()
                .contains(targetLower)) {

            return node;
        }

        CharSequence description =
                node.getContentDescription();

        if (description != null
                && description.toString()
                .toLowerCase()
                .contains(targetLower)) {

            return node;
        }

        for (int i = 0;
             i < node.getChildCount();
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            AccessibilityNodeInfo result =
                    findNode(
                            child,
                            target
                    );

            if (result != null) {
                return result;
            }
        }

        return null;
    }


    /*
     * Finds an editable text field.
     */
    private AccessibilityNodeInfo findEditableNode(
            AccessibilityNodeInfo node
    ) {

        if (node == null) {
            return null;
        }

        if (node.isEditable()) {
            return node;
        }

        for (int i = 0;
             i < node.getChildCount();
             i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            AccessibilityNodeInfo result =
                    findEditableNode(child);

            if (result != null) {
                return result;
            }
        }

        return null;
    }


    /*
     * Click the node.
     *
     * If the node itself isn't clickable,
     * walk upward until we find its clickable parent.
     */
    private boolean clickNodeOrParent(
            AccessibilityNodeInfo node
    ) {

        AccessibilityNodeInfo current =
                node;

        while (current != null) {

            if (current.isClickable()) {

                return current.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                );
            }

            current =
                    current.getParent();
        }

        return false;
    }


    /*
     * Existing generic click helper.
     */
    public static boolean clickText(
            String text
    ) {

        if (instance == null
                || text == null
                || text.trim().isEmpty()) {

            return false;
        }

        AccessibilityNodeInfo root =
                instance.getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        List<AccessibilityNodeInfo> nodes =
                root.findAccessibilityNodeInfosByText(
                        text
                );

        if (nodes == null
                || nodes.isEmpty()) {

            return false;
        }

        for (AccessibilityNodeInfo node : nodes) {

            if (instance.clickNodeOrParent(node)) {
                return true;
            }
        }

        return false;
    }


    /*
     * Existing generic text input helper.
     */
    public static boolean typeText(
            String text
    ) {

        if (instance == null
                || text == null) {

            return false;
        }

        AccessibilityNodeInfo root =
                instance.getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        AccessibilityNodeInfo focusedNode =
                root.findFocus(
                        AccessibilityNodeInfo.FOCUS_INPUT
                );

        if (focusedNode == null) {

            focusedNode =
                    instance.findEditableNode(root);
        }

        if (focusedNode == null) {
            return false;
        }

        Bundle arguments =
                new Bundle();

        arguments.putCharSequence(
                AccessibilityNodeInfo
                        .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
        );

        return focusedNode.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                arguments
        );
    }


    public static boolean isTextVisible(
            String text
    ) {

        if (instance == null
                || text == null
                || text.trim().isEmpty()) {

            return false;
        }

        AccessibilityNodeInfo root =
                instance.getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        AccessibilityNodeInfo result =
                instance.findNode(
                        root,
                        text
                );

        return result != null;
    }


    @Override
    protected void onServiceConnected() {

        super.onServiceConnected();

        instance = this;
    }


    @Override
    public void onAccessibilityEvent(
            AccessibilityEvent event
    ) {

        if (event == null
                || event.getPackageName() == null) {

            return;
        }

        String packageName =
                event.getPackageName().toString();

        /*
         * If YouTube has just opened and Vamshi
         * has a pending search request, continue it.
         */
        if (youtubeSearchRunning
                && YOUTUBE_PACKAGE.equals(packageName)) {

            handler.removeCallbacksAndMessages(null);

            handler.postDelayed(
                    this::continueYouTubeSearch,
                    350
            );
        }
    }


    @Override
    public void onInterrupt() {
    }


    @Override
    public void onDestroy() {

        super.onDestroy();

        handler.removeCallbacksAndMessages(null);

        youtubeSearchRunning = false;
        pendingYouTubeSearch = null;

        if (instance == this) {
            instance = null;
        }
    }
}
