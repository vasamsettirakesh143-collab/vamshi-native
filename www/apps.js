const APPS = {
    whatsapp: "com.whatsapp",
    instagram: "com.instagram.android",
    youtube: "com.google.android.youtube",
    calculator: "com.google.android.calculator",
    chrome: "com.android.chrome",
    maps: "com.google.android.apps.maps",
    gmail: "com.google.android.gm",
    photos: "com.google.android.apps.photos",
    camera: "com.android.camera2",
    settings: "com.android.settings",
    contacts: "com.google.android.contacts",
    phone: "com.google.android.dialer",
    dialer: "com.google.android.dialer",
    messages: "com.google.android.apps.messaging",
    files: "com.google.android.documentsui",
    drive: "com.google.android.apps.docs",
    calendar: "com.google.android.calendar",
    spotify: "com.spotify.music",
    telegram: "org.telegram.messenger",
    facebook: "com.facebook.katana",
    twitter: "com.twitter.android",
    x: "com.twitter.android",
    netflix: "com.netflix.mediaclient",
    amazon: "in.amazon.mShop.android.shopping",
    linkedin: "com.linkedin.android"
};

const APP_ALIASES = {
    "google maps": "maps",
    "google chrome": "chrome",
    "google photos": "photos",
    "google calendar": "calendar",
    "play store": "com.android.vending"
};

/*
 * Words to strip from the end of commands like
 * "open paytm app" so the real name "paytm" is
 * matched against installed apps.
 */
const FILLER_WORDS = ["app", "application", "please", "now"];

function stripFillerWords(name) {
    let words = name.split(/\s+/);

    while (
        words.length > 1 &&
        FILLER_WORDS.includes(words[words.length - 1])
    ) {
        words.pop();
    }

    return words.join(" ").trim();
}

