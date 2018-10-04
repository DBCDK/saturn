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
            <tr>
                <td><Link to={this.props.url}>{this.props.name}</Link></td>
                <td className="center">
                    <input name="enabled" type="checkbox"
                           checked={this.state.enabled}
                           onChange={this.handleCheckboxChange}/>
                </td>
            </tr>
        )
    }
}

ConfigEntry.propTypes = {
    name: PropTypes.string.isRequired,
    url: PropTypes.string.isRequired,
    enabled: PropTypes.bool,
    onEnabledChanged: PropTypes.func,
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
                            <th>Aktiv</th>
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
