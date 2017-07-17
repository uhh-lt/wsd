import React, { Component } from 'react'
import {CardText} from 'material-ui/Card';

import Badge from 'material-ui/Badge';
import IconButton from 'material-ui/IconButton';
import {ActionInfoOutline} from "material-ui/svg-icons/index";
import {grey500} from 'material-ui/styles/colors';
import {Popover} from "material-ui";


class CardTextWithTitle extends Component {
    constructor(props) {
        super(props);

        this.state = {
            open: false,
        };
    }

    handleTouchTap = (event) => {
        // This prevents ghost click.
        event.preventDefault();

        this.setState({
            open: true,
            anchorEl: event.currentTarget,
        });
    };

    handleRequestClose = () => {
        this.setState({
            open: false,
        });
    };

    render() {
        const {title, children, expandable, info} = this.props;
        const styles = {
            childrenWrapper: {
                display: 'flex',
                flexWrap: 'wrap',
            },
            container: {
                display: 'flex',
                flexDirection: 'row',
                alignItems: 'center'
            },
            title: {
                flex: "0 0 150px"
            },
            badge: {
                padding: "20px 20px 12px 0"
            },
            popover : {
                margin: 20,
                maxWidth: "300px",
            }
        };

        const TitleInfo = ({title, info}) =>
            <span>
                <Badge style={styles.badge} badgeContent={
                    <IconButton onTouchTap={this.handleTouchTap}>
                        <ActionInfoOutline color={grey500}/>
                    </IconButton>
                }>
                    {title}
                </Badge>
                <Popover
                    open={this.state.open}
                    anchorEl={this.state.anchorEl}
                    anchorOrigin={{horizontal: 'right', vertical: 'top'}}
                    targetOrigin={{horizontal: 'left', vertical: 'bottom'}}
                    onRequestClose={this.handleRequestClose}
                >
                    <div style={styles.popover}>{info}</div>
                </Popover>
            </span>;

        return (
            <CardText expandable={expandable}>
                <div style={styles.container}>
                    <div style={styles.title}>
                        {(info !== undefined) ?
                            <span>
                                <Badge style={styles.badge} badgeContent={
                                    <IconButton onTouchTap={this.handleTouchTap}>
                                        <ActionInfoOutline color={grey500}/>
                                    </IconButton>
                                }>
                                    {title}
                                </Badge>
                                <Popover
                                    open={this.state.open}
                                    anchorEl={this.state.anchorEl}
                                    anchorOrigin={{horizontal: 'right', vertical: 'top'}}
                                    targetOrigin={{horizontal: 'left', vertical: 'bottom'}}
                                    onRequestClose={this.handleRequestClose}
                                >
                                    <div style={styles.popover}>{info}</div>
                                </Popover>
                            </span>
                            : title
                        }
                    </div>
                    <div style={styles.childrenWrapper}>{children}</div>
                </div>
            </CardText>
        )

    }
}

export default CardTextWithTitle;
