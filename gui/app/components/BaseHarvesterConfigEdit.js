/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import PropTypes from "prop-types";

class FormEntry extends React.Component {
    constructor(props) {
        super(props);
        this.onChange = this.onChange.bind(this);
    }
    // all these callbacks are used to propagate changes up to the
    // originating component - i supspect it can be done simpler
    onChange({target}) {
        this.props.onChangeCallback(this.props.name, target.value);
    }
    render() {
        const {name} = this.props;
        return (
            <div className="form-group">
                <label htmlFor={name}>{name}</label>
                <input type="text" name={name} value={this.props.value}
                    onChange={this.onChange}/>
            </div>
        )
    }
};

FormEntry.propTypes = {
    value: PropTypes.string,
    onChangeCallback: PropTypes.func.isRequired,
};

FormEntry.defaultProps = {
    value: "",
};

class BaseHarvesterConfigEdit extends React.Component {
    constructor(props) {
        super(props);
        this.onClick = this.onClick.bind(this);
        this.onChangeCallback = this.onChangeCallback.bind(this);
    }
    onClick(event) {
        event.preventDefault();
        this.props.onSave(event.target.form);
    }
    onChangeCallback(name, value) {
        // we could use spread syntax here
        const config = this.props.config;
        config[name] = value;
        this.props.onConfigChanged(config);
    }
    render() {
        const config = this.props.config;
        return (
            <form id="upload-form">
                <FormEntry name="name" value={config.name}
                    onChangeCallback={this.onChangeCallback}/>
                <FormEntry name="schedule" value={config.schedule}
                    onChangeCallback={this.onChangeCallback}/>
                <FormEntry name="transfile" value={config.transfile}
                    onChangeCallback={this.onChangeCallback}/>
                <FormEntry name="seqno" value={config.seqno || "0"}
                    onChangeCallback={this.onChangeCallback}/>
                <FormEntry name="seqnoExtract" value={config.seqnoExtract || ""}
                    onChangeCallback={this.onChangeCallback}/>
                {this.props.children}
                <button type="submit" onClick={this.onClick}>save</button>
            </form>
        )
    }
}

BaseHarvesterConfigEdit.propTypes = {
    config: PropTypes.object,
    onSave: PropTypes.func,
    onConfigChanged: PropTypes.func.isRequired
};

BaseHarvesterConfigEdit.defaultProps = {
    config: {},
    onSave: (event) => console.log("no-op for BaseHarvesterConfigEdit.onSave"),
};

export {BaseHarvesterConfigEdit, FormEntry};
