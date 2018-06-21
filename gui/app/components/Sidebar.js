/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import {Link} from "react-router-dom";

import constants from "../constants";

const Sidebar = () => (
    <div className="wrapper">
        <nav id="sidebar">
            <h1>saturn</h1>
            <ul>
                <li><Link to={constants.paths.httpConfigList}>
                    http harvester configs
                </Link></li>
                <li><Link to={constants.paths.ftpConfigList}>
                    ftp harvester configs
                </Link></li>
            </ul>
        </nav>
    </div>
)

export default Sidebar;
