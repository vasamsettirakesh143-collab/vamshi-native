async function VamshiBrain(command) {
    command = String(command || "").toLowerCase().trim();

    if (!command) return "How can I help you?";

    const memoryReply = typeof remember === "function" ? remember(command) : null;
    if (memoryReply) return memoryReply;

    const recallReply = typeof recall === "function" ? recall(command) : null;
    if (recallReply) return recallReply;

    const jarvisReply = typeof tryJarvisCommand === "function"
        ? await tryJarvisCommand(command)
        : null;

    if (jarvisReply) return jarvisReply;

    if (
        command === "time" ||
        command.includes("what time") ||
        command.includes("the time") ||
        command.includes("what's the time")
    ) {
        return "The current time is " + new Date().toLocaleTimeString();
    }

    if (
        command === "date" ||
        command.includes("today's date") ||
        command.includes("what date") ||
        command.includes("what's today")
    ) {
        return "Today is " + new Date().toDateString();
    }

    if (
        command === "hi" ||
        command === "hello" ||
        command === "hey" ||
        command.startsWith("hi ") ||
        command.startsWith("hello ") ||
        command.startsWith("hey ")
    ) {
        return "Hello Rakesh. I am Vamshi.";
    }

    return await askAI(command);
}

async function askAI(message) {
    try {
        const response = await fetch(
            "https://vamshi-backend-y6ja.onrender.com/chat",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ message } )
            }
        );

        const data = await response.json();
        return data.reply || "I could not create a reply.";
    } catch (error) {
        console.error("AI connection error:", error);
        return "Sorry Rakesh, I cannot connect to my brain.";
    }
}

window.VamshiBrain = VamshiBrain;
window.askAI = askAI;
