function saveMemory(key, value){

    localStorage.setItem(key, value);

}


function getMemory(key){

    return localStorage.getItem(key);

}



function remember(text){

    if(text.includes("remember")){

        let data = text.replace("remember","").trim();


        if(data.includes("my project is")){

            let value = data.replace("my project is","").trim();

            saveMemory("project", value);

            return "I saved your project as " + value;

        }


        if(data.includes("my hobby is")){

            let value = data.replace("my hobby is","").trim();

            saveMemory("hobby", value);

            return "I saved your hobby as " + value;

        }


        if(data.includes("my favorite is")){

            let value = data.replace("my favorite is","").trim();

            saveMemory("favorite", value);

            return "I saved your favorite as " + value;

        }

    }


    return null;

}




function recall(text){


    if(text.includes("what is my project")){

        let data = getMemory("project");

        return data 
        ? "Your project is " + data
        : "I don't know your project yet.";

    }



    if(text.includes("what is my hobby")){

        let data = getMemory("hobby");

        return data
        ? "Your hobby is " + data
        : "I don't know your hobby yet.";

    }



    if(text.includes("what is my favorite")){

        let data = getMemory("favorite");

        return data
        ? "Your favorite is " + data
        : "I don't know your favorite yet.";

    }


    return null;

}
