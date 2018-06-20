/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";

import {BaseHarvesterConfigEdit, FormEntry} from "./BaseHarvesterConfigEdit";
import HttpHarvesterConfig from "../model/HttpHarvesterConfig";

class HttpHarvesterConfigEdit extends React.Component {
    constructor(props) {
        super(props);
        this.state = {"config": {}};
        this.fetchConfig = this.fetchConfig.bind(this);
        this.onSave = this.onSave.bind(this);
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
            case "name":
                config.name = form[i].value;
                break;
            case "schedule":
                config.schedule = form[i].value;
                break;
            case "transfile":
                config.transfile = form[i].value;
                break;
            default:
                break;
            }
        }
        HttpHarvesterConfig.addHttpHarvesterConfig(config).end()
            .catch(err => console.error("unexpected error when adding config",
            config, err));
    }
    componentWillMount() {
        this.fetchConfig(this.props.match.params.id);
    }
    render() {
        return (
            <BaseHarvesterConfigEdit config={this.state.config}
                    onSave={this.onSave}
                    onConfigChanged={this.onConfigChanged}>
                <FormEntry name="url" value={this.state.config.url}
                    onChangeCallback={this.onChangeCallback}/>
            </BaseHarvesterConfigEdit>
        )
    }
}

export default HttpHarvesterConfigEdit;
