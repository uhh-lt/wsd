import React from 'react'

import {Card, CardActions, CardText, CardTitle} from 'material-ui/Card';
import RaisedButton from 'material-ui/RaisedButton';
import ModelNameDropDownMenu from './ModelNameDropDownMenu'

import { FormsyText } from 'formsy-material-ui/lib';
import FormsyToggle from 'formsy-material-ui/lib/FormsyToggle';
import Formsy from 'formsy-react';

const title = "Input a sentence to disambiguate";
const subtitle = "The system will identify nouns and named entities and assign senses which best fit the given sentence.";
const style = {
    card: {
        marginTop: 4,
    },
};

const SingleWordFormCard = React.createClass({

    getInitialState() {
        return {
            canSubmit: false,
        };
    },

    enableButton() {
        this.setState({
            canSubmit: true,
        });
    },

    disableButton() {
        this.setState({
            canSubmit: false,
        });
    },

    notifyFormError(data) {
        console.error('Form error:', data);
    },

    render() {
        const {context, modelName, onValidSubmit, onRandomSample} = this.props;
        return (
            <Card style={style.card}>
                <CardTitle title={title} subtitle={subtitle}/>
                <CardText>
                    <Formsy.Form ref="form"
                        onValidSubmit={onValidSubmit}
                        onInvalidSubmit={this.notifyFormError}
                        onValid={this.enableButton}
                        onInvalid={this.disableButton}
                    >
                        <FormsyText
                            name="context"
                            required
                            updateImmediately
                            defaultValue={context}
                            floatingLabelText="Sentence"
                            floatingLabelFixed={true}
                            fullWidth={true}
                        />
                        <ModelNameDropDownMenu
                            required
                            value={modelName}
                            floatingLabelText="Model"
                            floatingLabelFixed={true}
                            fullWidth={true}
                        />
                    </Formsy.Form>
                </CardText>
                <CardActions>
                    <RaisedButton
                        label="Disambiguate sentence"
                        primary={true}
                        onTouchTap={() => this.refs.form.submit()}
                        disabled={!this.state.canSubmit}
                    />
                    <RaisedButton label="Random sample" onTouchTap={onRandomSample}/>

                </CardActions>
            </Card>
        )
    }
});

export default SingleWordFormCard