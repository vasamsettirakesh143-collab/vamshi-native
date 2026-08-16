package com.vamshi.ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
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

        requestOverlayPermissionIfNeeded();
        requestNeededPermissions();
    }

    private void requestOverlayPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
        }
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
