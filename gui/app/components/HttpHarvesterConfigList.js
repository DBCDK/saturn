/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import PropTypes from "prop-types";

import constants from "../constants";
import HttpHarvesterConfig from "../model/HttpHarvesterConfig";
import Path from "../Path";
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
            <BaseHarvesterConfigList
                    newConfigPath={constants.paths.newHttpHarvesterConfig}
                    title="Hentninger via HTTP" button="Ny HTTP høster">
                {this.state.configs.map(item => {
                    const path = new Path(
                        constants.paths.editHttpHarvesterConfig);
                    path.bind("id", item.id);
                    return <ConfigEntry key={item.id}
                        id={item.id} name={item.name} url={path.path}/>;
                    })
                }
            </BaseHarvesterConfigList>
        )
    }
}

export default HttpHarvesterConfigList;
