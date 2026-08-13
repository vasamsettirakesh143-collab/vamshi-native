const startBtn = document.getElementById("startBtn");
const listenBtn = document.getElementById("listenBtn");

const status = document.getElementById("status");
const userText = document.getElementById("userText");

function speak(text) {

    if (!text) return;

    window.speechSynthesis.cancel();

    const speech = new SpeechSynthesisUtterance(String(text));

    speech.lang = "en-US";
    speech.rate = 1;
    speech.pitch = 1;

    speech.onstart = () => {
        status.innerText = "Vamshi speaking...";
    };

    speech.onend = () => {
        status.innerText = "Vamshi ready";
    };

    window.speechSynthesis.speak(speech);
}

startBtn.addEventListener("click", () => {

    status.innerText = "Vamshi is online";

    speak("Hello Rakesh. Vamshi is online.");

});

const SpeechRecognitionImpl =
    window.SpeechRecognition || window.webkitSpeechRecognition;

listenBtn.addEventListener("click", () => {

    if (!SpeechRecognitionImpl) {

        status.innerText = "Voice recognition not supported in this browser";

        speak("Sorry Rakesh, this browser does not support voice recognition.");

        return;
    }

    const recognition = new SpeechRecognitionImpl();

    recognition.lang = "en-US";

    recognition.start();

    recognition.onstart = () => {
        status.innerText = "Listening...";
    };

    recognition.onresult = async (event) => {

        try {

            const text = event.results[0][0].transcript;

            userText.innerText = "You: " + text;

            status.innerText = "Thinking...";

            const reply = await VamshiBrain(text);

            userText.innerText =
                "You: " + text + "\n\nVamshi: " + String(reply);

            speak(String(reply));

        } catch (error) {

            console.error(error);

            speak("Sorry, something went wrong.");
        }
    };
