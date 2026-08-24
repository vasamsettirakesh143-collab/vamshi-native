const chatMessages = document.getElementById("chatMessages");
const statusEl = document.getElementById("status");
const textInput = document.getElementById("textInput");
const micBtn = document.getElementById("micBtn");
const sendBtn = document.getElementById("sendBtn");

function addBubble(sender, text, isThinking) {
    const bubble = document.createElement("div");
    bubble.className = "bubble " + sender + (isThinking ? " thinking" : "");
    bubble.textContent = text;
    chatMessages.appendChild(bubble);
    chatMessages.scrollTop = chatMessages.scrollHeight;
    return bubble;
}

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
    if (!tts) return;
    try {
        await tts.speak({ text: String(text), lang: "en-US", rate: 1.0, pitch: 1.0, volume: 1.0 });
    } catch (err) {
        console.error(err);
    }
}

async function sendToVamshi(text) {
    if (!text || !text.trim()) return;

    addBubble("user", text);
    statusEl.textContent = "Thinking...";

    const thinkingBubble = addBubble("assistant", "Vamshi is typing...", true);

    let reply;
    try {
        reply = await VamshiBrain(text.toLowerCase());
    } catch (err) {
        reply = "Sorry, something went wrong.";
    }

    thinkingBubble.remove();
    addBubble("assistant", String(reply));
    statusEl.textContent = "Ready";

    speak(String(reply));
}

sendBtn.addEventListener("click", () => {
    const text = textInput.value;
    textInput.value = "";
    sendToVamshi(text);
});

textInput.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
        const text = textInput.value;
        textInput.value = "";
        sendToVamshi(text);
    }
});

micBtn.addEventListener("click", async () => {
    const stt = getSTT();

    if (!stt) {
        statusEl.textContent = "Voice input not available";
        return;
    }

    try {
        const permission = await stt.requestPermissions();
        if (permission.speechRecognition !== "granted") {
            statusEl.textContent = "Microphone permission denied";
            return;
        }

        micBtn.classList.add("listening");
        statusEl.textContent = "Listening...";

        const result = await stt.start({
            language: "en-US",
            maxResults: 1,
            partialResults: false,
            popup: false,
        });

        micBtn.classList.remove("listening");
        statusEl.textContent = "Ready";

        const text = result && result.matches && result.matches[0] ? result.matches[0] : "";
        if (text) {
            sendToVamshi(text);
        }

    } catch (error) {
        micBtn.classList.remove("listening");
        statusEl.textContent = "Ready";
        console.error(error);
    }
});

// Opening greeting
addBubble("assistant", "Hello Rakesh. I'm Vamshi — type or tap the mic to talk to me.");
