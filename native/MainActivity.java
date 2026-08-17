package com.vamshi.ai;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.BridgeActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BridgeActivity {

    private static final int PERMISSION_REQUEST_CODE = 2001;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AppLauncherNativePlugin.class);
        super.onCreate(savedInstanceState);

        promptAccessibilityIfNeeded();
        requestNeededPermissions();
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
