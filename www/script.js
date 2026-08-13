const startBtn = document.getElementById("startBtn");
const listenBtn = document.getElementById("listenBtn");

const status = document.getElementById("status");
const userText = document.getElementById("userText");

function getTTS() {
    return window.Capacitor && window.Capacitor.Plugins
        ? window.Capacitor.Plugins.TextToSpeech
        : null;
}

function getSTT() {
    return window.Capacitor && window.Capacitor.Plugins
        ? window.Capacitor.Plugins.SpeechRecognition
        : null;
}

async function speak(text) {

    if (!text) return;

    const tts = getTTS();

    if (!tts) {
        status.innerText = "Voice output not available";
        return;
    }

    status.innerText = "Vamshi speaking...";

    try {
        await tts.speak({
            text: String(text),
            lang: "en-US",
            rate: 1.0,
            pitch: 1.0,
            volume: 1.0,
        });
    } catch (err) {
        console.error(err);
    }

    status.innerText = "Vamshi ready";

}

startBtn.addEventListener("click", () => {

    status.innerText = "Vamshi is online";

    speak("Hello Rakesh. Vamshi is online.");

});

listenBtn.addEventListener("click", async () => {

    const stt = getSTT();

    if (!stt) {
        status.innerText = "Voice input not available";
        speak("Sorry Rakesh, voice input is not available.");
        return;
    }

    try {

        const permission = await stt.requestPermissions();

        if (permission.speechRecognition !== "granted") {
            status.innerText = "Microphone permission denied";
            speak("Sorry Rakesh, I need microphone permission.");
            return;
        }

        status.innerText = "Listening...";

        const result = await stt.start({
            language: "en-US",
            maxResults: 1,
            partialResults: false,
            popup: false,
        });

        const text = result && result.matches && result.matches[0]
            ? result.matches[0]
            : "";

        if (!text) {
            status.innerText = "Didn't catch that";
            return;
        }

        userText.innerText = "You: " + text;

        status.innerText = "Thinking...";

        const reply = await VamshiBrain(text.toLowerCase());

        userText.innerText =
            "You: " + text + "\n\nVamshi: " + String(reply);

        await speak(String(reply));

    } catch (error) {

        console.error(error);

        status.innerText = "Voice error";

        speak("Sorry Rakesh. I could not hear you.");

    }

});
