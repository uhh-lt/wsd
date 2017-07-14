import React, { Component, PropTypes } from 'react'
import {Card, CardTitle, CardText} from 'material-ui/Card';
import {cyan100, purple100, blue100} from 'material-ui/styles/colors';
import ChipList, {FeatureChipList} from './ChipList'
import {Avatar, CardActions, CardHeader, List, ListItem} from "material-ui";
import FlatButton from 'material-ui/FlatButton';
import BabelNetIcon from "./BabelNetIcon";

import Badge from 'material-ui/Badge';
import IconButton from 'material-ui/IconButton';
import {ActionInfoOutline, NavigationExpandLess, NavigationExpandMore} from "material-ui/svg-icons/index";
import {grey500} from 'material-ui/styles/colors';
import CardTextWithTitle from "./CardTextWithTitle";



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
        rank: PropTypes.string.isRequired,
        simScore: PropTypes.number.isRequired,
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
            })
        ).isRequired,
        numClusterFeatures: PropTypes.number.isRequired,
        senseCluster: PropTypes.shape({
            id: PropTypes.string.isRequired,
            lemma: PropTypes.string.isRequired,
            hypernyms: PropTypes.array.isRequired,
            words: PropTypes.array.isRequired,
            babelnet_id: PropTypes.string
        }).isRequired,
        openFeatureDetails: PropTypes.func.isRequired,
        imagesEnabled: PropTypes.bool.isRequired,
        imageUrl: PropTypes.string
    };

    getTitle(data) {
        const {rank, senseCluster} = this.props;
        const lemma = senseCluster.lemma;
        const position = (parseInt(rank, 10) + 1);
        const mainHypernym = senseCluster.hypernyms[0];
        return `${position}. ${lemma} (${mainHypernym})`;
    }

    getSubtitle() {
        const {simScore, confidenceProb, senseCluster} = this.props;
        const score = simScore.toPrecision(3);
        const prob = (confidenceProb * 100).toFixed(2);
        const id = senseCluster.id;
        const babelnet_id = senseCluster.babelnet_id ? senseCluster.babelnet_id : "None";
        return `Similarity score: ${score} / Confidence: ${prob}% / Sense ID: ${id} / BabelNet ID: ${babelnet_id}`;
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


        const {senseCluster, imagesEnabled, imageUrl, mutualFeatures, openFeatureDetails, top20ClusterFeatures, numClusterFeatures} = this.props;
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

        const sampleSentencesText = (
            <CardTextWithTitle
                key="sample-sentences"
                title="Sample sentences"
                expandable={true}>
                <List>
                    <ListItem innerDivStyle={{fontSize:"14px", padding:"5px"}} style={{padding:"5px"}}>Lorem ipsum dolor sit amet, <b>consectetur</b> adipiscing elit.</ListItem>
                    <ListItem innerDivStyle={{fontSize:"14px", padding:"5px"}} style={{padding:"5px"}}>estibulum elementum, metus nec <b>consequat</b> congue, nisi libero imperdiet sem, quis congue diam lorem a nisl.</ListItem>
                    <ListItem innerDivStyle={{fontSize:"14px", padding:"5px"}} style={{padding:"5px"}}>Donec ut <b>quam</b> sed velit blandit scelerisque nec at purus.</ListItem>
                </List>
            </CardTextWithTitle>
        );

        const clusterWordsText = (
            <CardTextWithTitle
                expandable={true}
                key="cluster-words"
                title="Cluster words"
                info="estibulum elementum, metus nec consequat congue, nisi libero imperdiet sem, quis congue diam lorem a nisl."
            >
                <ChipList
                    labels={senseCluster.words.slice(0,20)}
                    totalNum={senseCluster.words.length}
                    color={cyan100}
                />
            </CardTextWithTitle>
        );

        const contextFeaturesText = (
            <CardTextWithTitle
                expandable={true}
                key="context-features"
                title="Context features"
                info="estibulum elementum, metus nec consequat congue, nisi libero imperdiet sem, quis congue diam lorem a nisl."
            >
                <FeatureChipList
                    features={top20ClusterFeatures}
                    totalNum={numClusterFeatures}
                    color={blue100}
                    onOpenDetails={onOpenDetails}
                />
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
                {sampleSentencesText}
                {(senseCluster.words.length > 0) ? clusterWordsText : <span />}
                {(contextFeaturesText.length > 0) ? contextFeaturesText : <span />}
                {(mutualFeatures.length > 0) ? matchingFeaturesText : <span />}
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
