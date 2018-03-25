import	'babel-polyfill'
import React from 'react';
import { render } from 'react-dom'

import {IntlProvider} from "react-intl";

import MuiThemeProvider from "material-ui/styles/MuiThemeProvider";
import getMuiTheme from 'material-ui/styles/getMuiTheme';
import injectTapEventPlugin from 'react-tap-event-plugin';

import { Router, Route, browserHistory, Redirect } from 'react-router';
import { useRouterHistory } from 'react-router'
import { createHistory } from 'history'
import { createStore, applyMiddleware, compose } from 'redux'
//import { composeWithDevTools } from 'redux-devtools-extension';
import { Provider } from 'react-redux'
import thunk from 'redux-thunk'
import createLogger from 'redux-logger'

import App from './containers/App'
import reducer from './reducers'
import SingleWordPrediction from "./containers/SingleWordPrediction";
import AllNounsPrediction from "./containers/AllNounsPrediction";
import {SINGLE_WORD_URL_PATH} from "./apppaths";
import {ALL_WORD_URL_PATH} from "./apppaths";
import {BASE_URL_PATH} from "./apppaths";

// This replaces the textColor value on the palette
// and then update the keys for each component that depends on it.
// More on Colors: http://www.material-ui.com/#/customization/colors
const muiTheme = getMuiTheme({
    palette: {
        primary1Color: "#137dc8",
    },
    fontFamily: "Sans-Serif",
});

// Needed for onTouchTap
// http://stackoverflow.com/a/34015469/988941
injectTapEventPlugin();

const middleware = [ thunk ];

if (process.env.NODE_ENV !== 'production') {
    middleware.push(createLogger())
}
const composeEnhancers = window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__ || compose;
const store = createStore(
    reducer,
    composeEnhancers(
        applyMiddleware(...middleware)
    )
);

render(
    <IntlProvider locale="en">
        <MuiThemeProvider muiTheme={muiTheme}>
            <Provider store={store}>
                <Router history={browserHistory}>
                    <Redirect from={BASE_URL_PATH} to={SINGLE_WORD_URL_PATH} />
                    <Route path="/" component={App}>
                        <Route path={SINGLE_WORD_URL_PATH} component={SingleWordPrediction} />
                        <Route path={ALL_WORD_URL_PATH} component={AllNounsPrediction} />
                    </Route>
                </Router>
            </Provider>
        </MuiThemeProvider>
    </IntlProvider>,
  document.getElementById('root')
);
