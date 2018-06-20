/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import PropTypes from "prop-types";
import {Link} from "react-router-dom";

import HttpHarvesterConfig from "../model/HttpHarvesterConfig";

import {mapResponseToConfigList} from "../model/BaseHarvesterConfig";

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
            <div>
                <input type="button" value="new"
                    onClick={this.props.onNewClicked}/>
                <table>
                    <tbody>
                        {this.state.configs.map(item => <ConfigEntry key={item.id} id={item.id} name={item.name}/>)}
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

export default BaseHarvesterConfigList;
