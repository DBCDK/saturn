/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";

import {BaseHarvesterConfigEdit, FormEntry} from "./BaseHarvesterConfigEdit";
import {BaseHarvesterConfig} from "../model/BaseHarvesterConfig";
import FtpHarvesterConfig from "../model/FtpHarvesterConfig";
import constants from "../constants";
import {getStringValue} from "../utils";

const FTP_PORT = "21";

class FtpHarvesterConfigEdit extends React.Component {
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
        FtpHarvesterConfig.fetchConfig(id).end().then(response => {
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
        const config = new FtpHarvesterConfig();
        // if id is undefined, a new entity will be created in the database
        // by the backend
        config.id = this.state.config.id;
        for(let i = 0; i < form.length; i++) {
            if(form[i] === undefined) continue;
            switch(form[i].name) {
            case "host":
                config.host = form[i].value;
                break;
            case "port":
                config.port = Number.parseInt(form[i].value);
                break;
            case "username":
                config.username = form[i].value;
                break;
            case "password":
                config.password = form[i].value;
                break;
            case "dir":
                config.dir = form[i].value;
                break;
            case "filesPattern":
                config.filesPattern = form[i].value;
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
            default:
                break;
            }
        }
        FtpHarvesterConfig.addFtpHarvesterConfig(config).end()
            .catch(err => console.error("unexpected error when adding config",
            config, err));
    }
    onDelete(id) {
        BaseHarvesterConfig.deleteConfig(constants.endpoints
            .deleteFtpHarvesterConfig, id).end().catch(err => alert(err));
    }
    componentWillMount() {
        this.fetchConfig(this.props.match.params.id);
    }
    render() {
        return (
            <BaseHarvesterConfigEdit config={getStringValue(this.state.config)}
                    onSave={this.onSave}
                    onDelete={this.onDelete}
                    onConfigChanged={this.onConfigChanged}>
                <FormEntry name="host" value={getStringValue(this.state.config.host)}
                    onChangeCallback={this.onChangeCallback}/>
                <FormEntry name="port" value={
                    this.state.config.port !== undefined ?
                        this.state.config.port.toString() : FTP_PORT}
                    onChangeCallback={this.onChangeCallback}/>
                <FormEntry name="username" value={getStringValue(this.state.config.username)}
                    onChangeCallback={this.onChangeCallback}/>
                <FormEntry name="password" value={getStringValue(this.state.config.password)}
                    onChangeCallback={this.onChangeCallback}/>
                <FormEntry name="dir" value={getStringValue(this.state.config.dir)}
                    onChangeCallback={this.onChangeCallback}/>
                <FormEntry name="filesPattern" value={getStringValue(this.state.config.filesPattern)}
                    onChangeCallback={this.onChangeCallback}/>
            </BaseHarvesterConfigEdit>
        )
    }
}

export default FtpHarvesterConfigEdit;
