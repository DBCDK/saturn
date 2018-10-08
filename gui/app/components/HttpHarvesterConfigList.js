/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";

import constants from "../constants";
import HttpHarvesterConfig from "../model/HttpHarvesterConfig";
import Path from "../Path";
import {BaseHarvesterConfigList,ConfigEntry} from "./BaseHarvesterConfigList";

import {mapResponseToConfigList} from "../model/BaseHarvesterConfig";

class HttpHarvesterConfigList extends React.Component {
    constructor(props) {
        super(props);
        this.state = {"configs": []};
        this.onEnabledChanged = this.onEnabledChanged.bind(this);
    }
    componentWillMount() {
        HttpHarvesterConfig.listHttpHarvesterConfigs().end().then(
                response => {
            const configs = mapResponseToConfigList(
                HttpHarvesterConfig, response.text);
            this.setState({configs});
        });
    }
    onEnabledChanged(id, active) {
        let config = {};
        for (let i=0; i<this.state.configs.length; i++) {
            if (this.state.configs[i].id == id) {
                config = this.state.configs[i];
                break;
            }
        }
        if (config == {}) {
            console.error(`Actual HTTP Config was not found for id=${id}`);
        } else {
            config.enabled = active;
        }
        HttpHarvesterConfig.addHttpHarvesterConfig(config).end()
            .catch(err => console.error(`unexpected error when changing enabled flag in HTTP config for id=${id}`,
                config, err));
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
                    return <ConfigEntry key={item.id} id={item.id}
                                        name={item.name} url={path.path}
                                        enabled={item.enabled}
                                        onEnabledChanged={this.onEnabledChanged}/>;
                    })
                }
            </BaseHarvesterConfigList>
        )
    }
}

export default HttpHarvesterConfigList;
