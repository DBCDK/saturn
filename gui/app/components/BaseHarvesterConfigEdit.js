/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import PropTypes from "prop-types";

import {BaseHarvesterConfig} from "../model/BaseHarvesterConfig";

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
        Promise.all(Object.keys(this.props.config).map(key => {
            if(this.props.config.hasOwnProperty(key)) {
                return this.validate(key, this.props.config[key]);
            }
        })).then(this.props.onSave(event.target.form))
            .catch(err => alert(err));
    }
    onDelete(event) {
        event.preventDefault();
        this.props.onDelete(this.props.config.id);
    }
    onChangeCallback(name, value) {
        // we could use spread syntax here
        const config = this.props.config;
        config[name] = value;
        this.props.onConfigChanged(config);
    }
    validate(name, value) {
        switch(name) {
        case "transfile":
            if(value === undefined || value === null || value.length === 0) {
                return Promise.reject("transfile cannot have empty content");
            } else if(value.indexOf("f=") !== -1) {
                return Promise.reject("transfile cannot contain f=");
            }
            break;
        case "schedule":
            if(value === undefined || value === null || value.length === 0) {
                return Promise.reject("schedule cannot have empty content");
            } else {
                return BaseHarvesterConfig.validateScheduleExpression(value)
                    .end().catch(_ => Promise.reject(
                    `invalid schedule value "${value}"`));
            }
            break;
        }
        return Promise.resolve();
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
                <FormEntry name="agency" value={config.agency}
                    onChangeCallback={this.onChangeCallback}/>
                {this.props.children}
                <button type="submit" onClick={this.onClick}>save</button>
                {this.props.config.id !== undefined ?
                    <button type="submit" onClick={this.onDelete}>delete</button>
                    : <div/> }
            </form>
        )
    }
}

BaseHarvesterConfigEdit.propTypes = {
    config: PropTypes.object,
    onSave: PropTypes.func,
    onDelete: PropTypes.func,
    onConfigChanged: PropTypes.func.isRequired
};

BaseHarvesterConfigEdit.defaultProps = {
    config: {},
    onSave: (event) => console.log("no-op for BaseHarvesterConfigEdit.onSave"),
    onDelete: event => console.log("no-op for BaseHarvesterConfigEdit.onDelete"),
};

export {BaseHarvesterConfigEdit, FormEntry};
