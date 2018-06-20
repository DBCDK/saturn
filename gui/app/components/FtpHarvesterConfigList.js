/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import PropTypes from "prop-types";

import constants from "../constants";
import FtpHarvesterConfig from "../model/FtpHarvesterConfig";
import Path from "../Path";
import {BaseHarvesterConfigList,ConfigEntry} from "./BaseHarvesterConfigList";

import {mapResponseToConfigList} from "../model/BaseHarvesterConfig";

class FtpHarvesterConfigList extends React.Component {
    constructor(props) {
        super(props);
        this.state = {"configs": []};
    }
    componentWillMount() {
        FtpHarvesterConfig.listFtpHarvesterConfigs().end().then(
                response => {
            const configs = mapResponseToConfigList(
                FtpHarvesterConfig, response.text);
            this.setState({configs});
        });
    }
    render() {
        return (
            <BaseHarvesterConfigList>
                {this.state.configs.map(item => {
                    const path = new Path(
                        constants.paths.editFtpHarvesterConfig);
                    path.bind("id", item.id);
                    return <ConfigEntry key={item.id}
                        id={item.id} name={item.name} url={path.path}/>;
                    })
                }
            </BaseHarvesterConfigList>
        )
    }
}

export default FtpHarvesterConfigList;
