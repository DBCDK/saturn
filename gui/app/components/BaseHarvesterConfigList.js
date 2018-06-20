/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";

import HttpHarvesterConfig from "../model/HttpHarvesterConfig";

class ConfigEntry extends React.Component {
    constructor(props) {
        super(props);
    }
    render() {
        return (
            <tr>
                <td><Link to={`configs/${this.props.id}/edit/`}>{this.props.name}</Link></td>
            </tr>
        )
    }
}

ConfigEntry.propTypes = {
    name: PropTypes.string.isRequired
};

class BaseHarvesterConfigList extends React.Component {
    render() {
        return (
            <div>
                <input type="button" value="new"
                    onClick={this.props.onNewClicked}/>
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
    onNewClicked: PropTypes.func,
};

BaseHarvesterConfigList.defaultProps = {
    onNewClicked: (event) => console.log(
        "no-op for BaseHarvesterConfigList.onNewClicked"),
};

export {BaseHarvesterConfigList,ConfigEntry};
