import React, {Component, PropTypes} from "react";
import "./../App.css";
import SiteLayout from './../components/SiteLayout';
import { connect } from 'react-redux'
import { withRouter } from 'react-router'
import {showImages, hideImages} from "../actions/index";
import ErrorSnackbar from '../containers/ErrorSnackbar';

const App = ({settings, children, dispatch, router}) =>
    <SiteLayout
        handleChangeList={(e, v) => router.push(v)}
        handleToggleShowImages={(e, v) => {

            if (v == true) {
                dispatch(showImages())
            } else {
                dispatch(hideImages())
            }
        }}
        showImages={settings.showImages}
    >
        <div>
        {children}
        <ErrorSnackbar message="asdf"/>
        </div>
    </SiteLayout>;

const mapStateToProps = (state, ownProps) => {
    const { settings } = state;

    return { settings };
};

export default connect(mapStateToProps)(withRouter(App))
