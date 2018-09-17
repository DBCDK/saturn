/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import ReactDOM from "react-dom";

import {BaseHarvesterConfigEdit, FormEntry} from "./BaseHarvesterConfigEdit";
import {BaseHarvesterConfig} from "../model/BaseHarvesterConfig";
import FtpHarvesterConfig from "../model/FtpHarvesterConfig";
import constants from "../constants";
import {getStringValue} from "../utils";

const FTP_PORT = "21";

const HOST_HELP =
    <div>
        <div className='help-title'>Den FTP adresse hvorfra filerne skal hentes</div>
        <div className='help-text'>Eksempler:</div>
        <div className='help-indent'>rs7.loc.gov</div>
        <div className='help-indent'>ftp.iacnet.com</div>
    </div>

const PORT_HELP =
    <div>
        <div className='help-title'>Den port, der skal benyttes, hvis den afviger fra standard-fpt-porten</div>
        <div className='help-text'>Forhåndsudfyldes med standardportnummeret</div>
    </div>

const USERNAME_HELP =
    <div>
        <div className='help-title'>Det navn eller den projektbetegnelse, der benyttes som login til sitet</div>
    </div>

const PASSWORD_HELP =
    <div>
        <div className='help-title'>Password der knytter sig til Brugerlogin</div>
    </div>

const DIR_HELP =
    <div>
        <div className='help-title'>Det (under)katalog som de ønskede filer skal hentes fra.</div>
        <div className='help-text'>Hvis feltet er tomt hentes direkte den første mappe på ftp-serveren.</div>
        <div className='help-text'>Eksempel:</div>
        <div className='help-indent'>/emds/maps</div>
    </div>

const FILES_PATTERN_HELP =
    <div>
        <div className='help-title'>Angivelse af kendetegn for de filer, der skal hentes fra kataloget</div>
        <div className='help-text'>Eksempel:</div>
        <div className='help-indent'>http://sa-cdn.clioonline.dk/tx_cliobibex/biblioteksguiden.xml</div>
    </div>



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
            .deleteFtpHarvesterConfig, id).end().then(() => {
                const a = document.createElement("a");
                a.href = "#/ftp";
                ReactDOM.findDOMNode(this).appendChild(a);
                a.click();
            }).catch(err => alert(err));
    }
    componentWillMount() {
        this.fetchConfig(this.props.match.params.id);
    }
    render() {
        return (
            <BaseHarvesterConfigEdit config={getStringValue(this.state.config)}
                    onSave={this.onSave}
                    onDelete={this.onDelete}
                    onConfigChanged={this.onConfigChanged}
                    title={"FTP Høster"}>
                <FormEntry label="FTP adresse" name="host" value={getStringValue(this.state.config.host)} help={HOST_HELP}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Port" name="port" help={PORT_HELP}
                           value={this.state.config.port !== undefined ? this.state.config.port.toString() : FTP_PORT}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Brugerlogin" name="username" value={getStringValue(this.state.config.username)} help={USERNAME_HELP}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Password" name="password" value={getStringValue(this.state.config.password)} help={PASSWORD_HELP}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Dir" name="dir" value={getStringValue(this.state.config.dir)} help={DIR_HELP}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Filnavnsstruktur" name="filesPattern" value={getStringValue(this.state.config.filesPattern)} help={FILES_PATTERN_HELP}
                           onChangeCallback={this.onChangeCallback}/>
            </BaseHarvesterConfigEdit>
        )
    }
}

export default FtpHarvesterConfigEdit;
