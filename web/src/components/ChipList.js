/**
 * Created by fide on 05.12.16.
 */

import React from 'react';
import {Chip}  from 'material-ui';
import {grey500} from 'material-ui/styles/colors';

const styles = {
    chip: {
        margin: 2
    },
    container: {
        display: 'flex',
        flexDirection: 'row',
        alignItems: 'center'
    },
    wrapper: {
        display: 'flex',
        flexWrap: 'wrap',
    },
    title : {
        flex: "0 0 120px"
    },
    subtitle : {
        paddingTop: "5px",
        color: grey500
    }
};

const MoreChip = ({num}) => <Chip key="more-chip" style={styles.chip}>{num} more not shown</Chip>;

const ChipListWrapper = ({title, subtitle, totalNum, numChips, children}) => {
    return (
        <div style={styles.container}>
            <div style={styles.title}>
                {title}
                <div style={styles.subtitle}>
                    {subtitle}
                </div>
            </div>
            <div style={styles.wrapper}>{children}
            {(numChips < totalNum) ? <MoreChip num={totalNum - numChips}/> : <span />}
            </div>
        </div>
    );
};

const ChipList = ({title, subtitle, totalNum, labels, color}) => {
    let chipSeqNum = 0;
    return (
        <ChipListWrapper title={title} subtitle={subtitle} totalNum={totalNum} numChips={labels.length}>{
            labels.map((text) =>
                <Chip
                    key={chipSeqNum++}
                    style={styles.chip}
                    backgroundColor={color}
                >{text}</Chip>
            )
        }</ChipListWrapper>
    );
};

const weightedFeatureToString = ({label, weight}) => `${label}: ${weight.toPrecision(2)}`;

export const FeatureChipList = ({title, subtitle, totalNum, features, color, onOpenDetails}) => {
    let chipSeqNum = 0;
    return (
        <ChipListWrapper title={title} subtitle={subtitle} totalNum={totalNum} numChips={features.length}>{
            features.map(({label, weight}) =>
                <Chip
                    key={chipSeqNum++}
                    style={styles.chip}
                    backgroundColor={color}
                    onTouchTap={onOpenDetails ? () => onOpenDetails(label) : null}
                >{weightedFeatureToString({label, weight})}</Chip>
            )
        }</ChipListWrapper>
    );
};

export default ChipList;