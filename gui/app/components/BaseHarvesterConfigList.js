/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import PropTypes from "prop-types";

import HttpHarvesterConfig from "../model/HttpHarvesterConfig";

import {mapResponseToConfigList} from "../model/BaseHarvesterConfig";

class ConfigEntry extends React.Component {
    constructor(props) {
        super(props);
    }
    render() {
        return (
            <tr>
                <td>{this.props.url}</td>
            </tr>
        )
    }
}

ConfigEntry.propTypes = {
    url: PropTypes.string.isRequired
};

class BaseHarvesterConfigList extends React.Component {
    constructor(props) {
        super(props);
        this.state = {"configs": []};
    }
    componentWillMount() {
        HttpHarvesterConfig.listHttpHarvesterConfigs().end().then(
                response => {
            const configs = mapResponseToConfigList(
                HttpHarvesterConfig, response.text);
            this.setState({configs});
        });
    }
    render() {
        return (
            <table>
                <tbody>
                    {this.state.configs.map(item => <ConfigEntry key={item.id} url={item.url}/>)}
                </tbody>
            </table>
        )
    }
}

export default BaseHarvesterConfigList;
