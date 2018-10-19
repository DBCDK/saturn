/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import ReactDOM from "react-dom";

import {BaseHarvesterConfigEdit, FormEntry} from "./BaseHarvesterConfigEdit";
import {BaseHarvesterConfig} from "../model/BaseHarvesterConfig";
import HttpHarvesterConfig from "../model/HttpHarvesterConfig";
import constants from "../constants";
import {getStringValue} from "../utils";

const URL_HELP =
    <div>
        <div className='help-title'>Den URL der skal benyttes mod sitet</div>
        <div className='help-text'>Eksempel:</div>
        <div className='help-indent'>http://sa-cdn.clioonline.dk/tx_cliobibex/biblioteksguiden.xml</div>
    </div>

const URL_PATTERN_HELP =
    <div>
        <div className='help-title'>URL Struktur</div>
    </div>


class HttpHarvesterConfigEdit extends React.Component {
    constructor(props) {
        super(props);
        this.state = {"config": {}};
        this.fetchConfig = this.fetchConfig.bind(this);
        this.onSave = this.onSave.bind(this);
        this.onDelete = this.onDelete.bind(this);
        this.onConfigChanged = this.onConfigChanged.bind(this);
        this.onChangeCallback = this.onChangeCallback.bind(this);
    }
    fetchConfig(id) {
        HttpHarvesterConfig.fetchConfig(id).end().then(response => {
            const config = JSON.parse(response.text);
            this.setState({config});
        });
    }
    onConfigChanged(config) {
        this.setState({config});
    }
    onChangeCallback(name, value) {
        const config = this.state.config;
        config[name] = value;
        this.setState({config});
    }
    onSave(form) {
        const config = new HttpHarvesterConfig();
        // if id is undefined, a new entity will be created in the database
        // by the backend
        config.id = this.state.config.id;
        for(let i = 0; i < form.length; i++) {
            if(form[i] === undefined) continue;
            switch(form[i].name) {
            case "url":
                config.url = form[i].value;
                break;
            case "urlPattern":
                config.urlPattern = form[i].value;
                break;
            case "name":
                config.name = form[i].value;
                break;
            case "schedule":
                config.schedule = form[i].value;
                break;
            case "transfile":
                config.transfile = form[i].value;
                break;
            case "seqno":
                config.seqno = form[i].value;
                break;
            case "seqnoExtract":
                config.seqnoExtract = form[i].value;
                break;
            case "agency":
                config.agency = form[i].value;
                break;
            case "enabled":
                config.enabled = form[i].checked;
                break;
            default:
                break;
            }
        }
        HttpHarvesterConfig.addHttpHarvesterConfig(config).end().then(() => {
                const a = document.createElement("a");
                a.href = "#/http";
                ReactDOM.findDOMNode(this).appendChild(a);
                a.click();
            }).catch(err => console.error("unexpected error when adding config", config, err));
    }
    onDelete(id) {
        BaseHarvesterConfig.deleteConfig(constants.endpoints
            .deleteHttpHarvesterConfig, id).end().then(() => {
                const a = document.createElement("a");
                a.href = "#/http";
                ReactDOM.findDOMNode(this).appendChild(a);
                a.click();
            }).catch(err => alert(err));
    }
    componentWillMount() {
        if (this.props.match.params.id !== undefined) {
            this.fetchConfig(this.props.match.params.id);
        }
    }
    render() {
        return (
            <BaseHarvesterConfigEdit config={this.state.config}
                    onSave={this.onSave}
                    onDelete={this.onDelete}
                    onConfigChanged={this.onConfigChanged}
                    title={"HTTP Høster"}>
                <FormEntry label="URL" name="url" value={getStringValue(this.state.config.url)} help={URL_HELP}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="URL struktur" name="urlPattern" value={getStringValue(this.state.config.urlPattern)} help={URL_PATTERN_HELP}
                           onChangeCallback={this.onChangeCallback}/>
            </BaseHarvesterConfigEdit>
        )
    }
}

export default HttpHarvesterConfigEdit;
