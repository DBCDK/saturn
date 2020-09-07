/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import ReactDOM from "react-dom";

import {BaseHarvesterConfigEdit, FormEntry} from "./BaseHarvesterConfigEdit";
import {BaseHarvesterConfig} from "../model/BaseHarvesterConfig";
import SFtpHarvesterConfig from "../model/SFtpHarvesterConfig";
import constants from "../constants";
import {getStringValue} from "../utils";

const SFTP_PORT = "22";

const SEQNO_HELP =
    <div>
        <div className='help-title'>Nummer på den sidst hentede fil. Forudfyldt med 0, så den første fil hentes. Hvis du ønsker at hente en fil igen, skal løbenummer sættes til nummeret før den fil, du vil have hentet.</div>
        <div className='help-text'>Eksempler:</div>
        <div className='help-indent'>20180901</div>
        <div className='help-indent'>4623</div>
        <br/>
        <div className='help-text'>Filer på f.eks. en sftp server:</div>
        <div className='help-indent'>v46.i23.records.utf8</div>
        <div className='help-indent'>v46.i24.records.utf8</div>
        <div className='help-indent'>v46.i25.records.utf8</div>
        <br/>
        <div className='help-text'>Løbenumre er her 4623, 4624 og 4625.</div>
        <div className='help-indent'>De bliver taget ud via 'løbenummerdel' 2-3,6-7. Det svarer til tegn nr. 2 og 3 plus tegn nr. 6 og 7.</div>
    </div>

const SEQNO_EXTRACT_HELP =
    <div>
        <div className='help-title'>Løbenummerdel</div>
        <div className='help-text'>Delelement af filnavn til brug for løbenummer. Dvs. de tegn i filnavnet som udgør løbenummeret.</div>
        <div className='help-text'>Eksempler:</div>
        <div className='help-indent'>2-3,6-7</div>
        <div className='help-indent'>17-24</div>
        <br/>
        <div className='help-text'>For at angive løbenummerdelen ’4623’ på denne fil:</div>
        <div className='help-indent'>v46.i23.records.utf8</div>
        <div className='help-text'>skal man hente tegn 2-3 og tegn 6-7, så løbenummerdelen bliver 2-3,6-7</div>
    </div>

const HOST_HELP =
    <div>
        <div className='help-title'>Den FTP adresse hvorfra filerne skal hentes</div>
        <div className='help-text'>Eksempler:</div>
        <div className='help-indent'>rs7.loc.gov</div>
        <div className='help-indent'>sftp.iacnet.com</div>
    </div>

const PORT_HELP =
    <div>
        <div className='help-title'>Den port, der skal benyttes, hvis den afviger fra standard-sftp-porten</div>
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
        <div className='help-text'>Hvis feltet er tomt hentes direkte den første mappe på sftp-serveren.</div>
        <div className='help-text'>Eksempel:</div>
        <div className='help-indent'>/maps</div>
    </div>

const FILES_PATTERN_HELP =
    <div>
        <div className='help-title'>Angivelse af kendetegn for de filer, der skal hentes fra kataloget</div>
        <div className='help-text'>Eksempler:</div>
        <div className='help-indent'>NREF-BIORC-FULL_*</div>
        <div className='help-indent'>*records.utf8</div>
    </div>



class SFtpHarvesterConfigEdit extends React.Component {
    constructor(props) {
        super(props);
        this.state = {"config": {}, "testResult": [],
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
        SFtpHarvesterConfig.fetchConfig(id).end().then(response => {
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
        const config = new SFtpHarvesterConfig();
        // if id is undefined, a new entity will be created in the database
        // by the backend
        config.id = this.state.config.id;
        config.lastHarvested = this.state.config.lastHarvested;
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
            case "enabled":
                config.enabled = form[i].checked;
                break;
            default:
                break;
            }
        }
        SFtpHarvesterConfig.addSFtpHarvesterConfig(config).end().then(() => {
                const a = document.createElement("a");
                a.href = "#/sftp";
                ReactDOM.findDOMNode(this).appendChild(a);
                a.click();
            }).catch(err => console.error("unexpected error when adding config", config, err));
    }
    onDelete(id) {
        BaseHarvesterConfig.deleteConfig(constants.endpoints
            .deleteSFtpHarvesterConfig, id).end().then(() => {
                const a = document.createElement("a");
                a.href = "#/sftp";
                ReactDOM.findDOMNode(this).appendChild(a);
                a.click();
            }).catch(err => alert(err));
    }
    onTest(form) {
        // clear any previous test result before new test
        this.setState({testResult: [], isWaitingForRemoteResponse: true,
            remoteResponseLabel: "testing complete"}, () => this.testConfig());
    }
    testConfig() {
        SFtpHarvesterConfig.testConfig(this.state.config.id).end().then(response => {
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
            <BaseHarvesterConfigEdit config={getStringValue(this.state.config)}
                    testResult={this.state.testResult}
                    isWaitingForRemoteResponse={this.state.isWaitingForRemoteResponse}
                    remoteResponseLabel={this.state.remoteResponseLabel}
                    onSave={this.onSave}
                    onDelete={this.onDelete}
                    onTest={this.onTest}
                    onConfigChanged={this.onConfigChanged}
                    title={"SFTP Høster"}>
                <FormEntry label="Løbenummer" name="seqno" help={SEQNO_HELP}
                           value={this.state.config.seqno !== undefined ? this.state.config.seqno.toString() : "0"}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Løbenummerdel" name="seqnoExtract" value={getStringValue(this.state.config.seqnoExtract)} help={SEQNO_EXTRACT_HELP}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="SFTP adresse" name="host" value={getStringValue(this.state.config.host)} help={HOST_HELP}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Port" name="port" help={PORT_HELP}
                           value={this.state.config.port !== undefined ? this.state.config.port.toString() : SFTP_PORT}
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

export default SFtpHarvesterConfigEdit;
