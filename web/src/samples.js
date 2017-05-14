const samples = [
    {
        "context": "Ruby is a dynamic, reflective, general-purpose object-oriented programming language developed in the 1990s.",
        "word": "Ruby"
    },
    {
        "context": "Ruby is a red gemstone that varies from a light pink to a blood red, a variety of the mineral corundum (aluminium oxide).",
        "word": "Ruby"
    },
    {
        "context": "In some classifications is python a family separate from Boidae comprising Old World boas.",
        "word": "Python"
    },
    {
        "context": "Python is a widely used general-purpose, high-level programming language.",
        "word": "Python"
    },
    {
        "context": "Jaguar is a british multinational car manufacturer headquartered in England",
        "word": "Jaguar"
    },
    {
        "context": "Jaguar is a large spotted feline of tropical America similar to the leopard; in some classifications considered a member of the genus Felis",
        "word": "Jaguar"
    },
    {
        "context": "Java is an object-oriented programming language, developed by Sun Microsystems, Inc.",
        "word": "Java"
    },
    {
        "context": "Java is an island of Indonesia and the site of its capital city, Jakarta.",
        "word": "Java"
    },

    {
        "context": "Apple is an American multinational technology company headquartered in Cupertino, California that designs, develops, and sells consumer electronics.",
        "word": "Apple"
    },
    {
        "context": "The apple tree is a deciduous tree in the rose family best known for its sweet, pomaceous fruit, the apple. ",
        "word": "apple"
    },
    {
        "context": "A bank is a financial institution that accepts deposits from the public and creates credit.",
        "word": "bank"
    },
    {
        "context": "In geography, the word bank generally refers to the land alongside a body of water.",
        "word": "bank"
    }
];

function randomElement(elements) {
    var id = Math.floor((Math.random() * elements.length)); // between 0 and max length - 1 of samples array
    return elements[id];
}

export const sampleContextsWithWords = samples;
export const sampleContexts = samples.map((el) => el.context);
export function getRandomContext() {return randomElement(sampleContexts);}
export function getRandomContextWithWord() {return randomElement(sampleContextsWithWords);}