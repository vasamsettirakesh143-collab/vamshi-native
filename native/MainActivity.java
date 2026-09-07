package com.vamshi.ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
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
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String BACKEND_URL =
            "https://vamshi-backend-y6ja.onrender.com/chat";
    private static final String ACTION_URL =
            "https://vamshi-backend-y6ja.onrender.com/action";

    private static final int REQUEST_MIC = 1;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton micButton;

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private boolean ttsReady = false;

    private final ArrayList<ChatMessage> chatMessages =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            setContentView(R.layout.activity_main);

            chatRecyclerView = findViewById(R.id.chatRecyclerView);
            messageInput = findViewById(R.id.messageInput);
            sendButton = findViewById(R.id.sendButton);
            micButton = findViewById(R.id.micButton);

            chatAdapter = new ChatAdapter(chatMessages);

            chatRecyclerView.setLayoutManager(
                    new LinearLayoutManager(this));

            chatRecyclerView.setAdapter(chatAdapter);

            chatMessages.add(new ChatMessage(
                    "Hello. I'm Vamshi — type or tap the mic to talk to me.",
                    false));

            chatAdapter.notifyDataSetChanged();

            initTextToSpeech();

            sendButton.setOnClickListener(v -> {

                String text = messageInput.getText()
                        .toString()
                        .trim();

                if (!text.isEmpty()) {
                    messageInput.setText("");
                    handleUserInput(text);
                }
            });

            micButton.setOnClickListener(v -> {

                boolean hasMic =
                        ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED;

                if (!hasMic) {

                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            REQUEST_MIC
                    );

                    return;
                }

                startVoiceInput();
            });

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.READ_CONTACTS,
                                Manifest.permission.CALL_PHONE
                        },
                        REQUEST_MIC
                );
            }

            startForegroundService();

        } catch (Throwable e) {

            showCrash(e);
        }
    }

    private void showCrash(Throwable e) {

        StringBuilder sb = new StringBuilder();

        sb.append("STARTUP ERROR:\n")
          .append(e.getClass().getName())
          .append("\n")
          .append(e.getMessage())
          .append("\n");

        if (e.getCause() != null) {
            sb.append("CAUSED BY:\n")
              .append(e.getCause().toString());
        }

        StackTraceElement[] stack = e.getStackTrace();

        if (stack.length > 0) {
            sb.append("\nAT: ")
              .append(stack[0].toString());
        }

        try {

            super.setContentView(
                    new TextView(this));

            TextView errorView = new TextView(this);

            errorView.setText(sb.toString());
            errorView.setTextColor(0xFFFF0000);
            errorView.setPadding(40, 80, 40, 40);
            errorView.setTextIsSelectable(true);
            errorView.setTextSize(14);

            super.setContentView(errorView);

        } catch (Throwable ignore) {
        }
    }

    private void startForegroundService() {

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(
                        new Intent(this, VamshiForegroundService.class));
            } else {
                startService(
                        new Intent(this, VamshiForegroundService.class));
            }

        } catch (Exception e) {

            Toast.makeText(this,
                    "Service error: "
                            + e.getClass().getSimpleName(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void initTextToSpeech() {

        textToSpeech = new TextToSpeech(this, status -> {

            if (status == TextToSpeech.SUCCESS
                    && textToSpeech != null) {

                textToSpeech.setLanguage(Locale.US);
                ttsReady = true;
            }
        });
    }

    private void startVoiceInput() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this,
                    "Speech recognition not available",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (speechRecognizer == null) {

            speechRecognizer =
                    SpeechRecognizer.createSpeechRecognizer(this);

            speechRecognizer.setRecognitionListener(
                    new RecognitionListener() {

                        @Override
                        public void onResults(Bundle results) {

                            ArrayList<String> matches =
                                    results.getStringArrayList(
                                            SpeechRecognizer.RESULTS_RECOGNITION);

                            if (matches != null
                                    && !matches.isEmpty()) {

                                String heard =
                                        matches.get(0).trim();

                                if (!heard.isEmpty()) {
                                    handleUserInput(heard);
                                }
                            }
                        }

                        @Override
                        public void onError(int error) {
                            Toast.makeText(MainActivity.this,
                                    "Didn't catch that, try again",
                                    Toast.LENGTH_SHORT).show();
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
                    });
        }

        Intent intent =
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.US);

        speechRecognizer.startListening(intent);
    }

    private void handleUserInput(String text) {

        chatMessages.add(new ChatMessage(text, true));
        chatAdapter.notifyItemInserted(
                chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(
                chatMessages.size() - 1);

        runOnUiThread(() ->
                new Thread(() -> {

                    String actionJson = null;

                    try {

                        URL actionUrl = new URL(ACTION_URL);

                        HttpURLConnection actionConn =
                                (HttpURLConnection)
                                        actionUrl.openConnection();

                        actionConn.setRequestMethod("POST");
                        actionConn.setRequestProperty(
                                "Content-Type", "application/json");
                        actionConn.setDoOutput(true);
                        actionConn.setConnectTimeout(45000);
                        actionConn.setReadTimeout(45000);

                        JSONObject actionBody = new JSONObject();
                        actionBody.put("message", text);

                        OutputStream aos =
                                actionConn.getOutputStream();

                        aos.write(actionBody.toString()
                                .getBytes("UTF-8"));
                        aos.close();

                        int actionStatus =
                                actionConn.getResponseCode();

                        if (actionStatus < 400) {

                            BufferedReader abr =
                                    new BufferedReader(
                                            new InputStreamReader(
                                                    actionConn.getInputStream()));

                            StringBuilder asb = new StringBuilder();
                            String aline;

                            while ((aline = abr.readLine()) != null) {
                                asb.append(aline);
                            }

                            abr.close();

                            JSONObject actionResp =
                                    new JSONObject(asb.toString());

                            if (actionResp.has("action")
                                    && !actionResp.isNull("action")) {

                                actionJson =
                                        actionResp.getJSONObject("action")
                                                .toString();
                            }
                        }

                        actionConn.disconnect();

                    } catch (Exception ignored) {
                    }

                    if (actionJson != null) {

                        final String actionFinal = actionJson;

                        runOnUiThread(() -> {

                            runAiAction(actionFinal);
                            addBotMessage("On it! 👍");
                        });

                        return;
                    }

                    String reply;

                    try {

                        URL url = new URL(BACKEND_URL);

                        HttpURLConnection conn =
                                (HttpURLConnection)
                                        url.openConnection();

                        conn.setRequestMethod("POST");
                        conn.setRequestProperty(
                                "Content-Type", "application/json");
                        conn.setDoOutput(true);
                        conn.setConnectTimeout(45000);
                        conn.setReadTimeout(45000);

                        JSONObject body = new JSONObject();
                        body.put("message", text);

                        OutputStream os = conn.getOutputStream();
                        os.write(body.toString().getBytes("UTF-8"));
                        os.close();

                        int statusCode = conn.getResponseCode();

                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(
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
                            reply = "Backend error code " + statusCode;
                        } else {
                            JSONObject respJson =
                                    new JSONObject(sb.toString());
                            reply = respJson.optString("reply",
                                    "Sorry, I could not get a reply.");
                        }

                    } catch (java.net.SocketTimeoutException e) {
                        reply = "The request timed out.";
                    } catch (java.net.UnknownHostException e) {
                        reply = "No internet connection.";
                    } catch (Exception e) {
                        reply = "Error: "
                                + e.getClass().getSimpleName();
                    }

                    final String finalReply = reply;

                    runOnUiThread(() -> addBotMessage(finalReply));

                }).start());
    }

    private void runAiAction(String actionJson) {

        try {

            JSONObject action = new JSONObject(actionJson);

            String type = action.optString("type", "");
            String contact = action.optString("contact", "");
            String msg = action.optString("message", "");
            String appName = action.optString("app", "");
            String destination = action.optString("destination", "");

            Intent serviceIntent =
                    new Intent(this, VamshiForegroundService.class);

            serviceIntent.putExtra("ai_type", type);
            serviceIntent.putExtra("ai_contact", contact);
            serviceIntent.putExtra("ai_message", msg);
            serviceIntent.putExtra("ai_app", appName);
            serviceIntent.putExtra("ai_destination", destination);

            startService(serviceIntent);

        } catch (Exception e) {
            addBotMessage("Action error: "
                    + e.getClass().getSimpleName());
        }
    }

    private void addBotMessage(String text) {

        chatMessages.add(new ChatMessage(text, false));
        chatAdapter.notifyItemInserted(
                chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(
                chatMessages.size() - 1);

        speak(text);
    }

    private void speak(String text) {

        if (ttsReady && textToSpeech != null) {
            textToSpeech.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "vamshi_chat_utterance");
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
    }
}
