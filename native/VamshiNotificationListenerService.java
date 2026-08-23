package com.vamshi.ai;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class VamshiNotificationListenerService extends NotificationListenerService {

    private TextToSpeech textToSpeech;
    private String lastSpokenKey = "";
    private long lastSpokenTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS && textToSpeech != null) {
                textToSpeech.setLanguage(Locale.US);
            }
        });
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;

        // Don't read our own "Listening for Vamshi..." notification.
        if (getPackageName().equals(sbn.getPackageName())) return;

        Notification notification = sbn.getNotification();
        if (notification == null) return;

        Bundle extras = notification.extras;
        if (extras == null) return;

        CharSequence titleChars = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence textChars = extras.getCharSequence(Notification.EXTRA_TEXT);

        String title = titleChars != null ? titleChars.toString() : "";
        String text = textChars != null ? textChars.toString() : "";

        if (title.isEmpty() && text.isEmpty()) return;

        // Skip if this exact same notification content was just spoken
        // in the last few seconds (Android often re-fires the same
        // notification once it finishes loading its icon/state).
        String key = sbn.getPackageName() + "|" + title + "|" + text;
        long now = System.currentTimeMillis();

        if (key.equals(lastSpokenKey) && (now - lastSpokenTime) < 4000) {
            return;
        }

        lastSpokenKey = key;
        lastSpokenTime = now;

        String appLabel = getAppLabel(sbn.getPackageName());

        String toSpeak = "Notification from " + appLabel;
        if (!title.isEmpty()) toSpeak += ": " + title;
        if (!text.isEmpty()) toSpeak += ". " + text;

        if (textToSpeech != null) {
            textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_ADD, null, "vamshi_notification");
        }
    }

    private String getAppLabel(String packageName) {
        try {
            android.content.pm.PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
    }
}
