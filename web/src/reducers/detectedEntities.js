import {REQUEST_ENTITIES, RECEIVE_ENTITIES, HIDE_ENTITIES} from "../actions/entities";
import {SELECT_ENTITY} from "../actions/entities";
import {SET_QUERY} from "../actions/index";
import {REQUEST_ENTITIES_FAILED} from "../actions/entities";

const defaultState = {
    isFetching: false,
    fragments: [],
    context: '',
    show: false,
    selected: -1,
    hasError: false
};

const detectedEntities= (state = defaultState, action) => {
    switch (action.type) {
        case REQUEST_ENTITIES:
            return {
                ...state,
                isFetching: true,
                context: action.context,
                show: true,
                selected: -1,
                hasError: false
            };
        case RECEIVE_ENTITIES:
            return {
                ...state,
                isFetching: false,
                fragments: action.fragments,
                context: action.context,
                lastUpdated: action.receivedAt,
                show: true,
                selected: -1,
                hasError: false
            };
        case REQUEST_ENTITIES_FAILED:
            return {
                ...state,
                show: true,
                isFetching: false,
                hasError: true
            };
        case HIDE_ENTITIES:
            return {
                ...state,
                show: false
            };
        case SELECT_ENTITY:
            return {
                ...state,
                selected: action.selected
            };
        case SET_QUERY:
            return defaultState;

        default:
            return state
    }
};

export default detectedEntities