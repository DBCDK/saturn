/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import React from "react";
import {HashRouter, Route, Switch} from "react-router-dom";

import App from "./App";

class AppRoutes extends React.Component {
    render() {
        return (
            /* use HashRouter as a workaround to handle routing in a payara server.
             * otherwise the server gives 404 if pages other than the root is
             * reloaded or accessed directly.
             * this staskoverflow question has the same problem:
             * https://stackoverflow.com/questions/39532073/reactjs-how-to-configure-browserhistory-of-react-router-on-glassfish-server
             * it is not recommended to use HashRouter for projects not
             * targetting legacy browsers. BrowserRouter should be used instead.
             */
            <HashRouter>
                <App/>
            </HashRouter>
        );
    }
}

export default AppRoutes;