async function openApp(appName) {
    let name = String(appName || "").toLowerCase().trim();
    const launcher = window.Capacitor?.Plugins?.AppLauncherNative;

    // "what's app" -> "whats app" so it can match "WhatsApp"
    name = name.replace(/['\u2019]/g, "");

    // Strip trailing filler words ("open paytm app" -> "paytm")
    name = stripFillerWords(name);

    if (!name) {
        return "Which app should I open?";
    }

    // This searches every installed app with a launcher icon.
    if (launcher?.findAndLaunch) {
        try {
            const result = await launcher.findAndLaunch({ name });
            return "Opening " + (result.label || name) + ".";
        } catch (error) {
            console.warn("Dynamic app search did not match.", error);
        }
    }

    const alias = APP_ALIASES[name] || name;
    const packageName = APPS[alias] || alias;

    if (!packageName || !packageName.includes(".")) {
        return "Sorry, I could not find an installed app called " + name + ".";
    }

    if (!launcher) {
        return "App launching works only inside the installed Vamshi app.";
    }

    try {
        await launcher.launch({ packageName });
        return "Opening " + name + ".";
    } catch (error) {
        console.error("App launch error:", error);
        return name + " is not installed on this phone.";
    }
}

/*
 * Sends a WhatsApp message to a contact via the native
 * plugin. WhatsApp opens in the contact's chat with the
 * message pre-filled; the user taps send (WhatsApp
 * requires this by design).
 */
async function sendWhatsAppMessage(contactName, message) {
    const contact = String(contactName || "").trim();
    const text = String(message || "").trim();

    if (!contact) return "Who should I send the WhatsApp message to?";
    if (!text) return "What message should I send to " + contact + "?";

    const launcher = window.Capacitor?.Plugins?.AppLauncherNative;

    if (!launcher?.sendWhatsApp) {
        return "Sending WhatsApp messages works only inside the installed Vamshi app.";
    }

    try {
        const result = await launcher.sendWhatsApp({
            contactName: contact,
            message: text
        });

        return "WhatsApp chat with " + (result.contact || contact) +
            " is open. The message is typed — tap send when ready.";
    } catch (error) {
        console.error("WhatsApp send error:", error);

        const reason = String(error?.message || error || "");

        if (reason.includes("No contact matched")) {
            return "I could not find " + contact + " in your contacts.";
        }

        if (reason.includes("App not installed") || reason.includes("Could not open WhatsApp")) {
            return "WhatsApp does not seem to be installed on this phone.";
        }

        return "Sorry, I could not open the WhatsApp chat with " + contact + ".";
    }
}

async function openWebSearch(query) {
    const value = String(query || "").trim();
    if (!value) return "What should I search for?";

    window.open(
        "https://www.google.com/search?q=" + encodeURIComponent(value),
        "_blank"
    );

    return "Searching Google for " + value + ".";
}

/*
 * YouTube search calls the native accessibility
 * automation (opens YouTube AND performs the search),
 * instead of only opening the app.
 */
async function openYouTubeSearch(query) {
    const value = String(query || "").trim();
    if (!value) return "What should I search for on YouTube?";

    const launcher = window.Capacitor?.Plugins?.AppLauncherNative;

    // Native automation path (Android app).
    if (launcher?.searchYouTube) {
        try {
            const result = await launcher.searchYouTube({ query: value });

            if (result && result.success) {
                return "Searching YouTube for " + value + ".";
            }

            return "Sorry, I could not search YouTube.";
        } catch (error) {
            console.error("YouTube search error:", error);
        }
    }

    // Browser fallback (if plugin or service unavailable).
    window.open(
        "https://www.youtube.com/results?search_query=" + encodeURIComponent(value),
        "_blank"
    );

    return "Searching YouTube for " + value + ".";
}

async function openMapSearch(query) {
    const value = String(query || "").trim();
    if (!value) return "Where should I search on Maps?";

    window.open(
        "https://www.google.com/maps/search/?api=1&query=" + encodeURIComponent(value),
        "_blank"
    );

    return "Showing Maps results for " + value + ".";
}

function extractAfter(command, phrases) {
    for (const phrase of phrases) {
        const index = command.indexOf(phrase);
        if (index !== -1) return command.slice(index + phrase.length).trim();
    }
    return "";
}

function findAppName(command) {
    const names = Object.keys(APPS).sort((a, b) => b.length - a.length);

    for (const name of names) {
        if (
            command.includes("open " + name) ||
            command.includes("launch " + name) ||
            command.includes("start " + name)
        ) return name;
    }

    for (const alias of Object.keys(APP_ALIASES)) {
        if (
            command.includes("open " + alias) ||
            command.includes("launch " + alias) ||
            command.includes("start " + alias)
        ) return alias;
    }

    return null;
}

/*
 * Parses WhatsApp messaging commands. Examples it handles:
 *   "send whatsapp message to mom saying I will be late"
 *   "send whatsapp to mom I will be late"
 *   "whatsapp mom saying I will be late"
 *   "tell mom on whatsapp that I will be late"
 */
function parseWhatsAppCommand(text) {
    // send whatsapp [message] to <contact> saying/telling/that <message>
    let match = text.match(
        /^send\s+whatsapp(?:\s+message)?(?:\s+to)?\s+(.+?)\s+(?:saying|telling|that|telling that)\s+(.+)$/
    );
    if (match) return { contact: match[1], message: match[2] };

    // send whatsapp to <contact> <message>  (no "saying")
    match = text.match(/^send\s+whatsapp(?:\s+message)?\s+to\s+(.+)$/);
    if (match) {
        const parts = match[1].split(/\s+(?:saying|that)\s+/);
        if (parts.length === 2) return { contact: parts[0], message: parts[1] };
        return null; // no message text -> need to ask
    }

    // tell <contact> on whatsapp that <message>
    match = text.match(/^tell\s+(.+?)\s+on\s+whatsapp\s+(?:that\s+)?(.+)$/);
    if (match) return { contact: match[1], message: match[2] };

    // whatsapp <contact> saying <message>
    match = text.match(/^whatsapp\s+(.+?)\s+(?:saying|that)\s+(.+)$/);
    if (match) return { contact: match[1], message: match[2] };

    return null;
}

async tryJarvisCommand(command) {
    const text = String(command || "").toLowerCase().trim();

    /*
     * WhatsApp messaging — checked before app opening,
     * so "send whatsapp to mom..." is not eaten by the
     * plain "open whatsapp" handler.
     */
    const whatsappMatch = parseWhatsAppCommand(text);
    if (whatsappMatch) {
        return sendWhatsAppMessage(
            whatsappMatch.contact,
            whatsappMatch.message
        );
    }

    // "open whatsapp chat with mom" — just open the chat
 const chatMatch = text.match(/open whatsapp chat with (.+)/);
    if (chatMatch && chatMatch[1].trim()) {
        return sendWhatsAppMessage(chatMatch[1].trim(), "");
    }

    /*
     * Handles "open youtube and search for cats".
     */
    const openAndSearch =
        text.match(/open youtube (?:and|&) search (?:for )?(.+)/);

    if (openAndSearch && openAndSearch[1].trim()) {
        return openYouTubeSearch(openAndSearch[1].trim());
    }

    if (text.includes("search youtube for") || text.includes("search youtube ")) {
        return openYouTubeSearch(
            extractAfter(text, ["search youtube for", "search youtube"])
        );
    }

    if (
        text.includes("search google for") ||
        text.startsWith("search for ") ||
        text.startsWith("google ")
    ) {
        return openWebSearch(
            extractAfter(text, ["search google for", "search for", "google "])
        );
    }

    if (
        text.includes("find on maps") ||
        text.includes("show me directions to") ||
        text.includes("navigate to")
    ) {
        return openMapSearch(
            extractAfter(text, [
                "find on maps",
                "show me directions to",
                "navigate to"
            ])
        );
    }

    /*
     * Known app opening (from the hardcoded map).
     */
    const appName = findAppName(text);
    if (appName) return openApp(appName);

    /*
     * Unknown app names (e.g. "open paytm app") go to the
     * native findAndLaunch scanner instead of the AI backend.
     */
    const openMatch = text.match(/^(?:open|launch|start|run)\s+(?:the\s+)?(.+)$/);

    if (openMatch && openMatch[1].trim()) {
        return openApp(openMatch[1].trim());
    }

    return null;
}

window.APPS = APPS;
window.openApp = openApp;
window.sendWhatsAppMessage = sendWhatsAppMessage;
window.openWebSearch = openWebSearch;
window.openYouTubeSearch = openYouTubeSearch;
window.openMapSearch = openMapSearch;
window.tryJarvisCommand = tryJarvisCommand;
