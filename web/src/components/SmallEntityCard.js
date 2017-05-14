import React from 'react';
import {Card, CardHeader} from 'material-ui/Card';
import {transparent, blue100} from 'material-ui/styles/colors';
import LinearProgress from 'material-ui/LinearProgress';

const styles = {
    mediaCard: {
        height: 150,
        overflow: "hidden"
    },
    emptyMediaCard: {
        height: 150
    },
    card: {
        margin: 4,
}
};

class EntityCard extends React.Component {

    static propTypes = {
        text: React.PropTypes.string.isRequired,
        isEntity: React.PropTypes.bool.isRequired,
        isFetching: React.PropTypes.bool.isRequired,
        src: React.PropTypes.string,
        hypernym: React.PropTypes.string,
        onClick: React.PropTypes.func,
    };

    render() {
        const {isFetching, hypernym, text, isEntity, type, selected, onClick} = this.props;
        const imgsrc = `http://serelex.org/image/${text.toLowerCase()}`;

        return (
            <Card style={{
                ...styles.card,
                backgroundColor: selected ? blue100 : transparent,
                cursor: isEntity ? 'pointer' : 'default'
            }} onClick={onClick}>
                <CardHeader
                    title={text}
                    subtitle={hypernym}
                    avatar={isEntity ? imgsrc : null}
                    style={isEntity ? {width: 200} : {}}
                />
                {!isFetching && isEntity ? <LinearProgress mode="indeterminate" /> : <span />}
            </Card>
        )
    }
}

export default EntityCard;
