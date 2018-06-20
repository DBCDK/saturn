/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import {Route, Switch} from "react-router-dom";

import BaseHarvesterConfigList from "./BaseHarvesterConfigList";
import HttpHarvesterConfigEdit from "./HttpHarvesterConfigEdit";
import NotFound from "./NotFound";

const Main = () => (
    <div id="main">
        <Switch>
            <Route exact path="/" component={BaseHarvesterConfigList}/>
            <Route exact path="/configs/:id/edit/" component={HttpHarvesterConfigEdit}/>
            <Route path="*" component={NotFound}/>
        </Switch>
    </div>
)

export default Main;