import {REQUEST_DETAILS, RECEIVE_DETAILS, OPEN_DETAILS, CLOSE_DETAILS} from "../actions/details";

const defaultState = {
    isFetching: false,
    details: {},
    senseID: '',
    modelName: '',
    feature: '',
    open: false
};

const featureDetails= (state = defaultState, action) => {
    switch (action.type) {
        case REQUEST_DETAILS:
            return {
                ...state,
                isFetching: true,
                senseID: action.senseID,
                modelName: action.modelName,
                feature: action.feature
            };

        case RECEIVE_DETAILS:
            return {
                ...state,
                isFetching: false,
                senseID: action.senseID,
                modelName: action.modelName,
                feature: action.feature,
                details: action.details
            };

        case OPEN_DETAILS:
            return {
                ...state,
                open: true,
                senseID: action.senseID,
                modelName: action.modelName,
                feature: action.feature
            };

        case CLOSE_DETAILS:
            return {
                ...defaultState,
                open: false,
            };

        default:
            return state
    }
};

export default featureDetails