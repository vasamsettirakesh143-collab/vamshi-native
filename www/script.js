const chatMessages = document.getElementById("chatMessages");
const statusEl = document.getElementById("status");
const textInput = document.getElementById("textInput");
const micBtn = document.getElementById("micBtn");
const sendBtn = document.getElementById("sendBtn");
const attachBtn = document.getElementById("attachBtn");
const fileInput = document.getElementById("fileInput");

const BACKEND_URL = "https://vamshi-backend-y6ja.onrender.com";
let selectedFile = null;
fileInput.accept = "image/*";

function addBubble(sender, text, thinking = false ) {
    const bubble = document.createElement("div");
    bubble.className = "bubble " + sender + (thinking ? " thinking" : "");
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
        await tts.speak({ text: String(text), lang: "en-US", rate: 1, pitch: 1, volume: 1 });
    } catch (error) {
        console.error(error);
    }
}

async function sendToVamshi(text) {
    if (!text || !text.trim()) return;

    addBubble("user", text);
    statusEl.textContent = "Thinking...";
    const thinking = addBubble("assistant", "Vamshi is typing...", true);

    try {
        const reply = await VamshiBrain(text.toLowerCase());
        thinking.remove();
        addBubble("assistant", String(reply));
        speak(String(reply));
    } catch (error) {
        console.error(error);
        thinking.remove();
        addBubble("assistant", "Sorry, something went wrong.");
    }

    statusEl.textContent = "Ready";
}

function getLocalOperation(instruction) {
    const text = instruction.toLowerCase();
    const format = text.includes("webp")
        ? "webp"
        : text.includes("jpg") || text.includes("jpeg")
            ? "jpeg"
            : "png";

    if (text.includes("black and white") || text.includes("black-and-white") || text.includes("grayscale") || text.includes("greyscale")) {
        return { type: "grayscale", format: "png" };
    }
    if (text.includes("brightness") || text.includes("brighter") || text.includes("darker")) {
        return { type: "brightness", format: format };
    }
    if (text.includes("contrast")) return { type: "contrast", format: format };
    if (text.includes("crop")) return { type: "crop", format: format };
    if (text.includes("compress") || text.includes("smaller") || text.includes("reduce size")) {
        return { type: "compress", format: "jpeg" };
    }
    if (text.includes("rotate")) return { type: "rotate", format: format };
    if (text.includes("resize") || text.includes("size")) return { type: "resize", format: format };
    if (text.includes("png") || text.includes("jpg") || text.includes("jpeg") || text.includes("webp")) {
        return { type: "format", format: format };
    }
    return null;
}

function resizeDimensions(instruction, width, height) {
    const match = instruction.match(/(?:resize|size).*?(\d{2,4})(?:\s*x\s*(\d{2,4}))?/i);

    if (!match) {
        const maxSide = 1200;
        if (Math.max(width, height) <= maxSide) return { width, height };
        if (width >= height) return { width: maxSide, height: Math.round(height * maxSide / width) };
        return { width: Math.round(width * maxSide / height), height: maxSide };
    }

    const newWidth = Number(match[1]);
    const newHeight = match[2]
        ? Number(match[2])
        : Math.round(height * newWidth / width);

    return { width: newWidth, height: newHeight };
}

function adjustment(instruction, type) {
    const match = instruction.match(new RegExp(type + "\\s*(-?\\d+)", "i"));
    if (match) return Math.max(-100, Math.min(100, Number(match[1])));
    if (type === "brightness" && instruction.toLowerCase().includes("darker")) return -25;
    return 25;
}

