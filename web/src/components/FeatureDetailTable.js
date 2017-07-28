import React from 'react'
import {Table, TableBody, TableHeader, TableHeaderColumn, TableRow, TableRowColumn} from 'material-ui/Table';

const FeatureDetailTable = ({clusterFeatures}) => (
    <Table>
        <TableHeader displaySelectAll={false} adjustForCheckbox={false}>
            <TableRow>
                <TableHeaderColumn>Cluster Word</TableHeaderColumn>
                <TableHeaderColumn>Contribution</TableHeaderColumn>
            </TableRow>
        </TableHeader>
        <TableBody displayRowCheckbox={false}>{
            clusterFeatures.map(f => {
                return (
                    <TableRow key={f.clusterWord}>
                        <TableRowColumn>{f.clusterWord}</TableRowColumn>
                        <TableRowColumn>{(f.weightContribution*100).toFixed(2)} %</TableRowColumn>
                    </TableRow>
                );
            })
        }
        </TableBody>
    </Table>
);

export default FeatureDetailTable;