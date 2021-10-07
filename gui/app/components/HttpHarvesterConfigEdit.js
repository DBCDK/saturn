/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import ReactDOM from "react-dom";

import {BaseHarvesterConfigEdit, FormEntry, FormSelect} from "./BaseHarvesterConfigEdit";
import {BaseHarvesterConfig} from "../model/BaseHarvesterConfig";
import HttpHarvesterConfig from "../model/HttpHarvesterConfig";
import constants from "../constants";
import {getHttpHeadersAsText, getHttpHeadersTable, getStringValue} from "../utils";

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

const HTTP_HEADERS_HELP =
    <div>
        <div className='help-title'>Http headers</div>
        <div className='help-text'>Her kan sættes en række http headers der er nødvendige for at kunne
        nå igennem til http serveren. Det kan fx være ift authentication. Feltet efterlades tomt, hvis det ikke er nødvendigt med ekstra
        headers. Eksempel:</div>
        <div className='help-indent'>
            &#91;&#123;"Authorization"&#58; "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="&#125;,
            &#123;"My header"&#58; "My header value"&#125;&#93;
        </div>

    </div>

const LIST_FILES_HANDLER_HELP =
    <div>
        <div className='help-title'>Høstermetodik</div>
        <div className='help-text'>Der findes pt. to måder hvorpå HTTP høstninger kan håndteres:</div>
        <div className='help-indent'>STANDARD</div>
        <div className='help-indent'>LITTERATURSIDEN</div>
        <div className='help-text'>LITTERATURSIDEN er en specialisering som ene og alene håndterer den API som udstilles
            af litteratursiden.dk
        </div>
    </div>

const LIST_FILES_HANDLER_OPTIONS = [
    {
        label: "LITTERATURSIDEN",
        value: "LITTERATURSIDEN",
    },
    {
        label: "STANDARD",
        value: "STANDARD",
    }
]

class HttpHarvesterConfigEdit extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            "config": {}, "testResult": [],
            isWaitingForRemoteResponse: false,  // will be true when API request is in progress
            remoteResponseLabel: "",
        };
        this.fetchConfig = this.fetchConfig.bind(this);
        this.onSave = this.onSave.bind(this);
        this.onDelete = this.onDelete.bind(this);
        this.onTest = this.onTest.bind(this);
        this.testConfig = this.testConfig.bind(this);
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
        config.lastHarvested = this.state.config.lastHarvested;
        for (let i = 0; i < form.length; i++) {
            if (form[i] === undefined) continue;
            switch (form[i].name) {
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
                case "agency":
                    config.agency = form[i].value;
                    break;
                case "enabled":
                    config.enabled = form[i].checked;
                    break;
                case "listFilesHandler":
                    config.listFilesHandler = form[i].value;
                    break;
                case "httpHeaders":
                    JSON.parse(form[i].value);
                    const headerArray =  getHttpHeadersTable(form[i].value);
                    config.httpHeaders = headerArray;
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

    onTest(form) {
        // clear any previous test result before new test
        this.setState({
            testResult: [], isWaitingForRemoteResponse: true,
            remoteResponseLabel: "testing complete"
        }, () => this.testConfig());
    }

    testConfig() {
        HttpHarvesterConfig.testConfig(this.state.config.id).end().then(response => {
            const testResult = JSON.parse(response.text);
            const isWaitingForRemoteResponse = false;
            this.setState({testResult, isWaitingForRemoteResponse});
        });
    }

    componentWillMount() {
        if (this.props.match.params.id !== undefined) {
            this.fetchConfig(this.props.match.params.id);
        }
    }

    render() {
        return (
            <BaseHarvesterConfigEdit config={this.state.config}
                                     testResult={this.state.testResult}
                                     isWaitingForRemoteResponse={this.state.isWaitingForRemoteResponse}
                                     remoteResponseLabel={this.state.remoteResponseLabel}
                                     onSave={this.onSave}
                                     onDelete={this.onDelete}
                                     onTest={this.onTest}
                                     onConfigChanged={this.onConfigChanged}
                                     title={"HTTP Høster"}>
                <FormSelect label="Type" name="listFilesHandler"
                            value={this.state.config.listFilesHandler || "STANDARD"}
                            onChangeCallback={this.onChangeCallback} options={LIST_FILES_HANDLER_OPTIONS}
                            help={LIST_FILES_HANDLER_HELP}/>
                <FormEntry label="URL" name="url" value={getStringValue(this.state.config.url)} help={URL_HELP}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="URL struktur" name="urlPattern" value={getStringValue(this.state.config.urlPattern)}
                           help={URL_PATTERN_HELP}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="HTTP headers" name="httpHeaders" value={getHttpHeadersAsText(this.state.config.httpHeaders)}
                           help={HTTP_HEADERS_HELP}
                           onChangeCallback={this.onChangeCallback}/>
            </BaseHarvesterConfigEdit>
        )
    }
}

export default HttpHarvesterConfigEdit;
