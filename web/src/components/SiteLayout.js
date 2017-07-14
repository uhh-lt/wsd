import React from 'react'
import AppBar from 'material-ui/AppBar';
import spacing from 'material-ui/styles/spacing';
import {darkWhite, lightWhite, grey900} from 'material-ui/styles/colors';
import FullWidthSection from './FullWidthSection';
import {List, ListItem, makeSelectable} from 'material-ui/List';
import Subheader from 'material-ui/Subheader';
import Drawer from 'material-ui/Drawer';

import Checkbox from 'material-ui/Checkbox';

import Toggle from 'material-ui/Toggle';

import Divider from 'material-ui/Divider';
import ArrowDropRight from 'material-ui/svg-icons/navigation-arrow-drop-right';
import {ALL_WORD_URL_PATH} from "../apppaths";
import {SINGLE_WORD_URL_PATH} from "../apppaths";

const SelectableList = makeSelectable(List);

const styles = {
    appBar: {
        position: 'fixed',
        top: 0,
    },
    root: {
        paddingTop: spacing.desktopKeylineIncrement,
        paddingRight: 200,
        minHeight: 500
    },
    content: {
        margin: spacing.desktopGutter,
    },
    contentWhenMedium: {
        margin: `${spacing.desktopGutter * 2}px ${spacing.desktopGutter * 3}px`,
    },
    footer: {
        backgroundColor: grey900,
        textAlign: 'center',
        paddingRight: 200
    },
    a: {
        color: darkWhite,
    },
    p: {
        margin: '0 auto',
        padding: 0,
        color: lightWhite
    },
    browserstack: {
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: 'center',
        margin: '25px 15px 0',
        padding: 0,
        color: lightWhite,
        lineHeight: '25px',
        fontSize: 12,
    },
    browserstackLogo: {
        margin: '0 3px',
    },
    iconButton: {
        color: darkWhite,
    },
};


const SiteLayout = ({handleChangeList, handleToggleShowImages, showImages, children}) => (
    <div>
        <AppBar style={styles.appBar}
                title="Unsupervised & Knowledge-Free & Interpretable Word Sense Disambiguation"
                showMenuIconButton={false}
        />
        <div style={styles.root}>
            <div style={styles.content}>
                {children}
            </div>
        </div>
        <Drawer width={200} openSecondary={true}  style={styles.navDrawer}>
            <AppBar
                showMenuIconButton={false}
            />
            <SelectableList
                value={location.pathname}
                onChange={handleChangeList}
            >
                <Subheader>Disambiguation Target</Subheader>
                <ListItem
                    primaryText="Single Word"
                    value={SINGLE_WORD_URL_PATH}
                />
                <ListItem
                    primaryText="All Words"
                    value={ALL_WORD_URL_PATH}
                />

            </SelectableList>
            <Divider />
            <SelectableList
                value=""
                onChange={(event, value) => { window.location = value; }}
            >
                <Subheader>Resources</Subheader>
                <ListItem primaryText="Original Paper" value="TODO"/>
                <ListItem primaryText="Source Code" value="https://github.com/uhh-lt/wsd" />
                <ListItem primaryText="About" value="http://jobimtext.org/wsd" />
            </SelectableList>
        </Drawer>

        <FullWidthSection style={styles.footer}>
            <p style={styles.p}>
                {'Created by the '}
                <a style={styles.a} href="https://www.inf.uni-hamburg.de/en/inst/ab/lt/">
                    Language Technology Group
                </a>
                {' at the University of Hamburg.'}
            </p>
        </FullWidthSection>
    </div>
);

export default SiteLayout;
