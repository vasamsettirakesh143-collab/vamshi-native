async function VamshiBrain(command) {

    command = command.toLowerCase().trim();

    let memoryReply = remember(command);

    if (memoryReply) {

        return memoryReply;

    }

    let recallReply = recall(command);

    if (recallReply) {

        return recallReply;

    }

    // Open apps — checked early and specifically, so a command like
    // "open whatsapp and tell me the time" still opens WhatsApp.

    if (command.includes("open whatsapp")) {

        return openApp("whatsapp");

    }

    else if (command.includes("open instagram")) {

        return openApp("instagram");

    }

    else if (command.includes("open youtube")) {

        return openApp("youtube");

    }

    else if (command.includes("open calculator")) {

        return openApp("calculator");

    }

    // Time / date — specific phrasing, not just the bare word, so a
    // sentence that happens to mention "time" isn't misread.

    else if (

        command === "time" ||
        command.includes("what time") ||
        command.includes("the time") ||
        command.includes("what's the time")

    ) {

        return "The current time is " +

            new Date().toLocaleTimeString();

    }

    else if (

        command === "date" ||
        command.includes("today's date") ||
        command.includes("what date") ||
        command.includes("what's today")

    ) {

        return "Today is " +

            new Date().toDateString();

    }

    // Greeting — ONLY matches when the message actually IS a greeting
    // (starts with it, or is just that word alone), never when the word
    // merely appears somewhere inside a longer sentence.

    else if (

        command === "hi" || command === "hello" || command === "hey" ||
        command.startsWith("hi ") || command.startsWith("hello ") || command.startsWith("hey ")

    ) {

        return "Hello Rakesh. I am Vamshi.";

    }

    // Everything else — including code requests, general questions —
    // goes to the real AI backend.

    else {

        return await askAI(command);

    }

}


// AI function

async function askAI(message) {

    try {

        const response = await fetch(

            "https://vamshi-backend-y6ja.onrender.com/chat",

            {

                method: "POST",

                headers: {

                    "Content-Type": "application/json"

                },

                body: JSON.stringify({

                    message: message

                })

            }

        );

        const data = await response.json();

        return data.reply;

    }

    catch (error) {

        console.error(error);

        return "Sorry Rakesh, I cannot connect to my brain.";

    }

}
