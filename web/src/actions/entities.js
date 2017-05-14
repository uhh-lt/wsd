import * as API from "../api";
import {fetchResult} from "./result";

export const REQUEST_ENTITIES = 'REQUEST_ENTITIES';
export const RECEIVE_ENTITIES = 'RECEIVE_ENTITIES';
export const REQUEST_ENTITIES_FAILED = 'REQUEST_ENTITIES_FAILED';

export const SELECT_ENTITY = 'SELECT_ENTITY';
export const HIDE_ENTITIES = 'HIDE_ENTITIES';

export const selectEntity = id => ({
    type: SELECT_ENTITY,
    selected: id
});

export const hideEntities = () => ({
    type: HIDE_ENTITIES
});

export const fetchEntitiesAndResult = ({context, modelName}) => dispatch =>  {
    dispatch(requestEntities(context));

    return API.detectEntities(context)
        .then(json => {
            dispatch(receiveEntities(context, json));
            json.filter(f => f.isEntity).forEach(f =>
                dispatch(fetchResult({context, word: f.text, modelName}))
            )
        })
        .catch(function(error) {
            dispatch(requestEntitiesFailed())
        });
};
export function requestEntitiesFailed() {
    return {
        type: REQUEST_ENTITIES_FAILED,
        message: "Network communication failed.",
        date: Date.now()
    }
}


export function requestEntities(context) {
    return {
        type: REQUEST_ENTITIES,
        context
    }
}

export function receiveEntities(context, json) {
    return {
        type: RECEIVE_ENTITIES,
        context,
        fragments: json,
        receivedAt: Date.now()
    }
}