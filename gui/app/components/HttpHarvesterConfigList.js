/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import PropTypes from "prop-types";

import HttpHarvesterConfig from "../model/HttpHarvesterConfig";
import {BaseHarvesterConfigList,ConfigEntry} from "./BaseHarvesterConfigList";

import {mapResponseToConfigList} from "../model/BaseHarvesterConfig";

class HttpHarvesterConfigList extends React.Component {
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
            <BaseHarvesterConfigList>
                {this.state.configs.map(item => <ConfigEntry key={item.id}
                    id={item.id} name={item.name}/>)}
            </BaseHarvesterConfigList>
        )
    }
}

export default HttpHarvesterConfigList;
