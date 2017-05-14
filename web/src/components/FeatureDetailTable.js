import React from 'react'
import {Table, TableBody, TableHeader, TableHeaderColumn, TableRow, TableRowColumn} from 'material-ui/Table';

const FeatureDetailTable = ({clusterFeatures}) => (
    <Table>
        <TableHeader displaySelectAll={false} adjustForCheckbox={false}>
            <TableRow>
                <TableHeaderColumn>Cluster Word</TableHeaderColumn>
                <TableHeaderColumn>Cluster Word Weight</TableHeaderColumn>
                <TableHeaderColumn>Feature Weight</TableHeaderColumn>
            </TableRow>
        </TableHeader>
        <TableBody displayRowCheckbox={false}>{
            clusterFeatures.map(f => {
                return (
                    <TableRow key={f.clusterWord}>
                        <TableRowColumn>{f.clusterWord}</TableRowColumn>
                        <TableRowColumn>{f.clusterWordWeight}</TableRowColumn>
                        <TableRowColumn>{f.featureWeight}</TableRowColumn>
                    </TableRow>
                );
            })
        }
        </TableBody>
    </Table>
);

export default FeatureDetailTable;