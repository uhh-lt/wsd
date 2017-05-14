import React from "react"
import "./../App.css"
import FeatureDetailDialog from '../components/FeatureDetailDialog'
import FormCard from "../components/FormCard"
import { connect } from 'react-redux'
import ResultCard from '../components/ResultCard'
import { withRouter } from 'react-router'

import {fetchResult, showResult} from "../actions/result"
import {setQuery} from "../actions/index"
import {closeDetails} from "../actions/details";
import {openDetails} from "../actions/details";
import {fetchDetails} from "../actions/details";
import {getRandomContextWithWord} from './../samples'
import {SINGLE_WORD_URL_PATH} from "../apppaths";

const SingleWordPrediction = ({detailsDialog, result, show, query, dispatch, router, imagesEnabled}) =>
    <span>
        <FormCard {...query} onValidSubmit={(data) => {
            router.replace({
                pathname: SINGLE_WORD_URL_PATH,
                query: data
            });
            dispatch(setQuery(data));
            dispatch(showResult());
            dispatch(fetchResult(data));
        }} onRandomSample={ () => {
            let newQuery = {...query, ...getRandomContextWithWord()};
            dispatch(setQuery(newQuery));
            router.replace({
                pathname: SINGLE_WORD_URL_PATH,
                query: newQuery
            });
        }}/>
        {result.show ? <ResultCard {...result} imagesEnabled={imagesEnabled} onOpenDetails={ ({feature, senseID, modelName}) => {
            dispatch(fetchDetails({feature, senseID, modelName}));
            dispatch(openDetails({feature, senseID, modelName}))
        }} /> : <span />}
        <FeatureDetailDialog {...detailsDialog} onRequestClose={ () =>
            dispatch(closeDetails())
        } />
    </span>;

const mapStateToProps = (state, ownProps) => {
    const { query: stateQuery, resultCard, resultByWord, detailsDialog, settings} = state;

    const query = {
        ...stateQuery,
        ...ownProps.location.query
    };

    const result = {
        ...resultByWord[query.word],
        originalWord: query.word,
        ...resultCard
    };
    const imagesEnabled = settings.showImages;

    return {query, result, detailsDialog, imagesEnabled};
};

export default connect(mapStateToProps)(withRouter(SingleWordPrediction))