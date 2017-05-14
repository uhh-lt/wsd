export const SET_QUERY = 'SET_QUERY';
export const SET_WORD = 'SET_WORD';
export const SHOW_IMAGES = 'SHOW_IMAGES';
export const HIDE_IMAGES = 'HIDE_IMAGES';

export const setQuery = query => ({
    type: SET_QUERY,
    ...query
});

export const setWord = word => ({
    type: SET_WORD,
    word
});

export const hideImages = () => ({type: HIDE_IMAGES});
export const showImages = () => ({type: SHOW_IMAGES});