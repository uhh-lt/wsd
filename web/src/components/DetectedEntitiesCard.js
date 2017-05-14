/**
 * Created by fide on 05.12.16.
 */

import React, {Component, PropTypes} from 'react';
import LoadingIndicator from './LoadingIndicator'
import {Card, CardText, CardTitle} from 'material-ui/Card';
import EntityCard from './EntityCard'

const styles = {
    container: {
        display: 'flex',
        flexDirection: 'row',
        alignItems: 'center'
    },
    wrapper: {
        display: 'flex',
        flexWrap: 'nowrap',
        overflowX: 'auto',
    },
    title : {
        width: 120
    },
    card: {
        marginTop: 15
    }
};

class DetectedEntitiesCard extends Component {

    static propTypes = {
        fragmentResults: PropTypes.arrayOf(
            PropTypes.shape({
                id: PropTypes.number.isRequired,
                text: PropTypes.string.isRequired,
                isEntity: PropTypes.bool.isRequired,
                src: PropTypes.string,
                hypernym: PropTypes.string,
                type: PropTypes.string,
                isFetching: PropTypes.bool.isRequired,
                imageUrl: React.PropTypes.string
            })
        ).isRequired,
        selected: PropTypes.number.isRequired,
        onClickOnEntity: PropTypes.func,
        isFetching: PropTypes.bool,
        imagesEnabled: PropTypes.bool.isRequired
    };

    render() {
        let {fragmentResults, onClickOnEntity, selected, isFetching, imagesEnabled} = this.props;


        return (
            <Card style={styles.card}>
                <CardTitle
                    key={"title"}
                    title="Detected Entities"
                    subtitle="The system has detected these entities in the given sentence."
                />
                <CardText key={"entities"}>
                    <div style={styles.wrapper}>
                        {isFetching
                            ? <LoadingIndicator />
                            : fragmentResults.map((fragment) =>
                            <EntityCard {...fragment} imagesEnabled={imagesEnabled} key={fragment.id} selected={fragment.id === selected} onClick={
                                fragment.isEntity ? () => onClickOnEntity(fragment) : null
                            } />)
                        }
                    </div>
                </CardText>
            </Card>
        );
    }
}

export default DetectedEntitiesCard;