import {SHOW_RESULT, HIDE_RESULT} from "../actions/result";
import {SET_QUERY} from "../actions/index";

const resultCard = (state = {show: false}, action) => {
    switch (action.type) {
        case SHOW_RESULT:
            return {
                ...state,
                show: true,
            };
        case HIDE_RESULT:
            return {
                ...state,
                show: false,
            };
        case SET_QUERY:
            return {
            ...state,
            show: false,
        };
        default:
            return state
    }
};

export default resultCard;