package com.vamshi.ai;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class VamshiForegroundService extends Service implements RecognitionListener {

    private static final String CHANNEL_ID = "vamshi_service_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String BACKEND_URL = "https://vamshi-backend-y6ja.onrender.com/chat";

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private Handler handler;
    private boolean listeningEnabled = false;
    private boolean awaitingFollowUp = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;

        Notification notification = buildNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            int type = hasMic
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                : ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
            startForeground(NOTIFICATION_ID, notification, type);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        if (hasMic && !listeningEnabled) {
            listeningEnabled = true;
            initTextToSpeech();
            initSpeechRecognizer();
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Vamshi AI",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps Vamshi listening in the background");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vamshi AI")
            .setContentText("Listening for \"Vamshi\"...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS && textToSpeech != null) {
                textToSpeech.setLanguage(Locale.US);
            }
        });
    }

    private void initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            return;
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);
        startListening();
    }

    private void startListening() {
        if (speechRecognizer == null) return;

        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

        // Give the recognizer more patience before deciding you're done
        // talking, so "Vamshi, open WhatsApp" isn't cut off after "Vamshi".
        recognizerIntent.putExtra(
            "android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 2500
        );
        recognizerIntent.putExtra(
            "android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 2000
        );
        recognizerIntent.putExtra(
            "android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 3000
        );

        speechRecognizer.startListening(recognizerIntent);
    }

    private void restartListeningSoon() {
        handler.postDelayed(this::startListening, 500);
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        if (matches == null || matches.isEmpty()) {
            restartListeningSoon();
            return;
        }

        String heard = matches.get(0).toLowerCase(Locale.US);

        if (awaitingFollowUp) {
            // We already said "Yes?" after hearing just the wake word —
            // treat this next utterance as the command directly, no need
            // to say "vamshi" again.
            awaitingFollowUp = false;
            handleCommand(heard.trim());
            return;
        }

        if (heard.contains("vamshi")) {
            String command = heard.replace("hey vamshi", "")
                    .replace("vamshi", "")
                    .trim();
            handleCommand(command);
            return;
        }

        restartListeningSoon();
    }

    private void handleCommand(String command) {

        if (command.isEmpty()) {
            awaitingFollowUp = true;
            speak("Yes?");
            restartListeningSoon();
            return;
        }

        if (command.contains("hello") || command.contains("hi") || command.contains("hey")) {
            speak("Hello Rakesh. I am Vamshi.");
            restartListeningSoon();
        } else if (command.contains("time")) {
            String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date());
            speak("The current time is " + time);
            restartListeningSoon();
        } else if (command.contains("date") || command.contains("today")) {
            String date = DateFormat.getDateInstance(DateFormat.FULL).format(new Date());
            speak("Today is " + date);
            restartListeningSoon();
        } else if (command.contains("open whatsapp")) {
            launchAppByName("com.whatsapp", "whatsapp");
        } else if (command.contains("open instagram")) {
            launchAppByName("com.instagram.android", "instagram");
        } else if (command.contains("open youtube")) {
            launchAppByName("com.google.android.youtube", "youtube");
        } else if (command.contains("open calculator")) {
            launchAppByName("com.google.android.calculator", "calculator");
        } else {
            askAINative(command);
        }
    }

    private void launchAppByName(String packageName, String label) {
        boolean opened = AppLauncherUtil.launch(this, packageName);
        speak(opened ? "Opening " + label : label + " is not installed.");
        restartListeningSoon();
    }

    private void askAINative(String message) {
        new Thread(() -> {
            String reply;
            try {
                URL url = new URL(BACKEND_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                JSONObject body = new JSONObject();
                body.put("message", message);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject respJson = new JSONObject(sb.toString());
                reply = respJson.optString("reply", "Sorry, I could not get a reply.");

            } catch (Exception e) {
                reply = "Sorry Rakesh, I cannot connect to my brain.";
            }

            final String finalReply = reply;
            handler.post(() -> {
                speak(finalReply);
                restartListeningSoon();
            });

        }).start();
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vamshi_utterance");
        }
    }

    @Override
    public void onError(int error) {
        restartListeningSoon();
    }

    @Override public void onReadyForSpeech(Bundle params) {}
    @Override public void onBeginningOfSpeech() {}
    @Override public void onRmsChanged(float rmsdB) {}
    @Override public void onBufferReceived(byte[] buffer) {}
    @Override public void onEndOfSpeech() {}
    @Override public void onPartialResults(Bundle partialResults) {}
    @Override public void onEvent(int eventType, Bundle params) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
