/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";

import {BaseHarvesterConfigEdit, FormEntry} from "./BaseHarvesterConfigEdit";
import {BaseHarvesterConfig} from "../model/BaseHarvesterConfig";
import FtpHarvesterConfig from "../model/FtpHarvesterConfig";
import constants from "../constants";

const hostHelp =
    "<div class='help-title'>Den FTP adresse hvorfra filerne skal hentes</div>" +
    "<div class='help-text'>Eksempler:</div>" +
    "<div class='help-indent'>rs7.loc.gov</div>" +
    "<div class='help-indent'>ftp.iacnet.com</div>";
const portHelp =
    "<div class='help-title'>Den port, der skal benyttes, hvis den afviger fra standard-fpt-porten</div>" +
    "<div class='help-text'>Forhåndsudfyldes med standardportnummeret</div>";
const usernameHelp =
    "<div class='help-title'>Det navn eller den projektbetegnelse, der benyttes som login til sitet</div>";
const passwordHelp =
    "<div class='help-title'>Password der knytter sig til Brugerlogin</div>";
const dirHelp =
    "<div class='help-title'>Det (under)katalog som de ønskede filer skal hentes fra.</div>" +
    "<div class='help-text'>Hvis feltet er tomt hentes direkte den første mappe på ftp-serveren.</div>" +
    "<div class='help-text'>Eksempel:</div>" +
    "<div class='help-indent'>/emds/maps</div>";
const filesPatternHelp =
    "<div class='help-title'>Angivelse af kendetegn for de filer, der skal hentes fra kataloget</div>" +
    "<div class='help-text'>Eksempel:</div>" +
    "<div class='help-indent'>http://sa-cdn.clioonline.dk/tx_cliobibex/biblioteksguiden.xml</div>";


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
            <BaseHarvesterConfigEdit config={this.state.config}
                    onSave={this.onSave}
                    onDelete={this.onDelete}
                    onConfigChanged={this.onConfigChanged}
                    title={"FTP Høster"}>
                <FormEntry label="FTP adresse" name="host" value={this.state.config.host} help={hostHelp}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Port" name="port" help={portHelp}
                           value={this.state.config.port !== undefined ? this.state.config.port.toString() : "0"}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Brugerlogin" name="username" value={this.state.config.username} help={usernameHelp}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Password" name="password" value={this.state.config.password} help={passwordHelp}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Dir" name="dir" value={this.state.config.dir} help={dirHelp}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Filnavnsstruktur" name="filesPattern" value={this.state.config.filesPattern} help={filesPatternHelp}
                           onChangeCallback={this.onChangeCallback}/>
            </BaseHarvesterConfigEdit>
        )
    }
}

export default FtpHarvesterConfigEdit;
