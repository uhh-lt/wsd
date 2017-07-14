import React from 'react';
import FlatButton from 'material-ui/FlatButton';
import Popover from 'material-ui/Popover';
import Menu from 'material-ui/Menu';
import MenuItem from 'material-ui/MenuItem';
import {ActionSearch} from "material-ui/svg-icons/index";

export default class ContextSamplesPopoverButton extends React.Component {

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
        return (
            <span>
                <FlatButton
                    onTouchTap={this.handleTouchTap}
                    label="Sample sentences"
                    icon={<ActionSearch />}
                />
                <Popover
                    open={this.state.open}
                    anchorEl={this.state.anchorEl}
                    anchorOrigin={{horizontal: 'left', vertical: 'bottom'}}
                    targetOrigin={{horizontal: 'left', vertical: 'top'}}
                    onRequestClose={this.handleRequestClose}
                >
                    <Menu>
                        <MenuItem primaryText="Neque porro quisquam est qui dolorem ipsum quia dolor sit amet, consectetur." />
                        <MenuItem primaryText="Neque porro quisquam est qui dolorem ipsum quia dolor sit amet, consectetur." />
                        <MenuItem primaryText="Neque porro quisquam est qui dolorem ipsum quia dolor sit amet, consectetur." />
                        <MenuItem primaryText="Neque porro quisquam est qui dolorem ipsum quia dolor sit amet, consectetur." />
                        <MenuItem primaryText="Neque porro quisquam est qui dolorem ipsum quia dolor sit amet, consectetur." />
                    </Menu>
                </Popover>
            </span>
        );
    }
}
