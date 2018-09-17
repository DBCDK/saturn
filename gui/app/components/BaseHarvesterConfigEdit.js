/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import PropTypes from "prop-types";
import ReactTooltip from 'react-tooltip';

import {BaseHarvesterConfig} from "../model/BaseHarvesterConfig";

const NAME_HELP =
    <div>
        <div className='help-title'>Høsterens navn</div>
    </div>

const CRONTAB_HELP =
    <div>
        <div className='help-title'>Hentningsfrekvens</div>
        <div className='help-text'>Består af fem sæt af tal (eller stjerne som default):</div>
        <div className='help-indent'>* * * * *</div>
        <div className='help-text'>De fem sæt står for</div>
        <div className='help-indent'>'minut' 'time' 'nr dag i måneden' 'månedens nr' 'ugedagsnr'</div>
        <div className='help-text'>Eksempler:</div>
        <div className='help-indent'>1 4 26 * *</div>
        <div className='help-text'>Betyder 1 minut over 4 (om morgenen) på den 26. dag i måneden</div>
        <div className='help-indent'>10 16 * * *</div>
        <div className='help-text'>Betyder kl. 16.10 hver dag</div>
        <div className='help-indent'>* * * * 7</div>
        <div className='help-text'>Betyder hvert minut om søndagen</div>
        <div className='help-indent'>52 1 * * 1</div>
        <div className='help-text'>Betyder kl. 1.52 hver mandag</div>
        <div className='help-text'>Få evt. hjælp til cron på crontab.guru</div>
    </div>

const TRANSFILE_HELP =
    <div>
        <div className='help-title'>Transfilnavn</div>
        <div className='help-text'>Eksempler:</div>
        <div className='help-indent'>b=ticklerepo,c=utf8,t=xml,o=fysikkemi,m=kildepost@dbc.dk</div>
        <div className='help-indent'>b=marckonv,c=utf8,o=m21,t=iso,m=kildepost@dbc.dk</div>
    </div>

const SEQNO_HELP =
    <div>
        <div className='help-title'>Nummer på den sidst hentede fil. Forudfyldt med 0, så den første fil hentes. Hvis du ønsker at hente en fil igen, skal løbenummer sættes til nummeret før den fil, du vil have hentet.</div>
        <div className='help-text'>Eksempler:</div>
        <div className='help-indent'>20180901</div>
        <div className='help-indent'>4623</div>
        <br/>
        <div className='help-text'>Filer på f.eks. en ftp server:</div>
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

const AGENCY_HELP =
    <div>
        <div className='help-title'>Præfiks til filnavn indeholdende som minimum et biblioteksnummer/submitter + punktum</div>
        <div className='help-text'>Eksempler:</div>
        <div className='help-indent'>150067.</div>
        <div className='help-indent'>150023.BIOCON_FULL</div>
    </div>



class FormEntry extends React.Component {
    constructor(props) {
        super(props);
        this.onChange = this.onChange.bind(this);
    }
    // all these callbacks are used to propagate changes up to the
    // originating component - i suspect it can be done simpler
    onChange({target}) {
        this.props.onChangeCallback(this.props.name, target.value);
    }
    render() {
        const {name} = this.props;
        return (
            <div className="form-group">
                <label htmlFor={name}>{this.props.label}</label>
                <input type="text" name={name} value={this.props.value}
                    onChange={this.onChange}
                    data-tip data-for={name}
                    data-event="focus" data-event-off="blur"/>
                <ReactTooltip id={name} type="info" place="right" effect="solid">{this.props.help}</ReactTooltip>
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
        this.onDelete = this.onDelete.bind(this);
        this.onChangeCallback = this.onChangeCallback.bind(this);
    }
    onClick(event) {
        event.preventDefault();
        event.persist();
        Promise.all(Object.keys(this.props.config).map(key => {
            if(this.props.config.hasOwnProperty(key)) {
                return this.validate(key, this.props.config[key]);
            }
        })).then(() => {this.props.onSave(event.target.form)})
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
        case "port":
            if(value === undefined || value === null || value.length === 0) {
                return Promise.reject("port cannot have empty content");
            // Number.parseInt ignores trailing non-numeric characters,
            // so pattern matching is also needed for validation
            } else if(!/^[0-9]+$/.test(value) || isNaN(Number.parseInt(value))){
                return Promise.reject(`port has illegal value "${value}"`);
            }
            break;
        }
        return Promise.resolve();
    }
    render() {
        const config = this.props.config;
        return (
            <form id="upload-form">
                <div className="breadcrumb-title">Saturnhøster >> {this.props.config.id !== undefined ? "Rediger" : "Ny"} {this.props.title}</div>
                <FormEntry label="Navn" name="name" value={config.name} help={NAME_HELP}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Hentningsfrekvens" name="schedule" value={config.schedule} help={CRONTAB_HELP}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Transfil" name="transfile" value={config.transfile} help={TRANSFILE_HELP}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Løbenummer" name="seqno" value={config.seqno || "0"} help={SEQNO_HELP}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Løbenummerdel" name="seqnoExtract" value={config.seqnoExtract || ""} help={SEQNO_EXTRACT_HELP}
                           onChangeCallback={this.onChangeCallback}/>
                <FormEntry label="Biblioteksnummer+præfiks" name="agency" value={config.agency} help={AGENCY_HELP}
                           onChangeCallback={this.onChangeCallback}/>
                {this.props.children}
                <button type="submit" className="save-button" onClick={this.onClick}>Gem</button>
                {this.props.config.id !== undefined ?
                    <button type="submit" className="delete-button" onClick={this.onDelete}>Slet</button>
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
