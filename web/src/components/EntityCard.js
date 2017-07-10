import React from 'react';
import {Card, CardTitle, CardMedia} from 'material-ui/Card';
import {transparent, blue100, white} from 'material-ui/styles/colors';
import LinearProgress from 'material-ui/LinearProgress';
import LoadableImage from "./LoadableImage";

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

const apiHost = process.env.REACT_APP_WSP_API_HOST
    ? process.env.REACT_APP_WSP_API_HOST
    : "http://localhost:9000";

class EntityCard extends React.Component {

    static propTypes = {
        text: React.PropTypes.string.isRequired,
        isEntity: React.PropTypes.bool.isRequired,
        isFetching: React.PropTypes.bool.isRequired,
        src: React.PropTypes.string,
        hypernym: React.PropTypes.string,
        type: React.PropTypes.string,
        onClick: React.PropTypes.func,
        imagesEnabled: React.PropTypes.bool.isRequired,
        imageUrl: React.PropTypes.string
    };

    constructor(props) {
        super(props);
        this.state = {
            imageLoaded: false,
            imageError: false,
            selected: false
        }
    };

    render() {
        const {hypernym, text, isEntity, isFetching, type, selected, onClick, imagesEnabled, imageUrl} = this.props;
        const img = imagesEnabled && imageUrl ? <LoadableImage url={imageUrl} /> : <span />;

        const title = (
            <span>
                <CardTitle title={hypernym} subtitleColor={white} titleColor={white} />
                {isFetching ? <LinearProgress mode="indeterminate" /> : <span />}
            </span>
        );

        const media = (<CardMedia style={styles.mediaCard} overlay={title}>{img}</CardMedia>);

        const emptyMedia = (<CardMedia style={styles.emptyMediaCard} />);

        return (
            <Card style={{
                ...styles.card,
                backgroundColor: selected ? blue100 : transparent,
                cursor: isEntity ? 'pointer' : 'default'
            }} onClick={onClick}>
                {isEntity ? media : emptyMedia}
                <CardTitle style={isEntity ? {width: 200} : {}} title={text}/>
            </Card>
        )
    }
}

export default EntityCard;
