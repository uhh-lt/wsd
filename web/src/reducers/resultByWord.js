import {REQUEST_RESULT, RECEIVE_RESULT} from "../actions/result";
import {SET_QUERY} from "../actions/index";
import {REQUEST_RESULT_FAILED} from "../actions/result";

const result = (state = {
    isFetching: false,
    hasError: false
}, action) => {
    switch (action.type) {
        case REQUEST_RESULT:
            return {
                ...state,  // Note the API returns the context with the result in its own key
                isFetching: true,
                hasError: false
            };
        case RECEIVE_RESULT:
            return {
                ...state,
                result: action.result,
                lastUpdated: action.receivedAt,
                isFetching: false,
                hasError: false
            };
        case REQUEST_RESULT_FAILED:
            return {
                ...state,
                isFetching: false,
                hasError: true
            };
        default:
            return state
    }
};

const resultByWord = (state = { }, action) => {
    switch (action.type) {
        case REQUEST_RESULT:
        case RECEIVE_RESULT:
        case REQUEST_RESULT_FAILED:
            return {
                ...state,
                [action.word]: result(state[action.word], action)
            };
        case SET_QUERY:
            return {};
        default:
            return state
    }
};

export default resultByWord