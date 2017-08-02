import React from 'react'
import Dialog from 'material-ui/Dialog'
import LoadingIndicator from './LoadingIndicator'
import FeatureDetailTable from './FeatureDetailTable'

import FlatButton from 'material-ui/FlatButton';

const FeatureDetailDialog = ({open, onRequestClose, details, isFetching, senseID, feature, isSuperSense}) =>
    <Dialog modal={false} open={open}  onRequestClose={onRequestClose} title={
        <div>Cluster words of the {(isSuperSense) ? "super sense": "word sense" } <b>{senseID}</b> that contributed to the feature <b>{feature}</b></div>
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
