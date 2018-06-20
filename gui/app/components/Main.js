/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import {Route, Switch} from "react-router-dom";

import BaseHarvesterConfigList from "./BaseHarvesterConfigList";
import HttpHarvesterConfigEdit from "./HttpHarvesterConfigEdit";
import HttpHarvesterConfigList from "./HttpHarvesterConfigList";
import FtpHarvesterConfigEdit from "./FtpHarvesterConfigEdit";
import FtpHarvesterConfigList from "./FtpHarvesterConfigList";
import NotFound from "./NotFound";
import constants from "../constants";

const Main = () => (
    <div id="main">
        <Switch>
            <Route exact path="/" component={BaseHarvesterConfigList}/>
            <Route exact path={constants.paths.httpConfigList}
                component={HttpHarvesterConfigList}/>
            <Route exact path={constants.paths.ftpConfigList}
                component={FtpHarvesterConfigList}/>
            <Route exact path={constants.paths.editHttpHarvesterConfig}
                component={HttpHarvesterConfigEdit}/>
            <Route exact path={constants.paths.editFtpHarvesterConfig}
                component={FtpHarvesterConfigEdit}/>
            <Route path="*" component={NotFound}/>
        </Switch>
    </div>
)

export default Main;
