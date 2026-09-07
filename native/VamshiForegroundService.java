package com.vamshi.ai;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.net.Uri;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VamshiForegroundService extends Service implements RecognitionListener {

    private static final String CHANNEL_ID = "vamshi_service_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String BACKEND_URL =
            "https://vamshi-backend-y6ja.onrender.com/chat";
    private static final String ACTION_URL =
            "https://vamshi-backend-y6ja.onrender.com/action";

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private AudioManager audioManager;
    private Handler handler;

    private boolean listeningEnabled = false;
    private boolean awaitingFollowUp = false;
    private boolean awaitingCallName = false;

    /*
     * Set when Vamshi asks "who should I send the
     * WhatsApp message to?" so the next spoken
     * sentence is treated as the contact name.
     */
    private boolean awaitingWhatsAppName = false;

    @Override    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        handler = new Handler(Looper.getMainLooper());

        audioManager =
                (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        boolean hasMic =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED;

        Notification notification = buildNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {

            int type = hasMic
                    ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    : ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;

            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    type
            );

        } else {

            startForeground(
                    NOTIFICATION_ID,
                    notification
            );
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

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Vamshi AI",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "Keeps Vamshi listening in the background"
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {

        Intent openAppIntent =
                new Intent(this, MainActivity.class);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        openAppIntent,
                        PendingIntent.FLAG_IMMUTABLE
                );

        return new NotificationCompat.Builder(
                this,
                CHANNEL_ID
        )
                .setContentTitle("Vamshi AI")
                .setContentText("Listening for \"Vamshi\"...")
                .setSmallIcon(
                        android.R.drawable.ic_btn_speak_now
                )
                .setContentIntent(pendingIntent)
                .setOngoing)
                .build();
    }

    private void initTextToSpeech() {

        textToSpeech =
                new TextToSpeech(
                        this,
                        status -> {

                            if (status == TextToSpeech.SUCCESS
                                    && textToSpeech != null) {

                                textToSpeech.setLanguage(
                                        Locale.US
                                );
                            }
                        }
                );
    }

    private void initSpeechRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            return;
        }

        speechRecognizer =
                SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(this);

        startListening();
    }

    private void startListening() {

        if (speechRecognizer == null) {
            return;
        }

        if (audioManager != null
                && audioManager.isMusicActive()) {

            handler.postDelayed(
                    this::startListening,
                    2000
            );

            return;
        }

        Intent recognizerIntent =
                new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                );

        recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.US
        );

        recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_CALLING_PACKAGE,
                getPackageName()
        );

        recognizerIntent.putExtra(
                "android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS",
                2500
        );

        recognizerIntent.putExtra(
                "android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS",
                2000
        );

        recognizerIntent.putExtra(
                "android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS",
                3000
        );

        speechRecognizer.startListening(
                recognizerIntent
        );
    }

    private void restartListeningSoon() {

        handler.postDelayed(
                this::startListening,
                500
        );
    }

    @Override
    public void onResults(Bundle results) {

        ArrayList<String> matches =
                results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                );

        if (matches == null || matches.isEmpty()) {

            restartListeningSoon();

            return;
        }

        String heard =
                matches.get(0)
                        .toLowerCase(Locale.US);

        /*
         * Follow-up for "send whatsapp to..." with
         * no contact name given.
         */
        if (awaitingWhatsAppName) {

            awaitingWhatsAppName = false;

            handleWhatsAppCommand(
                    "send whatsapp to " + heard.trim()
            );

            return;
        }

        if (awaitingCallName) {

            awaitingCallName = false;

            handleCallCommand(
                    heard.trim()
            );

            return;
        }

        if (awaitingFollowUp) {

            awaitingFollowUp = false;

            handleCommand(
                    heard.trim()
            );

            return;
        }

        if (heard.contains("vamshi")) {

            String command =
                    heard
                            .replace("hey vamshi", "")
                            .replace("vamshi", "")
                            .trim();

            handleCommand(command);

            return;
        }

        restartListeningSoon();
    }

    private void searchYouTube(String query) {

        if (!VamshiAccessibilityService.isRunning()) {

            speak("Please enable Vamshi accessibility service first.");
            restartListeningSoon();
            return;
        }

        boolean started =
                VamshiAccessibility.searchYouTube(query);

        if (started) {

            speak("Searching for " + query);

        } else {

            speak("Sorry, I could not start the YouTube search.");
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

        /*
         * messaging commands. Checked BEFORE
         * the "open " and "call " handlers so phrases
         * like "open whatsapp chat with amma" and
         * "send whatsapp to amma" are not eaten by them.
         */
        if (command.contains("whatsapp")) {

            handleWhatsAppCommand(command);

            return;
        }

        // Maps navigation command        // "Open Maps and navigate to [destination]"
        if (command.startsWith("open maps")
                && command.contains("navigate to")) {

            String destination =
                    command.substring(
                            command.indexOf("navigate to")
                                    + "navigate to".length()
                    ).trim();

            if (destination.isEmpty()) {

                speak(
                        "Where would you like to navigate?"
                );

                restartListeningSoon();

                return;
            }

            navigateWith(destination);

            return;
        }

        // YouTube search command:
        // "Open YouTube and search for [something]"
        if (command.startsWith("open youtube")
                && command.contains("search for")) {

            String query =
                    command.substring(
                            command.indexOf("search for")
                                    + "search for".length()
                    ).trim();

            if (query.isEmpty()) {

                speak(
                        "What would you like to search for on YouTube?"
                );

                restartListeningSoon();

                return;
            }

            searchYouTube(query);

            return;
        }

        if (command.contains("call ")) {

            String spokenName =
                    command.substring(
                            command.indexOf("call ") + 5
                    ).trim();

            handleCallCommand(spokenName);

            return;
        }

        if (command.equals("call                || command.startsWith("call")) {

            handleCallCommand("");

            return;
        }

        if (command.startsWith("open ")) {

            String appName =
                    command.substring(5).trim();

            openAnyApp(appName);

            return;
        }

        if (command.equals("time")
                || command.contains("what time")
                || command.contains("the time")) {

            String time =
                    DateFormat
                            .getTimeInstance(
                                    DateFormat.SHORT
                            )
                            .format(
                                    new Date()
                            );

            speak(
                    "The current time is "
                            + time
            );

            restartListeningSoon();

            return;
        }

        if (command.equals("date")
                || command.contains("today")
                || command.contains("what date")) {

            String date =
                    DateFormat
                            .getDateInstance(
                                    DateFormat.FULL
                            )
                            .format(
                                    new Date()
                            );

            speak(
                    "Today is "
                            + date
            );

            restartListeningSoon();

            return;
        }

        if (command("hi")
                || command.equals("hello")
                || command.equals("hey")
                || command.startsWith("hi ")
                || command.startsWith("hello ")
                || command.startsWith("hey ")) {

            speak(
                    "Hello Rakesh. I am Vamshi."
            );

            restartListeningSoon();

            return;
        }

        // Temporary accessibility debug command.
        // Open YouTube manually, then say:
        // "Hey Vamshi, debug screen"
        if (command.equals("debug screen")
                || command.equals("debug youtube")) {

            String result =
                    VamshiAccessibilityService.debugCurrentScreen();

            speak(result);

            restartListeningSoon();

            return;
        }

        askAINative(command);
    }

    /*
     * Handles WhatsApp commands heard by voice:
     *
     *   "send whatsapp message to amma saying hi"
     *   "send whatsapp to amma hi"
     *   "whatsapp amma saying hi"
     *   "tell amma on whatsapp that hi"
     *   "open whatsapp chat with amma"
     *   "open whatsapp" -> opens the WhatsApp app itself
     *
     * handles bare "send whatsapp" by asking
     * for the contact name as a follow-up.
     */
    private void handleWhatsAppCommand(String command) {

        String text =
                command == null
                        ? ""
                        : command.trim();

        // Bare "whatsapp" / "send whatsapp" handling.
        // "open whatsapp" alone still opens the app,
        // like any other app name.
        boolean hasTarget =
                text.matches(".*\\b(?:to|with)\\s+.+")
                        || text.matches("^whatsapp\\s+.+");

        if (!hasTarget) {

            if (text("open whatsapp")
                    || text.equals("whatsapp")) {

                openAnyApp("whatsapp");

                return;
            }

            speak(
                    "Who should I send the WhatsApp message to?"
            );

            awaitingWhatsAppName = true;

            restartListeningSoon();

            return;
        }

        String contactName = null;
        String message = null;
        boolean openChatOnly = false;

        /*
         * Pattern 1:
         * "send whatsapp [message] to <contact> saying/telling/that <message>"
         */
        Matcher m = Pattern.compile(
                "send\\s+whatsapp(?:\\s+message)?\\s+to\\s+(.+?)\\s+(?:saying|telling|that)\\s+(.+)"
        ).matcher(text);

        if (m.find()) {
            contactName = m.group(1trim();
            message = m.group(2).trim();
        }

        /*
         * Pattern 2:
         * "send whatsapp to <contact> <message>"
         */
        if (contactName == null) {

            m = Pattern.compile(
                    "send\\s+whatsapp(?:\\s+message)?\\s+to\\s+(.+)"
            ).matcher(text);

            if (m.find()) {

                String rest = m.group(1).trim();

                String[] parts =
                        rest.split("\\s+(?:saying|that)\\s+", 2);

                if (parts.length == 2) {

                    contactName = parts[0].trim();
                    message = parts[1].trim();

                } else {

                    /*
                     * No "saying". The contact is
                     * normally the first word ("amma hi"),
                     * but spoken names can be two words
                     * ("rakesh brother"). Use the first
                     * word as the contact and the rest as
                     * the message; if there is no rest,
                     * the whole thing is the contact name.
                     */
                    String[] words = rest.split("\\s+", 2);

                    contactName = words[0].trim();

                    if (words.length > 1) {
                        message = words[1].trim();
                                   }
            }
        }

        /*
         * Pattern 3:
         * "tell <contact> on whatsapp [that] <message>"
         */
        if (contactName == null) {

            m = Pattern.compile(
                    "tell\\s+(.+?)\\s+on\\s+whatsapp\\s?:that\\s+)?(.+)"
            ).matcher(text);

            if (m.find()) {
                contactName = m.group(1).trim();
                message = m.group(2).trim();
            }
        }

        /*
         * Pattern 4:
         * "whatsapp <contact> saying [message]"
         */
        if (contactName == null) {

            m = Pattern.compile(
                    "^whatsapp\\s+(.+?)\\s+(?:saying|that)\\s+(.+)"
            ).matcher(text);

            if (m.find()) {
                contactName = m.group(1).trim();
                message = m.group(2).trim();
            }
        }

        /*
         * Pattern 5:
         * "open whatsapp chat with <contact>" -> chat only.
         */
        if (contactName == null) {

            m = Pattern.compile(
                    "(?:open|start)\\s+whatsapp\\s+chat\\s+with\\s+(.+)"
            ).matcher(text);

            if (m.find()) {

                contactName = m.group(1).trim();
                message = "";
                openChatOnly = true;
            }
        }

        /*
         * Pattern 6:
         * "whatsapp <contact>" alone -> chat only.
         */
        if (contactName == null) {

            m = Pattern.compile(
                    "^whatsapp\\s+(.+)$"
            ).matcher(text);

            if (m.find()) {

                contactName = m.group(1).trim();
                message = "";
                openChatOnly = true;
            }
        }

        if (contactName == null || contactName.isEmpty()) {

            speak(
                    "Who should I send the WhatsApp message to?"
            );

            awaitingWhatsAppName = true;

            restartListeningSoon();

            return;
        }

        // Strip filler words from the contact name.
        contactName = contactName
                .replaceAll("\\s+(?:app|please|now)$", "")
                .trim();

        if (openChat || message == null || message.isEmpty()) {

            openWhatsAppChat(contactName);

            return;
        }

        sendWhatsAppToContact(contactName, message);
    }

    /*
     * Resolves the spoken name to a contact and opens
     * WhatsApp in that chat with the message pre-filled,
     * using the same wa.me approach as the chat path.
     */
    private void sendWhatsAppToContact(
            String spokenName,
            String message
    ) {

        boolean hasContactsPermission =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED;

        if (!hasContactsPermission) {

            speak(
                    "I don't have contacts permission yet. Please open the app and grant the contacts permission."
            );

            restartListeningSoon();

            return;
        }

        try {

            ContactLookupUtil.Contact contact =
                    ContactLookupUtil.findBestMatch(
                            this,
                            spokenName
                    );

            if (contact == null) {

                speak(
                        "I could not find a contact named "
                                + spokenName
                );

                restartListeningSoon();

                return;
            }

            String digits =
                    contact.number.replaceAll("[^0-9]", "");

            if (digits.isEmpty()) {

                speak(
                        contact.name
                                + " has no usable phone number."
                );

                restartListeningSoon();

                return;
            }

            if (digits.length() == 10) {
                digits = "91" + digits;
            }

            String encodedText = Uri.encode(message);

            /*
             * Attempt 1: wa.me pinned to WhatsApp.
             */
            try {

                Intent intent = new Intent(Intent.ACTION_VIEW);

                intent.setData(Uri.parse(
                        "https://wa.me/" + digits
                                + "?text=" + encodedText
                ));

                intent.setPackage("com.whatsapp");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);

                speakWhatsAppConfirmation(contact.name);
                return;

            } catch (Exception ignored) {
            }

            /*
             * Attempt 2: wa.me unpinned.
             */
            try {

                Intent intent = new Intent(Intent.ACTION_VIEW);

                intent.setData(Uri.parse(
                        "https://wa.me/" + digits
                                + "?text=" + encodedText
                ));

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);

                speakWhatsAppConfirmation(contact.name);
                return;

            } catch (Exception ignored) {
            }

            /*
             * Attempt 3: whatsapp:// deep link.
             */
            try {

                Intent intent = new Intent(Intent.ACTION_VIEW);

                intent.setData(Uri.parse(
                        "whatsapp://send?phone=" + digits
                                + "&text=" + encodedText
                ));

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);

                speakWhatsAppConfirmation(contact.name);

            } catch (Exception e) {

                speak(
                        "Sorry, I could not open WhatsApp."
                );
            }

        } catch (Exception e) {

            speak(
                    "WhatsApp error: "
                            + e.getClass().getSimpleName()
            );
        }

        restartListeningSoon();
    }

    /*
     * Opens a WhatsApp chat without a message.
     */
    private void openWhatsAppChat(String spokenName) {
        sendWhatsAppToContact(spokenName, "");
    }

    /*
     * Confirmation message.
     */
    private void speakWhatsAppConfirmation(String contactName) {

        speak(
                "WhatsApp chat with "
                        + contactName
                        + " is open. Tap send when ready."
        );

        restartListeningSoon();
    }

    private void navigateWithMaps(String destination) {

        try {

            String encodedDestination =
                    Uri.encode(destination);

            Uri mapsUri =
                    Uri.parse(
                            "google.navigation:q="
                                    + encodedDestination
                    );

            Intent mapsIntent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            mapsUri
                    );

            mapsIntent.setPackage(
                    "com.google.android.apps.maps"
            );

            mapsIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            if (mapsIntent.resolveActivity(
                    getPackageManager()
            ) != null) {

                startActivity(
                        mapsIntent
                );

                speak(
                        "Opening Maps and navigating to "
                                + destination
                );

            } else {

                Intent fallbackIntent =
                        new Intent(
                                Intent.ACTION_VIEW,
                                mapsUri
                        );

                fallbackIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                startActivity(
                        fallbackIntent
                );

                speak(
                        "Opening navigation for "
                                + destination
                );
            }

        } catch (Exception e) {

            speak(
                    "Navigation error: "
                            + e.getClass()
                            .getSimpleName()
            );
        }

        restartListeningSoon();
    }

    private void handleCallCommand(
            String spokenName
    ) {

        if (spokenName.isEmpty()) {

            speak(
                    "Who do you want to call?"
            );

            awaitingCallName = true;

            restartListeningSoon();

            return;
        }

        boolean hasContactsPermission =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED;

        boolean hasCallPermission =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED;

        if (!hasContactsPermission
                || !hasCallPermission) {

            speak(
                    "I don't have permission to make calls yet. Please open the app and grant the contacts and phone permissions."
            );

            restartListeningSoon();

            return;
        }

        try {

            ContactLookupUtil.Contact contact =
                    ContactLookupUtil.findBestMatch(
                            this,
                            spokenName
                    );

            if (contact == null) {

                speak(
                        "I could not find a contact named "
                                + spokenName
                );

                restartListeningSoon();

                return;
            }

            boolean called =
                    CallUtil.placeCall(
                            this,
                            contact.number
                    );

            speak(
                    called
                            ? "Calling "
                            + contact.name
                            : "Sorry, I could not place the call."
            );

        } catch (Exception e) {

            speak(
                    "Call error: "
                            + e.getClass()
                            .getSimpleName()
                            + " "
                            + e.getMessage()
            );
        }

        restartListeningSoon();
    }

    private void openAnyApp(
            String spokenAppName
    ) {

        if (spokenAppName.isEmpty()) {

            speak(
                    "Which app do you want to open?"
            );

            restartListeningSoon();

            return;
        }

        try {

            AppLauncherUtil.AppEntry match =
                    AppLauncherUtil.findBestMatch(
                            this,
                            spokenAppName
                    );

            if (match == null) {

                speak(
                        "I could not find an app called "
                                + spokenAppName
                );

                restartListeningSoon();

                return;
            }

            boolean opened;

            if (VamshiAccessibilityService.isRunning()) {

                opened =
                        VamshiAccessibilityService
                                .launchApp(
                                        match.packageName
                                );

            } else {

                opened =
                        AppLauncherUtil.launch(
                                this,
                                match.packageName
                        );
            }

            speak(
                    opened
                            ? "Opening "
                            + match.label
                            : "Sorry, I could not open "
                            + match.label
            );

        } catch (Exception e) {

            speak(
                    "Open app error: "
                            + e.getClass()
                            .getSimpleName()
                            + " "
                            + e.getMessage()
            );
        }

        restartListeningSoon();
    }

    /*
     * JARVIS BRAIN:
     * Step 1: ask the backend /action route if this is
     *         a device action (AI decides, any phrasing).
     * Step 2: if yes, run the action.
     * Step 3: if no, fall back to normal /chat reply.
     */
    private void askAINative(
            String message
    ) {

        new Thread(() -> {

            // Step 1: ask the brain if this is an action.
            String actionJson = null;

            try {

                URL actionUrl =
                        new URL(
                                ACTION_URL
                        );

                HttpURLConnection actionConn =
                        (HttpURLConnection)
                                actionUrl.openConnection();

                actionConn.setRequestMethod("POST");

                actionConn.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                actionConn.setDoOutput(true);

                actionConn.setConnectTimeout(45000);

                actionConn.setReadTimeout(45000);

                JSONObject actionBody =
                        new JSONObject();

                actionBody.put("message", message);

                OutputStream aos =
                        actionConn.getOutputStream();

                aos.write(
                        actionBody.toString()
                                .getBytes("UTF-8")
                );

                aos.close();

                int actionStatus =
                        actionConn.getResponseCode();

                if (actionStatus < 400) {

                    BufferedReader abr =
                            new BufferedReader(
                                    new InputStreamReader(
                                            actionConn.getInputStream()
                                    )
                            );

                    StringBuilder asb =
                            new StringBuilder();

                    String aline;

                    while (
                            (aline = abr.readLine())
                                    != null
                    ) {
                        asb.append(aline);
                    }

                    abr.close();

                    JSONObject actionResp =
                            new JSONObject(
                                    asb.toString()
                            );

                    if (actionResp.has("action")
                            && !actionResp.isNull("action")) {

                        actionJson =
                                actionResp.getJSONObject("action")
                                        .toString();
                    }
                }

                actionConn.disconnect();

            } catch (Exception ignored) {
                // Action check failed — fall through
                // to normal chat.
            }

            // Step 2: if it IS an action, run it.
            if (actionJson != null) {

                final String actionFinal = actionJson;

                handler.post(() -> {

                    runAiAction(actionFinal);

                    restartListeningSoon();

                });

                return;
            }

            // Step 3: not an action — normal chat reply.
            String reply;

            try {

                URL url =
                        new URL(
                                BACKEND_URL
                        );

                HttpURLConnection conn =
                        (HttpURLConnection)
                                url.openConnection();

                conn.setRequestMethod("POST");

                conn.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                conn.setDoOutput(true);

                conn.setConnectTimeout(45000);

                conn.setReadTimeout(45000);

                JSONObject body =
                        new JSONObject();

                body.put("message", message);

                OutputStream os =
                        conn.getOutputStream();

                os.write(
                        body.toString()
                                .getBytes("UTF-8")
                );

                os.close();

                int statusCode =
                        conn.getResponseCode();

                BufferedReader br =
                        new BufferedReader(
                                new InputStreamReader(
                                        statusCode >= 400
                                                ? conn.getErrorStream()
                                                : conn.getInputStream()
                                )
                        );

                StringBuilder sb =
                        new StringBuilder();

                String line;

                while (
                        (line = br.readLine())
                                != null
                ) {
                    sb.append(line);
                }

                br.close();

                if (statusCode >= 400) {

                    reply =
                            "Backend returned error code "
                                    + statusCode;

                } else {

                    JSONObject respJson =
                            new JSONObject(
                                    sb.toString()
                            );

                    reply =
                            respJson.optString(
                                    "reply",
                                    "Sorry, I could not get a reply."
                            );
                }

            } catch (
                    java.net.SocketTimeoutException e
            ) {

                reply =
                        "Debug: request timed out.";

            } catch (
                    java.net.UnknownHostException e
            ) {

                reply =
                        "Debug: no internet connection.";

            } catch (Exception e) {

                reply =
                        "Debug error: "
                                + e.getClass()
                                .getSimpleName()
                                + " "
                                + e.getMessage();
            }

            final String finalReply =
                    reply;

            handler.post(() -> {

                speak(finalReply);

                restartListeningSoon();

            });

        }).start();
    }

    /*
     * Executes an action decided by the AI brain.
     * This is Jarvis mode: any phrasing works.
     */
    private void runAiAction(String actionJson) {

        try {

            JSONObject action =
                    new JSONObject(actionJson);

            String type =
                    action.optString("type", "");

            String contact =
                    action.optString("contact", "");

            String msg =
                    action.optString("message", "");

            String appName =
                    action.optString("app", "");

            String destination =
                    action.optString("destination", "");

            switch (type) {

                case "whatsapp":

                    if (msg.isEmpty()) {
                        openWhatsAppChat(contact);
                    } else {
                        sendWhatsAppToContact(contact, msg);
                    }
                    break;

                case "call":
                    handleCallCommand(contact);
                    break;

                case "open_app":
                    openAnyApp(appName);
                    break;

                case "navigate":
                    navigateWithMaps(destination);
                    break;

                default:
                    speak("Sorry, I could not perform that action.");
                    restartListeningSoon();
                    break;
            }

        } catch (Exception e) {

            speak(
                    "Action error: "
                            + e.getClass().getSimpleName()
            );

            restartListeningSoon();
        }
    }

    private void speak(String text) {

        if (textToSpeech != null) {

            textToSpeech.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "vamshi_utterance"
            );
        }
    }

    @Override
    public void onError(int error) {

        restartListeningSoon();
    }

    @Override
    public void onReadyForSpeech(
            Bundle params
    ) {
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onRmsChanged(
            float rmsdB
    ) {
    }

    @Override
    public void onBufferReceived(
            byte[] buffer
    ) {
    }

    @Override
    public void onEndOfSpeech() {
    }

    @Override
    public void onPartialResults(
            Bundle partialResults
    ) {
    }

    @Override
    public void onEvent(
            int eventType,
            Bundle params
    ) {
    }

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
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }
}
