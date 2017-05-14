import React from 'react'
import RefreshIndicator from 'material-ui/RefreshIndicator'

const LoadingIndicator = () =>
    <div style={{display: "flex", height: 50, justifyContent: "center", margin: '0 auto'}}>
        <RefreshIndicator
            size={40}
            left={10}
            top={0}
            status="loading"
            style={{
                display: 'inline-block',
                position: 'relative',
                margin: 2
            }}
        />
    </div>;

export default LoadingIndicator