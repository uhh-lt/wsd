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
    },
    {
        "context": "The Sun is the star at the center of the Solar System.",
        "word": "sun"
    },
    {
        "context": "Sun was an American company that sold computers, computer components, software, and information technology services, and that created the Java programming language, the Solaris operating system, ZFS, the Network File System (NFS) and SPARC.",
        "word": "sun"
    },
    {
        "context": "Apollo is one of the most important and complex of the Olympian deities in classical Greek and Roman religion and Greek and Roman mythology.",
        "word": "apollo"
    },
    {
        "context": "A bar is a retail business establishment that serves alcoholic beverages, such as beer, wine, liquor, cocktails, and other beverages such as mineral water and soft drinks and often sell snack foods such as crisps.",
        "word": "bar"
    },
    {
        "context": "Canon is a Japanese multinational corporation specialized in the manufacture of imaging and optical products, including cameras, camcorders, photocopiers, etc.",
        "word": "canon"
    },
    {
        "context": "A canon is a member of certain bodies subject to an ecclesiastical rule, a cleric living with others close to a cathedral and conducting his life according to the orders or rules of the church.",
        "word": "canon"
    },
    {
        "context": "Delphi is an integrated development environment (IDE) for desktop, mobile, web, and console applications.",
        "word": "delphi"
    },
    {
        "context": "Delphi is famous as the ancient sanctuary that grew rich as the seat of Pythia, the oracle consulted about important decisions throughout the ancient classical world.",
        "word": "delphi"
    },

];

function randomElement(elements) {
    var id = Math.floor((Math.random() * elements.length)); // between 0 and max length - 1 of samples array
    return elements[id];
}

export const sampleContextsWithWords = samples;
export const sampleContexts = samples.map((el) => el.context);
export function getRandomContext() {return randomElement(sampleContexts);}
export function getRandomContextWithWord() {return randomElement(sampleContextsWithWords);}