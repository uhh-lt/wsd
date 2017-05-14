import React from 'react'
import Dialog from 'material-ui/Dialog'
import LoadingIndicator from './LoadingIndicator'
import FeatureDetailTable from './FeatureDetailTable'

import FlatButton from 'material-ui/FlatButton';

const FeatureDetailDialog = ({open, onRequestClose, details, isFetching, senseID, feature}) =>
    <Dialog modal={false} open={open}  onRequestClose={onRequestClose} title={
        `Details for feature '${feature}' of sense '${senseID}'`
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
