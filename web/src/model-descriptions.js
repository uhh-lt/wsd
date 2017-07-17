/**
 * Created by fide on 14.07.17.
 */
import React from 'react'

const modelDescriptions = {
    'ensemble': {
        title: "Ensemble",
        text: <span>This model combines the traditional <b>“per-word” sense inventory</b> with the <b>super sense inventory</b>. First, the system tries to fetch senses of an ambiguous word from the word sense inventory. If a word is out of vocabulary (OOV) then this word is disambiguated using the super sense inventory. This model was induced from a text corpus which is a combination of Wikipedia, ukWaC, LCC News corpus, and Gigaword.</span>,
    },
    'cos_traditional_coocwords': {
        title: "Word Senses based on Cluster Word Features",
        text: <span>This model uses the <b>cluster words</b> from the induced <b>word sense inventory</b> as sparse features that represent the sense. This model was induced from a text corpus which is a combination of Wikipedia, ukWaC, LCC News corpus, and Gigaword.</span>,
    },
    'cos_traditional_self': {
        title: "Word Senses based on Cluster Word Features",
        text: <span>This representation is based on a sum of word vectors of all cluster words in the induced <b>word sense inventory</b> weighted by distributional similarity scores. Word vectors use <b>context words</b> (words that often co-occur in the text corpus with the word) as sparse features. This model was induced from a text corpus which is a combination of Wikipedia, ukWaC, LCC News corpus, and Gigaword.</span>,
    },
    'naivebayes_cosets1k_self': {
        title: "Super Senses based on Context Word Features",
        text: <span>To build this model, induced word senses are first globally clustered using the Chinese Whispers graph clustering algorithm. The edges in this sense graph are established by disambiguation of the cluster words. The resulting clusters represent semantic classes grouping words sharing a common hypernym, e.g. all programming languages. This set of semantic classes is used as an automatically learned <b>inventory of super senses</b>: There is only one global sense inventory shared among all words in contrast to the two previous 'traditional' models. Each semantic class is labeled with hypernyms. This model uses <b>cluster words</b> belonging to the semantic class as features. This model was induced from a text corpus which is a combination of Wikipedia, ukWaC, LCC News corpus, and Gigaword.</span>,
    },
    'naivebayes_cosets1k_coocwords': {
        title: "Super Senses based on Context Word Features",
        text: <span>To build this model, induced word senses are first globally clustered using the Chinese Whispers graph clustering algorithm. The edges in this sense graph are established by disambiguation of the cluster words. The resulting clusters represent semantic classes grouping words sharing a common hypernym, e.g. all programming languages. This set of semantic classes is used as an automatically learned <b>inventory of super senses</b>: There is only one global sense inventory shared among all words in contrast to the two previous `traditional' models. Each semantic class is labeled with hypernyms. This model relies on the same semantic classes as the previous one for its sense inventory, but its sense representations are based on the averaging of <b>word vectors of cluster words</b> in a semantic class. This model was induced from a text corpus which is a combination of Wikipedia, ukWaC, LCC News corpus, and Gigaword.</span>,
    },
    'external_model_depslm': { // This is an external model. The host is configured with REACT_APP_WSP_EXTERNAL_API_ENDPOINT
        title: "Word Senses with Dependency & Language Model Features",
        text: <span>This representation is based on two types of features: <b>syntactic dependencies</b> extracted using the Stanford parser and lexical context features extracted using a <b>language model</b>. This model was induced from the Wikipedia text corpus.</span>,
    },
};

export default modelDescriptions
