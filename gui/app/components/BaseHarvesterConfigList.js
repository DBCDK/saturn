/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";


class ConfigEntry extends React.Component {
    constructor(props) {
        super(props);
        this.state = { enabled: this.props.enabled };
        this.handleCheckboxChange = this.handleCheckboxChange.bind(this);
    }

    handleCheckboxChange(event) {
        const active = event.target.checked;
        this.setState({ enabled: active });
        this.props.onEnabledChanged(this.props.id, active);
    }

    render() {
        return (
            <tr className={this.props.running ? "running" : ""}>
                <td><Link to={this.props.url}>{this.props.name}</Link></td>
                <td>{this.props.lastHarvested}</td>
                <td className="center">
                    <input name="enabled" type="checkbox"
                           checked={this.state.enabled}
                           onChange={this.handleCheckboxChange}/>
                </td>
                <td>{this.props.progress}</td>
            </tr>
        )
    }
}

ConfigEntry.propTypes = {
    name: PropTypes.string.isRequired,
    url: PropTypes.string.isRequired,
    enabled: PropTypes.bool,
    lastHarvested: PropTypes.string,
    onEnabledChanged: PropTypes.func,
    progress: PropTypes.string,
    running: PropTypes.bool,
};

ConfigEntry.defaultProps = {
    onEnabledChanged: (event) => console.log("no-op for ConfigEntry.onEnabledChanged"),
    enabled: true,
};

class BaseHarvesterConfigList extends React.Component {
    render() {
        return (
            <div>
                <div className="breadcrumb-title">Saturnhøster >> {this.props.title}</div>
                <Link to={this.props.newConfigPath}><button className="action-button">{this.props.button}</button></Link>
                <table className="harvester-table">
                    <thead>
                        <tr>
                            <th>Navn</th>
                            <th>Sidst høstet</th>
                            <th>Aktiv</th>
                            <th>%</th>
                        </tr>
                    </thead>
                    <tbody>
                        {this.props.children}
                    </tbody>
                </table>
            </div>
        )
    }
}

BaseHarvesterConfigList.propTypes = {
    newConfigPath: PropTypes.string.isRequired,
};

export {BaseHarvesterConfigList,ConfigEntry};
