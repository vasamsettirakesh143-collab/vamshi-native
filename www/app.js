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

    // Greetings

    if (

        command.includes("hello") ||
        command.includes("hi") ||
        command.includes("hey")

    ) {

        return "Hello Rakesh. I am Vamshi.";

    }

    // Time

    else if (command.includes("time")) {

        return "The current time is " +

            new Date().toLocaleTimeString();

    }

    // Date

    else if (

        command.includes("date") ||
        command.includes("today")

    ) {

        return "Today is " +

            new Date().toDateString();

    }

    // Open apps

    else if (command.includes("open whatsapp")) {

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

    // Unknown command

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
