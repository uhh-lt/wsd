
const apiHost = process.env.REACT_APP_WSP_API_HOST
    ? process.env.REACT_APP_WSP_API_HOST
    : "http://localhost:9000";


/*
import fragments from "./eugen-fixture"
import data from "./fixture"
import details from "./details-fixture"
import fetchMock from 'fetch-mock'
*/
/*
fetchMock.post(`${apiHost}/predictWordSense`, data);
fetchMock.post(`${apiHost}/detectNamedEntities`, fragments);
fetchMock.get('*', details);
*/

/*
function timeout(ms, promise) {
    return new Promise(function(resolve, reject) {
        setTimeout(function() {
            reject(new Error("timeout"))
        }, ms);
        promise.then(resolve, reject)
    })
}

timeout(1000, fetch('/hello')).then(function(response) {
    // process response
}).catch(function(error) {
    // might be a timeout error
});
*/
const post = ({endpoint, data}) => fetch(`${apiHost}/${endpoint}`, {
    method: 'POST',
    mode: 'cors',
    body: JSON.stringify(data),
    redirect: 'follow',
    headers: new Headers({
        'Content-Type': 'application/json; charset=utf-8'
    })
});

const get = (endpoint) => fetch(`${apiHost}/${endpoint}`, {
    method: 'GET',
    mode: 'cors',
    redirect: 'follow',
    headers: new Headers({
        'Content-Type': 'application/json; charset=utf-8'
    })
});

export function predictSense(word, context, model) {
    return post({endpoint: 'predictSense', data: {word, context, model}})
        .then(response => response.json());
}

export function detectEntities(sentence) {
    return post({endpoint: 'detectNamedEntities', data: {sentence}})
        .then(response => response.json());
}

export function getFeatureDetails({modelName, feature, senseID}) {
    let featureURI = encodeURIComponent(feature);
    let senseIDURI = encodeURIComponent(senseID);
    return get(`${modelName}/featureDetails/${featureURI}/${senseIDURI}`)
        .then(response => response.json())
}
