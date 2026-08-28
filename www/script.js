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
    const format = text.includes("webp")
        ? "webp"
        : text.includes("jpg") || text.includes("jpeg")
            ? "jpeg"
            : text.includes("png")
                ? "png"
                : "png";

    if (text.includes("black and white") ||
        text.includes("black-and-white") ||
        text.includes("grayscale") ||
        text.includes("greyscale")) {
        return { type: "grayscale", format: "png" };
    }

    if (text.includes("brightness") ||
        text.includes("brighter") ||
        text.includes("darker")) {
        return { type: "brightness", format: format };
    }

    if (text.includes("contrast")) {
        return { type: "contrast", format: format };
    }

    if (text.includes("crop")) {
        return { type: "crop", format: format };
    }

    if (text.includes("compress") ||
        text.includes("smaller") ||
        text.includes("reduce size")) {
        return { type: "compress", format: "jpeg" };
    }

    if (text.includes("rotate")) {
        return { type: "rotate", format: format };
    }

    if (text.includes("resize") || text.includes("size")) {
        return { type: "resize", format: format };
    }

    if (text.includes("webp") ||
        text.includes("jpg") ||
        text.includes("jpeg") ||
        text.includes("png")) {
        return { type: "format", format: format };
    }

    return null;
}

function getResizeSize(instruction, originalWidth, originalHeight) {
    const match = instruction.match(
        /(?:resize|size).*?(\d{2,4})(?:\s*x\s*(\d{2,4}))?/i
    );

    if (!match) {
        const maxSide = 1200;

        if (Math.max(originalWidth, originalHeight) <= maxSide) {
            return {
                width: originalWidth,
                height: originalHeight
            };
        }

        if (originalWidth >= originalHeight) {
            return {
                width: maxSide,
                height: Math.round(originalHeight * maxSide / originalWidth)
            };
        }

        return {
            width: Math.round(originalWidth * maxSide / originalHeight),
            height: maxSide
        };
    }

    const width = Number(match[1]);
    const height = match[2]
        ? Number(match[2])
        : Math.round(originalHeight * width / originalWidth);

    return { width, height };
}

function getDegrees(instruction) {
    const match = instruction.match(/(90|180|270)/);
    return match ? Number(match[1]) : 90;
}

function getAdjustment(instruction, type) {
    const match = instruction.match(new RegExp(type + "\\s*(-?\\d+)", "i"));

    if (match) {
        return Math.max(-100, Math.min(100, Number(match[1])));
    }

    if (type === "brightness") {
        return instruction.toLowerCase().includes("darker") ? -25 : 25;
    }

    return 25;
}

function processImage(file, instruction) {
    return new Promise((resolve, reject) => {
        if (!file || !file.type.startsWith("image/")) {
            reject(new Error("Please select a photo."));
            return;
        }

        const operation = getOperation(instruction);

        if (!operation) {
            reject(new Error(
                "Try: resize, crop, rotate, grayscale, brightness, contrast, compress, or convert to PNG/JPG/WebP."
            ));
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
                        ? getResizeSize(instruction, image.width, image.height)
                        : {
                            width: image.width,
                            height: image.height
                        };

                    let width = size.width;
                    let height = size.height;

                    if (operation.type === "crop") {
                        const side = Math.min(width, height);
                        width = side;
                        height = side;
                    }

                    const degrees = operation.type === "rotate"
                        ? getDegrees(instruction)
                        : 0;

                    const sideways = degrees === 90 || degrees === 270;
                    const canvas = document.createElement("canvas");

                    canvas.width = sideways ? height : width;
                    canvas.height = sideways ? width : height;

                    const context = canvas.getContext("2d");

                    if (!context) {
                        throw new Error(
                            "Image processing is unavailable on this device."
                        );
                    }

                    if (operation.type === "brightness") {
                        context.filter = "brightness(" +
                            (100 + getAdjustment(instruction, "brightness")) +
                            "%)";
                    }

                    if (operation.type === "contrast") {
                        context.filter = "contrast(" +
                            (100 + getAdjustment(instruction, "contrast")) +
                            "%)";
                    }

                    if (operation.type === "grayscale") {
                        context.filter = "grayscale(100%)";
                    }

                    context.translate(canvas.width / 2, canvas.height / 2);
                    context.rotate(degrees * Math.PI / 180);

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
                    }

                    context.drawImage(
                        image,
                        sourceX,
                        sourceY,
                        sourceWidth,
                        sourceHeight,
                        -width / 2,
                        -height / 2,
                        width,
                        height
                    );

                    const mime = operation.format === "jpeg"
                        ? "image/jpeg"
                        : "image/" + operation.format;

                    const quality = operation.type === "compress"
                        ? 0.6
                        : 0.9;

                    const dataUrl = canvas.toDataURL(mime, quality);
                    const baseName = file.name.replace(/\.[^/.]+$/, "") ||
                        "vamshi-image";
                    const extension = operation.format === "jpeg"
                        ? "jpg"
                        : operation.format;

                    resolve({
                        dataUrl: dataUrl,
                        filename: baseName + "." + extension
                    });
                } catch (error) {
                    reject(error);
                }
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

    const request = instruction.trim() || "resize the image";
    addBubble("user", request + "\nAttached: " + selectedFile.name);
    statusEl.textContent = "Processing on this device...";

    try {
        const result = await processImage(selectedFile, request);
        addImageResult(result.dataUrl, result.filename);
        statusEl.textContent = "Ready";
    } catch (error) {
        console.error(error);
        addBubble(
            "assistant",
            error.message || "The photo could not be processed."
        );
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
    addBubble(
        "user",
        "Attached: " + selectedFile.name +
        "\nType an instruction and tap Send."
    );
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
            if (selectedFile) {
                processSelectedImage(text);
            } else {
                sendToVamshi(text);
            }
        }
    } catch (error) {
        micBtn.classList.remove("listening");
        statusEl.textContent = "Ready";
        console.error(error);
    }
});

addBubble(
    "assistant",
    "Hello Rakesh. I'm Vamshi — type or tap the mic to talk to me."
);
