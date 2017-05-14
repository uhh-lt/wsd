import {combineReducers} from "redux";

import resultByWord from "./resultByWord";
import detectedEntities from "./detectedEntities";
import resultCard from "./resultCard";
import query from "./query";
import detailsDialog from "./detailsDialog";
import settings from "./settings";
import errors from "./errors";

const predictorApp = combineReducers({
    query,
    resultByWord,
    resultCard,
    detectedEntities,
    detailsDialog,
    settings,
    errors
});

export default predictorApp

// TODO: use with router
// TODO: look into redux-thunk and redux-actions for generating


