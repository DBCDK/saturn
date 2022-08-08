/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";

import constants from "../constants";
import Path from "../Path";
import {BaseHarvesterConfigList, ConfigEntry} from "./BaseHarvesterConfigList";

import {mapResponseToConfigList} from "../model/BaseHarvesterConfig";
import SFtpHarvesterConfig from "../model/SFtpHarvesterConfig";
import {formatDate} from "../utils";

class SFtpHarvesterConfigList extends React.Component {
    constructor(props) {
        super(props);
        this.state = {"configs": []};
        this.onEnabledChanged = this.onEnabledChanged.bind(this);
    }
    componentWillMount() {
        SFtpHarvesterConfig.listSFtpHarvesterConfigs().end().then(
                response => {
            const configs = mapResponseToConfigList(
                SFtpHarvesterConfig, response.text);
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
            console.error(`Actual SFTP Config was not found for id=${id}`);
        } else {
            config.enabled = active;
        }
        SFtpHarvesterConfig.addSFtpHarvesterConfig(config).end()
            .catch(err => console.error(`unexpected error when changing enabled flag in FTP config for id=${id}`,
                config, err));
    }
    render() {
        return (
            <BaseHarvesterConfigList
                    newConfigPath={constants.paths.newSFtpHarvesterConfig}
                    title="Hentninger via SFTP" button="Ny SFTP høster">
                {this.state.configs.
                    sort((a,b) => a.name.localeCompare(b.name)).
                    map(item => {
                        const path = new Path(
                            constants.paths.editSFtpHarvesterConfig);
                        path.bind("id", item.id);
                        return <ConfigEntry key={item.id} id={item.id}
                                            name={item.name} url={path.path}
                                            enabled={item.enabled}
                                            lastHarvested={item.lastHarvested==null?"Endnu ikke høstet":formatDate(new Date(item.lastHarvested))}
                                            onEnabledChanged={this.onEnabledChanged}
                                            progress={item.progress}
                                            running={item.running}/>;
                        }
                    )
                }
            </BaseHarvesterConfigList>
        )
    }
}

export default SFtpHarvesterConfigList;
