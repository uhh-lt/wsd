
import {SET_QUERY} from './../actions'
import {getRandomContextWithWord} from './../samples'
import {SET_WORD} from "../actions/index";

const defaultQuery = {
    ...getRandomContextWithWord(),
    modelName: 'ensemble',
};

const query = (state = defaultQuery, action) => {
    switch (action.type) {
        case SET_QUERY:
            return Object.assign({}, state, {
                word: action.word,
                context: action.context,
                modelName: action.modelName
            });
        case SET_WORD:
            return {
                ...state,
                word: action.word
            };
        default:
            return state
    }
};

export default query;

