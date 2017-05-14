import {SHOW_IMAGES, HIDE_IMAGES} from "../actions/index";

const settings = (state = {showImages: true}, action) => {
    switch (action.type) {
        case SHOW_IMAGES:
            return Object.assign({}, state, {
                showImages: true,
            });
        case HIDE_IMAGES:
            return Object.assign({}, state, {
                showImages: false,
            });
        default:
            return state
    }
};

export default settings;