package com.vamshi.ai;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            Intent service =
                    new Intent(context, VamshiForegroundService.class);

            try {
                ContextCompat.startForegroundService(context, service);
            } catch (Exception ignored) {
                // Permissions may not be granted yet after boot.
            }
        }
    }
}
