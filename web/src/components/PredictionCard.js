import React, { Component, PropTypes } from 'react'
import {Card} from 'material-ui/Card';
import {cyan100, purple100, blue100, grey500} from 'material-ui/styles/colors';
import ChipList, {FeatureChipList} from './ChipList'
import {Avatar, CardActions, CardHeader, List, ListItem} from "material-ui";
import FlatButton from 'material-ui/FlatButton';
import BabelNetIcon from "./BabelNetIcon";

import {ActionInfoOutline, NavigationExpandLess, NavigationExpandMore} from "material-ui/svg-icons/index";
import CardTextWithTitle from "./CardTextWithTitle";
import featureDescription from "../feature-descriptions";



class PredictionCard extends Component {

    constructor(props) {
        super(props);
        this.state = {
            expanded: false,
        };
    }

    handleExpandChange = (expanded) => {
        this.setState({expanded: expanded});
    };

    handleExpand = () => {
        this.setState({expanded: true});
    };

    handleReduce = () => {
        this.setState({expanded: false});
    };


    static propTypes = {
        targetWord: PropTypes.string.isRequired,
        rank: PropTypes.string.isRequired,
        simScore: PropTypes.number.isRequired,
        model: PropTypes.shape({
            name: PropTypes.string.isRequired,
            classifier: PropTypes.string.isRequired,
            sense_inventory_name: PropTypes.string.isRequired,
            word_vector_model: PropTypes.string.isRequired,
            sense_vector_model: PropTypes.string.isRequired,
            is_super_sense_inventory: PropTypes.bool.isRequired,
        }).isRequired,
        confidenceProb: PropTypes.number.isRequired,
        mutualFeatures: PropTypes.arrayOf(
            PropTypes.shape({
                label: PropTypes.string.isRequired,
                weight: PropTypes.number.isRequired,
            })
        ).isRequired,
        top20ClusterFeatures:  PropTypes.arrayOf(
            PropTypes.shape({
                label: PropTypes.string.isRequired,
                weight: PropTypes.number.isRequired,
            }).isRequired
        ).isRequired,
        numClusterFeatures: PropTypes.number.isRequired,
        senseCluster: PropTypes.shape({
            id: PropTypes.string.isRequired,
            lemma: PropTypes.string.isRequired,
            hypernyms: PropTypes.array.isRequired,
            words: PropTypes.array.isRequired,
            babelnet_id: PropTypes.string,
            sampleSentences: PropTypes.arrayOf(
                PropTypes.shape({
                    sentence: PropTypes.string.isRequired,
                    sense_position: PropTypes.shape({
                        start: PropTypes.string.isRequired,
                        end: PropTypes.string.isRequired,
                    }).isRequired,
                }).isRequired
            )
        }).isRequired,
        openFeatureDetails: PropTypes.func.isRequired,
        imagesEnabled: PropTypes.bool.isRequired,
        imageUrl: PropTypes.string
    };

    getTitle() {
        const {targetWord, rank, senseCluster, model} = this.props;
        const lemma = senseCluster.lemma;
        const position = (parseInt(rank, 10) + 1);
        const mainHypernym = senseCluster.hypernyms[0];

        return <div>
            {position}. {(model.is_super_sense_inventory) ? <span style={{color:grey500}}>{targetWord}</span> : lemma} ({mainHypernym})
            <span style={{color:grey500}}><i> – {(model.is_super_sense_inventory) ? "Super Sense" : "Word Sense"}</i></span>
        </div>;
    }

    getSubtitle() {
        const {simScore, confidenceProb, senseCluster, model} = this.props;
        const score = simScore.toPrecision(3);
        const prob = (confidenceProb * 100).toFixed(2);
        const id = senseCluster.id;
        const babelnet_id = senseCluster.babelnet_id ? senseCluster.babelnet_id : "None";
        return `Confidence: ${prob}% / Features: ${featureDescription[model.word_vector_model]} / ${(model.is_super_sense_inventory) ? "Super" : "Word"} Sense ID: ${id} / BabelNet ID: ${babelnet_id}`;
    }


