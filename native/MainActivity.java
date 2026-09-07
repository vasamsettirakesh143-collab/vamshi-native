package com.vamshi.ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements RecognitionListener {

    private static final String BACKEND_URL =
            "https://vamshi-backend-y6ja.onrender.com/chat";
    private static final String ACTION_URL =
            "https://vamshi-backend-y6ja.onrender.com/action";

    private static final int PERMISSIONS_REQUEST = 100;

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton micButton;
    private TextView statusText;

    private ChatAdapter chatAdapter;
    private ArrayList<ChatMessage> chatMessages;

    private SpeechRecognizer speechRecognizer;
    private boolean micActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        try {

            setContentView(R.layout.activity_main);

            chatRecyclerView = findViewById(R.id.chatRecyclerView);
            messageInput = findViewById(R.id.messageInput);
            sendButton = findViewById(R.id.sendButton);
            micButton = findViewById(R.id.micButton);
            statusText = findViewById(R.id.statusText);

            chatMessages = new ArrayList<>();

            chatAdapter = new ChatAdapter(chatMessages);

            chatRecyclerView.setLayoutManager(
                    new LinearLayoutManager(this));

            chatRecyclerView.setAdapter(chatAdapter);

            sendButton.setOnClickListener(v -> sendMessage());

            micButton.setOnClickListener(v -> toggleMic());

            Button btnAccessibility =
                    findViewById(R.id.btnAccessibility);

            Button btnNotifications =
                    findViewById(R.id.btnNotifications);

            btnAccessibility.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(
                            Settings.ACTION_ACCESSIBILITY_SETTINGS));
                } catch (Exception ignored) {
                }
            });

            btnNotifications.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(
                            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                } catch (Exception ignored) {
                }
            });

            requestNeededPermissions();

            // Start the always-listening foreground service.
            Intent serviceIntent =
                    new Intent(this, VamshiForegroundService.class);

            ContextCompat.startForegroundService(this, serviceIntent);

            checkAccessibilityPrompt();

            requestIgnoreBatteryOptimization();

        } catch (Throwable e) {

            android.widget.LinearLayout root =
                    new android.widget.LinearLayout(this);

            root.setOrientation(android.widget.LinearLayout.VERTICAL);

            TextView errorView = new TextView(this);

            errorView.setTextColor(0xFFFF3333);
            errorView.setTextSize(16);
            errorView.setPadding(40, 100, 40, 40);
            errorView.setText(
                    "STARTUP ERROR:\n\n"
                            + android.util.Log.getStackTraceString(e));

            setContentView(root, new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));

            root.addView(errorView);
        }
    }

    /*
     * Prompts the user to enable the accessibility service
     * if it is not already on.
     */
    private void checkAccessibilityPrompt() {

        if (VamshiAccessibilityService.isRunning()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Enable Vamshi Assistant")
                .setMessage(
                        "Vamshi needs the Accessibility service to open apps, "
                                + "search YouTube, and read the screen.\n\n"
                                + "Tap ENABLE, find \"Vamshi AI\" in the list, "
                                + "and turn it ON.")
                .setPositiveButton("ENABLE", (dialog, which) -> {
                    try {
                        startActivity(new Intent(
                                Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    } catch (Exception ignored) {
                    }
                })
                .setNegativeButton("Later", null)
                .show();
    }

    /*
     * Asks Android to never battery-kill Vamshi.
     */
    private void requestIgnoreBatteryOptimization() {

        PowerManager pm =
                (PowerManager) getSystemService(POWER_SERVICE);

        if (pm == null) {
            return;
        }

        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {

            try {
                startActivity(new Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {
            }
        }
    }

    private void requestNeededPermissions() {

        ArrayList<String> needed = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.READ_CONTACTS);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.CALL_PHONE);
        }

        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    needed.toArray(new String[0]),
                    PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode,
                permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST) {

            boolean micGranted =
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED;

            if (!micGranted) {
                statusText.setText("Microphone permission needed for voice");
                statusText.setTextColor(0xFFFF7043);
            }
        }
    }

    /*
     * ================= CHAT =================
     */

    private void sendMessage() {

        String text = messageInput.getText().toString().trim();

        if (text.isEmpty()) {
            return;
        }

        messageInput.setText("");

        chatMessages.add(new ChatMessage(text, true));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();

        statusText.setText("Thinking...");
        statusText.setTextColor(0xFFFFD54F);

        askBackend(text);
    }

    /*
     * Brain flow:
     * Step 1: ask /action if this is an action command.
     * Step 2: if yes, run the action (and show it in chat).
     * Step 3: if no, show the normal /chat reply.
     */
    private void askBackend(String message) {

        new Thread(() -> {

            String actionJson = null;

            try {

                URL actionUrl = new URL(ACTION_URL);

                HttpURLConnection actionConn =
                        (HttpURLConnection) actionUrl.openConnection();

                actionConn.setRequestMethod("POST");
                actionConn.setRequestProperty("Content-Type", "application/json");
                actionConn.setDoOutput(true);
                actionConn.setConnectTimeout(45000);
                actionConn.setReadTimeout(45000);

                JSONObject actionBody = new JSONObject();
                actionBody.put("message", message);

                OutputStream aos = actionConn.getOutputStream();
                aos.write(actionBody.toString().getBytes("UTF-8"));
                aos.close();

                int actionStatus = actionConn.getResponseCode();

                if (actionStatus < 400) {

                    BufferedReader abr = new BufferedReader(
                            new InputStreamReader(actionConn.getInputStream()));

                    StringBuilder asb = new StringBuilder();
                    String aline;

                    while ((aline = abr.readLine()) != null) {
                        asb.append(aline);
                    }

                    abr.close();

                    JSONObject actionResp = new JSONObject(asb.toString());

                    if (actionResp.has("action")
                            && !actionResp.isNull("action")) {

                        actionJson =
                                actionResp.getJSONObject("action").toString();
                    }
                }

                actionConn.disconnect();

            } catch (Exception ignored) {
                // Fall through to normal chat.
            }

            if (actionJson != null) {

                final String actionFinal = actionJson;

                runOnUiThread(() -> {
                    runAiAction(actionFinal);
                    statusText.setText("Ready");
                    statusText.setTextColor(0xFF8BC34A);
                });

                return;
            }

            String reply;

            try {

                URL url = new URL(BACKEND_URL);

                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(45000);
                conn.setReadTimeout(45000);

                JSONObject body = new JSONObject();
                body.put("message", message);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                int statusCode = conn.getResponseCode();

                BufferedReader br = new BufferedReader(new InputStreamReader(
                        statusCode >= 400
                                ? conn.getErrorStream()
                                : conn.getInputStream()));

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                br.close();

                if (statusCode >= 400) {

                    try {
                        JSONObject respJson = new JSONObject(sb.toString());
                        reply = extractReply(respJson);
                    } catch (Exception e) {
                        reply = "Backend returned error code " + statusCode;
                    }

                } else {

                    JSONObject respJson = new JSONObject(sb.toString());
                    reply = extractReply(respJson);
                }

            } catch (java.net.SocketTimeoutException e) {
                reply = "The request timed out.";
            } catch (java.net.UnknownHostException e) {
                reply = "There is no internet connection.";
            } catch (Exception e) {
                reply = "Error: " + e.getClass().getSimpleName();
            }

            final String finalReply = reply;

            runOnUiThread(() -> {

                chatMessages.add(new ChatMessage(finalReply, false));
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                scrollToBottom();

                statusText.setText("Ready");
                statusText.setTextColor(0xFF8BC34A);
            });

        }).start();
    }

    /*
     * The backend sometimes returns reply as a string,
     * sometimes as an array of message chunks.
     */
    private String extractReply(JSONObject respJson) {

        try {

            Object replyObj = respJson.get("reply");

            if (replyObj instanceof JSONArray) {

                JSONArray arr = (JSONArray) replyObj;

                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < arr.length(); i++) {

                    Object part = arr.get(i);

                    if (part instanceof JSONObject) {
                        sb.append(((JSONObject) part)
                                .optString("text", part.toString()));
                    } else {
                        sb.append(part.toString());
                    }

                    if (i < arr.length() - 1) {
                        sb.append("\n");
                    }
                }

                return sb.toString();
            }

            return replyObj.toString();

        } catch (Exception e) {
            return respJson.optString("reply",
                    "Sorry, I could not get a reply.");
        }
    }

    /*
     * Runs an action decided by the AI brain by
     * forwarding it to the foreground service.
     */
    private void runAiAction(String actionJson) {

        try {

            JSONObject action = new JSONObject(actionJson);

            String type = action.optString("type", "");
            String contact = action.optString("contact", "");
            String msg = action.optString("message", "");
            String appName = action.optString("app", "");
            String destination = action.optString("destination", "");

            chatMessages.add(new ChatMessage(
                    "⚡ Action: " + type
                            + (contact.isEmpty() ? "" : " → " + contact)
                            + (appName.isEmpty() ? "" : " → " + appName)
                            + (destination.isEmpty() ? "" : " → " + destination),
                    false));
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            scrollToBottom();

            Intent intent =
                    new Intent(this, VamshiForegroundService.class);

            intent.putExtra("ai_type", type);
            intent.putExtra("ai_contact", contact);
            intent.putExtra("ai_message", msg);
            intent.putExtra("ai_app", appName);
            intent.putExtra("ai_destination", destination);

            ContextCompat.startForegroundService(this, intent);

        } catch (Exception e) {

            chatMessages.add(new ChatMessage(
                    "Action error: " + e.getClass().getSimpleName(), false));
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            scrollToBottom();
        }
    }

    private void scrollToBottom() {

        if (chatMessages.size() > 0) {
            chatRecyclerView.smoothScrollToPosition(
                    chatMessages.size() - 1);
        }
    }

    /*
     * ================= VOICE INPUT (mic button) =================
     */

    private void toggleMic() {

        if (micActive) {
            stopMic();
            return;
        }

        boolean hasMic = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;

        if (!hasMic) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSIONS_REQUEST);

            Toast.makeText(this,
                    "Microphone permission needed",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {

            Toast.makeText(this,
                    "Speech recognition not available on this phone",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        speechRecognizer =
                SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(this);

        Intent recognizerIntent =
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE, "en-US");

        micActive = true;
        statusText.setText("Listening...");
        statusText.setTextColor(0xFF42A5F5);

        speechRecognizer.startListening(recognizerIntent);
    }

    private void stopMic() {

        micActive = false;

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        statusText.setText("Ready");
        statusText.setTextColor(0xFF8BC34A);
    }

    @Override
    public void onResults(Bundle results) {

        ArrayList<String> matches = results.getStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION);

        if (matches == null || matches.isEmpty()) {
            stopMic();
            return;
        }

        String heard = matches.get(0);

        chatMessages.add(new ChatMessage(heard, true));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();

        statusText.setText("Thinking...");
        statusText.setTextColor(0xFFFFD54F);

        askBackend(heard);
    }

    @Override
    public void onError(int error) {
        stopMic();
        Toast.makeText(this, "Mic error: " + error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
    }

    @Override
    public void onEndOfSpeech() {
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
}
