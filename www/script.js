// --- LIVE CAMERA VISION SYSTEM ---
const toggleCamBtn = document.getElementById("toggleCamBtn");
const cameraContainer = document.getElementById("cameraContainer");
const videoElement = document.getElementById("liveCamera");
const canvasElement = document.getElementById("snapshotCanvas");

let cameraStream = null;

// Start camera stream
async function startCamera() {
    try {
        cameraStream = await navigator.mediaDevices.getUserMedia({ 
            video: { facingMode: "user" }, // "user" = front camera looking at you
            audio: false 
        });
        videoElement.srcObject = cameraStream;
        cameraContainer.style.display = "block";
        statusEl.textContent = "Camera active (Vamshi can see you)";
    } catch (err) {
        console.error("Camera access error:", err);
        statusEl.textContent = "Camera permission denied";
    }
}

// Stop camera stream
function stopCamera() {
    if (cameraStream) {
        cameraStream.getTracks().forEach(track => track.stop());
        cameraStream = null;
    }
    cameraContainer.style.display = "none";
    statusEl.textContent = "Ready";
}

// Toggle camera with the 👁️ button
toggleCamBtn.addEventListener("click", () => {
    if (cameraStream) {
        stopCamera();
        toggleCamBtn.classList.remove("listening");
    } else {
        startCamera();
        toggleCamBtn.classList.add("listening");
    }
});

// Automatically take a snapshot from the video feed and convert to base64
function captureLiveSnapshot() {
    if (!cameraStream) return null;
    
    canvasElement.width = videoElement.videoWidth || 640;
    canvasElement.height = videoElement.videoHeight || 480;
    const ctx = canvasElement.getContext("2d");
    ctx.drawImage(videoElement, 0,Element.toDataURL("image/jpeg", 0.8),
        mimeType: "image/jpeg"
    };
}

// Override sendToVamshi to include live camera frame if camera is open!
const originalSendToVamshi = sendToVamshi;
sendToVamshi = async function(text) {
    if (!text || !text.trim()) return;

    // If the live camera is turned on, capture what the camera sees and send to /vision
    if (cameraStream) {
        const snapshot = captureLiveSnapshot();
        if (snapshot) {
            addBubble("user", text);
            statusEl.textContent = "Vamshi is looking at you...";
            const thinking = addBubble("assistant", "Analyzing...", true);

            try {
                const response = await fetch(BACKEND_URL + "/vision", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        message: text,
                        image: snapshot
                    })
                });

                const data = await response.json();
                thinking.remove();
                
                const reply = data.reply || "I couldn't quite see that clearly.";
                addBubble("assistant", String(reply));
                speak(String(reply));
            } catch (error) {
                console.error(error);
                thinking.remove();
                addBubble("assistant", "Sorry, I couldn't connect to my vision brain.");
            }
            statusEl.textContent = "Camera active";
            return;
        }
    }

    // Normal text chat if camera is off
    return originalSendToVamshi(text);
};
