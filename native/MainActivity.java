package com.vamshi.ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.BridgeActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BridgeActivity {

    private static final int PERMISSION_REQUEST_CODE = 2001;
    public static final String EXTRA_TARGET_PACKAGE = "target_package";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AppLauncherNativePlugin.class);
        super.onCreate(savedInstanceState);

        requestNeededPermissions();
        handleLaunchTarget(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleLaunchTarget(intent);
    }

    private void handleLaunchTarget(Intent intent) {
        if (intent == null) return;
        String targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE);
        if (targetPackage == null) return;

        intent.removeExtra(EXTRA_TARGET_PACKAGE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean opened = AppLauncherUtil.launch(MainActivity.this, targetPackage);
            if (opened) {
                moveTaskToBack(true);
            }
        }, 150);
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
