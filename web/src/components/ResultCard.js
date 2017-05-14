import React, {Component, PropTypes} from "react";
import PredictionCard from "./../components/PredictionCard"
import LinearProgress from 'material-ui/LinearProgress';
import {grey100} from 'material-ui/styles/colors';
import ChipList from './ChipList'
import ContentAdd from 'material-ui/svg-icons/content/add';
import FloatingActionButton from 'material-ui/FloatingActionButton';
import {red500} from 'material-ui/styles/colors';

import {Card, CardTitle, CardText} from 'material-ui/Card';

const styles = {
    card: {
        marginTop: 15
    },
    buttonContainer: {
        display: "flex",
        justifyContent: "center"
    },
    buttonElement: {
        paddingTop: 20,
        display: "relative"
    }
};

const loading = <LinearProgress mode="indeterminate" />;
const noSenses = <div>No senses found.</div>;

const lemmatizationWarning = (originalWord, lemma) =>
    <CardText key="lemma-warn" color={red500}>
        No senses were found for '{originalWord}', showing prediction instead for '{lemma}'.
    </CardText>;

const predictionCard = (prediction, openFeatureDetails, imagesEnabled) =>
    <PredictionCard
        {...prediction}
        key={prediction.rank}
        imagesEnabled={imagesEnabled}
        openFeatureDetails={openFeatureDetails}
    />;

const PredictionCards = ({showAll, predictions, openFeatureDetails, onShowAll, imagesEnabled}) =>
    showAll
        ? <div>{predictions.map(p => predictionCard(p, openFeatureDetails, imagesEnabled))}</div>
        : <div>
            {predictionCard(predictions[0], openFeatureDetails, imagesEnabled)}
            <div style={styles.buttonContainer}>
                <div style={styles.buttonElement}>
                    <FloatingActionButton onTouchTap={onShowAll}>
                        <ContentAdd/>
                    </FloatingActionButton>
                </div>
            </div>
        </div>;

const ErrorMsg = () => <CardText key="error" color={red500}>A network error occurred.</CardText>;
const PredictionTitle = ({word}) => <CardTitle title={`Predicted senses for '${word}'`} actAsExpander={true} showExpandableButton={true}/>;


class ResultCard extends Component {

    constructor(props) {
        super(props);
        this.state = {
            showAll: false,
        }
    };

    static propTypes = {
        result: PropTypes.shape({
            word: PropTypes.string.isRequired,
            context: PropTypes.string.isRequired,
            contextFeatures: PropTypes.array.isRequired,
            predictions: PropTypes.array.isRequired,
        }),
        originalWord: PropTypes.string.isRequired,
        isFetching: PropTypes.bool,
        hasError:  PropTypes.bool,
        onOpenDetails: PropTypes.func.isRequired,
        imagesEnabled: PropTypes.bool.isRequired,
    };

    render() {
        const {result, isFetching, hasError, onOpenDetails, originalWord, imagesEnabled} = this.props;
        if (isFetching) {
            return loading;
        } else if (hasError) {
            return <Card style={styles.card}>
                <PredictionTitle word={originalWord} />
                <ErrorMsg />
                </Card>
        } else {
            const {word, contextFeatures, predictions, modelName} = result;
            const openFeatureDetails = ({feature, senseID}) => onOpenDetails({feature, senseID, modelName});
            return <Card style={styles.card}>
                <PredictionTitle word={word} />
                {originalWord !== result.word ? lemmatizationWarning(originalWord, result.word) : <span />}
                <CardText expandable={true} key="context-features">
                    <ChipList
                        title="Context features"
                        labels={contextFeatures.slice(0,20)}
                        totalNum={contextFeatures.length}
                        color={grey100}
                    />
                </CardText>
                <CardText>{predictions.length > 0
                    ? <PredictionCards
                        showAll={this.state.showAll}
                        predictions={predictions}
                        openFeatureDetails={openFeatureDetails}
                        imagesEnabled={imagesEnabled}
                        onShowAll={() => this.setState({showAll: true})}/>
                    : noSenses
                }</CardText>
            </Card>
        }
    }
}

export default ResultCard

