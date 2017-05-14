import * as API from "../api";

export const REQUEST_DETAILS = 'REQUEST_DETAILS';
export const RECEIVE_DETAILS = 'RECEIVE_DETAILS';
export const REQUEST_DETAILS_FAILED = 'REQUEST_DETAILS_FAILED';

export const OPEN_DETAILS = 'OPEN_DETAILS';
export const CLOSE_DETAILS = 'CLOSE_DETAILS';

export const openDetails = ({feature, senseID, modelName}) => ({
    type: OPEN_DETAILS,
    feature,
    senseID,
    modelName
});

export const closeDetails = () => ({
    type: CLOSE_DETAILS
});

export const fetchDetails = ({feature, modelName, senseID}) => dispatch =>  {
    dispatch(requestDetails({feature, modelName, senseID}));

    return API.getFeatureDetails({feature, modelName, senseID})
        .then(json =>
            dispatch(receiveDetails({feature, modelName, senseID}, json))
        ).catch(function(error) {
            dispatch(requestDetailsFailed())
        });
};

export function requestDetails({feature, modelName, senseID}) {
    return {
        type: REQUEST_DETAILS,
        feature, modelName, senseID
    }
}

export function receiveDetails({feature, modelName, senseID}, json) {
    return {
        type: RECEIVE_DETAILS,
        feature, modelName, senseID,
        details: json,
        receivedAt: Date.now()
    }
}

export function requestDetailsFailed() {
    return {
        type: REQUEST_DETAILS_FAILED,
        date: Date.now(),
        message: "Network communication failed.",
    }
}