import {REQUEST_RESULT_FAILED} from "../actions/result";
import {REQUEST_ENTITIES_FAILED} from "../actions/entities";
import {REQUEST_DETAILS_FAILED} from "../actions/details";

const errors = (state = {message: "", date: null}, action) => {
    switch (action.type) {
        case REQUEST_DETAILS_FAILED:
        case REQUEST_ENTITIES_FAILED:
        case REQUEST_RESULT_FAILED:
            return {
                ...state,
                message: action.message,
                date: action.date
            };
        default:
            return state
    }
};

export default errors