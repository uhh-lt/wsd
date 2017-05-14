import React from 'react'
import { FormsySelect } from 'formsy-material-ui/lib';

import MenuItem from 'material-ui/MenuItem';

const ModelNameDropDownMenu = (props) =>
    <FormsySelect {...props} name="modelName">
        <MenuItem value={'ensemble'} primaryText="Ensemble" />
        <MenuItem value={'cos_traditional_coocwords'} primaryText="Context Features" />
        <MenuItem value={'cos_traditional_self'} primaryText="Cluster Features" />
        <MenuItem value={'cos_hybridwords'} primaryText="Cluster and Context Features" />
        <MenuItem value={'cos_cosets1k_coocwords'} primaryText="CoSet with Context Features" />
        <MenuItem value={'cos_cosets1k_self'} primaryText="CoSet with Cluster Features" />
        <MenuItem value={'naivebayes_cosets1k_self'} primaryText="CoSet with Cluster Features (Naive Bayes)" />
        <MenuItem value={'naivebayes_cosets1k_coocwords'} primaryText="CoSet with Context Features (Naive Bayes)" />
        <MenuItem value={'depslm'} primaryText="Dependencies + LM" />
    </FormsySelect>;
export default ModelNameDropDownMenu;