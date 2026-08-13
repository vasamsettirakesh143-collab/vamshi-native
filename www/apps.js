const APPS = {

    whatsapp: "com.whatsapp",

    instagram: "com.instagram.android",

    youtube: "com.google.android.youtube",

    calculator: "com.google.android.calculator"

};

async function openApp(appName) {

    appName = appName.toLowerCase();

    const pkg = APPS[appName];

    if (!pkg) {

        return "Sorry, I cannot open that app yet.";

    }

    const nativeLauncher = window.Capacitor && window.Capacitor.Plugins
        ? window.Capacitor.Plugins.AppLauncherNative
        : null;

    if (!nativeLauncher) {

        // Running in a plain browser tab, not the installed app —
        // there's no reliable way to launch another app from here.
        return "App launching only works inside the installed Vamshi app.";

    }

    try {

        await nativeLauncher.launch({ packageName: pkg });

        return `Opening ${appName}`;

    } catch (err) {

        // The native side rejects only when the app genuinely isn't
        // installed — safe to send the user to its Play Store page.
        window.location.href = `https://play.google.com/store/apps/details?id=${pkg}`;

        return `${appName} isn't installed. Opening its Play Store page.`;

    }

}
