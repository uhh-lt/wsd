/**
 * Created by fide on 02.05.17.
 */
import React from 'react';

class LoadableImage extends React.Component {

    static propTypes = {
        url: React.PropTypes.string.isRequired
    };

    constructor(props) {
        super(props);
        this.state = {
            imageLoaded: false,
            imageError: false,
        }
    };

    render() {
        const {url} = this.props;
        return (
            <img
                src={url}
                role="presentation"
                alt="The Pulpit Rock"
                style={{opacity: !this.state.imageLoaded ? 0 : 1}}
                onLoad={() => this.setState({imageLoaded: true})}
                onError={() => this.setState({imageError: true})}
            />
        )
    }
}

export default LoadableImage;