function processImage(file, instruction) {
    return new Promise((resolve, reject) => {
        if (!file || !file.type.startsWith("image/")) {
            reject(new Error("Please select a photo."));
            return;
        }

        const operation = getLocalOperation(instruction);
        if (!operation) {
            reject(new Error("This request will be sent to Gemini, not local editing."));
            return;
        }

        const reader = new FileReader();
        reader.onerror = () => reject(new Error("The photo could not be read."));

        reader.onload = () => {
            const image = new Image();
            image.onerror = () => reject(new Error("This photo could not be opened."));

            image.onload = () => {
                try {
                    const size = operation.type === "resize"
                        ? resizeDimensions(instruction, image.width, image.height)
                        : { width: image.width, height: image.height };

                    let width = size.width;
                    let height = size.height;
                    let sourceX = 0;
                    let sourceY = 0;
                    let sourceWidth = image.width;
                    let sourceHeight = image.height;

                    if (operation.type === "crop") {
                        const side = Math.min(image.width, image.height);
                        sourceX = (image.width - side) / 2;
                        sourceY = (image.height - side) / 2;
                        sourceWidth = side;
                        sourceHeight = side;
                        width = side;
                        height = side;
                    }

                    const rotateMatch = instruction.match(/(90|180|270)/);
                    const degrees = operation.type === "rotate"
                        ? Number(rotateMatch ? rotateMatch[1] : 90)
                        : 0;
                    const sideways = degrees === 90 || degrees === 270;

                    const canvas = document.createElement("canvas");
                    canvas.width = sideways ? height : width;
                    canvas.height = sideways ? width : height;

                    const context = canvas.getContext("2d");
                    if (!context) throw new Error("Image processing is unavailable on this device.");

                    if (operation.type === "grayscale") context.filter = "grayscale(100%)";
                    if (operation.type === "brightness") context.filter = "brightness(" + (100 + adjustment(instruction, "brightness")) + "%)";
                    if (operation.type === "contrast") context.filter = "contrast(" + (100 + adjustment(instruction, "contrast")) + "%)";

                    context.translate(canvas.width / 2, canvas.height / 2);
                    context.rotate(degrees * Math.PI / 180);
                    context.drawImage(image, sourceX, sourceY, sourceWidth, sourceHeight, -width / 2, -height / 2, width, height);

                    const mime = operation.format === "jpeg" ? "image/jpeg" : "image/" + operation.format;
                    const quality = operation.type === "compress" ? 0.6 : 0.9;
                    const dataUrl = canvas.toDataURL(mime, quality);
                    const baseName = file.name.replace(/\.[^/.]+$/, "") || "vamshi-image";
                    const extension = operation.format === "jpeg" ? "jpg" : operation.format;

                    resolve({ dataUrl, filename: baseName + "." + extension });
                } catch (error) {
                    reject(error);
                }
            };

            image.src = reader.result;
        };

        reader.readAsDataURL(file);
    });
}

async function askGeminiAboutImage(file, question) {
    const reader = new FileReader();

    return new Promise((resolve, reject) => {
        reader.onerror = () => reject(new Error("The image could not be read."));

        reader.onload = async () => {
            try {
                const response = await fetch(BACKEND_URL + "/vision", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        message: question || "Describe this image in detail.",
                        image: {
                            data: reader.result,
                            mimeType: file.type
                        }
                    })
                });

                const data = await response.json();

                if (!response.ok) {
                    reject(new Error(data.reply || "Gemini could not process this image."));
                    return;
                }

                resolve(data.reply || "I could not understand this image.");
            } catch (error) {
                console.error(error);
                reject(new Error("Could not connect to Gemini image understanding."));
            }
        };

        reader.readAsDataURL(file);
    });
}

async function processSelectedImage(instruction) {
    if (!selectedFile) {
        addBubble("assistant", "Please tap + and select a photo first.");
        return;
    }

    const request = instruction.trim();
    const localOperation = getLocalOperation(request);
    const question = request || "Describe this image in detail.";

    addBubble("user", question + "\nAttached: " + selectedFile.name);
    statusEl.textContent = localOperation
        ? "Processing on this device..."
        : "Vamshi is looking at the image...";

    try {
        if (localOperation) {
            const result = await processImage(selectedFile, request);
            addImageResult(result.dataUrl, result.filename);
        } else {
            const reply = await askGeminiAboutImage(selectedFile, question);
            addBubble("assistant", String(reply));
            speak(String(reply));
        }
    } catch (error) {
        console.error(error);
        addBubble("assistant", error.message || "The image could not be processed.");
    }

    selectedFile = null;
    fileInput.value = "";
    statusEl.textContent = "Ready";
}

function submitMessage() {
    const text = textInput.value.trim();
    textInput.value = "";

    if (selectedFile) processSelectedImage(text);
    else if (text) sendToVamshi(text);
}

attachBtn.addEventListener("click", () => fileInput.click());

fileInput.addEventListener("change", () => {
    if (!fileInput.files || !fileInput.files.length) return;
    selectedFile = fileInput.files[0];
    statusEl.textContent = "Selected: " + selectedFile.name;
    addBubble("user", "Attached: " + selectedFile.name + "\nAsk a question or type an editing command, then tap Send.");
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