    render() {
        const styles = {
            chip: {
                margin: 2
            },
            wrapper: {
                display: 'flex',
                flexWrap: 'wrap',
            },
            card: {
                marginTop: 4
            }
        };


        const {senseCluster, model, imageUrl, mutualFeatures, openFeatureDetails, top20ClusterFeatures, numClusterFeatures} = this.props;
        const onOpenDetails = (label) => openFeatureDetails({feature: label, senseID: senseCluster.id});

        const babelNetButton = (
                <FlatButton
                    label="BabelNet link"
                    icon={<BabelNetIcon />}
                    href={`http://babelnet.org/synset?word=${senseCluster.babelnet_id}&details=1`}
                    target="_blank"
                />);

        const hypernymText = (
            <CardTextWithTitle
                key="hypernyms"
                title="Hypernyms"
            >
                <ChipList
                    labels={senseCluster.hypernyms}
                    totalNum={senseCluster.hypernyms.length}
                />
            </CardTextWithTitle>
        );
        const SampleSentence = ({start, end, text}) =>
            <ListItem innerDivStyle={{fontSize:"14px", padding:"5px"}} style={{padding:"5px"}}>
                {text.slice(0, start)}<b>{text.slice(start, end)}</b>{text.slice(end)}
            </ListItem>;

        const sampleSentencesText = (
            <CardTextWithTitle
                key="sample-sentences"
                title="Sample sentences"
                expandable={true}>
                <List>
                    {senseCluster.sampleSentences.map(({sense_position, sentence}) =>
                        <SampleSentence {...sense_position} text={sentence} />
                    )}

                </List>
            </CardTextWithTitle>
        );

        const clusterWordsText = (
            <CardTextWithTitle
                expandable={true}
                key="cluster-words"
                title="Cluster words"
                info={<span>The cluster words are words that are distributionally related to the ambiguous target word being disambiguated (so-called "second-order" features). In contrast to the distributionally related words obtained with word-based models, such as word2vec, words in a word cluster are densely connected and are usually refer to one sense. In contrast, in word2vec the list of related words can contain a mixture of related words belonging to different senses. For instance, the word "jaguar" has one word cluster that contains words related to animals and another cluster that contains words related to cars. The clusters are obtained using the JoBimText method.</span>}
            >
                <ChipList
                    labels={senseCluster.words.slice(0,20)}
                    totalNum={senseCluster.words.length}
                    color={cyan100}
                />
            </CardTextWithTitle>
        );

        const NoFeatures = <span>

        </span>

        const contextFeaturesText = (
            <CardTextWithTitle
                expandable={true}
                key="context-words"
                title="Context words"
                info={<span>The context words are words that often co-occur with the ambiguous target word in the given sense (so-called "first-order" features). The context features are specific to word sense, not to a word. They are computed not by simple co-occurrence method, as this would give sense-unaware representations. Instead, we perform aggregation of co-occurrences on the basis on the sense clusters which provides us sense-aware co-occurrence representations. Thus, the sense clusters are used as pivot vocabularies to obtain the sense representations. Note that context word features are much less sparse than the cluster word features.</span>}
            >
                { (top20ClusterFeatures.length > 0)
                    ? <FeatureChipList
                        features={top20ClusterFeatures}
                        totalNum={numClusterFeatures}
                        color={blue100}
                        onOpenDetails={onOpenDetails}
                    />
                    : <NoFeatures />
                }

            </CardTextWithTitle>
        );

        const matchingFeaturesText = (
            <CardTextWithTitle key="matching-features" title="Matching features">
                <FeatureChipList
                    features={mutualFeatures.slice(0,20)}
                    totalNum={mutualFeatures.length}
                    color={purple100}
                    onOpenDetails={onOpenDetails}
                />
            </CardTextWithTitle>
        );

        return (
            <Card
                expanded={this.state.expanded}
                key={senseCluster.id}
                style={styles.card}
                onExpandChange={this.handleExpandChange}
            >
                <CardHeader
                    key="header"
                    actAsExpander={true}
                    avatar={(imageUrl) ? <Avatar src={imageUrl} size={80}/> : null}
                    title={this.getTitle()}
                    subtitle={this.getSubtitle()}
                    showExpandableButton={true}
                    titleStyle={{
                        fontSize: 24,
                        display: 'block',
                        lineHeight: '36px'}}
                />
                {(senseCluster.hypernyms.length > 0) ? hypernymText : <span />}
                {(senseCluster.sampleSentences.length > 0) ? sampleSentencesText : <span />}
                {(senseCluster.words.length > 0) ? clusterWordsText : <span />}
                {(model.word_vector_model === "coocwords"  && top20ClusterFeatures.length > 0) ? contextFeaturesText : <span />}
                {matchingFeaturesText}
                <CardActions>
                    {senseCluster.babelnet_id ? babelNetButton : <span />}
                    {(!this.state.expanded)
                        ? <FlatButton icon={<NavigationExpandMore />}label="Show more" onTouchTap={this.handleExpand} />
                        : <FlatButton icon={<NavigationExpandLess />}label="Show less" onTouchTap={this.handleReduce} />
                    }

                    />
                </CardActions>
            </Card>
        );
    }
}

export default PredictionCard;
