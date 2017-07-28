import React from 'react'
import Dialog from 'material-ui/Dialog'
import LoadingIndicator from './LoadingIndicator'
import FeatureDetailTable from './FeatureDetailTable'

import FlatButton from 'material-ui/FlatButton';

const FeatureDetailDialog = ({open, onRequestClose, details, isFetching, senseID, feature, isSuperSense}) =>
    <Dialog modal={false} open={open}  onRequestClose={onRequestClose} title={
        <div>Cluster Words of the {(isSuperSense) ? "Super Sense": "Word Sense" } <i>{senseID}</i> that contributed to the feature "<i>{feature}</i>"</div>
    } actions={<FlatButton
        label="Close"
        primary={true}
        onTouchTap={onRequestClose}
    />}>
        {isFetching
            ? <LoadingIndicator />
            : <FeatureDetailTable {...details} />
        }
    </Dialog>;

export default FeatureDetailDialog;
