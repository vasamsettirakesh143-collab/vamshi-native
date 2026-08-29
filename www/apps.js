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

async function openApp(appName) {
    const name = String(appName || "").toLowerCase().trim();
    const launcher = window.Capacitor?.Plugins?.AppLauncherNative;

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

async function openWebSearch(query) {
    const value = String(query || "").trim();
    if (!value) return "What should I search for?";

    window.open(
        "https://www.google.com/search?q=" + encodeURIComponent(value ),
        "_blank"
    );

    return "Searching Google for " + value + ".";
}

async function openYouTubeSearch(query) {
    const value = String(query || "").trim();
    if (!value) return "What should I search for on YouTube?";

    await openApp("youtube");
    return "Searching YouTube for " + value + ".";
}

async function openMapSearch(query) {
    const value = String(query || "").trim();
    if (!value) return "Where should I search on Maps?";

    window.open(
        "https://www.google.com/maps/search/?api=1&query=" + encodeURIComponent(value ),
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

async function tryJarvisCommand(command) {
    const text = String(command || "").toLowerCase().trim();

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

    const appName = findAppName(text);
    if (appName) return openApp(appName);

    return null;
}

window.APPS = APPS;
window.openApp = openApp;
window.openWebSearch = openWebSearch;
window.openYouTubeSearch = openYouTubeSearch;
window.openMapSearch = openMapSearch;
window.tryJarvisCommand = tryJarvisCommand;
