/**
 * Created by fide on 05.12.16.
 */

import React from 'react';
import {Chip}  from 'material-ui';

const styles = {
    chip: {
        margin: 2
    },
    container: {
        display: 'flex',
        flexDirection: 'row',
        alignItems: 'center'
    },
    title : {
        flex: "0 0 120px"
    },
    wrapper: {
        display: 'flex',
        flexWrap: 'wrap',
    },
};

const MoreChip = ({num}) => <Chip key="more-chip" style={styles.chip}>{num} more not shown</Chip>;

const ChipList = ({totalNum, labels, color}) => {
    let chipSeqNum = 0;
    let numChips = labels.length;

    const StyledChip = ({label}) =>
        <Chip
            style={styles.chip}
            backgroundColor={color}
        >
            {label}
        </Chip>;

    return (<span style={styles.wrapper}>
        {labels.map((label) => <StyledChip key={chipSeqNum++} label={label} />)}
        {(numChips < totalNum) ? <MoreChip num={totalNum - numChips}/> : <span key="span" />}
    </span>);
};

const weightedFeatureToString = ({label, weight}) => `${label}: ${weight.toPrecision(2)}`;



export const FeatureChipList = ({totalNum, features, color, onOpenDetails}) => {
    let chipSeqNum = 0;
    let numChips = features.length;
    const FeatureChip = ({label, weight}) =>
        <Chip
            style={styles.chip}
            backgroundColor={color}
            onTouchTap={onOpenDetails ? () => onOpenDetails(label) : null}
        >
            {weightedFeatureToString({label, weight})}
        </Chip>;
    return (
        <span style={styles.wrapper}>
            {features.map(({label, weight}) => <FeatureChip key={chipSeqNum++} label={label} weight={weight} />)}
            {(numChips < totalNum) ? <MoreChip num={totalNum - numChips}/> : <span key="span" />}
        </span>
    );
};

export default ChipList;