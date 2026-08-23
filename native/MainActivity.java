package com.vamshi.ai;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.BridgeActivity;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BridgeActivity {

    private static final int PERMISSION_REQUEST_CODE = 2001;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AppLauncherNativePlugin.class);
        super.onCreate(savedInstanceState);

        showLastCrashIfAny();
        promptAccessibilityIfNeeded();
        requestNeededPermissions();
    }

    private void showLastCrashIfAny() {
        try {
            File file = new File(getFilesDir(), "crash_log.txt");
            if (!file.exists()) return;

            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            file.delete();

            String crashText = new String(data);

            TextView textView = new TextView(this);
            textView.setText(crashText);
            textView.setPadding(32, 32, 32, 32);
            textView.setTextIsSelectable(true);

            ScrollView scrollView = new ScrollView(this);
            scrollView.addView(textView);

            new AlertDialog.Builder(this)
                .setTitle("Last crash")
                .setView(scrollView)
                .setPositiveButton("OK", null)
                .show();

        } catch (Exception ignored) {
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        ComponentName expected = new ComponentName(this, VamshiAccessibilityService.class);
        String enabledServices = Settings.Secure.getString(
            getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        return enabledServices != null && enabledServices.contains(expected.flattenToString());
    }

    private void promptAccessibilityIfNeeded() {
        if (isAccessibilityServiceEnabled()) {
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("One-time setup needed")
            .setMessage("For Vamshi to open apps for you automatically, turn on \"Vamshi AI\" under Accessibility settings on the next screen.")
            .setPositiveButton("Open Settings", (dialog, which) -> {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            })
            .setNegativeButton("Skip", null)
            .show();
    }

    private void requestNeededPermissions() {
        List<String> toRequest = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            toRequest.add(Manifest.permission.RECORD_AUDIO);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            toRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            toRequest.add(Manifest.permission.CALL_PHONE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            toRequest.add(Manifest.permission.READ_CONTACTS);
        }

        if (!toRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                toRequest.toArray(new String[0]),
                PERMISSION_REQUEST_CODE
            );
        } else {
            startVamshiService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            startVamshiService();
        }
    }

    private void startVamshiService() {
        Intent serviceIntent = new Intent(this, VamshiForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}
