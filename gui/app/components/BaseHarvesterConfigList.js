/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";

import HttpHarvesterConfig from "../model/HttpHarvesterConfig";
import constants from "../constants";

class ConfigEntry extends React.Component {
    constructor(props) {
        super(props);
    }
    render() {
        return (
            <tr>
                <td><Link to={this.props.url}>{this.props.name}</Link></td>
            </tr>
        )
    }
}

ConfigEntry.propTypes = {
    name: PropTypes.string.isRequired,
    url: PropTypes.string.isRequired,
};

class BaseHarvesterConfigList extends React.Component {
    render() {
        return (
            <div>
                <Link to={this.props.newConfigPath}><button>new</button></Link>
                <table>
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
