/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";

import constants from "../constants";
import FtpHarvesterConfig from "../model/FtpHarvesterConfig";
import Path from "../Path";
import {BaseHarvesterConfigList, ConfigEntry} from "./BaseHarvesterConfigList";

import {mapResponseToConfigList} from "../model/BaseHarvesterConfig";
import {formatDate} from "../utils";

class FtpHarvesterConfigList extends React.Component {
    constructor(props) {
        super(props);
        this.state = {"configs": []};
        this.onEnabledChanged = this.onEnabledChanged.bind(this);
    }
    componentWillMount() {
        FtpHarvesterConfig.listFtpHarvesterConfigs().end().then(
                response => {
            const configs = mapResponseToConfigList(
                FtpHarvesterConfig, response.text);
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
            console.error(`Actual FTP Config was not found for id=${id}`);
        } else {
            config.enabled = active;
        }
        FtpHarvesterConfig.addFtpHarvesterConfig(config).end()
            .catch(err => console.error(`unexpected error when changing enabled flag in FTP config for id=${id}`,
                config, err));
    }
    handleAbort(id) {
        FtpHarvesterConfig.abort(id).end()
            .then(response => window.location.reload())
            .catch(err => console.error(`Unable to abort job with config id=${id}`, err));
    }

    render() {
        return (
            <BaseHarvesterConfigList
                    newConfigPath={constants.paths.newFtpHarvesterConfig}
                    title="Hentninger via FTP" button="Ny FTP høster">
                {this.state.configs.
                    sort((a,b) => a.name.localeCompare(b.name)).
                    map(item => {
                        const path = new Path(
                            constants.paths.editFtpHarvesterConfig);
                        path.bind("id", item.id);
                        return <ConfigEntry key={item.id} id={item.id}
                                            name={item.name} url={path.path}
                                            enabled={item.enabled}
                                            lastHarvested={item.lastHarvested==null?"Endnu ikke høstet":formatDate(new Date(item.lastHarvested))}
                                            onEnabledChanged={this.onEnabledChanged}
                                            progress={item.progress}
                                            handleAbort={this.handleAbort}
                                            running={item.running}/>;
                        }
                    )
                }
            </BaseHarvesterConfigList>
        )
    }
}

export default FtpHarvesterConfigList;
