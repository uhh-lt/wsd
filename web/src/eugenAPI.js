/*
 import data from "./eugen-fixture"
 import fetchMock from 'fetch-mock'

 fetchMock.post(`${apiHost}/predictWordSense`, data);
*/

const endpoint = process.env.REACT_APP_WSP_EXTERNAL_API_ENDPOINT
    ? process.env.REACT_APP_WSP_EXTERNAL_API_ENDPOINT
    : "http://ltmaggie.informatik.uni-hamburg.de/wsd-server/predictWordSense";

const post = ({data}) => fetch(endpoint, {
    method: 'POST',
    mode: 'cors',
    body: JSON.stringify(data),
    redirect: 'follow',
    headers: new Headers({
        'Content-Type': 'application/json; charset=utf-8'
    })
});


function convertLegacyToNew(json) {
    const tupledFeatToDict = (t) => {
        return {label: t[0], weight: t[1]}
    };

    const strFeatToDict = (s) => tupledFeatToDict(s.split(":"));

    return {
        ...json,
        contextFeatures: [],
        predictions: (json.predictions) ? json.predictions.map(p => {
            return {
                ...p,
                rank: p.rank.toString(),
                model: {
                    name: "external_model_depslm",
                    classifier: "unknown",
                    sense_inventory_name: "external_traditional",
                    word_vector_model: "depslm",
                    sense_vector_model : "unknown",
                    is_super_sense_inventory: false
                },
                senseCluster: {
                    sampleSentences: [],
                    ...p.senseCluster,
                    hypernyms: p.senseCluster.hypernyms.map((s) => s.split(":")[0])
                },
                contextFeatures: (p['contextFeatures']) ? p['contextFeatures'] : [],
                mutualFeatures: (p['mutualFeatures']) ? p['mutualFeatures'] : [],
                top20ClusterFeatures: (p['top20ClusterFeatures']) ? p['top20ClusterFeatures'] : []
            }}) : []
    }
}

export function predictSense(word, context, modelName) {
    return post({data: {word, context, featureType: 'depslm'}})
        .then(response => response.json())
        .then(convertLegacyToNew)
}