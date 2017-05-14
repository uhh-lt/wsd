
import React from "react";
import Snackbar from 'material-ui/Snackbar';
import {connect} from 'react-redux';
import moment from "moment";
import {red900} from 'material-ui/styles/colors';

const ErrorSnackbar = ({message, date}) => <Snackbar
    open={message !== ""}
    message={moment(date).format("LTS") + ": " + message}
    bodyStyle={{backgroundColor: red900}}
/>;

function mapStoreToProps(state) {
    return {
        message: state.errors.message,
        date: state.errors.date
    };
}

export default connect(mapStoreToProps)(ErrorSnackbar);
