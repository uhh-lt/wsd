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
    return {
        ...json,
        contextFeatures: [],
        predictions: json.predictions.map(p => {
            return {
                ...p,
                rank: p.rank.toString(),
                senseCluster: {
                    ...p.senseCluster,
                    hypernyms: p.senseCluster["weighted_hypernyms"].map(x => x[0])
                },
                contextFeatures: p['contextFeatures'].map(tupledFeatToDict),
                mutualFeatures: [],
                top20ClusterFeatures: p['top20ClusterFeatures'].map(tupledFeatToDict),
            }})

    }
}

export function predictSense(word, context, modelName) {
    return post({data: {word, context, featureType: 'clusterwords'}})
        .then(response => response.json())
        //.then(convertLegacyToNew)
}