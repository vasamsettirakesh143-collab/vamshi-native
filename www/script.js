const chatMessages = document.getElementById("chatMessages");
const statusEl = document.getElementById("status");
const textInput = document.getElementById("textInput");
const micBtn = document.getElementById("micBtn");
const sendBtn = document.getElementById("sendBtn");
const attachBtn = document.getElementById("attachBtn");
const fileInput = document.getElementById("fileInput");

let selectedFile = null;
fileInput.accept = "image/*";

function addBubble(sender, text, isThinking = false) {
    const bubble = document.createElement("div");
    bubble.className = "bubble " + sender + (isThinking ? " thinking" : "");
    bubble.textContent = text;
    chatMessages.appendChild(bubble);
    chatMessages.scrollTop = chatMessages.scrollHeight;
    return bubble;
}

function addImageResult(dataUrl, filename) {
    const bubble = document.createElement("div");
    bubble.className = "bubble assistant";

    const image = document.createElement("img");
    image.src = dataUrl;
    image.alt = "Processed image";
    image.style.maxWidth = "100%";
    image.style.borderRadius = "12px";
    image.style.display = "block";

    const link = document.createElement("a");
    link.href = dataUrl;
    link.download = filename;
    link.textContent = "Download image";
    link.style.display = "inline-block";
    link.style.marginTop = "8px";

    bubble.appendChild(image);
    bubble.appendChild(link);
    chatMessages.appendChild(bubble);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function getTTS() {
    return window.Capacitor?.Plugins?.TextToSpeech || null;
}

function getSTT() {
    return window.Capacitor?.Plugins?.SpeechRecognition || null;
}

async function speak(text) {
    const tts = getTTS();
    if (!tts || !text) return;

    try {
        await tts.speak({
            text: String(text),
            lang: "en-US",
            rate: 1,
            pitch: 1,
            volume: 1
        });
    } catch (error) {
        console.error(error);
    }
}

async function sendToVamshi(text) {
    if (!text || !text.trim()) return;

    addBubble("user", text);
    statusEl.textContent = "Thinking...";
    const thinking = addBubble("assistant", "Vamshi is typing...", true);

    let reply;
    try {
        reply = await VamshiBrain(text.toLowerCase());
    } catch (error) {
        console.error(error);
        reply = "Sorry, something went wrong.";
    }

    thinking.remove();
    addBubble("assistant", String(reply));
    statusEl.textContent = "Ready";
    speak(String(reply));
}

function getOperation(instruction) {
    const text = instruction.toLowerCase();

    if (text.includes("webp")) return { type: "format", format: "webp" };
    if (text.includes("jpg") || text.includes("jpeg")) return { type: "format", format: "jpeg" };
    if (text.includes("png")) return { type: "format", format: "png" };
    if (text.includes("compress") || text.includes("smaller")) return { type: "compress", format: "jpeg" };
    if (text.includes("rotate")) return { type: "rotate", format: "png" };
    if (text.includes("resize") || text.includes("size")) return { type: "resize", format: "png" };

    return null;
}

function processImage(file, instruction) {
    return new Promise((resolve, reject) => {
        if (!file || !file.type.startsWith("image/")) {
            reject(new Error("Please select a photo."));
            return;
        }

        const operation = getOperation(instruction);
        if (!operation) {
            reject(new Error("Try: convert to PNG, convert to JPG, convert to WebP, resize, rotate, or compress."));
            return;
        }

        const reader = new FileReader();
        reader.onerror = () => reject(new Error("The photo could not be read."));

        reader.onload = () => {
            const image = new Image();
            image.onerror = () => reject(new Error("This photo could not be opened."));

            image.onload = () => {
                const canvas = document.createElement("canvas");
                let width = image.width;
                let height = image.height;

                const resizeMatch = instruction.match(/(?:resize|size).*?(\d{2,4})(?:\s*x\s*(\d{2,4}))?/i);
                if (operation.type === "resize" && resizeMatch) {
                    width = Number(resizeMatch[1]);
                    height = resizeMatch[2]
                        ? Number(resizeMatch[2])
                        : Math.round(image.height * width / image.width);
                }

                let degrees = 0;
                if (operation.type === "rotate") {
                    const rotateMatch = instruction.match(/(90|180|270)/);
                    degrees = rotateMatch ? Number(rotateMatch[1]) : 90;
                }

                const sideways = degrees === 90 || degrees === 270;
                canvas.width = sideways ? height : width;
                canvas.height = sideways ? width : height;

                const context = canvas.getContext("2d");
                if (!context) {
                    reject(new Error("Image processing is unavailable on this device."));
                    return;
                }

                context.translate(canvas.width / 2, canvas.height / 2);
                context.rotate(degrees * Math.PI / 180);
                context.drawImage(image, -width / 2, -height / 2, width, height);

                const mime = operation.format === "jpeg"
                    ? "image/jpeg"
                    : "image/" + operation.format;
                const quality = operation.type === "compress" ? 0.65 : 0.9;
                const dataUrl = canvas.toDataURL(mime, quality);
                const baseName = file.name.replace(/\.[^/.]+$/, "") || "vamshi-image";
                const extension = operation.format === "jpeg" ? "jpg" : operation.format;

                resolve({
                    dataUrl: dataUrl,
                    filename: baseName + "." + extension
                });
            };

            image.src = reader.result;
        };

        reader.readAsDataURL(file);
    });
}

async function processSelectedImage(instruction) {
    if (!selectedFile) {
        addBubble("assistant", "Please tap + and select a photo first.");
        return;
    }

    const request = instruction.trim() || "convert to PNG";
    addBubble("user", request + "\nAttached: " + selectedFile.name);
    statusEl.textContent = "Processing on this device...";

    try {
        const result = await processImage(selectedFile, request);
        addImageResult(result.dataUrl, result.filename);
        statusEl.textContent = "Ready";
    } catch (error) {
        console.error(error);
        addBubble("assistant", error.message || "The photo could not be processed.");
        statusEl.textContent = "Ready";
    }

    selectedFile = null;
    fileInput.value = "";
}

function submitMessage() {
    const text = textInput.value.trim();
    textInput.value = "";

    if (selectedFile) {
        processSelectedImage(text);
    } else if (text) {
        sendToVamshi(text);
    }
}

attachBtn.addEventListener("click", () => fileInput.click());

fileInput.addEventListener("change", () => {
    if (!fileInput.files || !fileInput.files.length) return;

    selectedFile = fileInput.files[0];
    statusEl.textContent = "Selected: " + selectedFile.name;
    addBubble("user", "Attached: " + selectedFile.name + "\nType an instruction and tap Send.");
});

sendBtn.addEventListener("click", submitMessage);

textInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter") submitMessage();
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
            popup: false
        });

        micBtn.classList.remove("listening");
        statusEl.textContent = "Ready";
        const text = result?.matches?.[0] || "";

        if (text) {
            if (selectedFile) processSelectedImage(text);
            else sendToVamshi(text);
        }
    } catch (error) {
        micBtn.classList.remove("listening");
        statusEl.textContent = "Ready";
        console.error(error);
    }
});

addBubble("assistant", "Hello Rakesh. I'm Vamshi — type or tap the mic to talk to me.");
