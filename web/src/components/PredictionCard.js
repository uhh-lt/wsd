import React, { Component, PropTypes } from 'react'
import {Card, CardTitle, CardText} from 'material-ui/Card';
import {cyan100, purple100, blue100} from 'material-ui/styles/colors';
import ChipList, {FeatureChipList} from './ChipList'
import {Avatar, CardActions, CardHeader} from "material-ui";
import FlatButton from 'material-ui/FlatButton';
import BabelNetIcon from "./BabelNetIcon";

class PredictionCard extends Component {

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
        const actions = (
            <CardActions>
                <FlatButton
                    label="BabelNet link"
                    icon={<BabelNetIcon />}
                    href={`http://babelnet.org/synset?word=${senseCluster.babelnet_id}&details=1`}
                    target="_blank"
                />
            </CardActions>);

        const onOpenDetails = (label) => openFeatureDetails({feature: label, senseID: senseCluster.id});
        return (
            <Card key={senseCluster.id} style={styles.card}>
                <CardHeader
                    key="header"
                    actAsExpander={true}
                    avatar={imagesEnabled ? <Avatar src={imageUrl} size={80}/> : null}
                    title={this.getTitle()}
                    subtitle={this.getSubtitle()}
                    showExpandableButton={true}
                    titleStyle={{
                        fontSize: 24,
                        display: 'block',
                        lineHeight: '36px'}}
                />
                <CardText key="hypernyms">
                    <ChipList
                        title="Hypernyms"
                        labels={senseCluster.hypernyms}
                        totalNum={senseCluster.hypernyms.length}
                    />
                </CardText>
                <CardText expandable={true} key="cluster-words">
                    <ChipList
                        title="Related words"
                        subtitle="aka Cluster words"
                        labels={senseCluster.words.slice(0,20)}
                        totalNum={senseCluster.words.length}
                        color={cyan100}
                    />
                </CardText>
                <CardText expandable={true} key="context-features">
                    <FeatureChipList
                        title="Features"
                        subtitle="aka Context features"
                        features={top20ClusterFeatures}
                        totalNum={numClusterFeatures}
                        color={blue100}
                        onOpenDetails={onOpenDetails}
                    />
                </CardText>
                <CardText  key="matching-features">
                    <FeatureChipList
                        title="Matching features"
                        features={mutualFeatures.slice(0,20)}
                        totalNum={mutualFeatures.length}
                        color={purple100}
                        onOpenDetails={onOpenDetails}
                    />
                </CardText>
                {senseCluster.babelnet_id ? actions : <span />}
            </Card>
        );
    }
}

export default PredictionCard;
