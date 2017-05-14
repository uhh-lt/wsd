import React from "react";
import { connect } from 'react-redux'
import { withRouter } from 'react-router'
import ResultCard from './../components/ResultCard'
import OnlyContextFormCard from './../components/OnlyContextFormCard'
import DetectedEntitiesCard from './../components/DetectedEntitiesCard'
import FeatureDetailDialog from '../components/FeatureDetailDialog'
import {fetchEntitiesAndResult, selectEntity} from "../actions/entities";
import {setQuery} from "../actions/index";
import {showResult, hideResult} from "../actions/result";
import {fetchDetails,openDetails, closeDetails} from "../actions/details";
import {getRandomContextWithWord} from './../samples'
import {setWord} from "../actions/index";
import {ALL_WORD_URL_PATH} from "../apppaths";

const AllNounsPrediction = ({detectedEntitiesCard, currentResultCard, query, imagesEnabled, dispatch, router, detailsDialog}) =>
    <span>
        <OnlyContextFormCard {...(router.query ? router.query : query)} onValidSubmit={(partialQuery) => {
            router.push({
                pathname: ALL_WORD_URL_PATH,
                query: partialQuery,
            });
            dispatch(hideResult());
            dispatch(setQuery(partialQuery));
            dispatch(fetchEntitiesAndResult(partialQuery));
        }} onRandomSample={ () => {
            let newQuery = {...query, ...getRandomContextWithWord()};
            dispatch(setQuery(newQuery));
            router.replace({
                pathname: ALL_WORD_URL_PATH,
                query: newQuery
            });
        }}/>
        {detectedEntitiesCard.show
            ? <DetectedEntitiesCard {...detectedEntitiesCard} imagesEnabled={imagesEnabled} onClickOnEntity={ fragment => {
                dispatch(setWord(fragment.text));
                dispatch(selectEntity(fragment.id));
                dispatch(showResult())
            }}/>
            : <span />
        }
        {currentResultCard.show ? <ResultCard
            {...currentResultCard}
            imagesEnabled={imagesEnabled}
            onOpenDetails={ ({feature, senseID, modelName}) => {
            dispatch(fetchDetails({feature, senseID, modelName}));
            dispatch(openDetails({feature, senseID, modelName}))
        }}/> : <span />}
        <FeatureDetailDialog {...detailsDialog} onRequestClose={ () =>
            dispatch(closeDetails())
        } />
    </span>;


const mapStateToProps = (state, ownProps) => {
    const { query: stateQuery, resultByWord, detectedEntities, resultCard, detailsDialog, settings} = state;

    const query = {
        ...stateQuery,
        ...ownProps.location.query
    };

    const firstPredictionForWord = word => {
        let element = resultByWord[word] || { result: undefined };
        return (element.result && element.result.predictions)? element.result.predictions[0]: null
    };

    const hypernymForWord = word => {
        let prediction = firstPredictionForWord(word);
        return prediction ? prediction.senseCluster.hypernyms[0] : '–';
    };

    const imageUrlForWord = word => {
        let prediction = firstPredictionForWord(word);
        return prediction ? prediction.imageUrl : null;
    };

    const isFetchingForWord = word => {
        let element = resultByWord[word] || { isFetching: false };
        return element.isFetching
    };

    let fragmentResults = detectedEntities.fragments.map(fragment => {
        return {
            ...fragment,
            hypernym: fragment.isEntity ? hypernymForWord(fragment.text) : null,
            imageUrl: fragment.isEntity ? imageUrlForWord(fragment.text) : null,
            isFetching: fragment.isEntity ? isFetchingForWord(fragment.text) : false
        }
    });

    let detectedEntitiesCard = {
        ...detectedEntities,
        fragmentResults,

    };

    const currentResultCard = {
        ...resultByWord[query.word],
        originalWord: query.word,
        ...resultCard,
        show: resultCard.show && detectedEntities.show
    };

    return {
        query,
        currentResultCard,
        detectedEntitiesCard,
        detailsDialog,
        imagesEnabled: settings.showImages
    };
};

export default connect(mapStateToProps)(withRouter(AllNounsPrediction))