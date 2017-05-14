import * as API from "../api";
import * as EugenAPI from "../eugenAPI";

export const RECEIVE_RESULT = 'RECEIVE_RESULT';
export const REQUEST_RESULT = 'REQUEST_RESULT';
export const REQUEST_RESULT_FAILED = 'REQUEST_RESULT_FAILED';

export const SHOW_RESULT = 'SHOW_RESULT';
export const HIDE_RESULT = 'HIDE_RESULT';


export function showResult() {
    return {
        type: SHOW_RESULT
    }
}

export function hideResult() {
    return {
        type: HIDE_RESULT
    }
}

export const fetchResult = query => dispatch => {
    dispatch(requestResult(query));

    let api = (query.modelName == 'depslm') ? EugenAPI : API;

    api.predictSense(query.word, query.context, query.modelName)
        .then(json => dispatch(receiveResult(query, json)))
        .catch(function(error){
            console.error(error);
            dispatch(requestFailed({word: query.word}))
        })
};

export function requestResult(query) {
    return {
        type: REQUEST_RESULT,
        ...query
    }
}

export function receiveResult(query, json) {
    return {
        type: RECEIVE_RESULT,
        ...query,
        result: json,
        receivedAt: Date.now()
    }
}

export function requestFailed({word}) {
    return {
        type: REQUEST_RESULT_FAILED,
        message: "Network communication failed.",
        date: Date.now(),
        word
    }
}